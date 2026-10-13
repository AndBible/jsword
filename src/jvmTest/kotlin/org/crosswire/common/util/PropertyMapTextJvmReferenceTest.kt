package org.crosswire.common.util

import java.util.Properties
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyMapTextJvmReferenceTest {
    @Test fun loadRetainsIndependentPropertiesEnumerationOrder() {
        val bytes = (0..60).joinToString("\n") { "key$it=value$it" }.toByteArray(Charsets.ISO_8859_1)
        val reference = Properties().apply { load(bytes.inputStream()) }
        val actual = PropertyMap().apply { load(bytes) }
        assertEquals(reference.keys().toList(), actual.keys.toList())
        assertEquals(reference.entries.associate { it.key as String to it.value as String }, actual.toMap())
    }

    @Test fun storeUsesPropertiesEscapingCommentsAndTimestamp() {
        val map = PropertyMap().apply { put(" a:b=#!", " leading\t\n\r\u000c\\ä中") }
        val comments = "first\r\nsecond中"
        val reference = java.io.ByteArrayOutputStream().also { output ->
            Properties().apply { putAll(map) }.store(output, comments)
        }.toByteArray().toString(Charsets.ISO_8859_1).lines()
        val actual = map.store(comments).toString(Charsets.ISO_8859_1).lines()
        // The timestamp is deliberately not compared across separate clock reads.
        assertEquals(reference.filterNot { it.startsWith("#") && !it.startsWith("#first") && !it.startsWith("#second") },
            actual.filterNot { it.startsWith("#") && !it.startsWith("#first") && !it.startsWith("#second") })
        assertTrue(actual.any { it.startsWith("#") && !it.startsWith("#first") && !it.startsWith("#second") })
    }
}
