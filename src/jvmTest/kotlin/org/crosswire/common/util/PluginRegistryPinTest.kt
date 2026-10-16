package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginRegistryPinTest {
    // The .plugin class mappings of jsword c460c2eb, with the search entries pointing at the K6 FTS5 classes.
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
        "org.crosswire.jsword.index.IndexManager" to listOf("default" to "org.crosswire.jsword.index.fts.FtsIndexManager"),
        "org.crosswire.jsword.index.search.Searcher" to listOf("default" to "org.crosswire.jsword.index.search.DefaultSearcher"),
        "org.crosswire.jsword.index.query.QueryBuilder" to listOf("default" to "org.crosswire.jsword.index.query.DefaultQueryBuilder"),
        "org.crosswire.jsword.index.query.QueryDecorator" to listOf("default" to "org.crosswire.jsword.index.query.DefaultQueryDecorator"),
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
