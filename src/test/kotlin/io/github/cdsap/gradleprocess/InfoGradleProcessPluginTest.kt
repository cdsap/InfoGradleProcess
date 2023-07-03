package io.github.cdsap.gradleprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InfoGradleProcessPluginTest {

    private val gradleVersions = listOf("7.5.1", "7.6", "8.0.1", "8.1.1")

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun testOutputIsGeneratedWhenPluginIsApplied() {
        createBuildGradle()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }


    @Test
    fun testPluginIsCompatibleWithConfigurationCacheWithoutGradleEnterprise() {
        createBuildGradle()

        gradleVersions.forEach {
            val firstBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--configuration-cache")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            val secondBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--configuration-cache")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()

            assertTrue(firstBuild.output.contains("Configuration cache entry stored"))
            assertTrue(secondBuild.output.contains("Configuration cache entry reused."))
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgs() {
        testProjectDir.newFile("gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx256m -Dfile.encoding=UTF-8
        """.trimIndent()
        )

        createBuildGradle()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgsAndKotlinJvm() {
        testProjectDir.newFile("gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx600m
        """.trimIndent()
        )
        createBuildGradle()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmArgsAndKotlinGCJvm() {
        testProjectDir.newFile("gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx750m -Dfile.encoding=UTF-8 -XX:+UseParallelGC
        """.trimIndent()
        )
        createBuildGradle()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinJvm() {
        testProjectDir.newFile("gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx512m -XX:+UseParallelGC -Dfile.encoding=UTF-8
        """.trimIndent()
        )
        createBuildGradle()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
            assertTrue(build.output.contains("G1"))
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinGCJvm() {
        testProjectDir.newFile("gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx400m -XX:+UseParallelGC
        """.trimIndent()
        )
        createBuildGradle()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
            assertTrue(build.output.contains("UseParallelGC"))
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinZ1Jvm() {
        Assume.assumeTrue(Runtime.version().feature() >= 15)

        testProjectDir.newFile("gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx512m  -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Dfile.encoding=UTF-8
        """.trimIndent()
        )

        createBuildGradle17()

        gradleVersions.forEach {
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
            assertTrue(build.output.contains("Z"))

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

    private fun createBuildGradle() {
        testProjectDir.newFile("build.gradle").appendText(
            """
                    plugins {
                        id 'org.jetbrains.kotlin.jvm' version '1.7.21'
                        id 'application'
                        id 'io.github.cdsap.gradleprocess'
                    }
                    repositories {
                        mavenCentral()
                    }

                """.trimIndent()
        )
    }

    private fun createBuildGradle17() {
        testProjectDir.newFile("build.gradle").appendText(
            """
                    plugins {
                        id 'application'
                        id 'io.github.cdsap.gradleprocess'
                    }
                    repositories {
                        mavenCentral()
                    }
                    java {
                        toolchain {
                            languageVersion = JavaLanguageVersion.of(17)
                        }
                    }
                """.trimIndent()
        )
    }
}
