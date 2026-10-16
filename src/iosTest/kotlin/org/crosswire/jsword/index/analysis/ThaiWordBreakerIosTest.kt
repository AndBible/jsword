package org.crosswire.jsword.index.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the pure-Kotlin OpenJDK Thai break port on Kotlin/Native over the Swift oracle cases and 20+ hand-written
 * sentences whose expected tokens are oracle literals (Lucene 3.6.2 + java.text.BreakIterator on Temurin 17). The JVM
 * run of the same cases (ThaiAnalyzerTest) proves the literals; ThaiPortVsJdkTest proves the port against the JDK.
 */
class ThaiWordBreakerIosTest {
    @Test fun swiftOracleThaiCases() {
        val analyzer = ThaiAnalyzer()
        for (c in SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "th" }) {
            assertEquals(c.tokens, analyzer.analyze(c.input).map { it.text }, c.name)
        }
    }

    @Test fun handWrittenSentences() {
        assertTrue(THAI_SENTENCE_CASES.size >= 20)
        val analyzer = ThaiAnalyzer()
        for (c in THAI_SENTENCE_CASES) assertEquals(c.tokens, analyzer.analyze(c.input).map { it.text }, c.input)
    }

    @Test fun wordRangesTileTheText() {
        val text = "พระเจ้าทรงรักโลก"
        val ranges = ThaiWordBreaker.words(text)
        assertEquals(listOf("พระเจ้า", "ทรง", "รัก", "โลก"), ranges.map { text.substring(it.first, it.last + 1) })
        assertEquals(emptyList(), ThaiWordBreaker.words(""))
    }
}
