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
        target.gradle.rootProject {
            val hasDevelocity = try {
                Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")
                true
            } catch (_: ClassNotFoundException) {
                false
            }
            println("has deelociu")
            println(hasDevelocity)

            if (hasDevelocity) {
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

}
