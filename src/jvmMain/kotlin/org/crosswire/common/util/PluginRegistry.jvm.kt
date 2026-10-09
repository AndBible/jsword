package org.crosswire.common.util

import org.crosswire.common.config.BooleanChoice
import org.crosswire.common.config.Choice
import org.crosswire.common.config.ClassChoice
import org.crosswire.common.config.DirectoryChoice
import org.crosswire.common.config.FileChoice
import org.crosswire.common.config.FontChoice
import org.crosswire.common.config.IntOptionsChoice
import org.crosswire.common.config.MappedOptionsChoice
import org.crosswire.common.config.NumberChoice
import org.crosswire.common.config.PathChoice
import org.crosswire.common.config.StringArrayChoice
import org.crosswire.common.config.StringChoice
import org.crosswire.common.config.StringOptionsChoice
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.Bookmark
import org.crosswire.jsword.book.basic.DefaultBookmark
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
    builder.register(BookDriver::class, "sword", SwordBookDriver::class) { SwordBookDriver.instance() }

    builder.register(SourceFilter::class, "default", PlainTextFilter::class) { PlainTextFilter() }
    builder.register(SourceFilter::class, "gbf", GBFFilter::class) { GBFFilter() }
    builder.register(SourceFilter::class, "osis", OSISFilter::class) { OSISFilter() }
    builder.register(SourceFilter::class, "plaintext", PlainTextFilter::class) { PlainTextFilter() }
    builder.register(SourceFilter::class, "tei", OSISFilter::class) { OSISFilter() }
    builder.register(SourceFilter::class, "thml", THMLFilter::class) { THMLFilter() }

    builder.register(InstallerFactory::class, "sword-http", HttpSwordInstallerFactory::class) { HttpSwordInstallerFactory() }
    builder.register(InstallerFactory::class, "sword-https", HttpsSwordInstallerFactory::class) { HttpsSwordInstallerFactory() }

    builder.register(Bookmark::class, "default", DefaultBookmark::class) { DefaultBookmark() }
    builder.register(IndexManager::class, "default", LuceneIndexManager::class) { LuceneIndexManager() }
    builder.register(Searcher::class, "default", LuceneSearcher::class) { LuceneSearcher() }
    builder.register(QueryBuilder::class, "default", LuceneQueryBuilder::class) { LuceneQueryBuilder() }
    builder.register(QueryDecorator::class, "default", LuceneQueryDecorator::class) { LuceneQueryDecorator() }

    // Choices are constructed by ChoiceFactory through implClass (they need initialisation arguments).
    builder.register(Choice::class, "string", StringChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "password", StringChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "boolean", BooleanChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "int-options", IntOptionsChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "map-options", MappedOptionsChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "string-options", StringOptionsChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "string-array", StringArrayChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "file", FileChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "path", PathChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "directory", DirectoryChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "number", NumberChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "font", FontChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
    builder.register(Choice::class, "class", ClassChoice::class) { error("Choice is created by ChoiceFactory through implClass") }
}
