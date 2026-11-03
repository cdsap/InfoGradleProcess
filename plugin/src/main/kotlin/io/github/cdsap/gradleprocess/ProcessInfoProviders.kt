package io.github.cdsap.gradleprocess

import io.github.cdsap.valuesourceprocess.execute
import io.github.cdsap.valuesourceprocess.jInfo
import io.github.cdsap.valuesourceprocess.jStat
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal data class ProcessInfoProviders(
    val jStat: Provider<String>,
    val jInfo: Provider<String>
) {
    fun resolve(): ProcessInfo = ProcessInfo(jStat.get(), jInfo.get())

    companion object {
        fun create(project: Project): ProcessInfoProviders {
            return ProcessInfoProviders(
                jStat = project.jStat(Constants.GRADLE_PROCESS_NAME),
                jInfo = project.jInfo(Constants.GRADLE_PROCESS_NAME)
            )
        }
    }
}

internal data class ProcessInfo(
    val jStat: String,
    val jInfo: String
)

internal object ParallelProcessInfoProviders {
    fun create(project: Project): Provider<String> =
        project.execute(parallelProbeCommand(Constants.GRADLE_PROCESS_NAME))

    fun resolve(output: Provider<String>): ProcessInfo = resolve(output.get())

    fun resolve(output: String): ProcessInfo {
        val sections = output.split(OUTPUT_SEPARATOR, limit = 2)
        return ProcessInfo(
            jStat = sections.firstOrNull().orEmpty(),
            jInfo = sections.getOrNull(1).orEmpty(),
        )
    }

    private fun parallelProbeCommand(processName: String): String =
        """
        jstat_file=${'$'}(mktemp "${'$'}{TMPDIR:-/tmp}/infogradleprocess-jstat.XXXXXX")
        jinfo_file=${'$'}(mktemp "${'$'}{TMPDIR:-/tmp}/infogradleprocess-jinfo.XXXXXX")
        trap 'rm -f "${'$'}jstat_file" "${'$'}jinfo_file"' EXIT
        (
          jps | grep $processName | sed 's/$processName//' |
            while read ln; do
              jstat -gc -t ${'$'}ln
              echo "${'$'}ln"
            done
        ) >"${'$'}jstat_file" &
        jstat_job=${'$'}!
        (
          jps | grep $processName | sed 's/$processName//' |
            while read ln; do
              jinfo ${'$'}ln | grep "XX:MaxHeapSize"
              echo "${'$'}ln"
            done
        ) >"${'$'}jinfo_file" &
        jinfo_job=${'$'}!
        wait "${'$'}jstat_job" "${'$'}jinfo_job"
        cat "${'$'}jstat_file"
        printf '%s' '$OUTPUT_SEPARATOR'
        cat "${'$'}jinfo_file"
        """.trimIndent()

    private const val OUTPUT_SEPARATOR = "__INFO_GRADLE_PROCESS_JINFO__"
}
