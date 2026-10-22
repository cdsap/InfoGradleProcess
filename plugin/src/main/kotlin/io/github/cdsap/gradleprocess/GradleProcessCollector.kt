package io.github.cdsap.gradleprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess

internal class GradleProcessCollector {
    fun collectGradle(processInfo: ProcessInfo): List<Process> =
        collectGradle(processInfo.jStat, processInfo.jInfo)

    fun collectGradle(
        jStat: String,
        jInfo: String
    ): List<Process> {
        return ConsolidateProcesses().consolidate(jStat, jInfo, TypeProcess.Gradle)
    }
}
