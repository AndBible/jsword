package org.crosswire.jsword.index.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

class ClassicTokenizerTest {
    private val tokenizer = ClassicTokenizer()

    private fun texts(s: String) = tokenizer.tokenize(s).map { it.text }

    @Test
    fun swiftClassicScannerInputsMatchOracle() {
        // Oracle (Swift fixture, Lucene 3.6.2 StandardTokenizer(LUCENE_29)): the Hebrew case keeps case, so it shows raw scanner output.
        val raw = "שָׁלוֹם U.S.A. O'Reilly test@example.com 3.14 中文"
        assertEquals(
            listOf("ש", "לו", "ם", "U.S.A.", "O'Reilly", "test@example.com", "3.14", "中", "文"),
            texts(raw),
        )
    }

    @Test
    fun tokenTypes() {
        val types = tokenizer.tokenize("abc O'Neil's U.S.A. AT&T a@b.com foo.com 3.14 B-52 \u4E2D").map { it.type }
        assertEquals(
            listOf("<ALPHANUM>", "<APOSTROPHE>", "<ACRONYM>", "<COMPANY>", "<EMAIL>", "<HOST>", "<HOST>", "<NUM>", "<CJ>"),
            types,
        )
    }

    @Test
    fun invalidAcronymBecomesHostWithoutTrailingDot() {
        // Lucene 2.9 replaceInvalidAcronym: a host-like token that scanned a trailing dot loses the dot and becomes HOST.
        val t = tokenizer.tokenize("see foo.bar.")
        assertEquals(listOf("see", "foo.bar"), t.map { it.text })
        assertEquals("<HOST>", t[1].type)
        assertEquals(4, t[1].start)
        assertEquals(11, t[1].end)
    }

    @Test
    fun offsetsAreUtf16Ranges() {
        val t = tokenizer.tokenize("  ab cd")
        assertEquals(listOf(2 to 4, 5 to 7), t.map { it.start to it.end })
    }

    @Test
    fun overlongTokenIsDroppedAndCountsAsSkippedPosition() {
        val t = tokenizer.tokenize("a ${"b".repeat(256)} c")
        assertEquals(listOf("a", "c"), t.map { it.text })
        assertEquals(listOf(1, 2), t.map { it.positionIncrement })
    }

    @Test
    fun maxLengthTokenIsKept() {
        assertEquals(listOf("b".repeat(255)), texts("b".repeat(255)))
    }
}
