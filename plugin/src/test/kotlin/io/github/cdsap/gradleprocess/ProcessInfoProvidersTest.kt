package io.github.cdsap.gradleprocess

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
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

    @Test
    fun resolveKeepsJstatAndJinfoAsOneProcessInfoInput() {
        val project = ProjectBuilder.builder().build()
        val providers = ProcessInfoProviders(
            jStat = project.providers.provider { "jstat" },
            jInfo = project.providers.provider { "jinfo" }
        )

        assertEquals(ProcessInfo("jstat", "jinfo"), providers.resolve())
    }
}
