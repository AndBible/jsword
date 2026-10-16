package org.crosswire.ksword.parity.search

import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import okio.Path.Companion.toOkioPath
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.index.fts.FtsIndexManager
import org.crosswire.jsword.index.Index
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * K6 task 13: sloppy phrases (`"a b"~N`) against the baseline `LuceneIndex.find` (Lucene 3.6.2), whole verse sets,
 * on KJV, ChiUns and the gap-prone Greek modules; plus a seeded randomized differential run over phrases taken from
 * real verses (words in any order, repeated words, slops 0..12).
 */
class SearchProximityOracleTest {
    private val dir = Files.createTempDirectory("k6-proximity").toFile()

    @AfterTest fun cleanup() { dir.deleteRecursively() }

    private fun manager() = FtsIndexManager(dir.resolve("search").toOkioPath(), dir.resolve("lucene").toOkioPath(),
        { b, k -> org.crosswire.jsword.book.BookData(b, k).getOsisFragment(false) })

    private fun book(initials: String): Book {
        SwordBookPathJvm.setAugmentPath(arrayOf(K6Corpus.root))
        return requireNotNull(Books.installed().getBook(initials)) { "no such book $initials" }
    }

    private fun baseline(initials: String, query: String): List<String>? {
        val out = OracleSearch.call("find", K6Corpus.root.path, initials, File(dir, "base-$initials").path, query)
        if (out.startsWith("!")) return null
        return out.lines().filter { it.isNotEmpty() }
    }

    private fun mine(index: Index, query: String): List<String>? = try {
        index.find(query).map { k: org.crosswire.jsword.passage.Key -> k.getOsisRef() }.sorted()
    } catch (e: BookException) { null }

    private inline fun withIndex(initials: String, body: (Index) -> Unit) {
        val b = book(initials)
        val built = OracleSearch.call("buildIndex", K6Corpus.root.path, initials, File(dir, "base-$initials").path)
        assertTrue(built.startsWith("ok "), built)
        val m = manager()
        m.scheduleIndexCreation(b)
        try { body(m.getIndex(b)) } finally { m.closeAllIndexes() }
    }

    private fun compare(initials: String, queries: List<String>) = withIndex(initials) { index ->
        val problems = ArrayList<String>()
        var nonEmpty = 0
        for (q in queries) {
            val expected = baseline(initials, q)
            val actual = mine(index, q)
            if (expected?.isNotEmpty() == true) nonEmpty++
            if (expected != actual) problems += "$initials '$q': baseline ${expected?.let { "${it.size} ${it.take(3)}" } ?: "FAIL"}, mine ${actual?.let { "${it.size} ${it.take(3)}" } ?: "FAIL"}"
        }
        println("K6 proximity $initials: ${queries.size} queries, $nonEmpty with matches, ${problems.size} divergences")
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test fun kjvShapes() = compare("KJV", KJV_SHAPES)

    @Test fun chiUnsShapes() = compare("ChiUns", listOf(
        "\"神 爱\"~0", "\"神 爱\"~1", "\"神 爱\"~3", "\"爱 神\"~1", "\"爱 神\"~2", "\"爱 神\"~5", "\"神爱\"~2", "\"神爱 世人\"~2", "\"世人 神爱\"~4", "\"起初 神\"~3", "\"神 创造\"~1", "\"神 创造\"~10",
        "\"神 神\"~2", "\"神 神\"~5", "\"神 光\"~6", "\"光 神\"~6", "\"神 说 光\"~4", "\"光 说 神\"~8", "\"神 神 神\"~4",
    ))

    @Test fun greekGapModules() {
        compare("GreVamvas", GREEK_SHAPES)
        compare("Byz", GREEK_SHAPES)
    }

    /** Words (or CJK characters) of a sample of verses; every query is built from one verse so most have matches. */
    private fun randomQueries(initials: String, count: Int, seed: Int, cjk: Boolean = false): List<String> {
        val rnd = Random(seed)
        val lines = OracleSearch.call("canonicalText", K6Corpus.root.path, initials, "37").lines().filter { it.contains('\t') }
        val queries = LinkedHashSet<String>()
        while (queries.size < count) {
            val text = lines[rnd.nextInt(lines.size)].substringAfter('\t')
            val words = if (cjk) text.filter { it.code in 0x4E00..0x9FFF }.map { it.toString() }
            else text.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
            if (words.size < 3) continue
            val n = if (rnd.nextInt(3) == 0) 3 else 2
            val start = rnd.nextInt(words.size)
            val window = (start until minOf(words.size, start + 12)).toList()
            val picks = ArrayList<Int>()
            repeat(n) { picks += window[rnd.nextInt(window.size)] } // may repeat a position: a repeated word
            val phrase = picks.joinToString(" ") { words[it] }
            queries += "\"$phrase\"~${rnd.nextInt(13)}"
        }
        return queries.toList()
    }

    @Test fun randomizedKjv() = compare("KJV", randomQueries("KJV", 400, 1306))

    @Test fun randomizedChiUns() = compare("ChiUns", randomQueries("ChiUns", 150, 1307, cjk = true))

    @Test fun randomizedGreek() {
        compare("GreVamvas", randomQueries("GreVamvas", 200, 1308))
        compare("Byz", randomQueries("Byz", 150, 1309))
    }

    /**
     * Oracle literals (baseline result sizes on KJV for slop 0..12): the reverse order needs a larger slop than the
     * forward order (`"light God"` first matches at slop 2 where `"God light"` already does at slop 1).
     */
    @Test fun kjvProximityCountsAreTheBaselineLiterals() = withIndex("KJV") { index ->
        val expected = mapOf(
            "God light" to listOf(0, 1, 6, 7, 9, 10, 12, 15, 16, 17, 18, 18, 19),
            "light God" to listOf(0, 0, 1, 2, 8, 10, 11, 13, 14, 16, 18, 19, 21),
            "holy holy" to listOf(2, 2, 3, 11, 18, 23, 26, 30, 32, 33, 36, 37, 38),
            "faith hope love" to listOf(0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2),
            "love hope faith" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2),
        )
        for ((phrase, counts) in expected) {
            val q = { n: Int -> "\"$phrase\"~$n" }
            assertEquals(counts, (0..12).map { baseline("KJV", q(it))!!.size }, "baseline $phrase")
            assertEquals(counts, (0..12).map { index.find(q(it)).getCardinality() }, "mine $phrase")
        }
    }

    /** Logged, not asserted (input for task 20): a proximity query on KJV, warm. */
    @Test fun proximityPerformanceIsLogged() = withIndex("KJV") { index ->
        for (q in listOf("\"God light\"~10", "\"faith hope\"~3", "\"the Lord\"~5", "\"love love\"~8", "\"in beginning God\"~4")) {
            index.find(q)
            val t0 = System.nanoTime()
            val n = index.find(q).getCardinality()
            println("K6 PERF proximity '$q' on KJV: $n verses, ${(System.nanoTime() - t0) / 1_000_000} ms (warm, second run)")
        }
    }

    companion object {
        val KJV_SHAPES: List<String> =
            (0..12).flatMap { n -> listOf("\"God light\"~$n", "\"light God\"~$n") } +
            (0..8).flatMap { n -> listOf("\"holy holy\"~$n", "\"love love\"~$n", "\"Lord God\"~$n", "\"God Lord\"~$n", "\"faith hope love\"~$n", "\"love hope faith\"~$n", "\"hope faith love\"~$n") } +
            listOf(
                "\"God light\"~100", "\"God light\"~1000", "\"light God\"~1000", "\"faith hope\"~3", "\"faith hope\"~1", "\"hope faith\"~2", "\"the Lord\"~5", "\"Lord the\"~5",
                "\"in the beginning\"~2", "\"beginning the in\"~6", "\"God God\"~3", "\"God God God\"~4", "\"God God God\"~8", "\"holy one holy\"~2", "\"holy holy one\"~3", "\"one holy holy\"~3",
                "\"a b c d e f g h i j k l m n o p q r s t u v w x y z\"~5", "\"love the Lord thy God\"~2", "\"God God Lord God\"~6", "\"the the\"~3", "\"of the of the\"~4",
                "\"well-known God\"~2", "\"zzzz God\"~5", "\"God zzzz\"~5", "+\"God light\"~5 -dark", "\"God light\"~5 AND \"faith hope\"~5", "\"Jesus Christ\"~2", "\"Christ Jesus\"~2", "\"Christ Jesus\"~3",
                "\"Jesus Jesus\"~1", "\"Jesus Jesus Jesus\"~1", "\"love one another\"~1", "\"another one love\"~5", "\"God said\"~0", "\"said God\"~1", "\"said God\"~2", "\"God\"~3", "\"the\"~2", "\"in the\"~0", "\"in the\"~1",
                "\"God that God\"~3", "\"Lord Lord Lord Lord\"~5", "\"Lord Lord Lord Lord\"~10", "\"Lord God Lord\"~5", "\"God Lord God\"~4", "\"God Lord God\"~6", "\"God Lord God Lord\"~7",
            )

        val GREEK_SHAPES: List<String> =
            listOf("θεός", "ο θεός", "ὁ θεὸς").flatMap { a -> listOf("λόγος", "ἀγάπη", "κύριος", "θεος").flatMap { b -> (0..5).flatMap { n -> listOf("\"$a $b\"~$n", "\"$b $a\"~$n") } } } +
            listOf("\"θεος θεος\"~2", "\"θεος θεος\"~6", "\"ο λογος\"~1", "\"ο λογος\"~3", "\"και ο\"~0", "\"και ο\"~2", "\"ο και\"~2", "\"ο και\"~4", "\"ο ο\"~2", "\"ο ο\"~5", "\"και και και\"~4", "\"και ο θεος\"~3", "\"θεος ο και\"~6")
    }
}
