package io.github.cdsap.gradleprocess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsRepositoriesTest {

    private val settingsText = File("settings.gradle.kts").readText()

    @Test
    fun settingsDoesNotDeclareGoogleRepository() {
        assertFalse(
            "google() is unused for this JVM plugin and should not be declared",
            settingsText.contains("google()")
        )
    }

    @Test
    fun pluginManagementUsesExclusiveContentForGradlePluginPortal() {
        assertRepositoryBlockUsesFilteredPluginPortal("pluginManagement")
    }

    @Test
    fun dependencyResolutionManagementUsesExclusiveContentForGradlePluginPortal() {
        assertRepositoryBlockUsesFilteredPluginPortal("dependencyResolutionManagement")
    }

    @Test
    fun settingsLeavesMavenCentralAsUnfilteredFallback() {
        assertTrue(
            "mavenCentral() should remain as the unfiltered fallback repository",
            settingsText.contains("mavenCentral()")
        )
        assertFalse(
            "gradlePluginPortal() must not appear outside exclusiveContent",
            Regex("""(?m)^\s*gradlePluginPortal\(\)\s*$""").containsMatchIn(settingsText)
        )
    }

    private fun assertRepositoryBlockUsesFilteredPluginPortal(blockName: String) {
        val block = extractBlock(settingsText, blockName)
        assertTrue(
            "$blockName should declare exclusiveContent for gradlePluginPortal()",
            block.contains("exclusiveContent") && block.contains("gradlePluginPortal()")
        )
        assertTrue(
            "$blockName should filter the plugin portal to com.gradle.* groups",
            block.contains("""includeGroupByRegex("com\\.gradle.*")""")
        )
        assertTrue(
            "$blockName should filter the plugin portal to org.gradle.* groups",
            block.contains("""includeGroupByRegex("org\\.gradle.*")""")
        )
        assertTrue(
            "$blockName should keep mavenCentral() as the fallback",
            block.contains("mavenCentral()")
        )
        assertFalse(
            "$blockName should not declare google()",
            block.contains("google()")
        )
    }

    private fun extractBlock(source: String, blockName: String): String {
        val start = source.indexOf("$blockName {")
        require(start >= 0) { "Missing $blockName block in settings.gradle.kts" }
        var depth = 0
        for (i in start until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return source.substring(start, i + 1)
                    }
                }
            }
        }
        error("Unbalanced braces for $blockName in settings.gradle.kts")
    }
}
