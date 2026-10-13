package org.crosswire.common.util

import java.util.Properties
import org.junit.Test
import kotlin.test.*

class LegacyPropertyTextReferenceTest {
    @Test fun productionNativeCodecMatchesJdkParsingAndExplicitStore() {
        val cases = listOf(
            "#comment\r\nkey\\:part : café\ncontinued=one\\\r\n two\n",
            "duplicate=first\nduplicate=last\n=empty\nempty\n",
            "escaped\\ key=\\t\\n\\r\\f\\u0041\\q\n",
            "joined=x\\\n #not-comment\nslashes=two\\\\\nnext=x\n"
        )
        for (text in cases) {
            val bytes = text.toByteArray(Charsets.ISO_8859_1)
            val reference = Properties().apply { load(bytes.inputStream()) }
            val actual = LegacyPropertyText.entries(bytes).toMap()
            assertEquals(reference.entries.associate { it.key as String to it.value as String }, actual)
            val stored = LegacyPropertyText.bytes(actual, "first\nsecond中", "fixed timestamp")
            val loaded = Properties().apply { load(stored.inputStream()) }
            assertEquals(reference, loaded)
            assertTrue(stored.toString(Charsets.ISO_8859_1).contains("#fixed timestamp\n"))
        }
        for (bad in listOf("\\u123", "\\u12xz", "\\uu0041")) {
            assertFailsWith<IllegalArgumentException> { LegacyPropertyText.entries("key=$bad".encodeToByteArray()) }
        }
    }
}
