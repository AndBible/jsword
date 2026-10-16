package org.crosswire.jsword.index.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleAnalyzerTest {
    private val analyzer = SimpleAnalyzer()

    private fun terms(text: String): List<String> = analyzer.analyze(text).map { it.text }

    @Test
    fun swiftSimpleCasesMatchOracleTokens() {
        val cases = SWIFT_ORACLE_ANALYZER_CASES.filter { it.name == "simple" }
        assertEquals(1, cases.size)
        for (case in cases) {
            assertEquals(case.tokens, terms(case.input), "input ${case.input}")
        }
    }

    @Test
    fun foldsAccentsAndLowercasesWithPositionsFromZero() {
        assertEquals(
            listOf(AnalyzedToken("surely", 0), AnalyzedToken("cafe", 1)),
            analyzer.analyze("Sûrely Café"),
        )
    }

    @Test
    fun foldsOneUnitToSeveralCharacters() {
        // Oracle (Lucene 3.6.2): "Ж Ǆ ß Æ" -> ж, dz, ss, ae. Ǆ lowercases to ǆ, which folds to "dz".
        assertEquals(listOf("ж", "dz", "ss", "ae"), terms("Ж Ǆ ß Æ"))
    }

    @Test
    fun longRunIsCutAtLuceneMaxWordLength() {
        // Oracle: a 300-unit run gives a 255-unit token, then the remaining 45 units, both at increment 1.
        val tokens = analyzer.analyze("a".repeat(300))
        assertEquals(listOf("a".repeat(255), "a".repeat(45)), tokens.map { it.text })
        assertEquals(listOf(0, 1), tokens.map { it.position })
    }

    @Test
    fun exactlyMaxLengthRunIsOneToken() {
        assertEquals(listOf("a".repeat(255)), terms("a".repeat(255)))
    }

    @Test
    fun supplementaryCharacterSeparatesTokensAndIsDropped() {
        // Oracle: "𝔄bc" -> bc (increment 1). Lucene's CharTokenizer sees two surrogate units, not letters.
        assertEquals(listOf(AnalyzedToken("bc", 0)), analyzer.analyze("𝔄bc"))
        // "x𝔄y" -> x, y (oracle: x 1, y 1).
        assertEquals(listOf(AnalyzedToken("x", 0), AnalyzedToken("y", 1)), analyzer.analyze("x𝔄y"))
    }

    @Test
    fun loneSurrogateAndPunctuationProduceNoTokens() {
        // Oracle: "\uD835" alone yields no tokens.
        assertEquals(emptyList(), analyzer.analyze("\uD835"))
        assertEquals(emptyList(), analyzer.analyze("  ,.;!? 123 "))
        assertEquals(emptyList(), analyzer.analyze(""))
    }
}
