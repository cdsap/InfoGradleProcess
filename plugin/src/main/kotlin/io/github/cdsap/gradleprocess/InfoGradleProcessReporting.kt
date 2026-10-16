package io.github.cdsap.gradleprocess

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Provider
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
        configureExtension(project)
        configureOutputService(project, consoleEnabled = true)
    }

    fun configureOutputService(project: Project, consoleEnabled: Boolean) {
        val extension = configureExtension(project)
        val service = project.gradle.sharedServices.registerIfAbsent(
            SERVICE_NAME,
            InfoGradleProcessBuildService::class.java,
        ) {
            val processInfoProviders = ProcessInfoProviders.create(project)
            parameters.jInfoProvider = processInfoProviders.jInfo
            parameters.jStatProvider = processInfoProviders.jStat
            parameters.outputDirectory.set(project.layout.buildDirectory.dir("info-gradle-process"))
            parameters.writeJson.set(extension.gbos.enabled)
            parameters.writeNdjson.set(extension.gbos.enabled)
            parameters.consoleEnabled.set(consoleEnabled)
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

    internal fun gbosEnabled(project: Project): Provider<Boolean> =
        configureExtension(project).gbos.enabled

    private fun configureExtension(project: Project): InfoGradleProcessExtension {
        val extension =
            project.extensions.findByType(InfoGradleProcessExtension::class.java)
                ?: project.extensions.create(
                    "infoGradleProcess",
                    InfoGradleProcessExtension::class.java,
                )
        GbosOptIn.configureConventions(extension.gbos, project.providers)
        return extension
    }

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
