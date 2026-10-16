package io.github.cdsap.gradleprocess.output

import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class GbosDevelocityProjectionTest {
    private val process =
        Process(
            "9011",
            1.0,
            0.5,
            0.75,
            0.25,
            2.5,
            "-XX:+UseG1GC",
            TypeProcess.Gradle,
        )

    @Test
    fun gbosOptInEmitsHeaderAndAStandardizedGradleDaemonObservation() {
        val values = DevelocityCustomValues.from(listOf(process), gbosEnabled = true).toMap()

        assertEquals("1.0.0", values["gbos.schema"])
        assertEquals("info-gradle-process", values["gbos.v1.producer.info_gradle_process.name"])
        assertEquals("0.0.4", values["gbos.v1.producer.info_gradle_process.version"])

        val observation = values.getValue("gbos.v1.producer.info_gradle_process.observation")
        assertTrue(observation.contains("\"scope\":\"jvm.process\""))
        assertTrue(observation.contains("\"aggregationScope\":\"entity\""))
        assertTrue(observation.contains("\"process.pid\":9011"))
        assertTrue(observation.contains("\"jvm.process.role\":\"gradle-daemon\""))
        assertTrue(observation.contains("\"jvm.gc.name\":\"G1\""))
        assertTrue(observation.contains("\"value\":1073741824,\"unit\":\"By\""))
        assertTrue(observation.contains("\"value\":536870912,\"unit\":\"By\""))
        assertTrue(observation, observation.contains("\"value\":15,\"unit\":\"s\""))
        assertTrue(observation, observation.contains("\"value\":150,\"unit\":\"s\""))
    }

    @Test
    fun gbosOptInPublishesGbosTogetherWithLegacyValues() {
        val gbosAndLegacy = DevelocityCustomValues.from(listOf(process), gbosEnabled = true, legacyEnabled = true)
        val legacyOnly = DevelocityCustomValues.from(listOf(process), gbosEnabled = false, legacyEnabled = true)

        assertTrue(gbosAndLegacy.any { (name, _) -> name.startsWith("gbos.") })
        assertTrue(gbosAndLegacy.any { (name, _) -> name.startsWith("Gradle-Process-") })
        assertFalse(legacyOnly.any { (name, _) -> name.startsWith("gbos.") })
        assertTrue(legacyOnly.all { (name, _) -> name.startsWith("Gradle-Process-") })
        assertTrue(legacyOnly.any { (name, _) -> name.endsWith("-max") })
    }
}
