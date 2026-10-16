package org.crosswire.ksword.k6

import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.crosswire.common.progress.JobManager
import org.crosswire.common.util.ReporterEvent
import org.crosswire.common.util.Reporter
import org.crosswire.common.util.ReporterListener
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.IndexPolicyAdapter
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.fts.FtsIndexManager
import org.crosswire.ksword.fixtures.fixtureRoot
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** K6 task 10 fix round: the manager used from several threads (builds blocked inside the fragment seam). */
class FtsIndexConcurrencyTest {
    private val dir = Files.createTempDirectory("fts-conc").toFile()
    private var saved: Array<okio.Path> = emptyArray()
    private val reports = AtomicInteger()
    private val listener = object : ReporterListener {
        override fun reportException(ev: ReporterEvent) { reports.incrementAndGet() }
        override fun reportMessage(ev: ReporterEvent) { reports.incrementAndGet() }
    }

    @BeforeTest fun setUp() {
        saved = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
        Reporter.addReporterListener(listener)
    }

    private val managers = ArrayList<FtsIndexManager>()
    private val statusBefore = HashMap<Book, IndexStatus>()

    @AfterTest fun tearDown() {
        managers.forEach { it.closeAllIndexes() }
        statusBefore.forEach { (b, st) -> b.indexStatus = st }
        Reporter.removeReporterListener(listener)
        SwordBookPath.setAugmentPath(saved)
        dir.deleteRecursively()
    }

    private fun book(initials: String = "K4RawText"): Book = assertNotNull(Books.installed().getBook(initials)).also { statusBefore.getOrPut(it) { it.indexStatus } }

    private val search get() = dir.resolve("search").toOkioPath()

    private fun files() = if (FileSystem.SYSTEM.exists(search)) FileSystem.SYSTEM.listRecursively(search).filter {
        FileSystem.SYSTEM.metadata(it).isRegularFile }.toList() else emptyList()

    /** A manager whose fragment seam counts calls and, once [gate] is set, blocks the first call until it is released. */
    private class Gate { val started = CountDownLatch(1); val release = CountDownLatch(1); val calls = AtomicInteger() }

    private fun manager(gate: Gate?) = FtsIndexManager(search, dir.resolve("lucene").toOkioPath()) { b, k ->
        if (gate != null && gate.calls.incrementAndGet() == 1) {
            gate.started.countDown()
            assertTrue(gate.release.await(20, TimeUnit.SECONDS), "gate not released")
        }
        BookData(b, k).getOsisFragment(false)
    }.also { managers += it }

    @Test fun twoSchedulesForOneBookBuildOnce() {
        val b = book()
        val baseline = Gate().also { it.release.countDown() }
        manager(baseline).scheduleIndexCreation(b)
        val oneBuild = baseline.calls.get()
        manager(null).deleteIndex(b)

        val gate = Gate()
        val m = manager(gate)
        val t = thread { m.scheduleIndexCreation(b) }
        assertTrue(gate.started.await(20, TimeUnit.SECONDS))
        m.scheduleIndexCreation(b) // dropped: a build is already running
        gate.release.countDown()
        t.join(30_000)
        assertEquals(oneBuild, gate.calls.get())
        assertEquals(IndexStatus.DONE, b.indexStatus)
        assertTrue(m.isIndexed(b))
    }

    @Test fun deleteIndexDuringABuildEndsUndoneQuietly() {
        val b = book()
        val gate = Gate()
        val m = manager(gate)
        val builder = thread { m.scheduleIndexCreation(b) }
        assertTrue(gate.started.await(20, TimeUnit.SECONDS))
        val cancelled = CountDownLatch(1)
        m.deleteHook = { cancelled.countDown() }
        val deleter = thread { m.deleteIndex(b) }
        assertTrue(cancelled.await(20, TimeUnit.SECONDS)) // deleteIndex has cancelled the job and now waits for the build
        gate.release.countDown()
        builder.join(30_000); deleter.join(30_000)
        assertFalse(builder.isAlive || deleter.isAlive)
        assertEquals(IndexStatus.UNDONE, b.indexStatus)
        assertTrue(files().isEmpty(), "leftovers: ${files()}")
        assertFalse(m.isIndexed(b))
        assertEquals(0, reports.get(), "user was informed of an error")
    }

    @Test fun closeAllIndexesDuringABuildDoesNotDisturbIt() {
        val b = book()
        manager(null).scheduleIndexCreation(b)
        val gate = Gate()
        val m = manager(gate)
        m.getIndex(b)
        val t = thread { m.scheduleIndexCreation(b) }
        assertTrue(gate.started.await(20, TimeUnit.SECONDS))
        m.closeAllIndexes()
        gate.release.countDown()
        t.join(30_000)
        assertEquals(IndexStatus.DONE, b.indexStatus)
        assertEquals(3, m.getIndex(b).find("beginning").getCardinality())
        m.closeAllIndexes()
        assertEquals(0, reports.get())
    }

    /** getIndex while a rebuild runs caches a handle on the old file; the rename must evict it. */
    @Test fun indexOpenedDuringARebuildIsEvictedByTheRename() {
        val b = book()
        manager(null).scheduleIndexCreation(b)
        val gate = Gate()
        val m = manager(gate)
        val t = thread { m.scheduleIndexCreation(b) }
        assertTrue(gate.started.await(20, TimeUnit.SECONDS))
        val old = m.getIndex(b)
        gate.release.countDown()
        t.join(30_000)
        val fresh = m.getIndex(b)
        assertTrue(old !== fresh, "stale cached index served after the rebuild")
        assertEquals(3, fresh.find("beginning").getCardinality())
        m.closeAllIndexes()
    }

    /** The BookIndexer sequence: deleteIndex during a build, then at once a new request, which must not be dropped. */
    @Test fun scheduleRightAfterDeleteDuringABuildRuns() {
        val b = book()
        val gate = Gate()
        val m = manager(gate)
        val builder = thread { m.scheduleIndexCreation(b) }
        assertTrue(gate.started.await(20, TimeUnit.SECONDS))
        val cancelled = CountDownLatch(1)
        m.deleteHook = { cancelled.countDown() }
        val deleter = thread { m.deleteIndex(b); m.scheduleIndexCreation(b) }
        assertTrue(cancelled.await(20, TimeUnit.SECONDS))
        gate.release.countDown()
        builder.join(30_000); deleter.join(30_000)
        assertFalse(builder.isAlive || deleter.isAlive)
        assertEquals(IndexStatus.DONE, b.indexStatus)
        assertTrue(m.isIndexed(b))
        assertEquals(0, reports.get())
    }

    private fun serial(m: FtsIndexManager) = m.also { it.indexPolicy = object : IndexPolicyAdapter() { override fun isSerial() = true } }

    /** I1: deleteIndex runs on the main thread; it must not wait for the (minutes long) build of ANOTHER book. */
    @Test fun deleteIndexDoesNotWaitForAnotherBooksSerialBuild() {
        val a = book("K4RawText")
        val b = book("K4RawCom")
        serial(manager(null)).scheduleIndexCreation(a)
        assertTrue(files().isNotEmpty())
        val gate = Gate()
        val m = serial(manager(gate))
        val builder = thread { m.scheduleIndexCreation(b) }
        assertTrue(gate.started.await(20, TimeUnit.SECONDS)) // B holds the serial lock, blocked in its build
        val deleted = CountDownLatch(1)
        val deleter = thread { m.deleteIndex(a); deleted.countDown() }
        assertTrue(deleted.await(10, TimeUnit.SECONDS), "deleteIndex(A) blocked behind the build of B")
        deleter.join(5_000)
        assertFalse(m.isIndexed(a))
        assertEquals(IndexStatus.UNDONE, a.indexStatus)
        gate.release.countDown()
        builder.join(60_000)
        assertFalse(builder.isAlive)
        assertEquals(IndexStatus.DONE, b.indexStatus)
        assertTrue(m.isIndexed(b))
        assertEquals(0, reports.get())
    }

    /** The number of fragment calls one complete build of [b] makes. */
    private fun callsOfOneBuild(b: Book): Int {
        val g = Gate().also { it.release.countDown() }
        manager(g).scheduleIndexCreation(b)
        val calls = g.calls.get()
        manager(null).deleteIndex(b)
        return calls
    }

    /**
     * A build of A that is only QUEUED behind B's build (serial policy) is dropped quietly when A is deleted, and a
     * request right after the delete is not lost: A is built exactly once (by the later request).
     */
    @Test fun deleteOfAQueuedBookDropsItAndALaterScheduleRuns() {
        val a = book("K4RawText")
        val b = book("K4RawCom")
        val oneBuildOfA = callsOfOneBuild(a)
        val gate = Gate()
        val aCalls = AtomicInteger()
        val m = serial(FtsIndexManager(search, dir.resolve("lucene").toOkioPath()) { bk, k ->
            if (bk.initials == a.initials) aCalls.incrementAndGet()
            if (bk.initials == b.initials && gate.calls.incrementAndGet() == 1) {
                gate.started.countDown()
                assertTrue(gate.release.await(20, TimeUnit.SECONDS), "gate not released")
            }
            BookData(bk, k).getOsisFragment(false)
        }.also { managers += it })
        val builderB = thread { m.scheduleIndexCreation(b) }
        assertTrue(gate.started.await(20, TimeUnit.SECONDS))
        val queuedA = thread { m.scheduleIndexCreation(a) } // waits for the serial lock
        Thread.sleep(300)
        m.deleteIndex(a)
        assertEquals(IndexStatus.UNDONE, a.indexStatus)
        val again = thread { m.scheduleIndexCreation(a) } // not dropped as "already building"
        Thread.sleep(300)
        gate.release.countDown()
        listOf(builderB, queuedA, again).forEach { it.join(120_000); assertFalse(it.isAlive) }
        assertEquals(IndexStatus.DONE, b.indexStatus)
        assertEquals(IndexStatus.DONE, a.indexStatus)
        assertTrue(m.isIndexed(a) && m.isIndexed(b))
        assertEquals(oneBuildOfA, aCalls.get(), "A must be built exactly once, by the later request")
        assertTrue(files().none { it.name.contains(".creating") })
        assertEquals(0, reports.get())
    }

    /** deleteIndex cancels the running build's own job, not a stale job of the same name that JobManager lists first. */
    @Test fun deleteCancelsTheRunningBuildNotAStaleJobWithTheSameName() {
        val b = book()
        val oneBuild = callsOfOneBuild(b)
        val stale = JobManager.createJob("CREATE_INDEX-${b.initials}", "stale") { }
        try {
            val gate = Gate()
            val m = manager(gate)
            val builder = thread { m.scheduleIndexCreation(b) }
            assertTrue(gate.started.await(20, TimeUnit.SECONDS))
            val cancelled = CountDownLatch(1)
            m.deleteHook = { cancelled.countDown() }
            val deleter = thread { m.deleteIndex(b) }
            assertTrue(cancelled.await(20, TimeUnit.SECONDS))
            gate.release.countDown()
            builder.join(60_000); deleter.join(60_000)
            assertFalse(builder.isAlive || deleter.isAlive)
            assertTrue(gate.calls.get() < oneBuild, "the build ran to the end: ${gate.calls.get()} of $oneBuild fragment calls")
            assertEquals(IndexStatus.UNDONE, b.indexStatus)
            assertFalse(m.isIndexed(b))
        } finally {
            stale.done()
        }
    }

    /** A failure before the build lock must not leave the book registered as building forever. */
    @Test fun failureBeforeTheBuildDoesNotBlockLaterSchedules() {
        val b = book()
        val m = manager(null)
        m.scheduleHook = { throw IllegalStateException("job creation failed") }
        m.scheduleIndexCreation(b)
        assertEquals(IndexStatus.UNDONE, b.indexStatus)
        assertEquals(1, reports.get())
        m.scheduleHook = null
        m.scheduleIndexCreation(b)
        assertEquals(IndexStatus.DONE, b.indexStatus)
    }
}
