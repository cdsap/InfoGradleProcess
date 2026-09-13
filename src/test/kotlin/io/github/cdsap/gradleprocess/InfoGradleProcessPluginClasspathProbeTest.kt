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

        assertNull(project.extensions.findByName(InfoGradleProcessPlugin.DEVELOCITY_EXTENSION_NAME))
        Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")

        assertFalse(
            "classpath presence must not select the Develocity path",
            InfoGradleProcessPlugin.shouldUseDevelocityReporting(project)
        )
    }

    @Test
    fun prefersDevelocityWhenExtensionPresentEvenIfAlsoOnClasspath() {
        val project = ProjectBuilder.builder().withName("root").build()
        project.extensions.add(InfoGradleProcessPlugin.DEVELOCITY_EXTENSION_NAME, Any())

        Class.forName("com.gradle.develocity.agent.gradle.DevelocityConfiguration")

        assertTrue(InfoGradleProcessPlugin.shouldUseDevelocityReporting(project))
    }
}
