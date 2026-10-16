package org.crosswire.common.util

import kotlin.reflect.KClass
import org.crosswire.jsword.index.IndexManager
import org.crosswire.jsword.index.fts.FtsIndexManager
import org.crosswire.jsword.index.query.DefaultQueryBuilder
import org.crosswire.jsword.index.query.DefaultQueryDecorator
import org.crosswire.jsword.index.query.QueryBuilder
import org.crosswire.jsword.index.query.QueryDecorator
import org.crosswire.jsword.index.search.DefaultSearcher
import org.crosswire.jsword.index.search.Searcher

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

/** The search plugins (K6): SQLite FTS5 on every platform. Called by each platform's [registerPlatformPlugins]. */
internal fun registerSearchPlugins(builder: PluginRegistryBuilder) {
    builder.register(IndexManager::class, PluginRegistry.DEFAULT, FtsIndexManager::class) { FtsIndexManager() }
    builder.register(Searcher::class, PluginRegistry.DEFAULT, DefaultSearcher::class) { DefaultSearcher() }
    builder.register(QueryBuilder::class, PluginRegistry.DEFAULT, DefaultQueryBuilder::class) { DefaultQueryBuilder() }
    builder.register(QueryDecorator::class, PluginRegistry.DEFAULT, DefaultQueryDecorator::class) { DefaultQueryDecorator() }
}

internal expect fun registerPlatformPlugins(builder: PluginRegistryBuilder)
