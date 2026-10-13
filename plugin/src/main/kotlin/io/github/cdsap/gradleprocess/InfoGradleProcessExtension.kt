package io.github.cdsap.gradleprocess

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class InfoGradleProcessExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val gbos: GbosReportingExtension = objects.newInstance(GbosReportingExtension::class.java)

        fun gbos(action: Action<in GbosReportingExtension>) {
            action.execute(gbos)
        }
    }

abstract class GbosReportingExtension {
    abstract val develocity: Property<Boolean>
}
