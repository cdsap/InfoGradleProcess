package io.github.cdsap.gradleprocess

import io.github.cdsap.gradleprocess.fake.FakeJdkTools
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ParallelProcessInfoProvidersTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun createBuildsCombinedProbeProvider() {
        val project = ProjectBuilder.builder().build()

        assertNotNull(ParallelProcessInfoProviders.create(project))
    }

    @Test
    fun resolveSplitsCombinedProbeOutputOnSeparator() {
        val jStat = FakeJdkTools.jStatOutput(listOf(FakeJdkTools.G1_DAEMON))
        val jInfo = FakeJdkTools.jInfoOutput(listOf(FakeJdkTools.G1_DAEMON))

        val processInfo = ParallelProcessInfoProviders.resolve("$jStat$SEPARATOR$jInfo")

        assertEquals(jStat, processInfo.jStat)
        assertEquals(jInfo, processInfo.jInfo)
    }

    @Test
    fun resolveTreatsMissingSeparatorAsEmptyJInfo() {
        val jStat = FakeJdkTools.jStatOutput(listOf(FakeJdkTools.G1_DAEMON))

        val processInfo = ParallelProcessInfoProviders.resolve(jStat)

        assertEquals(jStat, processInfo.jStat)
        assertEquals("", processInfo.jInfo)
    }

    @Test
    fun resolveAllowsEmptyJStatWhenSeparatorIsPresent() {
        val jInfo = FakeJdkTools.jInfoOutput(listOf(FakeJdkTools.G1_DAEMON))

        val processInfo = ParallelProcessInfoProviders.resolve("$SEPARATOR$jInfo")

        assertEquals("", processInfo.jStat)
        assertEquals(jInfo, processInfo.jInfo)
    }

    @Test
    fun resolveKeepsJInfoContentThatContainsSeparatorPrefixText() {
        val jStat = "jstat-section"
        val jInfo = "prefix${SEPARATOR}still-jinfo"

        val processInfo = ParallelProcessInfoProviders.resolve("$jStat$SEPARATOR$jInfo")

        assertEquals(jStat, processInfo.jStat)
        assertEquals(jInfo, processInfo.jInfo)
    }

    @Test
    fun resolveFromProviderMatchesStringResolve() {
        val project = ProjectBuilder.builder().build()
        val combined = "left${SEPARATOR}right"
        val provider = project.providers.provider { combined }

        assertEquals(
            ParallelProcessInfoProviders.resolve(combined),
            ParallelProcessInfoProviders.resolve(provider),
        )
    }

    @Test
    fun parallelShellProbeWithFakeJdkToolsProducesCollectableGradleDaemons() {
        val daemons = listOf(FakeJdkTools.G1_DAEMON, FakeJdkTools.PARALLEL_DAEMON)
        val fakeTools = FakeJdkTools.install(temporaryFolder.newFolder("jdk-tools"), daemons)
        val script = File(temporaryFolder.newFolder("probe"), "probe.sh")
        script.writeText(
            """
            #!/bin/sh
            set -eu
            export PATH='${fakeTools.absolutePath}:'"${'$'}PATH"
            jstat_file=${'$'}(mktemp "${'$'}{TMPDIR:-/tmp}/infogradleprocess-jstat.XXXXXX")
            jinfo_file=${'$'}(mktemp "${'$'}{TMPDIR:-/tmp}/infogradleprocess-jinfo.XXXXXX")
            trap 'rm -f "${'$'}jstat_file" "${'$'}jinfo_file"' EXIT
            (
              jps | grep GradleDaemon | sed 's/GradleDaemon//' |
                while read ln; do
                  jstat -gc -t ${'$'}ln
                  echo "${'$'}ln"
                done
            ) >"${'$'}jstat_file" &
            jstat_job=${'$'}!
            (
              jps | grep GradleDaemon | sed 's/GradleDaemon//' |
                while read ln; do
                  jinfo ${'$'}ln | grep "XX:MaxHeapSize"
                  echo "${'$'}ln"
                done
            ) >"${'$'}jinfo_file" &
            jinfo_job=${'$'}!
            wait "${'$'}jstat_job" "${'$'}jinfo_job"
            cat "${'$'}jstat_file"
            printf '%s' '$SEPARATOR'
            cat "${'$'}jinfo_file"
            """.trimIndent(),
        )
        check(script.setExecutable(true))

        val process = ProcessBuilder("sh", script.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor())

        val processInfo = ParallelProcessInfoProviders.resolve(output)
        val processes = GradleProcessCollector().collectGradle(processInfo)

        assertEquals(listOf("12345", "23456"), processes.map { it.pid })
        assertTrue(processes.all { it.typeProcess == TypeProcess.Gradle })
    }

    private companion object {
        const val SEPARATOR = "__INFO_GRADLE_PROCESS_JINFO__"
    }
}
