package io.github.cdsap.gradleprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gradleprocess.output.DevelocityValues
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.api.Project

class DevelocityWrapperConfiguration {

    fun configureProjectWithDevelocity(target: Project) {
        val extension = target.extensions.findByType(DevelocityConfiguration::class.java) != null
        if (extension) {
            buildScanDevelocityReporting(target, target.extensions.findByType(DevelocityConfiguration::class.java)!!)
        }
    }

    private fun buildScanDevelocityReporting(
        project: Project,
        buildScanExtension: DevelocityConfiguration
    ) {
        val processInfoProviders = ProcessInfoProviders.create(project)

        buildScanExtension.buildScan.buildFinished {
            val processes = ProcessInfoCollector().collect(
                processInfoProviders.jStat,
                processInfoProviders.jInfo,
                TypeProcess.Kotlin
            )
            DevelocityValues(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }
}
