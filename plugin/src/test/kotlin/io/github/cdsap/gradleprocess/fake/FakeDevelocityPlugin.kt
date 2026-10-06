package io.github.cdsap.gradleprocess.fake

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.develocity.agent.gradle.scan.BuildResult
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * TestKit-only stand-in for the Develocity settings plugin, resolved under the real plugin id
 * `com.gradle.develocity` through `src/test/resources/META-INF/gradle-plugins/com.gradle.develocity.properties`.
 * The test resources directory must precede the real Develocity jar on the TestKit plugin
 * classpath so this descriptor wins; [APPLIED_MARKER] is printed so a wrong order fails loudly.
 *
 * Like real Develocity 4.x it is a `Plugin<Settings>` and registers the `develocity` extension
 * typed as the real [DevelocityConfiguration] on settings, so
 * `pluginManager.withPlugin("com.gradle.develocity")` and `getByType(DevelocityConfiguration)`
 * both see it. Mirroring real Develocity 4.x (verified against 4.6.0), the same instance is also
 * added as a `develocity` extension on the root project from a `gradle.rootProject {}` hook
 * registered in [apply]; `hasPlugin("com.gradle.develocity")` stays false on projects.
 *
 * Build Scan calls are printed instead of published, one line per call:
 * - `value(name, value)` -> `SCAN-VALUE <name>=<value>`
 * - `tag(tag)`           -> `SCAN-TAG <tag>`
 *
 * `buildScan.buildFinished` actions run when [BuildFinishedService] closes at build end. They
 * survive the configuration cache: the configuration-cache codecs encode them, and the proxies
 * they capture are rebuilt on load through `writeReplace`/`readResolve`, so they also run when
 * the cache entry is reused.
 */
abstract class FakeDevelocityPlugin @Inject constructor(
    private val listenerRegistry: BuildEventsListenerRegistry,
    private val objects: ObjectFactory,
) : Plugin<Settings> {

    override fun apply(settings: Settings) {
        println("$APPLIED_MARKER settings")
        val buildFinishedActions = mutableListOf<Any>()
        val service = settings.gradle.sharedServices.registerIfAbsent(
            "fakeDevelocityBuildFinished",
            BuildFinishedService::class.java,
        ) {
            parameters.actions = BuildFinishedActions(buildFinishedActions)
        }
        listenerRegistry.onTaskCompletion(service)

        val develocity = newConfiguration(objects, buildFinishedActions)
        settings.extensions.add(DevelocityConfiguration::class.java, "develocity", develocity)
        settings.gradle.rootProject {
            extensions.add(DevelocityConfiguration::class.java, "develocity", develocity)
        }
    }

    abstract class BuildFinishedService :
        BuildService<BuildFinishedService.Params>,
        OperationCompletionListener,
        AutoCloseable {
        interface Params : BuildServiceParameters {
            var actions: BuildFinishedActions
        }

        override fun onFinish(event: FinishEvent?) {}

        override fun close() {
            val buildResult = newProxy(BuildResult::class.java) { proxy, method, args ->
                if (method.name == "getFailures") emptyList<Throwable>() else objectMethodOrDefault(proxy, method, args)
            }
            parameters.actions.actions.forEach {
                @Suppress("UNCHECKED_CAST")
                (it as Action<Any>).execute(buildResult)
            }
        }
    }

    /**
     * Carries `buildFinished` actions from configuration to the end of the build.
     *
     * Gradle isolates build service parameters with Java serialization, which the plugin's
     * Kotlin lambdas cannot go through, so isolation keeps the actions in an in-JVM registry.
     * Configuration-cache writes hand the actions to the configuration-cache codecs instead,
     * which is what the real Develocity plugin relies on.
     */
    class BuildFinishedActions(@Transient var actions: List<Any>) : Serializable {
        private fun writeObject(output: ObjectOutputStream) {
            val configurationCache = output.javaClass.name.startsWith(CONFIGURATION_CACHE_STREAM_PACKAGE)
            output.writeBoolean(configurationCache)
            if (configurationCache) {
                output.writeObject(ArrayList(actions))
            } else {
                val key = UUID.randomUUID().toString()
                isolatedActions[key] = actions
                output.writeUTF(key)
            }
        }

        private fun readObject(input: ObjectInputStream) {
            @Suppress("UNCHECKED_CAST")
            actions = if (input.readBoolean()) {
                input.readObject() as List<Any>
            } else {
                isolatedActions.getValue(input.readUTF())
            }
        }

        private companion object {
            const val CONFIGURATION_CACHE_STREAM_PACKAGE = "org.gradle.internal.serialize.codecs."
            val isolatedActions = ConcurrentHashMap<String, List<Any>>()
        }
    }

    /** Lets the configuration cache replace the generated proxy classes with [ProxyReplacement]. */
    interface ReplaceableProxy {
        fun writeReplace(): Any
    }

    class ProxyReplacement(private val configuration: Boolean) : Serializable {
        private fun readResolve(): Any =
            if (configuration) newConfiguration(null, mutableListOf()) else newBuildScan(null, mutableListOf())
    }

    private class DevelocityHandler(
        private val objects: ObjectFactory?,
        private val buildScan: BuildScanConfiguration,
    ) : InvocationHandler {
        private val properties = mutableMapOf<String, Property<*>>()

        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? =
            when (method.name) {
                "getBuildScan" -> buildScan
                "buildScan" -> execute(args!![0], buildScan)
                "writeReplace" -> ProxyReplacement(configuration = true)
                else -> propertyOrDefault(objects, properties, proxy, method, args)
            }
    }

    private class BuildScanHandler(
        private val objects: ObjectFactory?,
        private val buildFinishedActions: MutableList<Any>,
    ) : InvocationHandler {
        private val properties = mutableMapOf<String, Property<*>>()

        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? =
            when (method.name) {
                "value" -> println("SCAN-VALUE ${args!![0]}=${args[1]}")
                "tag" -> println("SCAN-TAG ${args!![0]}")
                "buildFinished" -> {
                    buildFinishedActions.add(args!![0]!!)
                    null
                }
                "background" -> execute(args!![0], proxy)
                "writeReplace" -> ProxyReplacement(configuration = false)
                else -> propertyOrDefault(objects, properties, proxy, method, args)
            }
    }

    companion object {
        const val APPLIED_MARKER = "FAKE-DEVELOCITY applied to"

        private fun newConfiguration(objects: ObjectFactory?, buildFinishedActions: MutableList<Any>): DevelocityConfiguration =
            newProxy(DevelocityConfiguration::class.java, DevelocityHandler(objects, newBuildScan(objects, buildFinishedActions)))

        private fun newBuildScan(objects: ObjectFactory?, buildFinishedActions: MutableList<Any>): BuildScanConfiguration =
            newProxy(BuildScanConfiguration::class.java, BuildScanHandler(objects, buildFinishedActions))

        private fun <T> newProxy(type: Class<T>, handler: InvocationHandler): T =
            type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type, ReplaceableProxy::class.java), handler))

        private fun execute(action: Any?, target: Any): Any? {
            @Suppress("UNCHECKED_CAST")
            (action as Action<Any>).execute(target)
            return null
        }

        private fun propertyOrDefault(
            objects: ObjectFactory?,
            properties: MutableMap<String, Property<*>>,
            proxy: Any,
            method: Method,
            args: Array<out Any?>?,
        ): Any? {
            if (method.returnType != Property::class.java || objects == null) {
                return objectMethodOrDefault(proxy, method, args)
            }
            return properties.getOrPut(method.name) {
                val valueType = (method.genericReturnType as ParameterizedType).actualTypeArguments[0] as Class<*>
                objects.property(valueType)
            }
        }

        private fun objectMethodOrDefault(proxy: Any, method: Method, args: Array<out Any?>?): Any? =
            when (method.name) {
                "toString" -> "FakeDevelocity(${proxy.javaClass.interfaces.first().simpleName})"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.get(0)
                else -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    Integer.TYPE -> 0
                    java.lang.Long.TYPE -> 0L
                    else -> null
                }
            }
    }
}
