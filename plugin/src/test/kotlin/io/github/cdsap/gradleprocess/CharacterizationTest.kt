package io.github.cdsap.gradleprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration
import io.github.cdsap.gradleprocess.fake.FakeDevelocityPlugin
import io.github.cdsap.gradleprocess.fake.FakeGradleDaemon
import io.github.cdsap.gradleprocess.fake.FakeJdkTools
import io.github.cdsap.gradleprocess.output.ConsoleOutput
import io.github.cdsap.gradleprocess.output.DevelocityValues
import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.gradle.api.Action
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.reflect.Proxy
import java.util.Properties

/**
 * Characterization tests pinning what consumers of `io.github.cdsap.gradleprocess` observe today:
 * Build Scan custom values (keys, order, value formats) and the console table. Expected values
 * are recorded from the current implementation, not derived from a spec.
 *
 * Process input is deterministic everywhere:
 * - Unit level: fixture `jstat`/`jinfo` value-source output goes through the real
 *   [GradleProcessCollector] into [DevelocityValues] (with a recording scan API proxy) and
 *   [ConsoleOutput].
 * - TestKit level: [FakeJdkTools] puts fake `jps`/`jstat`/`jinfo` scripts first on the build's
 *   `PATH`, so the real commandline value sources, collector, build service and Develocity
 *   wiring run against fixed output, independent of the host JDK tooling (e.g. asdf shims).
 *
 * Build Scan values are observed through [FakeDevelocityPlugin], which prints
 * `SCAN-VALUE <name>=<value>` for each `buildScan.value` call.
 */
class CharacterizationTest {
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    // ---------------------------------------------------------------- unit level

    @Test
    fun oneGradleDaemonFixtureIsCollectedWithExactFields() {
        assertEquals(
            listOf(Process("12345", 4.0, 1.27, 1.94, 0.01, 18.63, "-XX:+UseG1GC", TypeProcess.Gradle)),
            collect(FakeJdkTools.G1_DAEMON),
        )
    }

    @Test
    fun oneGradleDaemonEmitsExactlySixValuesInOrder() {
        assertEquals(G1_SCAN_VALUES, recordScanValues(collect(FakeJdkTools.G1_DAEMON)))
    }

    @Test
    fun twoGradleDaemonsEmitValuesGroupedPerProcessInJstatOrder() {
        assertEquals(
            G1_SCAN_VALUES + PARALLEL_SCAN_VALUES,
            recordScanValues(collect(FakeJdkTools.G1_DAEMON, FakeJdkTools.PARALLEL_DAEMON)),
        )
        assertEquals(
            PARALLEL_SCAN_VALUES + G1_SCAN_VALUES,
            recordScanValues(collect(FakeJdkTools.PARALLEL_DAEMON, FakeJdkTools.G1_DAEMON)),
        )
    }

    @Test
    fun consoleTablePinsTitleHeaderColumnsAndRowFormat() {
        assertEquals(G1_CONSOLE_TABLE, captureConsole(collect(FakeJdkTools.G1_DAEMON)))
    }

    // ---------------------------------------------------------------- TestKit level

    @Test
    fun settingsPluginWithoutDevelocityPrintsTheConsoleTable() {
        GRADLE_VERSIONS.forEach { gradleVersion ->
            val output = build(
                project("settings.gradle" to PLUGIN_ONLY, "build.gradle" to ""),
                gradleVersion,
                develocity = false,
            )

            assertEquals(output, listOf(G1_CONSOLE_TABLE), output.consoleTables())
            assertEquals(output, emptyList<String>(), output.scanValues())
        }
    }

    @Test
    fun buildScriptPluginWithoutDevelocityPrintsTheSameConsoleTable() {
        GRADLE_VERSIONS.forEach { gradleVersion ->
            val output = build(
                project("settings.gradle" to "", "build.gradle" to PLUGIN_ONLY),
                gradleVersion,
                develocity = false,
            )

            assertEquals(output, listOf(G1_CONSOLE_TABLE), output.consoleTables())
            assertEquals(output, emptyList<String>(), output.scanValues())
        }
    }

    @Test
    fun fakeDevelocityAndPluginInSettingsEmitScanValuesWithoutConsoleTable() {
        GRADLE_VERSIONS.forEach { gradleVersion ->
            val output = build(
                project("settings.gradle" to DEVELOCITY_BEFORE_PLUGIN, "build.gradle" to ""),
                gradleVersion,
                develocity = true,
            )

            assertFakeDevelocityApplied(output)
            assertEquals(output, G1_SCAN_VALUES, output.scanValues())
            assertEquals(output, emptyList<String>(), output.consoleTables())
        }
    }

    @Test
    fun fakeDevelocityDeclaredBeforeOrAfterThePluginInSettingsEmitsIdenticalScanValues() {
        GRADLE_VERSIONS.forEach { gradleVersion ->
            val before = build(
                project("settings.gradle" to DEVELOCITY_BEFORE_PLUGIN, "build.gradle" to ""),
                gradleVersion,
                develocity = true,
            )
            val after = build(
                project("settings.gradle" to DEVELOCITY_AFTER_PLUGIN, "build.gradle" to ""),
                gradleVersion,
                develocity = true,
            )

            assertFakeDevelocityApplied(before)
            assertFakeDevelocityApplied(after)
            assertEquals(before, G1_SCAN_VALUES, before.scanValues())
            assertEquals(after, G1_SCAN_VALUES, after.scanValues())
            assertEquals(before, emptyList<String>(), before.consoleTables())
            assertEquals(after, emptyList<String>(), after.consoleTables())
        }
    }

    /**
     * The build-script path (`configureFromProject`) reports to Develocity when the root project
     * has a `develocity` extension. Settings-applied Develocity 4.x adds its settings extension to
     * the root project as well (mirrored by the fake), so the build-script plugin emits the same
     * scan values as the settings-applied plugin and no console table.
     */
    @Test
    fun fakeDevelocityInSettingsWithPluginInRootBuildScriptEmitsScanValuesWithoutConsoleTable() {
        GRADLE_VERSIONS.forEach { gradleVersion ->
            val output = build(
                project("settings.gradle" to DEVELOCITY_ONLY, "build.gradle" to PLUGIN_ONLY),
                gradleVersion,
                develocity = true,
            )

            assertFakeDevelocityApplied(output)
            assertEquals(output, G1_SCAN_VALUES, output.scanValues())
            assertEquals(output, emptyList<String>(), output.consoleTables())
        }
    }

    @Test
    fun configurationCacheReuseEmitsTheSameConsoleTable() {
        GRADLE_VERSIONS.forEach { gradleVersion ->
            val project = project("settings.gradle" to PLUGIN_ONLY, "build.gradle" to "")

            val (store, reuse) = storeAndReuseConfigurationCache(project, gradleVersion, develocity = false)

            assertEquals(store, listOf(G1_CONSOLE_TABLE), store.consoleTables())
            assertEquals(reuse, listOf(G1_CONSOLE_TABLE), reuse.consoleTables())
            assertEquals(reuse, emptyList<String>(), reuse.scanValues())
        }
    }

    @Test
    fun configurationCacheReuseEmitsTheSameScanValues() {
        GRADLE_VERSIONS.forEach { gradleVersion ->
            val project = project("settings.gradle" to DEVELOCITY_BEFORE_PLUGIN, "build.gradle" to "")

            val (store, reuse) = storeAndReuseConfigurationCache(project, gradleVersion, develocity = true)

            assertFakeDevelocityApplied(store)
            assertFalse(
                "Reuse must replay buildFinished actions from the cache entry without configuring:\n$reuse",
                reuse.contains(FakeDevelocityPlugin.APPLIED_MARKER),
            )
            assertEquals(store, G1_SCAN_VALUES, store.scanValues())
            assertEquals(reuse, G1_SCAN_VALUES, reuse.scanValues())
            assertEquals(reuse, emptyList<String>(), reuse.consoleTables())
        }
    }

    @Test
    fun withoutGradleDaemonInJpsNothingIsReported() {
        GRADLE_VERSIONS.forEach { gradleVersion ->
            val console = build(
                project("settings.gradle" to PLUGIN_ONLY, "build.gradle" to ""),
                gradleVersion,
                develocity = false,
                daemons = emptyList(),
            )
            val scan = build(
                project("settings.gradle" to DEVELOCITY_BEFORE_PLUGIN, "build.gradle" to ""),
                gradleVersion,
                develocity = true,
                daemons = emptyList(),
            )

            assertEquals(console, emptyList<String>(), console.consoleTables())
            assertEquals(console, emptyList<String>(), console.scanValues())
            assertFakeDevelocityApplied(scan)
            assertEquals(scan, emptyList<String>(), scan.scanValues())
            assertEquals(scan, emptyList<String>(), scan.consoleTables())
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun collect(vararg daemons: FakeGradleDaemon): List<Process> =
        GradleProcessCollector().collect(
            FakeJdkTools.jStatOutput(daemons.toList()),
            FakeJdkTools.jInfoOutput(daemons.toList()),
            TypeProcess.Gradle,
        )

    /** Runs [DevelocityValues] against a scan API proxy that records `value` calls in order. */
    private fun recordScanValues(processes: List<Process>): List<String> {
        val recorded = mutableListOf<String>()
        val buildScan = proxy(BuildScanConfiguration::class.java) { name, args ->
            if (name == "value") recorded += "${args[0]}=${args[1]}"
            null
        }
        val develocity = proxy(DevelocityConfiguration::class.java) { name, args ->
            when (name) {
                "getBuildScan" -> buildScan
                "buildScan" -> {
                    @Suppress("UNCHECKED_CAST")
                    (args[0] as Action<BuildScanConfiguration>).execute(buildScan)
                    null
                }
                else -> null
            }
        }
        DevelocityValues(develocity, processes).addProcessesInfoToBuildScan()
        return recorded
    }

    private fun <T> proxy(type: Class<T>, handler: (String, Array<out Any?>) -> Any?): T =
        type.cast(
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
                handler(method.name, args ?: emptyArray())
            }
        )

    private fun captureConsole(processes: List<Process>): String {
        val original = System.out
        val captured = ByteArrayOutputStream()
        System.setOut(PrintStream(captured, true, Charsets.UTF_8.name()))
        try {
            ConsoleOutput(processes).print()
        } finally {
            System.setOut(original)
        }
        return captured.toString(Charsets.UTF_8.name()).trimEnd()
    }

    private fun assertFakeDevelocityApplied(output: String) {
        assertTrue(
            "Fake Develocity was not applied; check the TestKit plugin classpath order:\n$output",
            output.contains("${FakeDevelocityPlugin.APPLIED_MARKER} settings"),
        )
    }

    private fun project(vararg files: Pair<String, String>): File {
        val directory = temporaryFolder.newFolder()
        files.forEach { (name, content) -> File(directory, name).writeText(content) }
        return directory
    }

    private fun storeAndReuseConfigurationCache(
        project: File,
        gradleVersion: String,
        develocity: Boolean,
    ): Pair<String, String> {
        val store = build(project, gradleVersion, develocity, "--configuration-cache")
        val reuse = build(project, gradleVersion, develocity, "--configuration-cache")
        assertTrue(store, store.contains("Configuration cache entry stored."))
        assertTrue(reuse, reuse.contains("Reusing configuration cache."))
        return store to reuse
    }

    private fun build(
        project: File,
        gradleVersion: String,
        develocity: Boolean,
        vararg arguments: String,
        daemons: List<FakeGradleDaemon> = listOf(FakeJdkTools.G1_DAEMON),
    ): String {
        val fakeTools = FakeJdkTools.install(temporaryFolder.newFolder(), daemons)
        return GradleRunner.create()
            .withProjectDir(project)
            .withArguments(listOf("help") + arguments)
            .withPluginClasspath(if (develocity) pluginClasspathWithFakeDevelocity() else pluginUnderTestClasspath())
            .withEnvironment(
                System.getenv() + ("PATH" to "${fakeTools.absolutePath}${File.pathSeparator}${System.getenv("PATH")}")
            )
            .withGradleVersion(gradleVersion)
            .build()
            .output
    }

    private fun pluginUnderTestClasspath(): List<File> {
        val loader = CharacterizationTest::class.java.classLoader
        val metadata = Properties().apply {
            loader.getResourceAsStream("plugin-under-test-metadata.properties")!!.use { load(it) }
        }
        return metadata.getProperty("implementation-classpath").split(File.pathSeparator).map(::File)
    }

    /**
     * Test resources first so the fake `com.gradle.develocity` descriptor shadows the one inside
     * the real Develocity jar, which is appended only for the API interfaces the plugin and the
     * fake use (it is `compileOnly` in production, so absent from the plugin-under-test classpath).
     */
    private fun pluginClasspathWithFakeDevelocity(): List<File> {
        val loader = CharacterizationTest::class.java.classLoader
        val descriptor = loader.getResources(FAKE_DEVELOCITY_DESCRIPTOR).toList().single { it.protocol == "file" }
        val testResources = File(descriptor.toURI()).parentFile.parentFile.parentFile
        val testClasses = File(FakeDevelocityPlugin::class.java.protectionDomain.codeSource.location.toURI())
        val develocityJar = File(DevelocityConfiguration::class.java.protectionDomain.codeSource.location.toURI())
        return listOf(testResources, testClasses) + pluginUnderTestClasspath() + develocityJar
    }

    private fun String.scanValues(): List<String> =
        lines().filter { it.startsWith("SCAN-VALUE ") }.map { it.removePrefix("SCAN-VALUE ") }

    private fun String.consoleTables(): List<String> {
        val lines = lines()
        return lines.indices
            .filter { lines[it].startsWith("┌") }
            .map { start ->
                val end = (start until lines.size).first { lines[it].startsWith("└") }
                lines.subList(start, end + 1).joinToString("\n")
            }
    }

    private companion object {
        val GRADLE_VERSIONS = listOf("8.14.2", "9.1.0")
        const val FAKE_DEVELOCITY_DESCRIPTOR = "META-INF/gradle-plugins/com.gradle.develocity.properties"

        val PLUGIN_ONLY = """
            plugins {
                id 'io.github.cdsap.gradleprocess'
            }
        """.trimIndent()

        val DEVELOCITY_ONLY = """
            plugins {
                id 'com.gradle.develocity'
            }
        """.trimIndent()

        val DEVELOCITY_BEFORE_PLUGIN = """
            plugins {
                id 'com.gradle.develocity'
                id 'io.github.cdsap.gradleprocess'
            }
        """.trimIndent()

        val DEVELOCITY_AFTER_PLUGIN = """
            plugins {
                id 'io.github.cdsap.gradleprocess'
                id 'com.gradle.develocity'
            }
        """.trimIndent()

        val G1_SCAN_VALUES = listOf(
            "Gradle-Process-12345-max=4.0 GB",
            "Gradle-Process-12345-usage=1.27 GB",
            "Gradle-Process-12345-capacity=1.94 GB",
            "Gradle-Process-12345-uptime=18.63 minutes",
            "Gradle-Process-12345-gcTime=0.01 minutes",
            "Gradle-Process-12345-gcType=-XX:+UseG1GC",
        )

        val PARALLEL_SCAN_VALUES = listOf(
            "Gradle-Process-23456-max=2.0 GB",
            "Gradle-Process-23456-usage=1.0 GB",
            "Gradle-Process-23456-capacity=1.52 GB",
            "Gradle-Process-23456-uptime=60.0 minutes",
            "Gradle-Process-23456-gcTime=2.0 minutes",
            "Gradle-Process-23456-gcType=-XX:+UseParallelGC",
        )

        val G1_CONSOLE_TABLE = """
            ┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
            │  Gradle processes                                                                               │
            ├─────────┬──────────┬───────────┬────────────┬────────────────┬────────────────┬─────────────────┤
            │  PID    │  Max     │  Usage    │  Capacity  │  GC Time       │  GC Type       │  Uptime         │
            ├─────────┼──────────┼───────────┼────────────┼────────────────┼────────────────┼─────────────────┤
            │  12345  │  4.0 Gb  │  1.27 Gb  │  1.94 Gb   │  0.01 minutes  │  -XX:+UseG1GC  │  18.63 minutes  │
            └─────────┴──────────┴───────────┴────────────┴────────────────┴────────────────┴─────────────────┘
        """.trimIndent()
    }
}
