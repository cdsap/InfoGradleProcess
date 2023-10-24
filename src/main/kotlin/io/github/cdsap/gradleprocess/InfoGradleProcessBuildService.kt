package io.github.cdsap.gradleprocess

import io.github.cdsap.gradleprocess.output.ConsoleOutput
import io.github.cdsap.jdk.tools.parser.ConsolidateProcesses
import io.github.cdsap.jdk.tools.parser.JInfoData
import io.github.cdsap.jdk.tools.parser.JStatData
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class InfoGradleProcessBuildService :
    BuildService<InfoGradleProcessBuildService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        var jInfoProvider: Provider<String>
        var jStatProvider: Provider<String>
    }

    override fun close() {
        println("closing")
        val jsta = parameters.jStatProvider.get()
        val jinf = parameters.jInfoProvider.get()
        val processes =
            ConsolidateProcesses().consolidate(
                jsta, jinf,
                TypeProcess.Gradle
            )
        println(processes.isNotEmpty())
        if (processes.isNotEmpty()) {
            println("paso oir")
            ConsoleOutput(processes).print()
        } else {
            val jInfoData = JInfoData().process(jinf)
            val jStatData = JStatData().process(jsta)
            println(jInfoData.size)
            println(jStatData.size)
            jInfoData.forEach {
                println(it.key)
                println(it.value)
            }
            println("ccc")
            jStatData.forEach {
                println(it.key)
                println(it.value)
            }
            println(jsta)
            println(jinf)

        }
    }
}
