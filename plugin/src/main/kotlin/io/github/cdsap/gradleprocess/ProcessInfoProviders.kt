package io.github.cdsap.gradleprocess

import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal data class ProcessInfoProviders(
    val jStat: Provider<String>,
    val jInfo: Provider<String>
) {
    companion object {
        fun create(project: Project): ProcessInfoProviders {
            return ProcessInfoProviders(
                jStat = project.jStat(Constants.GRADLE_PROCESS_NAME),
                jInfo = project.jInfo(Constants.GRADLE_PROCESS_NAME)
            )
        }
    }
}
