package io.github.cdsap.gradleprocess.output

import io.github.cdsap.jdk.tools.parser.model.Process
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GbosFileOutputTest {
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private val process = Process("9011", 1.0, 0.5, 0.75, 0.25, 2.5, "-XX:+UseG1GC", TypeProcess.Gradle)

    @Test
    fun writesCanonicalJsonEnvelopeAndOneObservationPerNdjsonLine() {
        GbosFileOutput.write(temporaryFolder.root, listOf(process), writeJson = true, writeNdjson = true)

        val json = temporaryFolder.root.resolve("gbos.json").readText()
        val ndjson = temporaryFolder.root.resolve("gbos.ndjson").readLines()
        assertTrue(json, json.contains("\"schemaVersion\": \"1.0.0\""))
        assertTrue(json.contains("\"build.tool.name\": \"gradle\""))
        assertTrue(json.contains("\"producer\": {"))
        assertTrue(json.contains("\"name\": \"info-gradle-process\""))
        assertEquals(1, ndjson.size)
        assertTrue(ndjson.single().contains("\"schemaVersion\":\"1.0.0\""))
        assertFalse(ndjson.single().contains("\n"))
    }

    @Test
    fun failedReportKeepsPreviousSuccessfulFiles() {
        GbosFileOutput.write(temporaryFolder.root, listOf(process), writeJson = true, writeNdjson = true)
        val json = temporaryFolder.root.resolve("gbos.json")
        val ndjson = temporaryFolder.root.resolve("gbos.ndjson")
        val previousJson = json.readText()
        val previousNdjson = ndjson.readText()

        val invalid = process.copy(pid = "not-a-pid")
        var failed = false
        try {
            GbosFileOutput.write(temporaryFolder.root, listOf(invalid), writeJson = true, writeNdjson = true)
        } catch (_: IllegalArgumentException) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(previousJson, json.readText())
        assertEquals(previousNdjson, ndjson.readText())
    }
}
