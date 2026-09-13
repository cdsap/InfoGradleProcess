package io.github.cdsap.gradleprocess

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class GradlePropertiesConfigurationCacheTest {

    @Test
    fun configurationCacheIsEnabledInProjectGradleProperties() {
        val gradleProperties = File("gradle.properties")
        assertTrue(
            "gradle.properties should exist at the project root (working directory when tests run)",
            gradleProperties.isFile
        )

        val properties = Properties().apply {
            gradleProperties.inputStream().use { load(it) }
        }

        assertEquals(
            "Project build should enable the configuration cache by default",
            "true",
            properties.getProperty("org.gradle.configuration-cache")
        )
    }
}
