package org.crosswire.common.util

import kotlin.reflect.KClass

/** One implementation registered under [name] for a plugin type. */
class PluginEntry<T : Any>(val name: String, val implClass: KClass<out T>, val create: () -> T)

class PluginRegistryBuilder internal constructor() {
    internal val map = LinkedHashMap<KClass<*>, MutableList<PluginEntry<*>>>()

    fun <T : Any> register(type: KClass<T>, name: String, implClass: KClass<out T>, create: () -> T) {
        map.getOrPut(type) { ArrayList() }.add(PluginEntry(name, implClass, create))
    }
}

/**
 * Static replacement for JSword's `.plugin` files and `Class.forName` (K2 F3). Built once, lazily;
 * registration order is the order of the old files, `default` included as an ordinary name.
 */
object PluginRegistry {
    private val registered: Map<KClass<*>, List<PluginEntry<*>>> by lazy {
        PluginRegistryBuilder().also { registerPlatformPlugins(it) }.map
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> entries(type: KClass<T>): List<PluginEntry<T>> = (registered[type] ?: emptyList()) as List<PluginEntry<T>>

    fun <T : Any> entry(type: KClass<T>, name: String): PluginEntry<T>? = entries(type).firstOrNull { it.name == name }

    fun <T : Any> default(type: KClass<T>): PluginEntry<T>? = entry(type, DEFAULT)

    const val DEFAULT = "default"
}

internal expect fun registerPlatformPlugins(builder: PluginRegistryBuilder)
