package org.crosswire.common.util

import org.crosswire.jsword.index.IndexManager
import org.crosswire.jsword.index.UnsupportedIndexManager
import org.crosswire.jsword.index.query.QueryDecorator
import org.crosswire.jsword.index.query.UnsupportedQueryDecorator

// Book facade registrations remain platform work for K5b/K6.
internal actual fun registerPlatformPlugins(builder: PluginRegistryBuilder) {
    registerSourceFilters(builder)
    // K5b: search is unsupported on iOS until K6 (spec D1).
    builder.register(IndexManager::class, PluginRegistry.DEFAULT, UnsupportedIndexManager::class) { UnsupportedIndexManager() }
    builder.register(QueryDecorator::class, PluginRegistry.DEFAULT, UnsupportedQueryDecorator::class) { UnsupportedQueryDecorator() }
}
