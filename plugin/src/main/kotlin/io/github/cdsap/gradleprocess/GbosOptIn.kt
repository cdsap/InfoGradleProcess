package io.github.cdsap.gradleprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY_JSON = "infoGradleProcess.gbos.json.enabled"
    const val PROPERTY_NDJSON = "infoGradleProcess.gbos.ndjson.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.json.convention(
            providers.gradleProperty(PROPERTY_JSON).map(String::toBoolean).orElse(false),
        )
        extension.ndjson.convention(
            providers.gradleProperty(PROPERTY_NDJSON).map(String::toBoolean).orElse(false),
        )
    }
}
