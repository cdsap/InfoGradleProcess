package io.github.cdsap.gradleprocess

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Project-level compatibility shim for consumers who still apply the plugin from a
 * build script. Prefer [InfoGradleProcessPlugin] in `settings.gradle(.kts)`.
 *
 * Plugin id: `io.github.cdsap.gradleprocess.project`
 */
class InfoGradleProcessProjectPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        InfoGradleProcessReporting.configureFromProject(target)
    }
}
