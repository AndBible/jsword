package org.crosswire.common.util

// Search and book facade registrations remain platform work for K5b/K6.
internal actual fun registerPlatformPlugins(builder: PluginRegistryBuilder) {
    registerSourceFilters(builder)
}
