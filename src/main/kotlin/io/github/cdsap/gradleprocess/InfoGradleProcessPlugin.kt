package io.github.cdsap.gradleprocess

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.support.serviceOf

class InfoGradleProcessPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        target.rootProject.gradle.rootProject {
            val hasDevelocity = try {
                Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")
                true
            } catch (_: ClassNotFoundException) {
                false
            }


            if (hasDevelocity) {
                DevelocityWrapperConfiguration().configureProjectWithDevelocity(target)
            } else {
                consoleReporting(target)
            }
        }
    }


    private fun consoleReporting(project: Project) {
        val processInfoProviders = ProcessInfoProviders.create(project)
        val service = project.gradle.sharedServices.registerIfAbsent(
            "gradleProcessService", InfoGradleProcessBuildService::class.java
        ) {
            parameters.jInfoProvider = processInfoProviders.jInfo
            parameters.jStatProvider = processInfoProviders.jStat
        }
        project.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(service)
    }

}
