package io.github.cdsap.gradleprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY_ENABLED = "infoGradleProcess.gbos.enabled"
    const val PROPERTY_DEVELOCITY = "infoGradleProcess.gbos.develocity.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.enabled.convention(
            providers.gradleProperty(PROPERTY_ENABLED).map(String::toBoolean).orElse(false),
        )
        extension.develocity.enabled.convention(
            providers.gradleProperty(PROPERTY_DEVELOCITY)
                .map(String::toBoolean)
                .orElse(extension.enabled),
        )
    }
}
