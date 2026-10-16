package org.crosswire.common.util

import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.book.filter.gbf.GBFFilter
import org.crosswire.jsword.book.filter.osis.OSISFilter
import org.crosswire.jsword.book.filter.plaintext.PlainTextFilter
import org.crosswire.jsword.book.filter.thml.THMLFilter
import org.crosswire.jsword.book.install.InstallerFactory
import org.crosswire.jsword.book.install.sword.HttpSwordInstallerFactory
import org.crosswire.jsword.book.install.sword.HttpsSwordInstallerFactory
import org.crosswire.jsword.book.sword.SwordBookDriver

/**
 * JVM registrations: a transcription of the former `.plugin` class-mapping files, in file order.
 * Search (FTS5) is registered by the common [registerSearchPlugins].
 */
internal actual fun registerPlatformPlugins(builder: PluginRegistryBuilder) {
    registerSourceFilters(builder)
    builder.register(BookDriver::class, "sword", SwordBookDriver::class) { SwordBookDriver.instance() }


    builder.register(InstallerFactory::class, "sword-http", HttpSwordInstallerFactory::class) { HttpSwordInstallerFactory() }
    builder.register(InstallerFactory::class, "sword-https", HttpsSwordInstallerFactory::class) { HttpsSwordInstallerFactory() }

    registerSearchPlugins(builder)
}
