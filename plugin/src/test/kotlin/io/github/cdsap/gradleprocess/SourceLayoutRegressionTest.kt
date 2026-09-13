package io.github.cdsap.gradleprocess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the Gradle best-practice layout from issue #72: sources belong in
 * the `:plugin` module, not the root project.
 */
class SourceLayoutRegressionTest {

    @Test
    fun kotlinSourcesLiveInPluginModuleNotRoot() {
        val pluginProjectDir = File(System.getProperty("user.dir")).canonicalFile
        val pluginMain = File(pluginProjectDir, "src/main/kotlin/io/github/cdsap/gradleprocess")
        val pluginTest = File(pluginProjectDir, "src/test/kotlin/io/github/cdsap/gradleprocess")
        val rootSrc = File(pluginProjectDir.parentFile, "src")

        assertTrue(
            "Expected :plugin project directory named 'plugin', was '${pluginProjectDir.name}'",
            pluginProjectDir.name == "plugin"
        )
        assertTrue(
            "Missing plugin main sources at ${pluginMain.path}",
            pluginMain.resolve("InfoGradleProcessPlugin.kt").isFile
        )
        assertTrue(
            "Missing plugin test sources at ${pluginTest.path}",
            pluginTest.isDirectory && pluginTest.listFiles()?.isNotEmpty() == true
        )
        assertFalse(
            "Root project must not contain a src/ tree after the #72 split",
            rootSrc.exists()
        )
    }

    @Test
    fun publicationCoordinatesStayStableAfterModuleSplit() {
        val buildScript = File(System.getProperty("user.dir"), "build.gradle.kts").readText()
        assertTrue(
            "pluginMaven must keep historical artifactId InfoGradleProcess",
            buildScript.contains("artifactId = \"InfoGradleProcess\"")
        )
        assertTrue(
            "gradleProcessPublication must keep artifactId gradleprocess",
            buildScript.contains("artifactId = \"gradleprocess\"")
        )
        assertTrue(
            "archivesName must preserve InfoGradleProcess jar coordinates",
            buildScript.contains("archivesName.set(\"InfoGradleProcess\")")
        )
    }
}
