package io.github.cdsap.gradleprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gradleprocess.output.DevelocityValues
import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class DevelocityWrapperConfiguration {

    fun configureProjectWithDevelocity(target: Project) {
        val extension = target.extensions.findByType(DevelocityWrapperConfiguration::class.java) != null
        if (extension) {
            buildScanDevelocityReporting(target, target.extensions.findByType(DevelocityConfiguration::class.java)!!)
            target.extensions.create("develocity", DevelocityWrapperConfiguration::class.java)
        }
    }

    private fun buildScanDevelocityReporting(
        project: Project,
        buildScanExtension: DevelocityConfiguration
    ) {
        val (jStat, jInfo) = providerPair(project)

        buildScanExtension.buildScan.buildFinished {
            val processes = processes(jStat, jInfo)
            DevelocityValues(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }

    private fun processes(
        jStat: Provider<String>,
        jInfo: Provider<String>
    ): List<Process> {
        val processes = ConsolidateProcesses().consolidate(jStat.get(), jInfo.get(), TypeProcess.Kotlin)
        return processes
    }

    private fun providerPair(project: Project): Pair<Provider<String>, Provider<String>> {
        val jStat = project.jStat(Constants.GRADLE_PROCESS_NAME)
        val jInfo = project.jInfo(Constants.GRADLE_PROCESS_NAME)
        return Pair(jStat, jInfo)
    }
}
