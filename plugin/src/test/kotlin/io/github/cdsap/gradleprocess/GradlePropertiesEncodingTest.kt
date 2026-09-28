package io.github.cdsap.gradleprocess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GradlePropertiesEncodingTest {

    @Test
    fun gradleJvmArgsPinUtf8FileEncodingOnSingleLine() {
        val gradleProperties = resolveGradleProperties()
        val jvmArgsLines = gradleProperties.readLines()
            .map { it.trim() }
            .filter { it.startsWith("org.gradle.jvmargs=") }

        assertEquals(
            "Exactly one org.gradle.jvmargs entry is required so a later line cannot override encoding",
            1,
            jvmArgsLines.size
        )

        val jvmArgs = jvmArgsLines.single().removePrefix("org.gradle.jvmargs=")
        assertTrue(
            "org.gradle.jvmargs must pin -Dfile.encoding=UTF-8; was: $jvmArgs",
            jvmArgs.split(Regex("\\s+")).contains("-Dfile.encoding=UTF-8")
        )
    }

    private fun resolveGradleProperties(): File {
        var dir = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            val candidate = File(dir, "gradle.properties")
            if (candidate.isFile) {
                return candidate
            }
            dir = dir.parentFile ?: error("gradle.properties not found above ${System.getProperty("user.dir")}")
        }
    }
}
