package io.github.cdsap.gradleprocess

import com.gradle.scan.plugin.BuildScanExtension
import io.github.cdsap.gradleprocess.output.BuildScanOutput
import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Plugin
import org.gradle.api.Project

class InfoGradleProcessPlugin : Plugin<Project> {
    private val nameProcess = "GradleDaemon"
    override fun apply(target: Project) {
        target.gradle.rootProject {
            val buildScanExtension = extensions.findByType(com.gradle.scan.plugin.BuildScanExtension::class.java)
            if (buildScanExtension != null) {
                buildScanReporting(project, buildScanExtension)
            } else {
                consoleReporting(target)
            }
        }
    }

    private fun consoleReporting(project: Project) {
        project.gradle.sharedServices.registerIfAbsent(
            "gradleProcessService", InfoGradleProcessBuildService::class.java
        ) {
            parameters.jInfoProvider = project.jInfo(nameProcess)
            parameters.jStatProvider = project.jStat(nameProcess)
        }.get()
    }

    private fun buildScanReporting(
        project: Project,
        buildScanExtension: BuildScanExtension
    ) {
        val jStat = project.jStat(nameProcess)
        val jInfo = project.jInfo(nameProcess)

        buildScanExtension.buildFinished {
            val processes = ConsolidateProcesses().consolidate(jStat.get(), jInfo.get(), TypeProcess.Kotlin)
            BuildScanOutput(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }
}
