package org.crosswire.ksword.parity.search

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.index.analysis.AnalyzedToken
import org.crosswire.jsword.index.analysis.BookAnalyzers
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.Verse
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * K6 analyzer parity over the real corpus: for every verse key of a module, the analyzer [BookAnalyzers.forBook] selects
 * must produce the token stream the baseline (Lucene 3.6.2) analyzer produces, and the canonical text both sides analyze
 * must be equal, so a filter difference (K5) is never blamed on an analyzer. Every corpus module has a test.
 */
class AnalyzerCorpusParityTest {
    /** Every n-th key per module is checked. 1 = all keys. */
    private val keyStride = 1

    private fun currentBook(initials: String): Book {
        SwordBookPathJvm.setAugmentPath(arrayOf(K6Corpus.root))
        return requireNotNull(Books.installed().getBook(initials)) { "no such book $initials in ${K6Corpus.root}" }
    }

    private fun isVerseZero(key: Key) = key is Verse && key.verse == 0

    /** The canonical text of every checked key, as the current code computes it: verse 0 skipped, then every stride-th. */
    private fun currentTexts(book: Book): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        var n = 0
        for (key in book.getGlobalKeyList()) {
            if (isVerseZero(key)) continue
            if (n++ % keyStride != 0) continue
            out += key.getOsisRef() to OSISUtil.getCanonicalText(BookData(book, key).getOsisFragment(false))
        }
        return out
    }

    /** The tokens of [key] as the current analyzer yields them. */
    internal fun currentTokensFor(book: Book, key: Key): List<AnalyzedToken> {
        return BookAnalyzers.forBook(book).analyze(OSISUtil.getCanonicalText(BookData(book, key).getOsisFragment(false)))
    }

    private fun escape(text: String) =
        text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    /** Renders tokens like the probe's analyzeBook: terms separated by space, `@n` when the position increment is not 1. */
    internal fun render(tokens: List<AnalyzedToken>): String {
        val sb = StringBuilder()
        var previous = -1
        for ((i, t) in tokens.withIndex()) {
            if (i > 0) sb.append(' ')
            sb.append(t.text)
            val increment = t.position - previous
            if (increment != 1) sb.append('@').append(increment)
            previous = t.position
        }
        return sb.toString()
    }

    private fun lines(s: String) = s.lines().filter { it.isNotEmpty() }

    private fun assertSame(what: String, oracle: List<String>, current: List<String>) {
        val oracleByKey = oracle.associate { it.substringBefore('\t') to it.substringAfter('\t', "") }
        val currentByKey = current.associate { it.substringBefore('\t') to it.substringAfter('\t', "") }
        val report = StringBuilder()
        var shown = 0
        var different = 0
        for (key in (oracleByKey.keys + currentByKey.keys)) {
            val a = oracleByKey[key]
            val b = currentByKey[key]
            if (a == b) continue
            different++
            if (shown < 20) {
                shown++
                report.append("  $key\n    oracle : $a\n    current: $b\n")
            }
        }
        if (different > 0) fail("$what: $different differing keys (oracle ${oracle.size}, current ${current.size}); first $shown:\n$report")
    }

    private fun check(initials: String) {
        val root = K6Corpus.root
        val book = currentBook(initials)
        val analyzer = BookAnalyzers.forBook(book)

        val oracleTexts = OracleSearch.call("canonicalText", root.path, initials, keyStride.toString())
        assertTrue(!oracleTexts.startsWith("!"), oracleTexts.take(300))
        val texts = currentTexts(book)
        assertTrue(texts.size > 1000, "suspiciously few keys in $initials: ${texts.size}")
        assertSame("canonical text of $initials", lines(oracleTexts), texts.map { it.first + "\t" + escape(it.second) })

        val oracleTokens = OracleSearch.call("analyzeBook", root.path, initials, keyStride.toString())
        assertTrue(!oracleTokens.startsWith("!"), oracleTokens.take(300))
        val current = texts.map { (ref, text) -> ref + "\t" + render(analyzer.analyze(text)) }
        assertSame("tokens of $initials", oracleTokens.lines().filter { it.isNotEmpty() }, current)
    }

    @Test fun everyKeyMatchesBaseline_KJV() = check("KJV")

    @Test fun everyKeyMatchesBaseline_MHC() = check("MHC")

    @Test fun everyKeyMatchesBaseline_GreVamvas() = check("GreVamvas")

    @Test fun everyKeyMatchesBaseline_Byz() = check("Byz")

    @Test fun everyKeyMatchesBaseline_HebModern() = check("HebModern")

    @Test fun everyKeyMatchesBaseline_FinPR() = check("FinPR")

    @Test fun everyKeyMatchesBaseline_FreCrampon() = check("FreCrampon")

    @Test fun everyKeyMatchesBaseline_RusSynodal() = check("RusSynodal")

    @Test fun everyKeyMatchesBaseline_GerBoLut() = check("GerBoLut")

    @Test fun everyKeyMatchesBaseline_AraSVD() = check("AraSVD")

    @Test fun everyKeyMatchesBaseline_FarOPV() = check("FarOPV")

    @Test fun everyKeyMatchesBaseline_CzeBKR() = check("CzeBKR")

    @Test fun everyKeyMatchesBaseline_ChiUns() = check("ChiUns")

    @Test fun everyKeyMatchesBaseline_ChiUn() = check("ChiUn")

    @Test fun everyKeyMatchesBaseline_JapKougo() = check("JapKougo")

    @Test fun everyKeyMatchesBaseline_ThaiKJV() = check("ThaiKJV")

    @Test fun everyKeyMatchesBaseline_WLC() = check("WLC")
}
