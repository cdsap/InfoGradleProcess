package io.github.cdsap.gradleprocess

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InfoGradleProcessPluginClasspathProbeTest {

    @Test
    fun develocityConfigurationIsVisibleWithoutPluginApplied() {
        // Guards the regression fixture: without this class on the test classpath,
        // Class.forName would be false and the old branch bug would not reproduce.
        Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")
    }

    @Test
    fun prefersConsoleWhenDevelocityJarPresentButExtensionAbsent() {
        val project = ProjectBuilder.builder().withName("root").build()

        assertNull(project.extensions.findByName("develocity"))
        Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")

        assertFalse(
            "classpath presence must not select the Develocity path",
            InfoGradleProcessReporting.shouldUseDevelocityReporting(project)
        )
    }

    @Test
    fun prefersDevelocityWhenExtensionPresentEvenIfAlsoOnClasspath() {
        val project = ProjectBuilder.builder().withName("root").build()
        project.extensions.add("develocity", Any())

        Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")

        assertTrue(InfoGradleProcessReporting.shouldUseDevelocityReporting(project))
    }
}
