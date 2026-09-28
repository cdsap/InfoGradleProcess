package io.github.cdsap.gradleprocess

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProcessInfoProvidersTest {

    @Test
    fun createBuildsGradleJdkToolProviders() {
        val project = ProjectBuilder.builder().build()

        val providers = ProcessInfoProviders.create(project)

        assertNotNull(providers.jStat)
        assertNotNull(providers.jInfo)
    }
}
