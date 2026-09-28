package io.github.cdsap.gradleprocess

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Settings plugin entry point. Apply once from `settings.gradle(.kts)` so process
 * observation is configured for the whole build (including multi-project builds).
 */
class InfoGradleProcessPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        InfoGradleProcessReporting.configureFromSettings(target)
    }
}
