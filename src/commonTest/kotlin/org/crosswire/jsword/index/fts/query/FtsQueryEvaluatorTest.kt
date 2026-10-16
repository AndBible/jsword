package org.crosswire.jsword.index.fts.query

import okio.FileSystem
import okio.Path
import org.crosswire.jsword.index.analysis.AnalyzedToken
import org.crosswire.jsword.index.analysis.TextAnalyzer
import org.crosswire.jsword.index.fts.FtsIndexMetadata
import org.crosswire.jsword.index.fts.FtsIndexStore
import org.crosswire.jsword.index.fts.FTS_SCHEMA_VERSION
import org.crosswire.jsword.index.fts.SearchTokenCodec
import org.crosswire.jsword.index.fts.UnsupportedSearchSyntaxException
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FtsQueryEvaluatorTest {
    private val files = mutableListOf<Path>()
    private val stores = mutableListOf<FtsIndexStore>()
    private fun tmp(): Path =
        (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "fts-eval-${Random.nextLong().toString(16)}.db").also { files += it }

    @AfterTest fun cleanup() { stores.forEach { it.close() }; files.forEach { FileSystem.SYSTEM.delete(it) } }

    /**
     * Test analyzer: lowercased Latin words, every CJK character its own token (an mmseg-like several-tokens term),
     * and the stopword "the" removed but its position kept (a gap, like the Greek/Hebrew analyzers).
     */
    private val analyzer = TextAnalyzer { text ->
        val out = ArrayList<AnalyzedToken>()
        var pos = 0
        val word = StringBuilder()
        fun flush() {
            if (word.isEmpty()) return
            val w = word.toString().lowercase(); word.clear()
            if (w != "the") out += AnalyzedToken(w, pos)
            pos++
        }
        for (c in text) {
            when {
                c.isLetterOrDigit() && c.code < 0x2E80 -> word.append(c)
                c.isLetterOrDigit() -> { flush(); out += AnalyzedToken(c.toString(), pos++) }
                else -> flush()
            }
        }
        flush()
        out
    }

    private fun e(t: String) = SearchTokenCodec.encode(t)
    private fun tokens(text: String) = analyzer.analyze(text)

    private fun store(): FtsIndexStore {
        val s = FtsIndexStore.create(tmp())
        stores += s
        s.beginBuild()
        s.addEntry(1, "Gen.1.1", tokens("In the beginning God created"), listOf("H7225", "H0430"))
        s.addEntry(2, "Gen.1.2", tokens("God said let there be light"), listOf("H0430", "H0216"))
        s.addEntry(3, "Gen.1.3", tokens("light and darkness"), listOf("H0216"))
        s.addEntry(4, "John.3.16", tokens("神爱世人 God"), emptyList())
        s.addEntry(5, "John.3.17", tokens("神 世人"), emptyList())
        s.addEntry(6, "Ps.1.1", tokens("well known light"), emptyList())
        s.addEntry(7, "Ps.1.2", tokens("light well god known"), emptyList())
        s.writeMetadata(FtsIndexMetadata(FTS_SCHEMA_VERSION, "test", SearchTokenCodec.ID, "T", "1.0", "fp"))
        s.commitBuild()
        return s
    }

    private val s = store()
    private fun run(query: String): Set<Long> =
        FtsQueryEvaluator(s, analyzer).evaluate(LuceneSyntaxParser.parse(query, analyzer))

    @Test fun anyAllAndPhrase() {
        assertEquals(setOf(1L, 2L, 4L, 7L), run("god"))
        assertEquals(setOf(1L, 2L, 3L, 4L, 6L, 7L), run("god light"), "default operator is OR")
        assertEquals(setOf(2L, 7L), run("god AND light"))
        assertEquals(setOf(2L, 7L), run("+god +light"))
        assertEquals(setOf(2L, 7L), run("god && light"))
        assertEquals(setOf(1L, 2L, 3L, 4L, 6L, 7L), run("god || light"))
        assertEquals(setOf(6L), run("\"well known\""))
        assertEquals(emptySet(), run("\"known well\""))
        assertEquals(emptySet(), run("zzz"))
        assertEquals(setOf(1L, 2L, 3L, 4L, 6L, 7L), run("GOD Light"), "case is the analyzer's job")
    }

    @Test fun notAndPureNegative() {
        assertEquals(setOf(1L, 4L), run("god NOT light"))
        assertEquals(setOf(1L, 4L), run("god -light"))
        assertEquals(setOf(1L, 4L), run("+god -light"))
        assertEquals(setOf(1L, 4L), run("god AND NOT light"))
        assertEquals(emptySet(), run("-light"))
        assertEquals(emptySet(), run("NOT light"))
        assertEquals(emptySet(), run("!light"))
        assertEquals(emptySet(), run("-light -god"))
    }

    @Test fun shouldIsOptionalNextToMust() {
        assertEquals(setOf(2L, 3L, 6L, 7L), run("+light god"))
        assertEquals(setOf(2L, 3L, 6L, 7L), run("light OR +light"))
        assertEquals(setOf(2L, 3L), run("+light -well"))
    }

    @Test fun nestedParentheses() {
        assertEquals(setOf(2L, 3L, 7L), run("(god OR darkness) AND light"))
        assertEquals(setOf(1L, 2L, 3L, 7L), run("(god OR darkness) AND (light OR beginning)"))
        assertEquals(setOf(1L, 2L), run("god AND (beginning OR said)"))
        assertEquals(setOf(1L, 2L), run("((god AND (beginning OR said)))"))
        assertEquals(setOf(4L), run("god -(light OR beginning)"))
        assertEquals(emptySet(), run("god AND -(god)"))
        assertEquals(setOf(1L), run("+(god -light) +beginning"))
        assertEquals(emptySet(), run("(-god)"))
        assertEquals(emptySet(), run("god AND (-light)"))
    }

    @Test fun termThatTheAnalyzerSplitsIsAPhrase() {
        // LUCENE_29: autoGeneratePhraseQueries = true; checked against the baseline oracle (SearchQueryOracleSmokeTest).
        assertEquals(setOf(4L), run("神爱世人"))
        assertEquals(setOf(4L, 5L), run("神 世人"))
        assertEquals(setOf(4L), run("神爱"))
        assertEquals(setOf(6L), run("well-known"))
        assertEquals(setOf(6L), run("\"well known\""))
        assertEquals(setOf(4L), run("神爱 AND god"))
        assertEquals(setOf(5L), run("神世人"), "adjacent only in entry 5")
    }

    @Test fun strongsNumbers() {
        assertEquals(setOf(1L), run("strong:H07225"))
        assertEquals(setOf(1L), run("strong:h7225"))
        assertEquals(setOf(1L, 2L), run("strong:H430"))
        assertEquals(setOf(2L), run("strong:h430 AND light"))
        assertEquals(setOf(2L), run("strong:h430 AND strong:h216"))
        assertEquals(setOf(1L), run("strong:H430 -light"))
        assertEquals(emptySet(), run("strong:h7225 AND light"))
        assertEquals(setOf(1L, 2L, 3L), run("strong:h430 strong:h216"))
        assertEquals(emptySet(), run("strong:h99999"))
    }

    @Test fun anInvalidStrongsNumberIsDroppedLikeAnAnalyzedAwayTerm() {
        // The baseline strong analyzer drops an invalid number: the clause vanishes (a MUST does not empty the result).
        assertEquals(setOf(2L, 3L, 6L, 7L), run("light AND strong:xyz"))
    }

    @Test fun aPhraseNeverMatchesAcrossAPositionGap() {
        val g = FtsIndexStore.create(tmp()).also { stores += it }
        g.beginBuild()
        g.addEntry(1, "A.1.1", listOf(AnalyzedToken("a", 0), AnalyzedToken("b", 2)), emptyList()) // a, gap, b
        g.addEntry(2, "A.1.2", listOf(AnalyzedToken("a", 0), AnalyzedToken("b", 1)), emptyList())
        g.writeMetadata(FtsIndexMetadata(FTS_SCHEMA_VERSION, "test", SearchTokenCodec.ID, "T", "1.0", "fp"))
        g.commitBuild()
        val ev = FtsQueryEvaluator(g, analyzer)
        assertEquals(setOf(2L), ev.evaluate(SearchNode.Phrase("a b", 0)), "adjacent only")
        assertEquals(setOf(1L), ev.evaluate(SearchNode.Phrase("a the b", 0)), "a stopword keeps its position in the query too")
        assertEquals(setOf(1L, 2L), ev.evaluate(SearchNode.Bool(listOf(Clause(SearchNode.Term("a"), Occur.MUST), Clause(SearchNode.Term("b"), Occur.MUST)))))
    }

    @Test fun wildcardFuzzyAndProximityAreNotSilentlyWrong() {
        assertEquals(emptySet(), run("\"god light\"~2"))
        assertEquals(setOf(7L), run("\"god light\"~3"), "row 7 has light god: reverse order, one token between, slop 3")
        assertEquals(setOf(2L, 7L), run("\"god light\"~4"))
        assertEquals(setOf(7L), run("\"light god\"~1"))
        assertEquals(setOf(7L), run("\"light god\"~5"))
        assertEquals(setOf(2L, 7L), run("\"light god\"~6"))
        assertEquals(setOf(6L), run("\"well known\"~0"), "slop 0 is a plain phrase")
        assertEquals(setOf(2L, 3L, 6L, 7L), run("\"light\"~5"), "one token: a term, slop is irrelevant")
    }

    @Test fun wildcardsPrefixesAndFuzzy() {
        assertEquals(setOf(1L, 2L, 4L, 7L), run("god*"), "prefix")
        assertEquals(setOf(1L, 2L, 4L, 7L), run("g?d"))
        assertEquals(setOf(1L, 2L, 4L, 7L), run("g*d"))
        assertEquals(setOf(2L, 3L, 6L, 7L), run("*ight"))
        assertEquals(setOf(6L, 7L), run("w?ll*"))
        assertEquals(setOf(6L, 7L), run("k?own"))
        assertEquals(setOf(1L, 2L, 4L, 7L), run("GOD~"), "fuzzy, default 0.5: god is itself")
        assertEquals(setOf(2L), run("ligth~0.5 AND said"), "ligth is close to light")
        assertEquals(emptySet(), run("zzz*"))
        assertEquals(emptySet(), run("g??d"))
    }

    @Test fun strongsWildcards() {
        assertEquals(setOf(1L, 2L), run("strong:h04*"))
    }

    @Test fun strongsWildcardIsCaseInsensitive() {
        assertEquals(setOf(1L), run("strong:H72*"))
        assertEquals(setOf(1L), run("strong:h72*"))
        assertTrue(1L in run("strong:h*"))
    }

    @Test fun anEmptyQueryMatchesNothing() {
        assertEquals(emptySet(), run("\"\""))
        assertEquals(emptySet(), run("\\*"))
        assertEquals(emptySet(), run("+ "))
        assertEquals(setOf(2L, 7L), run("\\* AND light AND god"))
    }

    /**
     * The candidate query (FTS5 NEAR) must never lose a match: on random small rows with gaps (the analyzer drops "the"
     * but keeps its position) a sloppy phrase gives exactly the rows the matcher accepts, found by brute force.
     */
    @Test fun sloppyPhraseEqualsBruteForceOverAllRows() {
        val rnd = Random(1313)
        val vocab = listOf("a", "b", "c", "d", "the")
        val rs = FtsIndexStore.create(tmp()).also { stores += it }
        rs.beginBuild()
        for (id in 1L..300L) rs.addEntry(id, "X.1.$id", tokens(List(rnd.nextInt(1, 13)) { vocab[rnd.nextInt(vocab.size)] }.joinToString(" ")), emptyList())
        rs.writeMetadata(FtsIndexMetadata(FTS_SCHEMA_VERSION, "test", SearchTokenCodec.ID, "T", "1.0", "fp"))
        rs.commitBuild()
        val ev = FtsQueryEvaluator(rs, analyzer)
        var matched = 0
        repeat(400) {
            val text = List(rnd.nextInt(2, 6)) { vocab[rnd.nextInt(vocab.size)] }.joinToString(" ")
            val slop = rnd.nextInt(1, 9)
            val q = tokens(text)
            val expected = if (q.size < 2) null else rs.allRowIds().filter { SloppyPhraseMatcher.matches(q, slop, rs.tokensOf(it)) }.toSet()
            if (expected != null) {
                assertEquals(expected, ev.evaluate(SearchNode.Phrase(text, slop)), "\"$text\"~$slop")
                matched += expected.size
            }
        }
        check(matched > 500) { "the generated phrases should match something: $matched" }
    }
}
