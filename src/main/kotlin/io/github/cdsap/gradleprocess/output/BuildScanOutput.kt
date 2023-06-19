package io.github.cdsap.gradleprocess.output

import com.gradle.scan.plugin.BuildScanExtension
import io.github.cdsap.jdk.tools.parser.model.Process

class BuildScanOutput(
    private val buildScanExtension: BuildScanExtension,
    private val processes: List<Process>,
) {

    fun addProcessesInfoToBuildScan() {
        processes.map {
            buildScanExtension.value(
                "Gradle-Process-${it.pid}-max",
                "${it.max} GB"
            )
            buildScanExtension.value(
                "Gradle-Process-${it.pid}-usage",
                "${it.usage} GB"
            )
            buildScanExtension.value(
                "Gradle-Process-${it.pid}-capacity",
                "${it.capacity} GB"
            )
            buildScanExtension.value(
                "Gradle-Process-${it.pid}-uptime",
                "${it.uptime} minutes"
            )
            buildScanExtension.value(
                "Gradle-Process-${it.pid}-gcTime",
                "${it.gcTime} minutes"
            )
            buildScanExtension.value(
                "Gradle-Process-${it.pid}-gcType",
                it.typeGc
            )
        }
    }
}
