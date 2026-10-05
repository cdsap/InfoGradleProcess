package io.github.cdsap.gradleprocess

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

/**
 * Entry point for `io.github.cdsap.gradleprocess`. Can be applied from
 * `settings.gradle(.kts)` (preferred, covers the whole build) or from a build script
 * (compatible with releases before 0.3.1, where this was a project plugin).
 */
class InfoGradleProcessPlugin : Plugin<Any> {
    override fun apply(target: Any) {
        when (target) {
            is Settings -> InfoGradleProcessReporting.configureFromSettings(target)
            is Project -> InfoGradleProcessReporting.configureFromProject(target)
            else -> throw GradleException("InfoGradleProcessPlugin can only be applied to Settings or Project")
        }
    }
}
