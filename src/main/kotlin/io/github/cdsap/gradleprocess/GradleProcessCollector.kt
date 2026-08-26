package io.github.cdsap.gradleprocess

import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.api.provider.Provider

internal class GradleProcessCollector {
    fun collect(
        jStat: Provider<String>,
        jInfo: Provider<String>,
        typeProcess: TypeProcess
    ): List<Process> {
        return ConsolidateProcesses().consolidate(jStat.get(), jInfo.get(), typeProcess)
    }
}
