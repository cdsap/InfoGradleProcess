package io.github.cdsap.gradleprocess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class GradleWrapperPropertiesTest {

    @Test
    fun distributionSha256SumIsSetForConfiguredDistribution() {
        val propertiesFile = File(repositoryRoot(), "gradle/wrapper/gradle-wrapper.properties")
        assertTrue(
            "Expected gradle-wrapper.properties at ${propertiesFile.absolutePath}",
            propertiesFile.exists()
        )

        val props = Properties().apply {
            propertiesFile.inputStream().use { load(it) }
        }

        val distributionUrl = props.getProperty("distributionUrl")
        assertNotNull("distributionUrl must be set", distributionUrl)
        assertTrue(
            "distributionUrl should reference gradle-9.8.0-bin.zip",
            distributionUrl!!.contains("gradle-9.8.0-bin.zip")
        )

        val distributionSha256Sum = props.getProperty("distributionSha256Sum")
        assertNotNull(
            "distributionSha256Sum must be set so the wrapper verifies the distribution ZIP",
            distributionSha256Sum
        )
        assertTrue(
            "distributionSha256Sum must be a 64-character hex digest",
            distributionSha256Sum!!.matches(Regex("[0-9a-fA-F]{64}"))
        )
        assertEquals(
            // Official binary-only checksum from https://gradle.org/release-checksums/
            "bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c",
            distributionSha256Sum
        )
    }

    private fun repositoryRoot(): File {
        var directory = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (File(directory, "settings.gradle.kts").isFile) return directory
            directory = directory.parentFile ?: error("repository root not found")
        }
    }
}
