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
            FtsIndexStore.open(finalPath(book))?.use { it.metadata() }
        } catch (e: Exception) { null } ?: return true
        val version = book.bookMetaData.getProperty(org.crosswire.jsword.book.sword.SwordBookMetaData.KEY_VERSION)
        return meta.schemaVersion != FTS_SCHEMA_VERSION ||
            meta.analyzerId != BookAnalyzers.analyzerId(book.language.code) ||
            meta.codec != SearchTokenCodec.ID ||
            meta.bookVersion != version
    }

    override fun scheduleIndexCreation(book: Book?) {
        requireNotNull(book) { "null book" }
        // A second request for a book that is already scheduled or building is dropped: the running build serves both.
        if (!synchronized(lock) { building.add(book.initials) }) return
        book.indexStatus = IndexStatus.SCHEDULED
        var finalStatus = IndexStatus.UNDONE
        val final = finalPath(book)
        val creating = creatingPath(final)
        val jobName = JSMsg.gettext("Creating index. Processing {0}", book.initials)
        val cancelled = kotlinx.atomicfu.atomic(false)
        val job = JobManager.createJob(Progress.CREATE_INDEX.replace("%s", book.initials), jobName) { cancelled.value = true }
        job.beginJob(jobName)
        val currentPolicy = indexPolicy!!
        try {
            // Serial policy: one build at a time overall; otherwise one per book (as Lucene's mutex choice).
            val mutex = if (currentPolicy.isSerial()) serialLock else bookLock(book)
            synchronized(mutex) {
                try {
                    fs.createDirectories(final.parent!!)
                    deleteDb(creating)
                    // Drop a stale open handle: the final file is about to be replaced.
                    closeOpen(book)
                    book.indexStatus = IndexStatus.CREATING
                    val errors = FtsIndexBuilder(book, currentPolicy, fragmentOf)
                        .build(creating, job) { cancelled.value || job.isFinished }
                    job.isCancelable = false
                    if (cancelled.value || job.isFinished) throw IndexBuildCancelled()
                    fs.atomicMove(creating, final)
                    // Evict a handle opened on the replaced file while the build ran; getIndex reopens the new one.
                    closeOpen(book)
                    finalStatus = IndexStatus.DONE
                    if (errors.isNotEmpty()) {
                        // TRANSLATOR: It is likely that one or more verses could not be indexed due to errors in those verses.
                        // This message gives a listing of them to the user.
                        Reporter.informUser(this, JSMsg.gettext("The following verses have errors and could not be indexed\n{0}",
                            errors.joinToString("") { "$it\n" }))
                    }
                } finally {
                    deleteDb(creating)
                }
            }
        } catch (e: IndexBuildCancelled) {
            // cancelled by the user: UNDONE, nothing to report
        } catch (e: Exception) {
            Reporter.informUser(this, e)
        } finally {
            book.indexStatus = finalStatus
            job.done()
            synchronized(lock) { building.remove(book.initials) }
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
        val mutex = if (indexPolicy!!.isSerial()) serialLock else bookLock(book)
        synchronized(mutex) {
            closeOpen(book)
            val final = finalPath(book)
            try {
                deleteDb(creatingPath(final))
                deleteDb(final)
            } catch (e: IOException) {
                // TRANSLATOR: Error condition: The index could not be deleted.
                throw BookException(JSMsg.gettext("Failed to delete search index."), e)
            }
            book.indexStatus = IndexStatus.UNDONE
            book.resetSearcher()
        }
    }

    override fun closeAllIndexes() {
        val all = synchronized(lock) { open.values.toList().also { open.clear() } }
        all.forEach { it.close() }
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

    private fun closeOpen(book: Book) {
        val index = synchronized(lock) { open.remove(book.initials) }
        index?.close()
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
