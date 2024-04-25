package io.github.cdsap.gradleprocess.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.jdk.tools.parser.model.Process
class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val processes: List<Process>,
) {

    fun addProcessesInfoToBuildScan() {
        processes.map {
            develocityConfiguration.buildScan {
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
