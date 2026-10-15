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
import org.crosswire.jsword.index.IndexManager
import org.crosswire.jsword.index.lucene.LuceneIndexManager
import org.crosswire.jsword.index.lucene.LuceneQueryBuilder
import org.crosswire.jsword.index.lucene.LuceneQueryDecorator
import org.crosswire.jsword.index.lucene.LuceneSearcher
import org.crosswire.jsword.index.query.QueryBuilder
import org.crosswire.jsword.index.query.QueryDecorator
import org.crosswire.jsword.index.search.Searcher

/**
 * JVM registrations: a transcription of the former `.plugin` class-mapping files, in file order.
 * Every implementation still lives in jvmMain, so all registrations are here.
 */
internal actual fun registerPlatformPlugins(builder: PluginRegistryBuilder) {
    registerSourceFilters(builder)
    builder.register(BookDriver::class, "sword", SwordBookDriver::class) { SwordBookDriver.instance() }


    builder.register(InstallerFactory::class, "sword-http", HttpSwordInstallerFactory::class) { HttpSwordInstallerFactory() }
    builder.register(InstallerFactory::class, "sword-https", HttpsSwordInstallerFactory::class) { HttpsSwordInstallerFactory() }

    builder.register(IndexManager::class, "default", LuceneIndexManager::class) { LuceneIndexManager() }
    builder.register(Searcher::class, "default", LuceneSearcher::class) { LuceneSearcher() }
    builder.register(QueryBuilder::class, "default", LuceneQueryBuilder::class) { LuceneQueryBuilder() }
    builder.register(QueryDecorator::class, "default", LuceneQueryDecorator::class) { LuceneQueryDecorator() }
}
