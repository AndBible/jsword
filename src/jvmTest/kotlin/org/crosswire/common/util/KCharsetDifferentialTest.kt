package org.crosswire.common.util

import java.nio.charset.Charset
import kotlin.random.Random
import kotlin.test.*

class KCharsetDifferentialTest {
    private val names = listOf("UTF-8", "ISO-8859-1", "WINDOWS-1252")
    private val charsets = names.associateWith { Charset.forName(it) }
    private fun checkDecode(b: ByteArray, cs: String, offset: Int = 0, length: Int = b.size) {
        val expected = String(b, offset, length, charsets.getValue(cs))
        val actual = KCharset.decode(b, offset, length, cs)
        if (expected != actual) assertEquals(expected, actual, "$cs ${b.toList()} offset=$offset length=$length")
    }

    @Test fun everySingleByteInEveryCharset() {
        assertEquals("17", System.getProperty("java.specification.version"), "Reference must be JDK 17")
        for (cs in names) for (v in 0..255) checkDecode(byteArrayOf(v.toByte()), cs)
    }

    @Test fun everyTwoAndThreeByteUtf8Sequence() {
        val start = System.nanoTime()
        val b2 = ByteArray(2)
        for (a in 0..255) for (b in 0..255) {
            b2[0] = a.toByte(); b2[1] = b.toByte()
            checkDecode(b2, "UTF-8")
        }
        val b3 = ByteArray(3)
        for (a in 0xc0..0xff) for (b in 0..255) for (c in 0..255) {
            b3[0] = a.toByte(); b3[1] = b.toByte(); b3[2] = c.toByte()
            checkDecode(b3, "UTF-8")
        }
        println("KCharset exhaustive 4,259,840 sequences: ${(System.nanoTime() - start) / 1_000_000} ms")
    }

    @Test fun randomLongerUtf8() {
        val r = Random(4)
        repeat(200_000) {
            val b = ByteArray(r.nextInt(1, 12)) {
                (if (r.nextInt(4) == 0) r.nextInt(0x80, 0x100) else r.nextInt(0, 0x100)).toByte()
            }
            checkDecode(b, "UTF-8")
        }
    }

    @Test fun offsetAndLengthAreHonoured() {
        val b = "xx\u00e9yy".toByteArray(Charsets.UTF_8)
        for (cs in names) for (offset in 0..b.size) for (length in 0..b.size - offset) {
            checkDecode(b, cs, offset, length)
        }
        for (cs in names) for ((offset, length) in listOf(-1 to 1, 0 to -1, 7 to 0, 5 to 2, Int.MAX_VALUE to 2, 1 to Int.MAX_VALUE)) {
            assertFailsWith<IndexOutOfBoundsException> { KCharset.decode(b, offset, length, cs) }
        }
    }

    private fun checkEncode(s: String, cs: String) {
        assertContentEquals(s.toByteArray(charsets.getValue(cs)), KCharset.encode(s, cs), "$cs ${s.map { it.code }}")
    }

    @Test fun encodeMatchesJavaIncludingUnmappable() {
        for (cs in names) {
            for (s in listOf("abc", "\u00e9", "\u201c\u201d\u2013\u2014\u2026", "\u20ac", "\u0100", "\ud83d\ude00", "\ud800", "a\u0301")) checkEncode(s, cs)
            for (v in 0..0xffff) checkEncode(v.toChar().toString(), cs)
            val r = Random(5)
            repeat(10_000) { checkEncode(CharArray(r.nextInt(0, 24)) { r.nextInt(0x10000).toChar() }.concatToString(), cs) }
        }
    }

    @Test fun aliasesAndCase() {
        for ((canonical, aliases) in mapOf("UTF-8" to listOf("UTF-8", "UTF8"), "ISO-8859-1" to listOf("ISO-8859-1", "ISO8859_1", "latin1", "8859_1"), "windows-1252" to listOf("WINDOWS-1252", "windows-1252", "Cp1252"))) {
            for (n in aliases.flatMap { listOf(it, it.lowercase(), it.uppercase()) }) {
                assertEquals(canonical, KCharset.canonicalName(n))
                val b = byteArrayOf(0x93.toByte())
                assertEquals(String(b, Charset.forName(n)), KCharset.decode(b, 0, 1, n))
                assertContentEquals("\u201c\ud800".toByteArray(Charset.forName(n)), KCharset.encode("\u201c\ud800", n))
            }
        }
    }

    @Test fun unknownAndNullNames() {
        for (name in listOf("EBCDIC-XYZ", "", " UTF-8", "UTF-8 ")) {
            assertNull(KCharset.canonicalName(name))
            assertEquals(name, assertFailsWith<UnsupportedCharsetException> { KCharset.decode(byteArrayOf(1), 0, 1, name) }.charsetName)
            assertEquals(name, assertFailsWith<UnsupportedCharsetException> { KCharset.encode("a", name) }.charsetName)
        }
        assertFailsWith<NullPointerException> { KCharset.decode(byteArrayOf(1), 0, 1, null) }
        assertFailsWith<NullPointerException> { KCharset.encode("a", null) }
    }
}
