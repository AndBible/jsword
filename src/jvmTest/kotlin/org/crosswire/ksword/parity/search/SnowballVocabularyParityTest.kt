package org.crosswire.ksword.parity.search

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.index.analysis.LowerCaseLetterTokenizer
import org.crosswire.jsword.index.analysis.snowball.snowballStemmer
import org.crosswire.jsword.passage.Verse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * K6 Snowball parity: every distinct word the analyzer's tokenizer yields for a corpus module (what the stemmer is
 * really fed: lowercased letter runs) must stem to what the baseline Lucene 3.6.2 stemmer produces, asked through the
 * probe's `stem` op in batches of [batchSize].
 *
 * Modules with a Snowball language of their own (fi, fr, ru) are the gate for that language. The other seven stemmers
 * (and those three again) also run over the vocabulary of every Latin or Cyrillic corpus module, which is not their
 * language but is real text with real suffix patterns; the baseline is the judge, so the output need not be sensible.
 */
class SnowballVocabularyParityTest {
    private val batchSize = 2000

    private fun vocabulary(initials: String): Set<String> {
        SwordBookPathJvm.setAugmentPath(arrayOf(K6Corpus.root))
        val book: Book = requireNotNull(Books.installed().getBook(initials)) { "no such book $initials" }
        val words = HashSet<String>()
        for (key in book.getGlobalKeyList()) {
            if (key is Verse && key.verse == 0) continue
            val text = OSISUtil.getCanonicalText(BookData(book, key).getOsisFragment(false))
            for (t in LowerCaseLetterTokenizer.tokenize(text)) words.add(t.text)
        }
        return words
    }

    private fun check(stemmerName: String, words: Collection<String>, label: String) {
        val stemmer = requireNotNull(snowballStemmer(stemmerName))
        val sorted = words.sorted()
        assertTrue(sorted.size > 1000, "suspiciously small vocabulary for $label: ${sorted.size}")
        var different = 0
        val report = StringBuilder()
        for (batch in sorted.chunked(batchSize)) {
            val oracle = OracleSearch.call("stem", stemmerName, *batch.toTypedArray())
            assertTrue(!oracle.startsWith("!"), oracle.take(300))
            val expected = oracle.split('\n').dropLast(1)
            assertEquals(batch.size, expected.size, "oracle answer size for $label")
            for ((i, word) in batch.withIndex()) {
                val actual = stemmer.stem(word)
                if (actual != expected[i]) {
                    different++
                    if (different <= 20) report.append("  $word: oracle '${expected[i]}' current '$actual'\n")
                }
            }
        }
        if (different > 0) fail("$label: $different of ${sorted.size} words differ; first 20:\n$report")
    }

    @Test fun finnishVocabulary_FinPR() = check("Finnish", vocabulary("FinPR"), "Finnish/FinPR")

    @Test fun frenchVocabulary_FreCrampon() = check("French", vocabulary("FreCrampon"), "French/FreCrampon")

    @Test fun russianVocabulary_RusSynodal() = check("Russian", vocabulary("RusSynodal"), "Russian/RusSynodal")

    @Test fun everyStemmerOnLatinCorpusVocabulary() {
        val latin = HashSet<String>()
        for (m in listOf("KJV", "FinPR", "FreCrampon", "GerBoLut", "CzeBKR")) latin += vocabulary(m)
        for (name in listOf("Danish", "Dutch", "Finnish", "French", "Italian", "Norwegian", "Portuguese", "Spanish", "Swedish")) {
            check(name, latin, "$name/latin corpus")
        }
    }

    @Test fun everyStemmerOnCyrillicVocabulary() {
        val words = vocabulary("RusSynodal")
        for (name in listOf("Danish", "Dutch", "Finnish", "French", "Italian", "Norwegian", "Portuguese", "Russian", "Spanish", "Swedish")) {
            check(name, words, "$name/RusSynodal")
        }
    }
}
