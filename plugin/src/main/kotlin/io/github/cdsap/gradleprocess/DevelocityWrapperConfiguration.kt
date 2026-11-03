package io.github.cdsap.gradleprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gradleprocess.output.DevelocityValues
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

class DevelocityWrapperConfiguration {

    fun configureFromSettings(settings: Settings, onMissing: () -> Unit) {
        var develocityConfigured = false

        settings.pluginManager.withPlugin(DEVELOCITY_PLUGIN_ID) {
            val develocity = settings.extensions.getByType(DevelocityConfiguration::class.java)
            settings.gradle.rootProject {
                configure(this, develocity)
            }
            develocityConfigured = true
        }

        settings.gradle.settingsEvaluated {
            if (develocityConfigured) {
                return@settingsEvaluated
            }
            if (settings.extensions.findByType(DevelocityConfiguration::class.java) != null) {
                return@settingsEvaluated
            }
            if (settings.pluginManager.hasPlugin(DEVELOCITY_PLUGIN_ID)) {
                return@settingsEvaluated
            }
            onMissing()
        }
    }

    fun configureIfPresent(target: Project) {
        val develocity = target.extensions.findByType(DevelocityConfiguration::class.java) ?: return
        configure(target, develocity)
    }

    fun configure(project: Project, buildScanExtension: DevelocityConfiguration) {
        if (!InfoGradleProcessReporting.claimConfiguration(project)) {
            return
        }
        val parallelProcessInfo = ParallelProcessInfoProviders.create(project)
        val gbosEnabled = InfoGradleProcessReporting.gbosDevelocityEnabled(project)
        InfoGradleProcessReporting.configureOutputService(project, consoleEnabled = false)

        buildScanExtension.buildScan.buildFinished {
            val processes = GradleProcessCollector().collectGradle(
                ParallelProcessInfoProviders.resolve(parallelProcessInfo)
            )
            val gbos = gbosEnabled.get()
            // GBOS opt-in replaces legacy Gradle-Process-* values; without it, legacy only.
            DevelocityValues(
                buildScanExtension,
                processes,
                gbos,
                legacyEnabled = !gbos,
            ).addProcessesInfoToBuildScan()
        }
    }

    companion object {
        private const val DEVELOCITY_PLUGIN_ID = "com.gradle.develocity"
    }
}
