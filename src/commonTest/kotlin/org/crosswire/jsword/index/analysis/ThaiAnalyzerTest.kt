package org.crosswire.jsword.index.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

/** [ThaiAnalyzer] against the Swift oracle `th` cases and the hand-written sentences (both from the Lucene/Temurin oracle). */
class ThaiAnalyzerTest {
    private val analyzer = ThaiAnalyzer()

    @Test fun swiftOracleThaiCases() {
        val cases = SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "th" }
        assertEquals(2, cases.size)
        for (c in cases) assertEquals(c.tokens, analyzer.analyze(c.input).map { it.text }, c.name)
    }

    @Test fun handWrittenSentencesMatchOracleTokens() {
        for (c in THAI_SENTENCE_CASES) {
            val tokens = analyzer.analyze(c.input)
            assertEquals(c.tokens, tokens.map { it.text }, c.input)
            assertEquals(c.tokens.indices.toList(), tokens.map { it.position }, "positions of ${c.input}")
        }
    }

    @Test fun emptyAndPunctuationOnlyYieldNoTokens() {
        assertEquals(emptyList(), analyzer.analyze(""))
        assertEquals(emptyList(), analyzer.analyze(" ,.; "))
    }

    @Test fun repeatedAndInterleavedCallsAreIndependent() {
        val a = THAI_SENTENCE_CASES[3]
        val b = THAI_SENTENCE_CASES[5]
        val first = analyzer.analyze(a.input)
        analyzer.analyze(b.input)
        assertEquals(first, analyzer.analyze(a.input))
    }

    /** Increments as the baseline oracle printed them (`analyze th "x <300 a> พระเจ้าทรงสร้างโลก y"`): x 1, pieces 2 each, y 1. */
    @Test fun overlongTokenGapIsRepeatedForEveryPieceOfTheNextThaiToken() {
        val long = "a".repeat(300)
        val tokens = analyzer.analyze("x $long พระเจ้าทรงสร้างโลก y")
        assertEquals(listOf("x", "พระเจ้า", "ทรง", "สร้าง", "โลก", "y"), tokens.map { it.text })
        assertEquals(listOf(0, 2, 4, 6, 8, 9), tokens.map { it.position })
    }

    @Test fun overlongTokenLeadingTheTextShiftsEveryPiece() {
        // Oracle `analyze th "<300 a> พระเจ้าทรงสร้างโลก"`: increments 2, 2, 2, 2.
        val tokens = analyzer.analyze("${"a".repeat(300)} พระเจ้าทรงสร้างโลก")
        assertEquals(listOf(1, 3, 5, 7), tokens.map { it.position })
    }
}
