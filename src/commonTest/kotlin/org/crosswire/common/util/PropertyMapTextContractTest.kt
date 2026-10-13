package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PropertyMapTextContractTest {
    private fun latin1(text: String) = ByteArray(text.length) { text[it].code.toByte() }

    @Test fun loadDecodesLatin1EscapesSeparatorsAndContinuations() {
        val map = PropertyMap()
        map.load(latin1("  # ignored\r\n! ignored\n" +
            "plain = caf\u00e9\r" +
            "escaped\\ key\\:part : value\\=x\\t\\n\\r\\f\n" +
            "continued=first\\\r\n  second\\\n\tthird\n" +
            "unicode=\\u0041\\u00e4\\uD83D\\uDE00\n" +
            "unknown=\\q\nempty\n=empty-key\n"))
        assertEquals(mapOf("plain" to "caf\u00e9", "escaped key:part" to "value=x\t\n\r\u000c",
            "continued" to "firstsecondthird", "unicode" to "A\u00e4\uD83D\uDE00",
            "unknown" to "q", "empty" to "", "" to "empty-key"), map.toMap())
    }

    @Test fun continuationTreatsCommentMarkerAsValueAndDuplicateLastValueWins() {
        val map = PropertyMap().apply { put("retained", "old") }
        map.load(latin1("duplicate=first\nduplicate:last\njoined=value\\\n  #not-comment\n" +
            "slashes=two\\\\\nnext=independent\n"))
        assertEquals("last", map["duplicate"])
        assertEquals("value#not-comment", map["joined"])
        assertEquals("two\\", map["slashes"])
        assertEquals("independent", map["next"])
        assertEquals("old", map["retained"])
    }

    @Test fun malformedUnicodeLeavesExistingMapUnchanged() {
        for (bad in listOf("\\u123", "\\u12xz", "\\uu0041")) {
            val map = PropertyMap().apply { put("retained", "old") }
            assertFailsWith<IllegalArgumentException> { map.load(latin1("new=valid\nbad=$bad\n")) }
            assertEquals(mapOf("retained" to "old"), map.toMap())
        }
    }

    @Test fun storeRoundTripsEscapesAndExplicitEntriesWithoutDefaults() {
        val defaults = PropertyMap().apply { put("inherited", "not-stored") }
        val map = PropertyMap(defaults).apply {
            put(" leading:=#!\\", " leading\t\n\r\u000c\\\u00e4\u4e2d")
            put("empty", "")
        }
        val loaded = PropertyMap().apply { load(map.store("line one\nline two\u4e2d")) }
        assertEquals(map.toMap(), loaded.toMap())
        assertFalse(loaded.containsKey("inherited"))
        assertEquals("not-stored", map["inherited"])
    }
}
