package io.github.cdsap.gradleprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY_DEVELOCITY = "infoGradleProcess.gbos.develocity.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.develocity.convention(
            providers.gradleProperty(PROPERTY_DEVELOCITY).map(String::toBoolean).orElse(false),
        )
    }
}
