package io.github.cdsap.gradleprocess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BuildWorkflowProcessWatcherTest {

    @Test
    fun gradleBuildJobsUseBuildProcessWatcher() {
        val workflow = File(".github/workflows/build.yaml").readText()
        val watcherUses = Regex("""uses:\s*cdsap/build-process-watcher@v[\d.]+""")
            .findAll(workflow)
            .toList()

        assertEquals(
            "Expected build-process-watcher in prBranch and integrationJavaTests",
            2,
            watcherUses.size
        )
        assertTrue(workflow.contains("remote_monitoring: 'true'"))
        assertTrue(workflow.contains("export_to_bigquery: 'true'"))
    }
}
