package io.github.cdsap.gradleprocess.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.jdk.tools.parser.model.Process

class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val processes: List<Process>,
    private val gbosEnabled: Boolean = false,
) {

    fun addProcessesInfoToBuildScan() {
        val customValues = DevelocityCustomValues.from(processes, gbosEnabled)
        if (customValues.isNotEmpty()) {
            develocityConfiguration.buildScan {
                customValues.forEach { (key, customValue) ->
                    value(key, customValue)
                }
            }
        }
    }

}

internal object DevelocityCustomValues {
    fun from(
        processes: List<Process>,
        gbosEnabled: Boolean,
    ): List<Pair<String, String>> =
        processes.flatMap(::legacyValues) +
            if (gbosEnabled) GbosDevelocityProjection.values(processes) else emptyList()

    private fun legacyValues(process: Process): List<Pair<String, String>> =
        listOf(
            "Gradle-Process-${process.pid}-max" to "${process.max} GB",
            "Gradle-Process-${process.pid}-usage" to "${process.usage} GB",
            "Gradle-Process-${process.pid}-capacity" to "${process.capacity} GB",
            "Gradle-Process-${process.pid}-uptime" to "${process.uptime} minutes",
            "Gradle-Process-${process.pid}-gcTime" to "${process.gcTime} minutes",
            "Gradle-Process-${process.pid}-gcType" to process.typeGc,
        )
}
