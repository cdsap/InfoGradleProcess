package io.github.cdsap.gradleprocess.output

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.github.cdsap.gbos.core.GbosJson
import io.github.cdsap.jdk.tools.parser.model.Process
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object GbosFileOutput {
    private const val JSON_NAME = "gbos.json"
    private const val NDJSON_NAME = "gbos.ndjson"

    fun write(outputDirectory: File, processes: List<Process>, writeJson: Boolean, writeNdjson: Boolean) {
        val json = outputDirectory.resolve(JSON_NAME)
        val ndjson = outputDirectory.resolve(NDJSON_NAME)
        if (processes.isEmpty()) {
            json.delete()
            ndjson.delete()
            return
        }
        val observations = GbosDevelocityProjection.observations(processes)
        writeAtomically(json, writeJson) { GbosJson.encodeReport(observations) }
        writeAtomically(ndjson, writeNdjson) {
            observations.joinToString("\n", postfix = "\n", transform = ::compactObservation)
        }
    }

    private fun compactObservation(observation: io.github.cdsap.gbos.core.GbosObservation): String =
        JsonOutput.toJson(JsonSlurper().parseText(GbosJson.encode(observation)))

    private fun writeAtomically(target: File, enabled: Boolean, content: () -> String) {
        if (!enabled) {
            target.delete()
            return
        }
        target.parentFile?.mkdirs()
        val parent = target.parentFile ?: error("GBOS output must have a parent directory")
        val temporary = Files.createTempFile(parent.toPath(), target.name, ".tmp").toFile()
        try {
            temporary.writeText(content())
            try {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temporary.delete()
        }
    }
}
