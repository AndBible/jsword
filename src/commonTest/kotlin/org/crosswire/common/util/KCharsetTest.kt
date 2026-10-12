package org.crosswire.common.util

import kotlin.test.*

class KCharsetTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    // Captured with OpenJDK 17.0.20.1, not inferred from Unicode maximal-subpart rules.
    @Test fun fixedJdk17DecodeExpectations() {
        val cases = listOf(
            bytes(0xc3, 0xa9) to "\u00e9",
            bytes(0xe2, 0x82) to "\ufffd",
            bytes(0xed, 0xa0, 0x80) to "\ufffd",
            bytes(0xed, 0xa0) to "\ufffd",
            bytes(0xf0, 0x9f, 0x98, 0x80) to "\ud83d\ude00",
            bytes(0xc0, 0xaf) to "\ufffd\ufffd",
            bytes(0xe0, 0x80, 0x80) to "\ufffd\ufffd\ufffd",
            bytes(0xf0, 0x80, 0x80, 0x80) to "\ufffd\ufffd\ufffd\ufffd",
            bytes(0xf4, 0x90, 0x80, 0x80) to "\ufffd\ufffd\ufffd\ufffd"
        )
        for ((b, expected) in cases) assertEquals(expected, KCharset.decode(b, 0, b.size, "UTF-8"))
        assertEquals("\u201c\u201d", KCharset.decode(bytes(0x93, 0x94), 0, 2, "Cp1252"))
        assertEquals("\ufffd\ufffd\ufffd\ufffd\ufffd", KCharset.decode(bytes(0x81, 0x8d, 0x8f, 0x90, 0x9d), 0, 5, "windows-1252"))
    }

    @Test fun fixedJdk17EncodeExpectations() {
        assertContentEquals(bytes(0x93, 0x94), KCharset.encode("\u201c\u201d", "Cp1252"))
        assertContentEquals(bytes(0xc3, 0xa9), KCharset.encode("\u00e9", "UTF8"))
        assertContentEquals(bytes(0xf0, 0x9f, 0x98, 0x80), KCharset.encode("\ud83d\ude00", "UTF-8"))
        for (cs in listOf("UTF-8", "latin1", "Cp1252")) assertContentEquals(bytes(0x3f), KCharset.encode("\ud800", cs))
        for (cs in listOf("latin1", "Cp1252")) assertContentEquals(bytes(0x3f), KCharset.encode("\ud83d\ude00", cs))
    }

    @Test fun namesAndErrors() {
        assertEquals("UTF-8", KCharset.canonicalName("utf8"))
        assertEquals("ISO-8859-1", KCharset.canonicalName("8859_1"))
        assertEquals("windows-1252", KCharset.canonicalName("CP1252"))
        assertNull(KCharset.canonicalName("unknown"))
        assertFailsWith<UnsupportedCharsetException> { KCharset.decode(bytes(1), 0, 1, "unknown") }
        assertFailsWith<UnsupportedCharsetException> { KCharset.encode("", "unknown") }
        assertFailsWith<NullPointerException> { KCharset.decode(bytes(1), 0, 1, null) }
        assertFailsWith<NullPointerException> { KCharset.encode("", null) }
    }
}
