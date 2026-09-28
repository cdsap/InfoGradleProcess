package io.github.cdsap.gradleprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class InfoGradleProcessPluginTest {

    private val gradleVersions = listOf("8.14.2", "9.1.0")

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun testOutputIsGeneratedWhenPluginIsApplied() {
        createGroovySettingsWithPlugin()
        createBuildGradle()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedFromKotlinSettings() {
        createKotlinSettingsWithPlugin()
        createBuildGradle()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedForMultiProjectBuild() {
        createGroovySettingsWithPlugin(
            extra = """
                include 'app'
                include 'lib'
            """.trimIndent()
        )
        testProjectDir.newFile("build.gradle").writeText(
            """
                subprojects {
                    repositories {
                        mavenCentral()
                    }
                }
            """.trimIndent()
        )
        testProjectDir.newFolder("app")
        testProjectDir.newFolder("lib")
        testProjectDir.newFile("app/build.gradle").writeText(
            """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '2.0.20'
                }
            """.trimIndent()
        )
        testProjectDir.newFile("lib/build.gradle").writeText(
            """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '2.0.20'
                }
            """.trimIndent()
        )

        gradleVersions.forEach {
            val build = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--info")
                .withPluginClasspath()
                .withGradleVersion(it)
                .withDebug(true)
                .build()
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testProjectPluginCompatibilityPathStillReportsOutput() {
        testProjectDir.newFile("settings.gradle").writeText("")
        writeGradleProperties()
        testProjectDir.newFile("build.gradle").writeText(
            """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '2.0.20'
                    id 'application'
                    id 'io.github.cdsap.gradleprocess.project'
                }
                repositories {
                    mavenCentral()
                }
            """.trimIndent()
        )

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testPluginIsCompatibleWithConfigurationCacheWithoutGradleEnterprise() {
        createGroovySettingsWithPlugin()
        createBuildGradle()

        gradleVersions.forEach {
            val firstBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(
                    "compileKotlin",
                    "--no-build-cache",
                    "--configuration-cache",
                    "--configuration-cache-problems=fail"
                )
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            val secondBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(
                    "compileKotlin",
                    "--no-build-cache",
                    "--configuration-cache",
                    "--configuration-cache-problems=fail"
                )
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()

            assertTrue(
                "Gradle $it first run should store configuration cache",
                firstBuild.output.contains("Configuration cache entry stored")
            )
            assertTrue(
                "Gradle $it second run should be a configuration-cache HIT",
                secondBuild.output.contains("Configuration cache entry reused.")
            )
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgs() {
        writeGradleProperties(
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-Xmx256m -Dfile.encoding=UTF-8
        """.trimIndent()
        )
        createGroovySettingsWithPlugin()
        createBuildGradle(writeProperties = false)

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgsAndKotlinJvm() {
        writeGradleProperties(
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-Xmx600m
        """.trimIndent()
        )
        createGroovySettingsWithPlugin()
        createBuildGradle(writeProperties = false)

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgsAndKotlinGCJvm() {
        writeGradleProperties(
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-Xmx750m -Dfile.encoding=UTF-8 -XX:+UseParallelGC
        """.trimIndent()
        )
        createGroovySettingsWithPlugin()
        createBuildGradle(writeProperties = false)

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinJvm() {
        writeGradleProperties(
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-Xmx512m -XX:+UseParallelGC -Dfile.encoding=UTF-8
        """.trimIndent()
        )
        createGroovySettingsWithPlugin()
        createBuildGradle(writeProperties = false)

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
            assertTrue(build.output.contains("G1"))
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinGCJvm() {
        writeGradleProperties(
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-Xmx400m -XX:+UseParallelGC
        """.trimIndent()
        )
        createGroovySettingsWithPlugin()
        createBuildGradle(writeProperties = false)

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
            assertTrue(build.output.contains("UseParallelGC"))
        }
    }

    private fun simpleKotlinCompileBuild(it: String): BuildResult = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments("compileKotlin", "--info")
        .withPluginClasspath()
        .withGradleVersion(it)
        .withDebug(true)
        .build()

    private fun assertTerminalOutput(build: BuildResult) {
        print(build.output)
        assertTrue(build.output.contains("Gradle processes"))
        assertTrue(build.output.contains("PID"))
        assertTrue(build.output.contains("Capacity"))
        assertTrue(build.output.contains("Uptime"))
        assertTrue(build.output.contains("minutes"))
        assertTrue(build.output.contains("Gb"))
    }

    private fun createBuildGradle(writeProperties: Boolean = true) {
        if (writeProperties) {
            writeGradleProperties()
        }
        testProjectDir.newFile("build.gradle").writeText(
            """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '2.0.20'
                    id 'application'
                }
                repositories {
                    mavenCentral()
                }
            """.trimIndent()
        )
    }

    private fun writeGradleProperties(extra: String = "") {
        val props = File(testProjectDir.root, "gradle.properties")
        // Disable Kotlin FUS metrics that fail under configuration cache / settings plugins.
        props.writeText(
            """
                kotlin.internal.collectFUSMetrics=false
                $extra
            """.trimIndent()
        )
    }

    private fun createGroovySettingsWithPlugin(extra: String = "") {
        testProjectDir.newFile("settings.gradle").writeText(
            """
                plugins {
                    id 'io.github.cdsap.gradleprocess'
                }
                $extra
            """.trimIndent()
        )
    }

    private fun createKotlinSettingsWithPlugin() {
        testProjectDir.newFile("settings.gradle.kts").writeText(
            """
                plugins {
                    id("io.github.cdsap.gradleprocess")
                }
            """.trimIndent()
        )
    }
}
