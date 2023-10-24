package io.github.cdsap.gradleprocess

import junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InfoGradleProcessPluginTest {

    private val gradleVersions = listOf("7.6", "8.1.1", "8.3", "8.4")

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun testOutputIsGeneratedWhenPluginIsApplied() {
        testProjectDir.newFile("gradle.properties").appendText(
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-qXmx878m -XX:MaxMetaspaceSize=250m -Dfile.encoding=UTF-8
        """.trimIndent()
        )
        createBuildGradle()

        gradleVersions.forEach {
            println(it)
            killDaemon()
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
        }
    }


    @Test
    fun testPluginIsCompatibleWithConfigurationCacheWithoutGradleEnterprise() {
        createBuildGradle()

        gradleVersions.forEach {
            println(it)
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
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinJvm() {
        testProjectDir.newFile("gradle.properties").writeText(
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-Xmx100m  -Dfile.encoding=UTF-8
        """.trimIndent()
        )
        createBuildGradle()

        gradleVersions.forEach {
            println(it)
            killDaemon()
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
            assertTrue(build.output.contains("G1"))
        }
    }

    @Test
    fun testOutputIsGeneratedWhenPluginIsAppliedWithJvmGCArgsAndKotlinZ1Jvm() {
        Assume.assumeTrue(Runtime.version().feature() >= 15)

        testProjectDir.newFile("gradle.properties").writeText(
            """
            org.gradle.daemon=false
            org.gradle.jvmargs=-Xmx512m  -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Dfile.encoding=UTF-8
        """.trimIndent()
        )

        createBuildGradle17()

        gradleVersions.forEach {
            println(it)
            killDaemon()
            val build = simpleKotlinCompileBuild(it)
            assertTerminalOutput(build)
            assertTrue(build.output.contains("Z"))

        }
    }

    private fun simpleKotlinCompileBuild(it: String): BuildResult = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments("compileKotlin", "--info","-Dorg.gradle.jvmargs=-Xmx256m")
        .withPluginClasspath()
        .withGradleVersion(it)
        .withDebug(true)
        .build().also { Thread.sleep(2000) }

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
        //createFile()
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

    private fun killDaemon() {
        Runtime.getRuntime().exec("jps | grep \"GradleDaemon\" | sed 's/GradleDaemon//' | while read ln; do kill -9 \$ln; done")
    }
}
