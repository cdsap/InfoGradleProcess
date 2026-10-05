package io.github.cdsap.gradleprocess

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.support.serviceOf

/**
 * Shared process reporting wiring for settings and build-script application.
 * Uses [claimConfiguration] so applying more than one entry point configures once.
 *
 * Develocity types are only touched after a [Class.forName] check so consumers
 * without the Develocity plugin on the classpath do not fail class loading.
 */
internal object InfoGradleProcessReporting {
    private const val SERVICE_NAME = "gradleProcessService"

    fun configureFromSettings(settings: Settings) {
        val hasDevelocity = hasDevelocityClass()
        if (hasDevelocity) {
            DevelocityWrapperConfiguration().configureFromSettings(settings) {
                settings.gradle.rootProject {
                    configureConsole(this)
                }
            }
        } else {
            settings.gradle.rootProject {
                configureConsole(this)
            }
        }
    }

    fun configureFromProject(project: Project) {
        project.rootProject.gradle.rootProject {
            if (hasDevelocityClass() && shouldUseDevelocityReporting(this)) {
                DevelocityWrapperConfiguration().configureIfPresent(this)
            } else {
                configureConsole(this)
            }
        }
    }

    fun configureConsole(project: Project) {
        if (!claimConfiguration(project)) {
            return
        }
        val service = project.gradle.sharedServices.registerIfAbsent(
            SERVICE_NAME,
            InfoGradleProcessBuildService::class.java
        ) {
            val processInfoProviders = ProcessInfoProviders.create(project)
            parameters.jInfoProvider = processInfoProviders.jInfo
            parameters.jStatProvider = processInfoProviders.jStat
        }
        project.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(service)
    }

    /**
     * Returns true only for the first console or Develocity wiring in this build, so
     * applying several ids (settings + build script, legacy id + `.project`) reports once.
     */
    internal fun claimConfiguration(project: Project): Boolean {
        val extra = project.rootProject.extensions.extraProperties
        if (extra.has(CONFIGURED_MARKER)) {
            return false
        }
        extra.set(CONFIGURED_MARKER, true)
        return true
    }

    internal fun shouldUseDevelocityReporting(project: Project): Boolean =
        project.extensions.findByName(DEVELOCITY_EXTENSION_NAME) != null

    private fun hasDevelocityClass(): Boolean =
        try {
            Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")
            true
        } catch (_: ClassNotFoundException) {
            false
        }

    private const val DEVELOCITY_EXTENSION_NAME = "develocity"
    private const val CONFIGURED_MARKER = "io.github.cdsap.gradleprocess.configured"
}
