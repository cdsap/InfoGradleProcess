package io.github.cdsap.gradleprocess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VersionCatalogTest {

    private val projectRoot = File(System.getProperty("user.dir"))
    private val catalogFile = File(projectRoot, "gradle/libs.versions.toml")
    private val buildFile = File(projectRoot, "build.gradle.kts")

    @Test
    fun versionCatalogDefinesExpectedLibrariesAndPlugin() {
        assertTrue("Expected gradle/libs.versions.toml at project root", catalogFile.isFile)

        val catalog = catalogFile.readText()
        listOf(
            "jdkToolsParser",
            "commandlineValueSource",
            "picnic",
            "develocity",
            "junit",
            "cdsap-jdkToolsParser",
            "cdsap-commandlineValueSource",
            "develocity-gradlePlugin",
            "pluginPublish",
            "com.gradle.plugin-publish",
        ).forEach { expected ->
            assertTrue("Catalog should declare '$expected'", catalog.contains(expected))
        }
    }

    @Test
    fun buildScriptUsesCatalogAliasesInsteadOfHardcodedVersions() {
        assertTrue("Expected build.gradle.kts at project root", buildFile.isFile)

        val build = buildFile.readText()
        listOf(
            "alias(libs.plugins.pluginPublish)",
            "libs.cdsap.jdkToolsParser",
            "libs.cdsap.commandlineValueSource",
            "libs.picnic",
            "libs.develocity.gradlePlugin",
            "libs.junit",
        ).forEach { expected ->
            assertTrue("build.gradle.kts should use '$expected'", build.contains(expected))
        }

        listOf(
            "id(\"com.gradle.plugin-publish\") version \"2.1.1\"",
            "io.github.cdsap:jdk-tools-parser:0.1.1",
            "io.github.cdsap:commandline-value-source:0.1.0",
            "com.jakewharton.picnic:picnic:0.7.0",
            "com.gradle:develocity-gradle-plugin:4.5.0",
            "junit:junit:4.13.2",
        ).forEach { hardcoded ->
            assertFalse(
                "build.gradle.kts should not hardcode '$hardcoded'",
                build.contains(hardcoded),
            )
        }
    }
}
