package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.util.PropertyResourceBundle
import java.util.Properties

class PropertiesParserDifferentialTest {
    private fun jdk(text: String): Map<String, String> = Properties().apply { load(StringReader(text)) }.let { p -> p.stringPropertyNames().associateWith { p.getProperty(it) } }

    private val edges = listOf(
        "a=b", "a = b", "a:b", "a b", "a\tb", "a\u000Cb", "a=", "a", "=b", ":b",
        "a=b\\\n  c", "a=b\\\r\n\tc", "a=b\\\rc", "a=b\\\\\nc=d", "a=b\\\\\\\nc",
        "# c\na=b", "! c\na=b", "  # c\na=b", "a=b\\\n# not a comment",
        "a\\=b=c", "a\\:b=c", "a\\ b=c", "a\\\\=b",
        "a=\\u0041\\u00e4", "a=\\t\\n\\r\\f\\x", "a=b\\", "a=b\n\n\nc=d", "a=1\na=2",
        "  lead=space", "a=trailing  ", "a=b\r\nc=d", "a=b\rc=d", "﻿a=b",
        "# c\\\na=b", "a=b\n  # c\n  c=d", "a\\\n  b=c", "a=\\\n", "a \\\n = b", "a: = b", "a=b=c",
        "a\\u0041=x", "a=b\\\n\\\nc", "\\\n a=b",
    )

    @Test fun edgeCasesMatchJdk() {
        for (e in edges) assertEquals("case <$e>", jdk(e), PropertiesParser.parse(e))
    }

    @Test fun malformedUnicodeThrowsLikeJdk() {
        for (e in listOf("a=\\u00", "a=\\u00zz", "a=\\u")) {
            try { jdk(e); fail("jdk accepted $e") } catch (_: IllegalArgumentException) {}
            try { PropertiesParser.parse(e); fail("parser accepted $e") } catch (_: IllegalArgumentException) {}
        }
    }

    @Test fun everyEmbeddedPropertiesFileMatchesBothJdkPaths() {
        for (path in KswordResources.paths.filter { it.endsWith(".properties") || it.endsWith(".plugin") }) {
            val bytes = KswordResources.bytes(path)!!
            val rb = PropertyResourceBundle(ByteArrayInputStream(bytes)) // JDK 9+: UTF-8, ISO-8859-1 on malformed input
            assertEquals("bundle $path", rb.keySet().associateWith { rb.getString(it) }, PropertiesParser.parse(bytes, PropertiesCharset.UTF8_THEN_LATIN1))
            val p = Properties().apply { load(ByteArrayInputStream(bytes)) } // ISO-8859-1
            assertEquals("latin1 $path", p.stringPropertyNames().associateWith { p.getProperty(it) }, PropertiesParser.parse(bytes, PropertiesCharset.LATIN1))
        }
    }

    @Test fun malformedUtf8FallsBackToLatin1() {
        // "ä" in ISO-8859-1, invalid UTF-8. Followed by more text: the JDK decoder only falls back when the
        // bad byte is not the very last byte of the stream (a truncated trailing sequence throws in the JDK,
        // which the parser deliberately does not replicate; it falls back to ISO-8859-1 there too).
        for (tail in listOf("\n", "x\nb=c\n", " tail\n")) {
            val bytes = "a=".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0xE4.toByte()) + tail.toByteArray(Charsets.ISO_8859_1)
            val rb = PropertyResourceBundle(ByteArrayInputStream(bytes))
            assertEquals("tail <$tail>", rb.keySet().associateWith { rb.getString(it) }, PropertiesParser.parse(bytes, PropertiesCharset.UTF8_THEN_LATIN1))
        }
    }
}
