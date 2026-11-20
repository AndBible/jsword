package org.crosswire.jsword.index.fts

import okio.FileSystem
import okio.Path
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.RegexSearchBudget
import org.crosswire.jsword.index.RegexSearchableIndex
import org.crosswire.jsword.index.analysis.AnalyzedToken
import org.crosswire.jsword.passage.Key
import org.crosswire.ksword.fixtures.fixtureRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** [FtsIndex.findRegex] over K4's generated RawText fixture (runs on the JVM and on the iOS simulator). */
class FtsRegexSearchTest {
    private val fs = FileSystem.SYSTEM
    private var savedAugmentPath: Array<Path> = emptyArray()
    private val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "fts-regex-${Random.nextLong().toString(16)}"
    private var manager: FtsIndexManager? = null
    private var book: Book? = null
    private var statusBefore: org.crosswire.jsword.index.IndexStatus? = null

    @BeforeTest fun install() {
        savedAugmentPath = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
        fs.createDirectories(root)
    }

    @AfterTest fun restore() {
        manager?.closeAllIndexes()
        book?.let { b -> statusBefore?.let { b.indexStatus = it } }
        SwordBookPath.setAugmentPath(savedAugmentPath)
        fs.deleteRecursively(root, mustExist = false)
    }

    private fun index(): RegexSearchableIndex {
        val b = assertNotNull(Books.installed().getBook("K4RawText")).also { book = it; statusBefore = it.indexStatus }
        val m = FtsIndexManager(root / "search", root / "lucene") { bk, k ->
            org.crosswire.jsword.book.BookData(bk, k).getOsisFragment(false)
        }.also { manager = it }
        m.scheduleIndexCreation(b)
        return m.getIndex(b) as RegexSearchableIndex
    }

    private fun budget(isCancelled: () -> Boolean = { false }) = RegexSearchBudget(TimeSource.Monotonic.markNow() + 1.hours, isCancelled)
    private fun refs(k: Key) = k.map { it.getOsisRef() }
    private val ic = setOf(RegexOption.IGNORE_CASE)

    @Test fun matchesAcrossWordsAndPunctuation() {
        val idx = index()
        val r = idx.findRegex(Regex("in\\s+the\\s+beginning", ic), null, 100, budget())
        assertEquals(listOf("Gen.1.1", "John.1.1", "John.1.2"), refs(r.keys))
        assertFalse(r.truncated)
        // Spans the comma between the words.
        assertEquals(listOf("John.1.1"), refs(idx.findRegex(Regex("Word,\\s+and\\s+the", ic), null, 100, budget()).keys))
    }

    @Test fun caseInsensitiveByDefaultAndInlineFlagMakesItSensitive() {
        val idx = index()
        // "In the beginning" is capitalised in Gen.1.1 and John.1.1, lowercase in John.1.2.
        assertEquals(listOf("Gen.1.1", "John.1.1", "John.1.2"), refs(idx.findRegex(Regex("in the beginning", ic), null, 100, budget()).keys))
        assertEquals(listOf("John.1.2"), refs(idx.findRegex(Regex("(?-i)in the beginning", ic), null, 100, budget()).keys))
    }

    @Test fun restrictionLimitsToTheGivenVerses() {
        val idx = index()
        val john = assertNotNull(book).getKey("John")
        val r = idx.findRegex(Regex("beginning", ic), john, 100, budget())
        assertEquals(listOf("John.1.1", "John.1.2"), refs(r.keys))
    }

    @Test fun maxResultsCapsWithoutTruncation() {
        val r = index().findRegex(Regex(""), null, 2, budget())
        assertEquals(2, r.keys.getCardinality())
        assertFalse(r.truncated)
    }

    @Test fun exhaustedBudgetStopsAndReportsTruncation() {
        val past = RegexSearchBudget(TimeSource.Monotonic.markNow() - 10.milliseconds)
        val r = index().findRegex(Regex(""), null, 100, past)
        assertEquals(0, r.keys.getCardinality())
        assertTrue(r.truncated)
    }

    @Test fun cancelFlagStopsTheScan() {
        val idx = index()
        val all = idx.findRegex(Regex(""), null, 100, budget()).keys.getCardinality()
        assertTrue(all > 1, "the fixture needs several verses with text")
        var polls = 0
        val r = idx.findRegex(Regex(""), null, 100, budget { ++polls > 1 })
        assertTrue(r.truncated)
        assertTrue(r.keys.getCardinality() < all, "the cancelled scan must stop before the last verse")
    }

    @Test fun restrictionIsAppliedBeforeTheCap() {
        val idx = index()
        val john = assertNotNull(book).getKey("John")
        val r = idx.findRegex(Regex("beginning", ic), john, 1, budget())
        assertEquals(listOf("John.1.1"), refs(r.keys))
    }

    /** An index of the given (osisRef, text) rows over the K4RawText book, built outside the manager. */
    private fun indexOf(vararg rows: Pair<String, String>): FtsIndex {
        val b = assertNotNull(Books.installed().getBook("K4RawText")).also { book = it }
        val store = FtsIndexStore.create(root / "rows-${Random.nextLong().toString(16)}.db")
        store.beginBuild()
        rows.forEachIndexed { i, (ref, text) -> store.addEntry(i + 1L, ref, listOf(AnalyzedToken("a", 0)), emptyList(), text) }
        store.writeMetadata(FtsIndexMetadata(FTS_SCHEMA_VERSION, "simple", SearchTokenCodec.ID, "K4RawText", "1.0", "fp"))
        store.commitBuild()
        return FtsIndex(b, store)
    }

    @Test fun rowsWithoutTextAreNeverReturned() {
        val idx = indexOf("Gen.1.0" to "", "Gen.1.1" to "text", "Gen.1.2" to "")
        try {
            // `x*` and the empty pattern match an empty text: those rows are Strong's-only or verse 0, not verses.
            for (pattern in listOf("x*", "")) {
                assertEquals(listOf("Gen.1.1"), refs(idx.findRegex(Regex(pattern), null, 10, budget()).keys), pattern)
            }
            assertEquals(emptyList(), refs(idx.findRegex(Regex("^$"), null, 10, budget()).keys))
        } finally {
            idx.close()
        }
    }

    @Test fun aRowWhoseMatchFailsCountsAsNoMatchAndMarksTheResultIncomplete() {
        val idx = indexOf("Gen.1.1" to "boom", "Gen.1.2" to "fine", "Gen.1.3" to "fine")
        try {
            val r = idx.findRegex(Regex("fine|boom"), null, 10, budget()) { regex, text ->
                if (text.toString() == "boom") throw IllegalStateException("engine failure") else regex.containsMatchIn(text)
            }
            assertEquals(listOf("Gen.1.2", "Gen.1.3"), refs(r.keys))
            assertTrue(r.truncated, "a failed row must not look like a complete result")
        } finally {
            idx.close()
        }
    }

    @Test fun aFailureOfTheIndexItselfIsAnErrorNotAnEmptyResult() {
        val b = assertNotNull(Books.installed().getBook("K4RawText")).also { book = it }
        val store = FtsIndexStore.create(root / "broken.db")
        store.beginBuild()
        store.writeMetadata(FtsIndexMetadata(FTS_SCHEMA_VERSION, "simple", SearchTokenCodec.ID, "K4RawText", "1.0", "fp"))
        store.commitBuild()
        val idx = FtsIndex(b, store)
        store.close() // the connection dies behind the index's back, like a corrupt or unreadable file
        assertFailsWith<org.crosswire.jsword.book.BookException> { idx.findRegex(Regex("a"), null, 10, budget()) }
    }

    /** Fails the test, instead of hanging the build, when [block] has not returned within [limit] (it runs on its own thread/worker). */
    private fun boundedRun(limit: Duration = 30.seconds, block: () -> Unit) = runTest {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val worker = scope.async { block() }
        val finished = withContext(Dispatchers.Default) { withTimeoutOrNull(limit) { worker.await(); true } }
        assertNotNull(finished, "the scan did not return within $limit: the regex engine ignored the budget")
    }

    @Test fun catastrophicPatternStopsAtTheBudget() = boundedRun {
        val idx = indexOf("Gen.1.1" to "a".repeat(60) + "c")
        try {
            val start = TimeSource.Monotonic.markNow()
            val r = idx.findRegex(Regex("(a*)*\\1b"), null, 10, RegexSearchBudget(start + 200.milliseconds))
            assertTrue(start.elapsedNow() < 5.seconds, "took ${start.elapsedNow()}")
            assertTrue(r.truncated)
            assertEquals(0, r.keys.getCardinality())
        } finally {
            idx.close()
        }
    }

    @Test fun greekCaseFoldingAndTheInlineFlagWork() {
        val idx = indexOf("John.1.1" to "Ἐν ἀρχῇ ἦν ὁ λόγος", "John.1.2" to "ΟΥΤΟΣ ἦν ἐν ἀρχῇ")
        try {
            assertEquals(listOf("John.1.1", "John.1.2"), refs(idx.findRegex(Regex("ἐν ἀρχῇ", ic), null, 10, budget()).keys))
            assertEquals(listOf("John.1.2"), refs(idx.findRegex(Regex("(?-i)ἐν ἀρχῇ", ic), null, 10, budget()).keys))
            assertEquals(listOf("John.1.1"), refs(idx.findRegex(Regex("ΛΌΓΟΣ", ic), null, 10, budget()).keys))
            assertEquals(listOf("John.1.2"), refs(idx.findRegex(Regex("ουτοσ", ic), null, 10, budget()).keys))
            assertEquals(emptyList(), refs(idx.findRegex(Regex("(?-i)ουτοσ", ic), null, 10, budget()).keys))
        } finally {
            idx.close()
        }
    }

    @Test fun aDeepRepetitionDoesNotCrashTheScan() = boundedRun {
        // `(a|b)*` recurses once per character in java.util.regex (a stack overflow on the JVM, which counts as no match for
        // the row and marks the result incomplete); Kotlin/Native's engine must survive the same depth. Anchored, so that
        // the cost is linear and only the depth matters.
        val idx = indexOf("Gen.1.1" to "ab".repeat(50_000), "Gen.1.2" to "xyz")
        try {
            val r = idx.findRegex(Regex("^(a|b)*c|xyz"), null, 10, RegexSearchBudget(TimeSource.Monotonic.markNow() + 20.seconds))
            assertEquals(listOf("Gen.1.2"), refs(r.keys))
        } finally {
            idx.close()
        }
    }
}
