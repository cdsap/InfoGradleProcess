package io.github.cdsap.gradleprocess

import junit.framework.TestCase

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InfoGradleProcessPluginWtihBuildScanTest {

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun testPluginIsCompatibleWithConfigurationCacheWithDevelocity() {
        Assume.assumeTrue(
            "Gradle Enterprise URL and Access Key are set",
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null
        )

        testProjectDir.newFile("settings.gradle.kts").appendText(
            """
                plugins {
                    id("com.gradle.develocity") version("4.2")
                }
                develocity {
                    server = "https://ge.solutions-team.gradle.com/"
                    accessKey="${System.getenv("GE_API_KEY")}"
                    buildScan {
                        publishing { true }
                    }
                }
            """.trimIndent()
        )
        testProjectDir.newFile("build.gradle").appendText(
            """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '2.0.20'
                    id 'application'
                    id 'io.github.cdsap.gradleprocess'

                }
                repositories {
                    mavenCentral()
                }
            """.trimIndent()
        )
        listOf("8.14.1", "9.1.0").forEach {
            val firstBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--configuration-cache","--info")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            val secondBuild = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("compileKotlin", "--configuration-cache")
                .withPluginClasspath()
                .withGradleVersion(it)
                .build()
            TestCase.assertTrue(firstBuild.output.contains("Configuration cache entry stored"))
            TestCase.assertTrue(secondBuild.output.contains("Configuration cache entry reused."))
        }
    }
}
