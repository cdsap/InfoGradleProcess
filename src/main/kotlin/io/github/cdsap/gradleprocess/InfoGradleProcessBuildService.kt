package io.github.cdsap.gradleprocess

import io.github.cdsap.gradleprocess.output.ConsoleOutput
import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

abstract class InfoGradleProcessBuildService :
    BuildService<InfoGradleProcessBuildService.Params>, AutoCloseable,
    OperationCompletionListener {
    interface Params : BuildServiceParameters {
        var jInfoProvider: Provider<String>
        var jStatProvider: Provider<String>
    }

    override fun close() {
        val processes =
            ConsolidateProcesses().consolidate(
                parameters.jStatProvider.get(), parameters.jInfoProvider.get(),
                TypeProcess.Gradle
            )
        if (processes.isNotEmpty()) {
            ConsoleOutput(processes).print()
        }
    }

    override fun onFinish(event: FinishEvent?) {
    }
}
