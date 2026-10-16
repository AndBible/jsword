package org.crosswire.jsword.index.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

class GreekHebrewAnalyzerTest {
    @Test
    fun swiftGreekCasesMatchOracle() {
        val cases = SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "el" }
        assertEquals(2, cases.size)
        for (c in cases) assertEquals(c.tokens, GreekAnalyzer().analyze(c.input).map { it.text }, c.name)
    }

    @Test
    fun swiftHebrewCasesMatchOracle() {
        val cases = SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "he" }
        assertEquals(2, cases.size)
        for (c in cases) assertEquals(c.tokens, HebrewAnalyzer().analyze(c.input).map { it.text }, c.name)
    }

    @Test
    fun greekFoldsFinalSigmaAndTonos() {
        assertEquals(listOf("αγαπη", "θεοσ"), GreekAnalyzer().analyze("Αγάπη Θεός").map { it.text })
    }

    @Test
    fun greekPositionsSkipOverlongTokens() {
        val t = GreekAnalyzer().analyze("α ${"β".repeat(256)} γ")
        assertEquals(listOf(0, 2), t.map { it.position })
    }

    @Test
    fun hebrewNiqqudAndCantillationAreStrippedAndTokenSplitsAtMarks() {
        // Oracle (Lucene 3.6.2 on the baseline chain): combining marks are not letters for the classic scanner and
        // split the word, then the pointing filter has nothing left to strip.
        assertEquals(
            listOf("ב", "ר", "אש", "ית"),
            HebrewAnalyzer().analyze("בְּרֵאשִׁית").map { it.text },
        )
    }

    @Test
    fun acronymKeepsDotsAndGreekLowercases() {
        assertEquals(listOf("u.s.a."), GreekAnalyzer().analyze("U.S.A.").map { it.text })
        assertEquals(listOf("U.S.A."), HebrewAnalyzer().analyze("U.S.A.").map { it.text })
    }

    @Test
    fun apostropheWordStaysOneToken() {
        assertEquals(listOf("o'neil's"), GreekAnalyzer().analyze("O'Neil's").map { it.text })
    }
}
