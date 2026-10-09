package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginRegistryPinTest {
    // Snapshot of the .plugin class mappings as of jsword c460c2eb (the files are deleted in this task).
    private val expected: Map<String, List<Pair<String, String>>> = mapOf(
        "org.crosswire.jsword.book.BookDriver" to listOf("sword" to "org.crosswire.jsword.book.sword.SwordBookDriver"),
        "org.crosswire.jsword.book.filter.SourceFilter" to listOf(
            "default" to "org.crosswire.jsword.book.filter.plaintext.PlainTextFilter",
            "gbf" to "org.crosswire.jsword.book.filter.gbf.GBFFilter",
            "osis" to "org.crosswire.jsword.book.filter.osis.OSISFilter",
            "plaintext" to "org.crosswire.jsword.book.filter.plaintext.PlainTextFilter",
            "tei" to "org.crosswire.jsword.book.filter.osis.OSISFilter",
            "thml" to "org.crosswire.jsword.book.filter.thml.THMLFilter"),
        "org.crosswire.jsword.book.install.InstallerFactory" to listOf(
            "sword-http" to "org.crosswire.jsword.book.install.sword.HttpSwordInstallerFactory",
            "sword-https" to "org.crosswire.jsword.book.install.sword.HttpsSwordInstallerFactory"),
        "org.crosswire.jsword.book.Bookmark" to listOf("default" to "org.crosswire.jsword.book.basic.DefaultBookmark"),
        "org.crosswire.jsword.index.IndexManager" to listOf("default" to "org.crosswire.jsword.index.lucene.LuceneIndexManager"),
        "org.crosswire.jsword.index.search.Searcher" to listOf("default" to "org.crosswire.jsword.index.lucene.LuceneSearcher"),
        "org.crosswire.jsword.index.query.QueryBuilder" to listOf("default" to "org.crosswire.jsword.index.lucene.LuceneQueryBuilder"),
        "org.crosswire.jsword.index.query.QueryDecorator" to listOf("default" to "org.crosswire.jsword.index.lucene.LuceneQueryDecorator"),
        "org.crosswire.common.config.Choice" to listOf(
            "string" to "org.crosswire.common.config.StringChoice",
            "password" to "org.crosswire.common.config.StringChoice",
            "boolean" to "org.crosswire.common.config.BooleanChoice",
            "int-options" to "org.crosswire.common.config.IntOptionsChoice",
            "map-options" to "org.crosswire.common.config.MappedOptionsChoice",
            "string-options" to "org.crosswire.common.config.StringOptionsChoice",
            "string-array" to "org.crosswire.common.config.StringArrayChoice",
            "file" to "org.crosswire.common.config.FileChoice",
            "path" to "org.crosswire.common.config.PathChoice",
            "directory" to "org.crosswire.common.config.DirectoryChoice",
            "number" to "org.crosswire.common.config.NumberChoice",
            "font" to "org.crosswire.common.config.FontChoice",
            "class" to "org.crosswire.common.config.ClassChoice"),
    )

    @Test fun registryMatchesThePluginFiles() {
        for ((type, mappings) in expected) {
            val k = Class.forName(type).kotlin
            @Suppress("UNCHECKED_CAST")
            val actual = PluginRegistry.entries(k as kotlin.reflect.KClass<Any>).map { it.name to it.implClass.java.name }
            assertEquals(type, mappings, actual)
        }
    }
}
