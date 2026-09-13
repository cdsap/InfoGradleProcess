package io.github.cdsap.gradleprocess

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GradlePropertiesCachingTest {

    @Test
    fun buildCacheIsEnabledInGradleProperties() {
        val properties = File("gradle.properties").readText()
        assertTrue(
            "Expected org.gradle.caching=true so local and CI builds use the Gradle build cache",
            properties.lineSequence().any { it.trim() == "org.gradle.caching=true" }
        )
    }
}
