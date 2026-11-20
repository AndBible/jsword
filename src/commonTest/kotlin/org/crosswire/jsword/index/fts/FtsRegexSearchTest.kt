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
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
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
        var polls = 0
        val r = index().findRegex(Regex(""), null, 100, budget { ++polls > 1 })
        assertTrue(r.keys.getCardinality() <= 1)
        assertTrue(r.truncated)
    }
}
