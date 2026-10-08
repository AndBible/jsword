package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** The common replacements must behave exactly like the JDK calls they replace (conversion rules §4). */
class JavaCompatTest {
    private val strings = listOf("", "abc", "Ünïcödé", "Ω", "漢字", "emoji 😀", "\u0000ÿĀ",
        "a\uD83Db", "\uDE00x", "end\uD83D")

    @Test
    fun latin1BytesMatchesJavaEncoder() {
        for (s in strings) {
            assertContentEquals(s.toByteArray(Charsets.ISO_8859_1), s.latin1Bytes(), s)
        }
    }

    @Test
    fun latin1StringMatchesJavaDecoder() {
        val all = ByteArray(256) { it.toByte() }
        assertEquals(String(all, Charsets.ISO_8859_1), all.latin1String())
    }

    @Test
    fun splitLikeJavaMatchesPatternSplit() {
        val inputs = listOf("", "a", "a\nb", "a\nb\n", "a\n\n\n", "\n", "\n\n", "\na", "a\n\nb\n")
        for (input in inputs) {
            val java = java.util.regex.Pattern.compile("\n").split(input).toList()
            assertEquals(java, input.splitLikeJava(Regex("\n")), "input=${input.replace("\n", "\\n")}")
        }
    }
}
