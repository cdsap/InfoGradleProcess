package io.github.cdsap.gradleprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class LegacyProjectApplicationTest {

    private val gradleVersions = listOf("8.14.2", "9.1.0")

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun legacyIdAppliedInRootBuildScriptPrintsConsoleTable() {
        testProjectDir.newFile("settings.gradle.kts").writeText("")
        testProjectDir.newFile("build.gradle.kts").writeText(
            """
                plugins {
                    id("io.github.cdsap.gradleprocess")
                }
            """.trimIndent()
        )

        gradleVersions.forEach {
            assertConsoleTablePrintedOnce(it, build(it))
        }
    }

    @Test
    fun legacyIdAppliedInSettingsPrintsConsoleTable() {
        testProjectDir.newFile("settings.gradle.kts").writeText(
            """
                plugins {
                    id("io.github.cdsap.gradleprocess")
                }
            """.trimIndent()
        )
        testProjectDir.newFile("build.gradle.kts").writeText("")

        gradleVersions.forEach {
            assertConsoleTablePrintedOnce(it, build(it))
        }
    }

    @Test
    fun legacyIdInSettingsAndProjectAliasInBuildScriptConfiguresOnce() {
        testProjectDir.newFile("settings.gradle.kts").writeText(
            """
                plugins {
                    id("io.github.cdsap.gradleprocess")
                }
            """.trimIndent()
        )
        testProjectDir.newFile("build.gradle.kts").writeText(
            """
                plugins {
                    id("io.github.cdsap.gradleprocess.project")
                }
            """.trimIndent()
        )

        gradleVersions.forEach {
            assertConsoleTablePrintedOnce(it, build(it))
        }
    }

    @Test
    fun develocityPathRegistersBuildScanCallbackOncePerBuild() {
        val project = ProjectBuilder.builder().withName("root").build()
        val develocity = FakeDevelocity()

        DevelocityWrapperConfiguration().configure(project, develocity.configuration)
        DevelocityWrapperConfiguration().configure(project, develocity.configuration)
        InfoGradleProcessReporting.configureConsole(project)

        assertEquals(1, develocity.buildFinishedRegistrations)
    }

    @Test
    fun configureOnceGuardIsScopedToTheBuild() {
        val firstBuild = ProjectBuilder.builder().withName("root").build()
        val secondBuild = ProjectBuilder.builder().withName("root").build()
        val child = ProjectBuilder.builder().withName("child").withParent(firstBuild).build()

        assertTrue(InfoGradleProcessReporting.claimConfiguration(firstBuild))
        assertFalse(InfoGradleProcessReporting.claimConfiguration(child))
        assertTrue(InfoGradleProcessReporting.claimConfiguration(secondBuild))
    }

    private class FakeDevelocity {
        var buildFinishedRegistrations = 0

        private val buildScan = proxy<BuildScanConfiguration> { method ->
            if (method.name == "buildFinished") buildFinishedRegistrations++
            null
        }

        val configuration = proxy<DevelocityConfiguration> { method ->
            if (method.name == "getBuildScan") buildScan else null
        }

        private inline fun <reified T> proxy(crossinline handler: (Method) -> Any?): T =
            Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
                handler(method)
            } as T
    }

    private fun build(gradleVersion: String): BuildResult = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments("help")
        .withPluginClasspath()
        .withGradleVersion(gradleVersion)
        .build()

    private fun assertConsoleTablePrintedOnce(gradleVersion: String, build: BuildResult) {
        assertEquals(
            "Gradle $gradleVersion should print exactly one console table:\n${build.output}",
            1,
            Regex("Gradle processes").findAll(build.output).count()
        )
    }
}
