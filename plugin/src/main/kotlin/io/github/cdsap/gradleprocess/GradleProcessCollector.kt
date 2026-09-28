package io.github.cdsap.gradleprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess

internal class GradleProcessCollector {
    fun collect(
        jStat: String,
        jInfo: String,
        typeProcess: TypeProcess
    ): List<Process> {
        return ConsolidateProcesses().consolidate(jStat, jInfo, typeProcess)
    }
}
