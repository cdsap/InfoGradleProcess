package io.github.cdsap.gradleprocess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseWorkflowTest {

    @Test
    fun releasePublicationIsPinnedToFinalVersionAndJdk17() {
        val workflow = File(repositoryRoot(), ".github/workflows/publish.yaml")
        assertTrue("Expected release publication workflow at ${workflow.absolutePath}", workflow.isFile)

        val yaml = workflow.readText()
        listOf(
            "release:",
            "types: [published]",
            "ref: \${{ github.event.release.tag_name }}",
            "gradle/actions/wrapper-validation@v6",
            "java-version: 17",
            "gradle/actions/setup-gradle@v6",
            "GRADLE_PUBLISH_KEY",
            "GRADLE_PUBLISH_SECRET",
            "./gradlew validatePlugins --stacktrace",
            "./gradlew publishPlugins --stacktrace",
            "Refusing to publish snapshot project version",
            "does not match project version",
        ).forEach { expected ->
            assertTrue("Release workflow should contain '$expected'", yaml.contains(expected))
        }

        assertFalse("Publication must not use JDK 25", yaml.contains("java-version: 25"))
        assertFalse("Publication must not use the test matrix", yaml.contains("version: [17, 21]"))
    }

    private fun repositoryRoot(): File {
        var directory = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (File(directory, "settings.gradle.kts").isFile) return directory
            directory = directory.parentFile ?: error("repository root not found")
        }
    }
}
