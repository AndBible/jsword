package org.crosswire.common.util

import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.install.InstallerFactory
import org.crosswire.jsword.book.install.sword.HttpSwordInstallerFactory
import org.crosswire.jsword.book.install.sword.HttpsSwordInstallerFactory
import org.crosswire.jsword.book.sword.SwordBookDriver

internal actual fun registerPlatformPlugins(builder: PluginRegistryBuilder) {
    registerSourceFilters(builder)
    builder.register(BookDriver::class, "sword", SwordBookDriver::class) { SwordBookDriver.instance() }
    builder.register(InstallerFactory::class, "sword-http", HttpSwordInstallerFactory::class) { HttpSwordInstallerFactory() }
    builder.register(InstallerFactory::class, "sword-https", HttpsSwordInstallerFactory::class) { HttpsSwordInstallerFactory() }
    registerSearchPlugins(builder)
}
