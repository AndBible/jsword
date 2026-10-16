package org.crosswire.jsword.index.fts

import fi.sykero.kdom.Element
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import okio.FileSystem
import okio.IOException
import okio.Path
import org.crosswire.common.progress.JobManager
import org.crosswire.common.progress.Progress
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.KUri
import org.crosswire.common.util.Reporter
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.index.Index
import org.crosswire.jsword.index.IndexManager
import org.crosswire.jsword.index.IndexPolicy
import org.crosswire.jsword.index.IndexPolicyAdapter
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.analysis.BookAnalyzers
import org.crosswire.jsword.passage.Key

/**
 * The [IndexManager] over per-book SQLite FTS5 files (K6), replacing `LuceneIndexManager`.
 *
 * Files: `<searchDir>/<driverName>/<initials>.sqlite` ([SearchFields.DIR_SEARCH] in the writable project dir), where
 * driver and initials are the same segments `LuceneIndexManager.getStorageArea` uses. A build writes
 * `<name>.sqlite.creating` and renames it to the final name only on success (atomic move), so the final name never
 * holds a partial index; `*.creating` leftovers of an interrupted run are deleted when a manager is created.
 *
 * Status transitions as in Lucene: `SCHEDULED`, then `CREATING`, then `DONE`, or `UNDONE` on failure or cancellation
 * (cancelling the `CREATE_INDEX-<initials>` job). Failures are reported to the user via [Reporter], not thrown, as
 * `LuceneIndexManager.scheduleIndexCreation` does. Safe to use from several threads.
 *
 * Non-verse-keyed books (dictionaries, general books) are indexed but searching them fails exactly like the baseline
 * (see [FtsIndex.find]).
 */
class FtsIndexManager internal constructor(
    searchDir: Path?,
    luceneDir: Path?,
    private val fragmentOf: (Book, Key) -> Element,
) : IndexManager {
    /** A manager using the writable project dir's `search/` and `lucene/` subdirectories. */
    constructor() : this(null, null, { b, k -> BookData(b, k).getOsisFragment(false) })

    private val searchDir: Path = searchDir ?: projectSubdir(SearchFields.DIR_SEARCH)
    private val luceneDir: Path = luceneDir ?: projectSubdir(SearchFields.DIR_LEGACY_LUCENE)
    private val lock = SynchronizedObject()
    private val open = HashMap<String, FtsIndex>()
    private val serialLock = SynchronizedObject()
    private val bookLocks = HashMap<String, SynchronizedObject>()
    private val building = HashSet<String>()

    /** Test seams: run in [scheduleIndexCreation] once the book is registered, and in [deleteIndex] after it cancelled the job. */
    internal var scheduleHook: ((Book) -> Unit)? = null
    internal var deleteHook: ((Book) -> Unit)? = null
    private var policy: IndexPolicy = IndexPolicyAdapter()

    init { deleteCreatingLeftovers() }

    override var indexPolicy: IndexPolicy?
        get() = synchronized(lock) { policy }
        set(value) = synchronized(lock) { policy = value ?: IndexPolicyAdapter() }

    override fun isIndexed(book: Book?): Boolean = book != null && fs.exists(finalPath(book))

    @Throws(BookException::class)
    override fun getIndex(book: Book?): Index {
        requireNotNull(book) { "null book" }
        synchronized(lock) {
            open[book.initials]?.let { return it }
            val store = FtsIndexStore.open(finalPath(book))
                // TRANSLATOR: Error condition: The search index of a book is missing, unreadable or of an incompatible version.
                ?: throw BookException(JSMsg.gettext("The search index could not be opened. It may need to be rebuilt."))
            return FtsIndex(book, store).also { open[book.initials] = it }
        }
    }

    /**
     * True when the book has no usable index file, or its metadata differs from what a build now would write: another
     * schema version, analyzer id, token codec or book version. Safe to call for any book.
     */
    override fun needsReindexing(book: Book?): Boolean {
        if (book == null || !isIndexed(book)) return true
        val meta = try {
            FtsIndexStore.open(finalPath(book), readOnly = true)?.use { it.metadata() }
        } catch (e: Exception) { null } ?: return true
        val version = book.bookMetaData.getProperty(org.crosswire.jsword.book.sword.SwordBookMetaData.KEY_VERSION)
        return meta.schemaVersion != FTS_SCHEMA_VERSION ||
            meta.analyzerId != BookAnalyzers.analyzerId(book.language.code) ||
            meta.codec != SearchTokenCodec.ID ||
            meta.bookVersion != version
    }

    /**
     * Builds the index of [book] (status `SCHEDULED`, `CREATING`, then `DONE`, or `UNDONE` on failure or cancellation).
     * A request for a book that is already scheduled or building is dropped and returns immediately: the running build
     * serves both, so callers must observe the outcome through an [org.crosswire.jsword.index.IndexStatusListener].
     * The book leaves the "building" set, and its final status is set, inside the build lock, so a [deleteIndex] that
     * waited for the build can be followed at once by a new request.
     */
    override fun scheduleIndexCreation(book: Book?) {
        requireNotNull(book) { "null book" }
        var registered = false
        var job: Progress? = null
        try {
            if (!synchronized(lock) { building.add(book.initials) }) return
            registered = true
            scheduleHook?.invoke(book)
            book.indexStatus = IndexStatus.SCHEDULED
            val final = finalPath(book)
            val creating = creatingPath(final)
            val jobName = JSMsg.gettext("Creating index. Processing {0}", book.initials)
            val cancelled = kotlinx.atomicfu.atomic(false)
            val newJob = JobManager.createJob(Progress.CREATE_INDEX.replace("%s", book.initials), jobName) { cancelled.value = true }
            job = newJob
            newJob.beginJob(jobName)
            val currentPolicy = indexPolicy!!
            // Serial policy: one build at a time overall; otherwise one per book (as Lucene's mutex choice).
            val mutex = if (currentPolicy.isSerial()) serialLock else bookLock(book)
            synchronized(mutex) {
                var finalStatus = IndexStatus.UNDONE
                try {
                    fs.createDirectories(final.parent!!)
                    deleteDb(creating)
                    // The open index (if any) keeps serving searches from the old file until the rename below.
                    book.indexStatus = IndexStatus.CREATING
                    val errors = FtsIndexBuilder(book, currentPolicy, fragmentOf)
                        .build(creating, newJob) { cancelled.value || newJob.isFinished }
                    newJob.isCancelable = false
                    if (cancelled.value || newJob.isFinished) throw IndexBuildCancelled()
                    // The rename and the eviction of the cached handle are one step under [lock], so getIndex never
                    // opens the new file and has it evicted, nor serves the old one after the rename. A search that
                    // still holds the evicted index is forwarded to the new one (see [FtsIndex.successor]).
                    val replaced = synchronized(lock) {
                        fs.atomicMove(creating, final)
                        open.remove(book.initials)
                    }
                    retire(book, replaced, reopen = true)
                    finalStatus = IndexStatus.DONE
                    if (errors.isNotEmpty()) {
                        // TRANSLATOR: It is likely that one or more verses could not be indexed due to errors in those verses.
                        // This message gives a listing of them to the user.
                        Reporter.informUser(this, JSMsg.gettext("The following verses have errors and could not be indexed\n{0}",
                            errors.joinToString("") { "$it\n" }))
                    }
                } catch (e: IndexBuildCancelled) {
                    // cancelled by the user: UNDONE, nothing to report
                } catch (e: Exception) {
                    Reporter.informUser(this, e)
                } finally {
                    deleteDb(creating)
                    book.indexStatus = finalStatus
                    newJob.done()
                    synchronized(lock) { building.remove(book.initials) }
                    registered = false
                }
            }
        } catch (e: Exception) {
            Reporter.informUser(this, e)
        } finally {
            if (registered) { // failed before the build lock was reached
                book.indexStatus = IndexStatus.UNDONE
                job?.done()
                synchronized(lock) { building.remove(book.initials) }
            }
        }
    }

    /** Index download is gone with the Lucene indexes; Task 15 removes this from [IndexManager]. */
    override fun installDownloadedIndex(book: Book?, tempDest: KUri?) {
        throw UnsupportedOperationException("downloaded search indexes are not supported")
    }

    @Throws(BookException::class)
    override fun deleteIndex(book: Book?) {
        requireNotNull(book) { "null book" }
        // A build in progress is cancelled and waited for (same mutex as the build), so it ends UNDONE quietly instead
        // of failing at the rename after its files were deleted.
        JobManager.findJob(Progress.CREATE_INDEX.replace("%s", book.initials))?.cancel()
        deleteHook?.invoke(book)
        val mutex = if (indexPolicy!!.isSerial()) serialLock else bookLock(book)
        synchronized(mutex) {
            retire(book, synchronized(lock) { open.remove(book.initials) }, reopen = false)
            val final = finalPath(book)
            try {
                deleteDb(creatingPath(final))
                deleteDb(final)
            } catch (e: IOException) {
                // TRANSLATOR: Error condition: The index could not be deleted.
                throw BookException(JSMsg.gettext("Failed to delete search index."), e)
            }
            book.indexStatus = IndexStatus.UNDONE
        }
    }

    override fun closeAllIndexes() {
        val all = synchronized(lock) { open.values.toList().also { open.clear() } }
        all.forEach { retire(it.book, it, reopen = true) }
    }

    /** True when the legacy Lucene index directory `lucene/<driverName>/<initials>/` of [book] exists. */
    fun hasLegacyLuceneIndex(book: Book): Boolean = fs.exists(legacyPath(book))

    /** Deletes the legacy Lucene index directory of [book] (and its `.creating` sibling), if any. */
    fun deleteLegacyLuceneIndex(book: Book) {
        val dir = legacyPath(book)
        fs.deleteRecursively(dir, mustExist = false)
        fs.deleteRecursively(creatingPath(dir), mustExist = false)
    }

    /**
     * Deletes every legacy Lucene index directory `lucene/<driver>/<initials>` that belongs to none of the [installed]
     * books (same driver and initials), plus empty driver directories left behind. Returns how many were deleted.
     */
    fun deleteOrphanLegacyLuceneIndexes(installed: Collection<Book>): Int {
        if (!fs.exists(luceneDir)) return 0
        val keep = installed.mapTo(HashSet()) { driverSegment(it) to it.initials }
        var deleted = 0
        for (driver in fs.list(luceneDir)) {
            if (fs.metadata(driver).isDirectory.not()) continue
            for (index in fs.list(driver)) {
                if ((driver.name to index.name) in keep) continue
                fs.deleteRecursively(index, mustExist = false)
                deleted++
            }
            if (fs.list(driver).isEmpty()) fs.delete(driver)
        }
        return deleted
    }

    private val fs get() = FileSystem.SYSTEM

    private fun driverSegment(book: Book) = book.bookMetaData.driverName ?: error("book ${book.initials} has no driver")

    private fun finalPath(book: Book): Path = searchDir / driverSegment(book) / "${book.initials}.sqlite"

    private fun legacyPath(book: Book): Path = luceneDir / driverSegment(book) / book.initials

    private fun creatingPath(p: Path): Path = p.parent!! / "${p.name}.creating"

    private fun bookLock(book: Book): SynchronizedObject =
        synchronized(lock) { bookLocks.getOrPut(book.initials) { SynchronizedObject() } }

    /**
     * Closes [index], just evicted from [open], and drops the book's cached searcher, which holds it. With [reopen] a
     * search that still reaches the closed index is answered by [getIndex] (the new file after a rebuild, a reopened
     * one after [closeAllIndexes]); after a delete there is nothing to reopen and it fails.
     */
    private fun retire(book: Book, index: FtsIndex?, reopen: Boolean) {
        if (index != null) {
            if (reopen) index.successor = { getIndex(book) }
            index.close()
        }
        book.resetSearcher()
    }

    /** Deletes a SQLite file and the journal/WAL files next to it. */
    private fun deleteDb(p: Path) {
        for (suffix in listOf("", "-journal", "-wal", "-shm")) {
            val f = p.parent!! / (p.name + suffix)
            if (fs.exists(f)) fs.delete(f)
        }
    }

    /** Removes `*.creating` files left by an interrupted build anywhere under the search directory. */
    private fun deleteCreatingLeftovers() {
        try {
            if (!fs.exists(searchDir)) return
            for (f in fs.listRecursively(searchDir)) {
                if (f.name.contains(".creating")) fs.delete(f)
            }
        } catch (e: IOException) {
            // best effort: a leftover is also deleted by the next build of that book
        }
    }

    private companion object {
        fun projectSubdir(name: String): Path =
            CWProject.instance().getWritableProjectSubdirPath(name, false) ?: error("no writable project directory")
    }
}
