package io.github.cdsap.gradleprocess

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Alias kept for consumers of the `io.github.cdsap.gradleprocess.project` id.
 * [InfoGradleProcessPlugin] can itself be applied from a build script.
 *
 * Plugin id: `io.github.cdsap.gradleprocess.project`
 */
class InfoGradleProcessProjectPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        InfoGradleProcessReporting.configureFromProject(target)
    }
}
