package org.crosswire.ksword.parity.search

import java.io.File
import java.nio.file.Files
import okio.Path.Companion.toOkioPath
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.index.fts.FtsIndexManager
import org.crosswire.jsword.index.fts.UnsupportedSearchSyntaxException
import org.crosswire.jsword.index.fts.query.QueryParseException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * K6 task 11 smoke test: the new Lucene-syntax parser and evaluator against the baseline `LuceneIndex.find`
 * (Lucene 3.6.2) on KJV and ChiUns, for the query shapes of this task. The full corpus parity is task 14.
 *
 * Every query of [PARITY_QUERIES] must give the same verse set, or fail in both (the baseline with a
 * `BookException`, this implementation with a `BookException` too, never an untyped exception). The queries of
 * [UNSUPPORTED_QUERIES] are K6-3 forms the baseline accepts but this implementation rejects with
 * [UnsupportedSearchSyntaxException]; [PENDING_QUERIES] are wildcard/fuzzy/proximity forms that tasks 12/13 will
 * evaluate (until then they must throw [UnsupportedSearchSyntaxException], never return a wrong set).
 */
class SearchQueryOracleSmokeTest {
    private val dir = Files.createTempDirectory("k6-query-smoke").toFile()

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

    private fun mine(index: org.crosswire.jsword.index.Index, query: String): List<String>? = try {
        index.find(query).map { k: org.crosswire.jsword.passage.Key -> k.getOsisRef() }.sorted()
    } catch (e: UnsupportedSearchSyntaxException) {
        throw e
    } catch (e: BookException) {
        null
    }

    private fun run(initials: String, parity: List<String>) {
        val b = book(initials)
        val built = OracleSearch.call("buildIndex", K6Corpus.root.path, initials, File(dir, "base-$initials").path)
        assertTrue(built.startsWith("ok "), built)
        val m = manager()
        m.scheduleIndexCreation(b)
        val index = m.getIndex(b)
        val problems = ArrayList<String>()
        for (q in parity) {
            val expected = baseline(initials, q)
            val actual = try { mine(index, q) } catch (e: UnsupportedSearchSyntaxException) { problems += "$initials '$q': unexpected ${e.message}"; continue }
            if (expected != actual) {
                problems += "$initials '$q': baseline ${expected?.let { "${it.size} ${it.take(3)}" } ?: "FAIL"}, mine ${actual?.let { "${it.size} ${it.take(3)}" } ?: "FAIL"}"
            }
        }
        m.closeAllIndexes()
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test fun kjvParity() = run("KJV", PARITY_QUERIES)

    @Test fun chiUnsParity() = run("ChiUns", CJK_QUERIES + CJK_EXPANSION_QUERIES)

    /** Task 12: wildcards, prefixes, fuzzy and `strong:` wildcards compared with the baseline on KJV (the Strong's module). */
    @Test fun kjvExpansionParity() = run("KJV", EXPANSION_QUERIES)

    /** The expansion runs against stems in a Snowball-language index (Lucene's behaviour): same verse sets as the baseline. */
    @Test fun finnishExpansionParity() = run("FinPR", listOf("usko*", "*us", "jumal*", "Jumala~", "uskon~0.6", "?", "*", "armo?", "jee*", "Ä*", "ä*", "*ä", "isä*", "k?rkko*", "uskoa~", "j\\u0075mal*", "rakkau*", "*kkaus", "rakkaude~0.7", "usk\\*", "usk\\**"))

    @Test fun russianExpansionParity() = run("RusSynodal", listOf("бог*", "Бог*", "*ь", "Бог~", "любов?", "?", "*", "бо?", "Господ*", "господ~0.6", "*ов*", "люб\\?*", "\\u0431ог*"))

    @Test fun germanExpansionParity() = run("GerBoLut", listOf("gott*", "Gott~", "*ung", "ü*", "üb?r", "?", "*", "liebe~0.6", "lieb*", "ge?et*", "Ö*", "gläub*"))

    @Test fun greekExpansionParity() = run("GreVamVas", listOf("λογ*", "*ος", "Λογ*", "θε?ς", "θεος~", "θεός~0.6", "?", "*", "αγαπ*", "*ω"))

    /** Linear-ish: 1,000 clauses work, 1,025 fail as in Lucene (`TooManyClauses`); nesting up to the parser's bound works. */
    @Test fun longAndDeeplyNestedQueries() = run("KJV", listOf(
        (1..1000).joinToString(" ") { "faith" }, (1..1024).joinToString(" AND ") { "faith" }, (1..1025).joinToString(" ") { "faith" },
        (1..3000).joinToString(" ") { "faith" },
        "(".repeat(150) + "faith" + ")".repeat(150),
        "(".repeat(150) + "faith" + ")".repeat(149),
        "faith " + "(hope ".repeat(100) + "love" + ")".repeat(100),
    ))

    /** Nesting beyond the bound is a typed failure here; the baseline overflows its stack only beyond about 3,000 levels. */
    @Test fun absurdNestingIsATypedFailure() {
        val b = book("KJV")
        val m = manager()
        m.scheduleIndexCreation(b)
        val index = m.getIndex(b)
        for (n in listOf(5_000, 100_000)) {
            val e = org.junit.Assert.assertThrows(BookException::class.java) { index.find("(".repeat(n) + "faith" + ")".repeat(n)) }
            assertTrue(e.cause is QueryParseException, "${e.cause}")
        }
        m.closeAllIndexes()
    }

    /** Step 2 of task 12 (logged, not asserted; input for task 20): the cost of the broadest expansions on KJV. */
    @Test fun expansionPerformanceIsLogged() {
        val b = book("KJV")
        val m = manager()
        m.scheduleIndexCreation(b)
        val index = m.getIndex(b)
        for (q in listOf("a*", "*e*", "*", "believeth~0.1", "bless*")) {
            index.find(q) // warm: vocabulary read, page cache
            val t0 = System.nanoTime()
            val n = index.find(q).getCardinality()
            println("K6 PERF expansion '$q' on KJV: $n verses, ${(System.nanoTime() - t0) / 1_000_000} ms (warm, second run)")
        }
        m.closeAllIndexes()
    }

    @Test fun unsupportedAndPendingFormsAreRejectedNotWrong() {
        val b = book("KJV")
        val m = manager()
        m.scheduleIndexCreation(b)
        val index = m.getIndex(b)
        for (q in UNSUPPORTED_QUERIES + PENDING_QUERIES) {
            try {
                val r = index.find(q)
                fail("'$q' should be unsupported but found ${r.getCardinality()}")
            } catch (e: UnsupportedSearchSyntaxException) {
                // expected
            } catch (e: BookException) {
                fail("'$q' failed with a BookException, expected UnsupportedSearchSyntaxException: ${e.cause}")
            }
        }
        m.closeAllIndexes()
    }

    /** A parse failure keeps the baseline's shape: BookException "Search failed." caused by the parse exception. */
    @Test fun parseFailuresAreWrappedLikeTheBaseline() {
        val b = book("KJV")
        val m = manager()
        m.scheduleIndexCreation(b)
        val index = m.getIndex(b)
        for (q in listOf("", "+ -", "\"faith", "faith AND", "(faith", "faith~2", "faith~1")) {
            val e = org.junit.Assert.assertThrows(BookException::class.java) { index.find(q) }
            assertEquals("Search failed.", e.message, q)
            assertTrue(e.cause is QueryParseException, "$q: ${e.cause}")
        }
        m.closeAllIndexes()
    }

    companion object {
        val PARITY_QUERIES: List<String> = listOf(
            // terms, case, defaults
            "faith", "FAITH", "faith hope", "faith OR hope", "faith || hope", "jesus christ", "the", "a", "zzzz",
            // required / prohibited / operators
            "+faith +hope", "faith AND hope", "faith && hope", "faith -hope", "faith NOT hope", "faith AND NOT hope", "-the +god",
            "+faith hope", "faith +hope", "faith OR +hope", "faith AND -hope", "faith OR -hope", "NOT faith", "-faith", "!faith", "faith ! hope",
            // nested
            "faith AND (hope OR love)", "+(faith hope) +love", "((faith))", "(the) god", "(faith OR hope) AND (love OR charity)", "god -(light OR darkness)",
            "(+faith -hope) love", "+(-faith) love",
            // phrases and several-token terms
            "\"faith hope\"", "\"in the beginning\"", "\"the god\"", "\"a b\"", "\"the\"", "well-known", "beginning-god", "in-the", "fa\\ ith", "\"\\\"faith\\\"\"",
            "\"faith hope\"~", "\"light\"", "\"light\"~3", "\"faith hope\"~3", "\"faith hope\"~1", "\"faith hope\"~3.5", "\"faith hope\"~1e1",
            // strong
            "strong:H07225", "strong:h7225", "strong:g26", "strong:h7225 AND god", "strong:h7225 AND light", "strong:h430 -god", "light AND strong:g26",
            "strong:xyz", "light AND strong:xyz", "strong:h7225 strong:g26", "content:faith", "strong:G3588!a", "strong:g3588",
            "strong:", "strong:h1 h2", "strong:\"h7225\"", "strong:(h7225 g26)", "content:(faith hope)", "strong:h7225 -strong:g26",
            // Review Focus 5: odd verbatim input
            "", " ", "  \t ", "+", "-", "!", "+ -", "+ ", "- ", "+ faith", "- works", "faith + hope", "faith - works", "faith +", "faith -", "--faith", "++faith",
            "\"faith", "faith\"", "\"", "\"\"", "\" \"", "(faith", "faith)", "()", "(()", ")(", "faith AND", "AND faith", "AND", "OR", "NOT", "faith AND OR hope",
            "\\*", "\\", "faith\\", "~", "~faith", "^", ":", "]", "}", "&&", "||", "&", "|", "faith &", "faith & hope", "3.14", "U.S.A.", "O'Reilly", "c3po",
            "faith \\* hope", "faith AND \\*", "faith OR hope AND \\*", "\"faith hope\"~0.5", "\"faith hope\"~0.9", "\"faith hope\"~-1", "\"faith hope\"~0.",  "faith OR \\* AND hope", "\\* AND faith", "\\* OR faith", "+ AND faith", "faith AND ! hope", "faith OR + AND hope",
            "a\\u0062c", "a\\uzz", "\\u0066aith",
            "　faith　hope", "faith\nhope", "faith\thope",
        )

        val CJK_QUERIES: List<String> = listOf(
            "神", "神爱", "神爱世人", "\"神爱世人\"", "神 爱", "+神 +爱", "神 AND 世人",
            "神爱 AND faith", "神\u000faith".replace("\u000f", ""), "神 faith", "\"神 faith\"", "faith神", "-神", "神 -爱",
            "(神)", "神 OR 爱 OR 世人", "(神 OR 爱) AND 世人", "\"神 爱\"", "神　爱", "。", "，",
            "神。爱", "+ 神", "神 + 爱", "\\*", "+ ", "神爱世人 AND NOT 神爱",
        )

        /** K6-3: accepted by the baseline, rejected here (field other than strong/content, boost, range). */
        val UNSUPPORTED_QUERIES: List<String> = listOf(
            "heading:x", "faith^2", "[a TO b]", "{a TO b}", "strong:\"h7225 g26\"", "faith AND [a TO b]", "(faith hope)^2", "\"faith hope\"^3", "a:b",
        )

        /** Nothing is pending since task 13 (sloppy phrases are evaluated; more shapes in `SearchProximityOracleTest`). */
        val PENDING_QUERIES: List<String> = listOf(
        )

        /** Task 12: expansion shapes, including the escape cases of the task 11 review. */
        val EXPANSION_QUERIES: List<String> = listOf(
            "bless*", "*ness", "b?ess", "?", "*", "??", "???", "faith*", "f*h", "fa?th", "Jesus*", "JESUS*", "g?d", "a*", "?a*", "*e*", "*ess*", "**", "*?", "?*", "b**s", "bless?",
            "faith~", "faith~0.8", "faith~0.1", "faith~0", "faith~0.99", "Faith~0.5", "believe~0.6", "believe~", "x~0.99", "x~", "a~", "jesus~0.9", "zzzzz~0.3", "believeth~0.1", "beginning~0.0", "godly~0.2", "jesus~0.2", "in~0.1", "the~0.5", "lord~0.4", "beliefe~0.3",
            "bless\\*", "b\\?ess", "b\\?ess*", "bles\\*", "b\\?es?", "fa\\*", "fa\\**", "\\u0066ai*", "ca\\u0066\\u00e9?", "caf\\u00e9*", "a\\*b*x", "b\\?ess~", "bless\\?~",
            "faith* hope*", "faith* AND hope~", "+bless* -blessed", "-bless*", "NOT god*", "(a* OR b*) AND c*", "content:fa*", "bless* -bless*", "*ness AND ?",
            "strong:H72*", "strong:h72*", "strong:H0722*", "strong:G*", "strong:g*", "strong:*", "strong:?", "strong:h7?25", "strong:h\\*", "strong:h7\\*", "strong:H*", "strong:h*", "strong:G3588!a", "strong:\"G3588!a\"", "strong:\"g3588\"", "strong:\"h7225\"",
            "light AND strong:h0*", "strong:G0?6", "strong:g02*",
        )

        val CJK_EXPANSION_QUERIES: List<String> = listOf("神*", "*爱", "?", "*", "神?", "世*", "神~", "神爱~0.5", "神爱世人~0.3", "爱?", "*人", "??", "神\\*", "神\\u7231*", "(神*) AND (爱*)", "神爱?", "*神*", "a*", "faith*")
    }
}
