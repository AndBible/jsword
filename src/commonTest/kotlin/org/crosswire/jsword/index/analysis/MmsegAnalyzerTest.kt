package org.crosswire.jsword.index.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Expected values are the baseline (mmseg4j 1.8.6 `ComplexAnalyzer` inside Lucene 3.6.2) outputs of the oracle
 * probe `analyze`, captured once. Every token has position increment 1.
 */
class MmsegAnalyzerTest {
    private fun terms(text: String): List<String> = MmsegAnalyzer().analyze(text).map { it.text }

    @Test fun swiftOracleCases() {
        SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "zh" || it.languageCode == "ja" }.also {
            assertEquals(2, it.size)
        }.forEach { assertEquals(it.tokens, terms(it.input), it.name) }
    }

    @Test fun mixedCjkLatinAndDigits() {
        // oracle: "耶稣说ABC 123" -> 耶稣 说 abc 123
        assertEquals(listOf("耶稣", "说", "abc", "123"), terms("耶稣说ABC 123"))
    }

    @Test fun fullWidthLettersAndDigitsAreNarrowedToAscii() {
        // oracle: "１２３ＡＢＣ２４ ｆｕｌｌ" -> 123abc24 full
        assertEquals(listOf("123abc24", "full"), terms("１２３ＡＢＣ２４ ｆｕｌｌ"))
    }

    @Test fun unitCharacterAfterDigitsIsSplitOff() {
        // oracle: "123个 5斤abc" -> 123 个 5 斤 abc
        assertEquals(listOf("123", "个", "5", "斤", "abc"), terms("123个 5斤abc"))
    }

    @Test fun longRunOfUnknownCharactersBecomesSingleCharacterTokens() {
        // oracle: U+3400..U+340E (CJK extension A, not in the dictionary) -> one token per character
        val text = (0x3400..0x340E).map { it.toChar() }.joinToString("")
        assertEquals(text.map { it.toString() }, terms(text))
    }

    @Test fun numbersLettersOfOtherScriptsAndDroppedCharacters() {
        // oracle: "Ⅷ② ½ ǅx" -> ⅷ ② ½ x (letter number lowercased; title-case ǅ is dropped)
        assertEquals(listOf("ⅷ", "②", "½", "x"), terms("Ⅷ② ½ ǅx"))
        // oracle: "Привет Ёлка αβγ Ωμέγα ñ" -> привет ёлка αβγ ωμ γα (έ and ñ end or start no run)
        assertEquals(listOf("привет", "ёлка", "αβγ", "ωμ", "γα"), terms("Привет Ёлка αβγ Ωμέγα ñ"))
    }

    @Test fun punctuationAndSpacesSeparateRuns() {
        // oracle: "耶稣，基督 ，神" -> 耶稣 基督 神
        assertEquals(listOf("耶稣", "基督", "神"), terms("耶稣，基督 ，神"))
    }

    @Test fun positionsAreConsecutive() {
        assertEquals(listOf(0, 1, 2, 3), MmsegAnalyzer().analyze("耶稣说ABC 123").map { it.position })
    }
}
