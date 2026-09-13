package io.github.cdsap.gradleprocess

import io.github.cdsap.gradleprocess.Constants.Companion.GRADLE_PROCESS_NAME
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.support.serviceOf

class InfoGradleProcessPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        target.rootProject.gradle.rootProject {
            // Develocity is normally applied as a settings plugin and registers a
            // "develocity" extension on the root project. Detect that extension —
            // not Class.forName — so a transitive Develocity jar without the plugin
            // applied cannot skip both the scan path and the console fallback.
            // project.pluginManager.withPlugin("com.gradle.develocity") never runs
            // for settings-applied Develocity.
            if (shouldUseDevelocityReporting(target)) {
                DevelocityWrapperConfiguration().configureProjectWithDevelocity(target)
            } else {
                consoleReporting(target)
            }
        }
    }


    private fun consoleReporting(project: Project) {
        val service = project.gradle.sharedServices.registerIfAbsent(
            "gradleProcessService", InfoGradleProcessBuildService::class.java
        ) {
            parameters.jInfoProvider = project.jInfo(GRADLE_PROCESS_NAME)
            parameters.jStatProvider = project.jStat(GRADLE_PROCESS_NAME)
        }
        project.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(service)
    }

    companion object {
        internal const val DEVELOCITY_EXTENSION_NAME = "develocity"

        internal fun shouldUseDevelocityReporting(project: Project): Boolean =
            project.extensions.findByName(DEVELOCITY_EXTENSION_NAME) != null
    }
}
