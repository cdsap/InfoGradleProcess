package io.github.cdsap.gradleprocess.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.jdk.tools.parser.model.Process

import com.gradle.scan.plugin.BuildScanExtension

class EnterpriseValues(
    private val buildScanExtension: BuildScanExtension,
    private val processes: List<Process>,
) {

    fun addProcessesInfoToBuildScan() {
        processes.map {
            with(buildScanExtension) {
                value("Gradle-Process-${it.pid}-max", "${it.max} GB")
                value("Gradle-Process-${it.pid}-usage", "${it.usage} GB")
                value("Gradle-Process-${it.pid}-capacity", "${it.capacity} GB")
                value("Gradle-Process-${it.pid}-uptime", "${it.uptime} minutes")
                value("Gradle-Process-${it.pid}-gcTime", "${it.gcTime} minutes")
                value("Gradle-Process-${it.pid}-gcType", it.typeGc)
            }
        }
    }
}
