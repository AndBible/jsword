package org.crosswire.jsword.index.fts.query

import org.crosswire.jsword.index.analysis.AnalyzedToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Expected values were checked against the baseline (Lucene 3.6.2) by `SearchProximityOracleTest`. */
class SloppyPhraseMatcherTest {
    private fun words(s: String) = s.split(' ')
    private fun m(phrase: String, slop: Int, text: String) = SloppyPhraseMatcher.matchesWords(words(phrase), slop, words(text))

    /** The smallest slop (0..20) at which the phrase matches, or null. */
    private fun minSlop(phrase: String, text: String): Int? = (0..20).firstOrNull { m(phrase, it, text) }

    @Test fun forwardOrderNeedsTheNumberOfInterveningTokens() {
        val verse = "god saw the light that it was good"
        assertEquals(2, minSlop("god light", verse))
        assertEquals(0, minSlop("god saw", verse))
        assertEquals(6, minSlop("god good", verse))
    }

    @Test fun reverseOrderNeedsInterveningTokensPlusTwo() {
        val verse = "god saw the light that it was good"
        assertEquals(4, minSlop("light god", verse))
        assertEquals(2, minSlop("saw god", verse)) // adjacent, reversed
        assertEquals(8, minSlop("good god", verse))
    }

    @Test fun slopZeroIsThePlainPhrase() {
        assertTrue(m("light that", 0, "the light that it was"))
        assertFalse(m("light it", 0, "the light that it was"))
        assertFalse(m("that light", 0, "the light that it was"))
    }

    @Test fun hugeSlopMatchesAnyOrderButNeedsAllTerms() {
        assertTrue(m("a b", 1000, "b x x x x x x a"))
        assertFalse(m("a b", 1000, "x x a x"))
    }

    @Test fun repeatedTermNeedsTwoDistinctOccurrences() {
        assertFalse(m("holy holy", 5, "the holy one"))
        assertTrue(m("holy holy", 0, "holy holy holy"))
        assertEquals(2, minSlop("holy holy", "holy one is holy"))
        assertEquals(0, minSlop("holy holy", "holy holy"))
    }

    @Test fun threeTermsAnyOrder() {
        val verse = "a x b y c"
        assertEquals(2, minSlop("a b c", verse))
        assertTrue(minSlop("c b a", verse)!! > 2)
        assertEquals(0, minSlop("a x b", verse))
    }

    @Test fun middleTermRepeated() {
        assertEquals(0, minSlop("a b b c", "a b b c"))
        assertEquals(true, minSlop("a b b c", "a b x b c")!! >= 1)
        assertEquals(null, minSlop("a b b c", "a b c"))
    }

    @Test fun phraseLongerThanTheVerseNeverMatches() {
        assertFalse(m("a b c d", 100, "a b c"))
    }

    @Test fun cjkTokensAreWordsToo() {
        val verse = "神 说 要 有 光"
        assertEquals(3, SloppyPhraseMatcher.matchesWords(listOf("神", "光"), 3, verse.split(' ')).let { if (it) 3 else -1 })
        assertFalse(SloppyPhraseMatcher.matchesWords(listOf("神", "光"), 2, verse.split(' ')))
    }

    @Test fun streamPositionsNotListIndexesDecideTheDistance() {
        // "a the b": the stop word is gone but its position stays: a=0, b=2.
        val doc = listOf(AnalyzedToken("a", 0), AnalyzedToken("b", 2))
        val q = listOf(AnalyzedToken("a", 0), AnalyzedToken("b", 1))
        assertFalse(SloppyPhraseMatcher.matches(q, 0, doc))
        assertTrue(SloppyPhraseMatcher.matches(q, 1, doc))
        // the query has the same gap: a the b
        val qGap = listOf(AnalyzedToken("a", 0), AnalyzedToken("b", 2))
        assertTrue(SloppyPhraseMatcher.matches(qGap, 0, doc))
        assertTrue(SloppyPhraseMatcher.matches(qGap, 1, doc))
        // reverse with the gap
        val rev = listOf(AnalyzedToken("b", 0), AnalyzedToken("a", 1))
        assertFalse(SloppyPhraseMatcher.matches(rev, 2, doc))
        assertTrue(SloppyPhraseMatcher.matches(rev, 3, doc))
    }

    @Test fun singleTokenPhraseIsAPlainTerm() {
        assertTrue(m("a", 3, "x a"))
        assertFalse(m("a", 3, "x y"))
    }

    @Test fun tokensAtSamePositionAreDetected() {
        assertTrue(SloppyPhraseMatcher.hasSamePosition(listOf(AnalyzedToken("a", 1), AnalyzedToken("b", 1))))
    }

    /** The simple formula is an upper bound of the scorer: `max(p_i - o_i) - min(p_i - o_i) <= slop`, distinct positions. */
    private fun formula(phrase: List<String>, slop: Int, doc: List<String>): Boolean {
        val chosen = IntArray(phrase.size)
        fun rec(i: Int): Boolean {
            if (i == phrase.size) {
                val d = chosen.indices.map { chosen[it] - it }
                return d.max() - d.min() <= slop
            }
            for (p in doc.indices) if (doc[p] == phrase[i] && (0 until i).none { chosen[it] == p }) { chosen[i] = p; if (rec(i + 1)) return true }
            return false
        }
        return rec(0)
    }

    @Test fun scorerNeverMatchesWhatTheFormulaRejectsAndIsClose() {
        val rnd = kotlin.random.Random(13)
        val vocab = listOf("a", "b", "c", "d")
        var both = 0
        var onlyFormula = 0
        repeat(4000) {
            val doc = List(rnd.nextInt(2, 11)) { vocab[rnd.nextInt(vocab.size)] }
            val phrase = List(rnd.nextInt(2, 5)) { vocab[rnd.nextInt(vocab.size)] }
            val slop = rnd.nextInt(0, 7)
            val port = SloppyPhraseMatcher.matchesWords(phrase, slop, doc)
            val f = formula(phrase, slop, doc)
            if (port) assertTrue(f, "scorer matched but formula did not: $phrase~$slop on $doc")
            if (port) both++ else if (f) onlyFormula++
        }
        println("K6 matcher vs formula: $both both, $onlyFormula only the formula (Lucene scorer quirks)")
    }
}
