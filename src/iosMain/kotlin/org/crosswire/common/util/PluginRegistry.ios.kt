package org.crosswire.common.util

import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.install.InstallerFactory
import org.crosswire.jsword.book.install.sword.HttpSwordInstallerFactory
import org.crosswire.jsword.book.install.sword.HttpsSwordInstallerFactory
import org.crosswire.jsword.book.sword.SwordBookDriver
import org.crosswire.jsword.index.IndexManager
import org.crosswire.jsword.index.UnsupportedIndexManager
import org.crosswire.jsword.index.query.QueryDecorator
import org.crosswire.jsword.index.query.UnsupportedQueryDecorator

internal actual fun registerPlatformPlugins(builder: PluginRegistryBuilder) {
    registerSourceFilters(builder)
    builder.register(BookDriver::class, "sword", SwordBookDriver::class) { SwordBookDriver.instance() }
    builder.register(InstallerFactory::class, "sword-http", HttpSwordInstallerFactory::class) { HttpSwordInstallerFactory() }
    builder.register(InstallerFactory::class, "sword-https", HttpsSwordInstallerFactory::class) { HttpsSwordInstallerFactory() }
    // K5b: search is unsupported on iOS until K6 (spec D1).
    builder.register(IndexManager::class, PluginRegistry.DEFAULT, UnsupportedIndexManager::class) { UnsupportedIndexManager() }
    builder.register(QueryDecorator::class, PluginRegistry.DEFAULT, UnsupportedQueryDecorator::class) { UnsupportedQueryDecorator() }
}
