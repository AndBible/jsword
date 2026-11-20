package org.crosswire.jsword.index.fts

import okio.FileSystem
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.RegexSearchBudget
import org.crosswire.jsword.index.analysis.AnalyzedToken
import org.crosswire.ksword.fixtures.fixtureRoot
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeSource

/**
 * JVM-only guards of [FtsIndex.findRegex] against the regex engine itself: a match that runs on (Android's ICU engine
 * cannot be interrupted, so it must not hold the index lock) and a match that overflows the stack.
 */
class FtsRegexEngineGuardTest {
    private val fs = FileSystem.SYSTEM
    private val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "fts-regex-guard-${Random.nextLong().toString(16)}"
    private var savedAugmentPath = SwordBookPath.getAugmentPath()

    @BeforeTest fun install() {
        savedAugmentPath = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
        fs.createDirectories(root)
    }

    @AfterTest fun restore() {
        SwordBookPath.setAugmentPath(savedAugmentPath)
        fs.deleteRecursively(root, mustExist = false)
    }

    private fun book(): Book = assertNotNull(Books.installed().getBook("K4RawText"))

    private fun indexOf(vararg rows: Pair<String, String>): FtsIndex {
        val store = FtsIndexStore.create(root / "rows-${Random.nextLong().toString(16)}.db")
        store.beginBuild()
        rows.forEachIndexed { i, (ref, text) -> store.addEntry(i + 1L, ref, listOf(AnalyzedToken("a", 0)), emptyList(), text) }
        store.writeMetadata(FtsIndexMetadata(FTS_SCHEMA_VERSION, "simple", SearchTokenCodec.ID, "K4RawText", "1.0", "fp"))
        store.commitBuild()
        return FtsIndex(book(), store)
    }

    @Test fun aRunningMatchDoesNotBlockClose() {
        // `(a*)*\1b` backtracks exponentially on a's without a b. The budget is an hour, so only `stop` ends the scan.
        val idx = indexOf("Gen.1.1" to "a".repeat(60) + "c")
        val stop = AtomicBoolean(false)
        val scanner = Thread {
            runCatching { idx.findRegex(Regex("(a*)*\\1b"), null, 10, RegexSearchBudget(TimeSource.Monotonic.markNow() + 1.hours) { stop.get() }) }
        }.apply { isDaemon = true; start() }
        try {
            Thread.sleep(300)
            assertTrue(scanner.isAlive, "the match must still be running")
            val closer = Thread { idx.close() }.apply { isDaemon = true; start() }
            closer.join(2000)
            assertFalse(closer.isAlive, "close() waited for the running match")
        } finally {
            stop.set(true)
            scanner.join(10_000)
            idx.close()
        }
    }

    @Test fun stackOverflowInOneRowCountsAsNoMatchAndTheScanContinues() {
        // java.util.regex recurses once per repetition of `(a|b)*`: 200 000 characters overflow a 512 KiB stack.
        val idx = indexOf("Gen.1.1" to "ab".repeat(100_000), "Gen.1.2" to "xyz")
        val refs = AtomicReference<List<String>>()
        val failure = AtomicReference<Throwable>()
        val t = Thread(null, {
            try {
                val r = idx.findRegex(Regex("(a|b)*c|xyz"), null, 10, RegexSearchBudget(TimeSource.Monotonic.markNow() + 1.hours))
                refs.set(r.keys.map { it.getOsisRef() })
            } catch (e: Throwable) {
                failure.set(e)
            }
        }, "deep-regex", 512L * 1024)
        t.start()
        t.join(60_000)
        idx.close()
        assertNull(failure.get(), "failure: ${failure.get()}")
        assertEquals(listOf("Gen.1.2"), refs.get())
    }
}
