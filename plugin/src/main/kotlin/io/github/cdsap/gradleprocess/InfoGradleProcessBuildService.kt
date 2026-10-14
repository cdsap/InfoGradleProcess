package io.github.cdsap.gradleprocess

import io.github.cdsap.gradleprocess.output.ConsoleOutput
import io.github.cdsap.gradleprocess.output.GbosFileOutput
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
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
        val outputDirectory: DirectoryProperty
        val writeJson: Property<Boolean>
        val writeNdjson: Property<Boolean>
        val consoleEnabled: Property<Boolean>
    }

    private var buildFailed = false

    override fun close() {
        if (buildFailed) return
        val processes = collectProcesses(
            ProcessInfoProviders(
                jStat = parameters.jStatProvider,
                jInfo = parameters.jInfoProvider
            ).resolve()
        )
        if (parameters.consoleEnabled.get() && processes.isNotEmpty()) {
            ConsoleOutput(processes).print()
        }
        GbosFileOutput.write(
            outputDirectory = parameters.outputDirectory.get().asFile,
            processes = processes,
            writeJson = parameters.writeJson.get(),
            writeNdjson = parameters.writeNdjson.get(),
        )
    }

    override fun onFinish(event: FinishEvent?) {
        if ((event?.result as? org.gradle.tooling.events.FailureResult)?.failures?.isNotEmpty() == true) {
            buildFailed = true
        }
    }

    internal companion object {
        fun collectProcesses(processInfo: ProcessInfo): List<Process> =
            GradleProcessCollector().collect(processInfo, TypeProcess.Gradle)

        fun collectProcesses(jStat: String, jInfo: String): List<Process> =
            collectProcesses(ProcessInfo(jStat, jInfo))
    }
}
