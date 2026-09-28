package io.github.cdsap.gradleprocess

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gradleprocess.output.DevelocityValues
import io.github.cdsap.jdk.tools.parser.model.TypeProcess
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
        val processInfoProviders = ProcessInfoProviders.create(project)

        buildScanExtension.buildScan.buildFinished {
            val processes = GradleProcessCollector().collect(
                processInfoProviders.jStat.get(),
                processInfoProviders.jInfo.get(),
                TypeProcess.Kotlin
            )
            DevelocityValues(buildScanExtension, processes).addProcessesInfoToBuildScan()
        }
    }

    companion object {
        private const val DEVELOCITY_PLUGIN_ID = "com.gradle.develocity"
    }
}
