package io.github.cdsap.gradleprocess

import org.gradle.api.provider.ProviderFactory

internal object GbosOptIn {
    const val PROPERTY_JSON = "infoGradleProcess.gbos.json.enabled"
    const val PROPERTY_SCHEMA = "infoGradleProcess.gbos.schema.enabled"
    const val PROPERTY_NDJSON = "infoGradleProcess.gbos.ndjson.enabled"
    const val PROPERTY_DEVELOCITY = "infoGradleProcess.gbos.develocity.enabled"
    const val PROPERTY_LEGACY = "infoGradleProcess.gbos.legacy.enabled"

    fun configureConventions(
        extension: GbosReportingExtension,
        providers: ProviderFactory,
    ) {
        extension.json.convention(
            providers.gradleProperty(PROPERTY_JSON)
                .orElse(providers.gradleProperty(PROPERTY_SCHEMA))
                .map(String::toBoolean)
                .orElse(false),
        )
        extension.ndjson.convention(
            providers.gradleProperty(PROPERTY_NDJSON).map(String::toBoolean).orElse(false),
        )
        extension.develocity.convention(
            providers.gradleProperty(PROPERTY_DEVELOCITY).map(String::toBoolean).orElse(false),
        )
        extension.legacy.convention(
            providers.gradleProperty(PROPERTY_LEGACY).map(String::toBoolean).orElse(true),
        )
    }
}
