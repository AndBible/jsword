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
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
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
    // All three maps are keyed by [indexKey], the same (driver, safe initials) pair that names the index file.
    private val open = HashMap<String, FtsIndex>()
    private val bookLocks = HashMap<String, SynchronizedObject>()

    /** Books scheduled or building, each with the token of the request that owns the registration. */
    private val building = HashMap<String, Registration>()

    /**
     * Held for a whole build under the serial policy, so only one book builds at a time. LOCK ORDER: [serialLock], then
     * a book lock, then [lock] (which is only ever held briefly, never while waiting for another lock).
     * [deleteIndex] takes only the book lock of its own book, never [serialLock]: it runs on the main thread, and
     * must not wait for the build of another book (which takes minutes).
     */
    private val serialLock = SynchronizedObject()

    /** Test seams: run in [scheduleIndexCreation] once the book is registered, and in [deleteIndex] after it cancelled the job. */
    internal var scheduleHook: ((Book) -> Unit)? = null
    internal var deleteHook: ((Book) -> Unit)? = null
    private var policy: IndexPolicy = IndexPolicyAdapter()

    init { deleteCreatingLeftovers() }

    override var indexPolicy: IndexPolicy?
        get() = synchronized(lock) { policy }
        set(value) = synchronized(lock) { policy = value ?: IndexPolicyAdapter() }

    /** False for a null book and for a book without a driver (it has no index location; the Lucene manager did the same). */
    override fun isIndexed(book: Book?): Boolean = book != null && hasDriver(book) && fs.exists(finalPath(book))

    @Throws(BookException::class)
    override fun getIndex(book: Book?): Index {
        requireNotNull(book) { "null book" }
        synchronized(lock) {
            val key = indexKey(book)
            open[key]?.let { return it }
            val store = FtsIndexStore.open(finalPath(book))
                // TRANSLATOR: Error condition: The search index of a book is missing, unreadable or of an incompatible version.
                ?: throw BookException(JSMsg.gettext("The search index could not be opened. It may need to be rebuilt."))
            return FtsIndex(book, store).also { open[key] = it }
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
     * waited for the build can be followed at once by a new request. A request still queued behind another book's
     * build (serial policy) when its book is deleted is dropped silently: [deleteIndex] takes over its registration.
     */
    override fun scheduleIndexCreation(book: Book?) {
        requireNotNull(book) { "null book" }
        var registered = false
        var job: Progress? = null
        val token = Registration()
        var key = ""
        try {
            key = indexKey(book)
            if (!synchronized(lock) { if (building.containsKey(key)) false else { building[key] = token; true } }) return
            registered = true
            scheduleHook?.invoke(book)
            book.indexStatus = IndexStatus.SCHEDULED
            val final = finalPath(book)
            val creating = creatingPath(final)
            val jobName = JSMsg.gettext("Creating index. Processing {0}", book.initials)
            val cancelled = kotlinx.atomicfu.atomic(false)
            val newJob = JobManager.createJob(Progress.CREATE_INDEX.replace("%s", book.initials), jobName) { cancelled.value = true }
            job = newJob
            token.job.value = newJob
            newJob.beginJob(jobName)
            val currentPolicy = indexPolicy!!
            // Serial policy: one build at a time overall, then the book's own lock inside it; otherwise one per book.
            val run = {
              synchronized(bookLock(key)) {
                var finalStatus = IndexStatus.UNDONE
                // Dropped by a deleteIndex while this request waited for the serial lock: touch nothing.
                val owner = synchronized(lock) { building[key] === token }
                try {
                    if (!owner) throw IndexBuildCancelled()
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
                        open.remove(key)
                    }
                    retire(book, replaced, reopen = true)
                    finalStatus = IndexStatus.DONE
                    // The legacy Lucene directory is also the "engine changed" signal, so it goes only now.
                    try { deleteLegacyLuceneIndex(book) } catch (e: Exception) { /* best effort: the orphan sweep never touches installed books, a retry happens at the next rebuild */ }
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
                    if (owner) book.indexStatus = finalStatus
                    newJob.done()
                    if (owner) synchronized(lock) { building.remove(key) }
                    registered = false
                }
              }
            }
            if (currentPolicy.isSerial()) synchronized(serialLock) { run() } else run()
        } catch (e: Exception) {
            Reporter.informUser(this, e)
        } finally {
            if (registered) { // failed before the build lock was reached
                if (synchronized(lock) { building[key] === token }) {
                    book.indexStatus = IndexStatus.UNDONE
                    synchronized(lock) { building.remove(key) }
                }
                job?.done()
            }
        }
    }

    @Throws(BookException::class)
    override fun deleteIndex(book: Book?) {
        requireNotNull(book) { "null book" }
        // A build of this book in progress is cancelled and waited for (the book lock it holds), so it ends UNDONE quietly
        // instead of failing at the rename after its files were deleted. The serial lock is NOT taken: a build of another
        // book must not block this call (see [serialLock]).
        if (!hasDriver(book)) { book.indexStatus = IndexStatus.UNDONE; return } // no index location, nothing to delete
        val key = indexKey(book)
        val queued = synchronized(lock) { building[key] }
        // Cancel the job of the registered request itself: JobManager.findJob returns the FIRST job of that name, which can
        // be a stale one (a dropped queued request's job that has not finished yet).
        (queued?.job?.value ?: JobManager.findJob(Progress.CREATE_INDEX.replace("%s", book.initials)))?.cancel()
        deleteHook?.invoke(book)
        synchronized(bookLock(key)) {
            // A request that is only queued behind another book's build (it does not hold the book lock) is dropped; one
            // that was building has already left the set. A newer request is not touched (different token).
            synchronized(lock) { if (queued != null && building[key] === queued) building.remove(key) }
            retire(book, synchronized(lock) { open.remove(key) }, reopen = false)
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
    fun hasLegacyLuceneIndex(book: Book): Boolean = hasDriver(book) && fs.exists(legacyPath(book))

    /** Deletes the legacy Lucene index directory of [book] (and its `.creating` sibling), if any. */
    fun deleteLegacyLuceneIndex(book: Book) {
        if (!hasDriver(book)) return
        val dir = try { legacyPath(book) } catch (e: IllegalArgumentException) {
            log.warn("Not deleting a legacy index outside the index directory: {}", e.message)
            return
        }
        fs.deleteRecursively(dir, mustExist = false)
        fs.deleteRecursively(creatingPath(dir), mustExist = false)
    }

    /**
     * Frees the disk space of the legacy Lucene indexes of [installed] books: deletes the CONTENTS of each
     * `lucene/<driver>/<initials>/` but keeps the (now empty) directory, because its existence is the "search engine
     * changed, rebuild the index" signal ([hasLegacyLuceneIndex]). The directory goes when the new index is built
     * ([deleteLegacyLuceneIndex]). Returns how many directories were emptied.
     */
    fun emptyLegacyLuceneIndexes(installed: Collection<Book>): Int {
        var emptied = 0
        for (book in installed) {
            if (!hasDriver(book)) continue
            val dir = try { legacyPath(book) } catch (e: IllegalArgumentException) { continue }
            if (!fs.exists(dir) || fs.metadata(dir).isDirectory.not()) continue
            val children = safeChildren(dir)
            if (children.isEmpty()) continue
            for (c in children) fs.deleteRecursively(c, mustExist = false)
            emptied++
        }
        return emptied
    }

    /**
     * Deletes the index files `search/<driver>/<initials>.sqlite` (with their `.creating`, `-journal`, `-wal` and `-shm`
     * files) of books that are not among [installed], and empty driver directories. An index of a document that was
     * removed without going through the app (or whose delete failed) would otherwise stay forever. Returns how many
     * books' files were removed. Call it only after every installed book is registered.
     */
    fun deleteOrphanIndexes(installed: Collection<Book>): Int {
        if (!fs.exists(searchDir)) return 0
        val keep = installed.filter { hasDriver(it) }.mapTo(HashSet()) { driverSegment(it) to safeSegment(it.initials) }
        val orphans = HashSet<Pair<String, String>>()
        for (driver in safeChildren(searchDir)) {
            if (!fs.metadata(driver).isDirectory) continue
            for (f in safeChildren(driver)) {
                val at = f.name.lastIndexOf(".sqlite")
                if (at <= 0) continue // not one of ours
                val id = f.name.substring(0, at)
                if ((driver.name to id) in keep) continue
                fs.deleteRecursively(f, mustExist = false)
                orphans += driver.name to id
            }
            if (fs.list(driver).isEmpty()) fs.delete(driver)
        }
        return orphans.size
    }

    /**
     * Deletes every legacy Lucene index directory `lucene/<driver>/<initials>` that belongs to none of the [installed]
     * books (same driver and initials), plus empty driver directories left behind. Returns how many were deleted.
     */
    fun deleteOrphanLegacyLuceneIndexes(installed: Collection<Book>): Int {
        if (!fs.exists(luceneDir)) return 0
        // Lucene named its directories by the raw initials, so the raw name keeps an installed book's directory too.
        val keep = installed.filter { hasDriver(it) }.flatMapTo(HashSet()) {
            listOf(driverSegment(it) to safeSegment(it.initials), driverSegment(it) to it.initials)
        }
        var deleted = 0
        for (driver in safeChildren(luceneDir)) {
            if (fs.metadata(driver).isDirectory.not()) continue
            for (index in safeChildren(driver)) {
                if ((driver.name to index.name) in keep) continue
                fs.deleteRecursively(index, mustExist = false)
                deleted++
            }
            if (fs.list(driver).isEmpty()) fs.delete(driver)
        }
        return deleted
    }

    private val fs get() = FileSystem.SYSTEM

    /**
     * The entries of [dir] that really are direct children of it. okio builds each entry as `dir / name` and parses `\`
     * (and `/`) in the name as a separator, so a directory entry literally named `..\..` comes back as `dir/../..`, a path
     * OUTSIDE [dir]. Such entries (and `.`/`..`) are skipped and logged, never touched, so no sweep can delete through them.
     */
    private fun safeChildren(dir: Path): List<Path> = fs.list(dir).filter { c ->
        val ok = c.parent == dir && c.name != "." && c.name != ".." && c.name.isNotEmpty()
        if (!ok) log.warn("Skipping the entry {} of {}: not a plain child", c, dir)
        ok
    }

    private fun hasDriver(book: Book) = book.bookMetaData.driverName != null

    private fun driverSegment(book: Book) = safeSegment(book.bookMetaData.driverName ?: error("book ${book.initials} has no driver"))

    /** Key of the per-book caches and locks: the (driver, initials) pair of the file path, both as safe segments. */
    private fun indexKey(book: Book) = driverSegment(book) + "/" + safeSegment(book.initials)

    internal fun finalPathForTest(book: Book): Path = finalPath(book)

    private fun finalPath(book: Book): Path = contained(searchDir, driverSegment(book), "${safeSegment(book.initials)}.sqlite")

    private fun legacyPath(book: Book): Path = contained(luceneDir, driverSegment(book), safeSegment(book.initials))

    /** [base] / [segments], failing closed (IllegalArgumentException) if the result is not strictly below [base]. */
    private fun contained(base: Path, vararg segments: String): Path {
        val p = segments.fold(base) { a, s -> a / s }
        require(isStrictlyUnder(base, p)) { "path escapes ${base.name}: $p" }
        return p
    }

    private fun creatingPath(p: Path): Path = p.parent!! / "${p.name}.creating"

    private fun bookLock(key: String): SynchronizedObject =
        synchronized(lock) { bookLocks.getOrPut(key) { SynchronizedObject() } }

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
            fun walk(dir: Path) {
                for (f in safeChildren(dir)) {
                    if (f.name.contains(".creating")) fs.delete(f)
                    else if (fs.metadata(f).isDirectory) walk(f)
                }
            }
            walk(searchDir)
        } catch (e: IOException) {
            // best effort: a leftover is also deleted by the next build of that book
        }
    }

    /** A scheduled or building request: identity token of the registration, plus its progress job once created. */
    private class Registration {
        val job = kotlinx.atomicfu.atomic<Progress?>(null)
    }

    internal companion object {
        private val log: Logger = LoggerFactory.getLogger(FtsIndexManager::class)

        /**
         * [name] as ONE safe file-name segment: it can never be empty, `.`/`..`, or contain a separator, so it cannot
         * climb out of the directory it is appended to. Ordinary names (letters, digits, `_`, `-`, `.` not at the
         * start, non-ASCII, ...) come out byte-for-byte unchanged, so existing index file names do not change.
         * Percent-encoded as `%XX`: `/`, `\`, control characters (NUL included), DEL, a LEADING `.`, a drive-letter colon (`C:x`, which okio reads as a root), and a `%` that
         * is followed by two hex digits (it would look like an escape) or is the whole name. Other `%` (`100%`) stay as they are.
         * The mapping is injective: a `%` followed by two hex digits in the result is always an escape, and the empty name
         * (the only one that yields a bare `%`) becomes `%`.
         */
        fun safeSegment(name: String): String {
            if (name.isEmpty()) return "%"
            val sb = StringBuilder()
            for ((i, c) in name.withIndex()) {
                val escape = c == '/' || c == '\\' || c.code < 0x20 || c.code == 0x7f || (c == '.' && i == 0) || (c == ':' && i == 1 && name[0].isAsciiLetter()) ||
                    (c == '%' && (name.length == 1 || (i + 2 < name.length && name[i + 1].isHexDigit() && name[i + 2].isHexDigit())))
                if (escape) sb.append('%').append(c.code.toString(16).uppercase().padStart(2, '0')) else sb.append(c)
            }
            return sb.toString()
        }

        private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'

        private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

        /** True when [p] (normalised) is a proper descendant of [base] (normalised). */
        fun isStrictlyUnder(base: Path, p: Path): Boolean {
            val b = base.normalized().segments
            val q = p.normalized().segments
            return q.size > b.size && q.subList(0, b.size) == b && p.normalized().root == base.normalized().root
        }

        fun projectSubdir(name: String): Path =
            CWProject.instance().getWritableProjectSubdirPath(name, false) ?: error("no writable project directory")
    }
}
