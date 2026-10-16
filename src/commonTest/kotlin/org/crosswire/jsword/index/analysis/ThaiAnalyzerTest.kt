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
}
