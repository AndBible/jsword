package org.crosswire.jsword.index.fts

import fi.sykero.kdom.Element
import okio.Path
import org.crosswire.common.progress.Progress
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.index.IndexPolicy
import org.crosswire.jsword.index.analysis.BookAnalyzers
import org.crosswire.jsword.index.analysis.StrongsTokens
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.Verse

/** Thrown inside a build when its job was cancelled; the manager maps it to status UNDONE. */
internal class IndexBuildCancelled : Exception("index build cancelled")

/**
 * Builds one book's FTS5 index file. Mirrors `LuceneIndex.generateSearchIndexImpl`:
 * every key the global key list iterates (the iterated root of a tree keyed book excepted), the entry text is the canonical text of the OSIS
 * fragment ([OSISUtil.getCanonicalText]) with verse 0 left out as Lucene's `content` field did (K6-4), Strong's numbers
 * only when the book has the feature and the policy asks for them, and an entry without text and without Strong's
 * numbers is not added (Lucene added a document only when it had more than the key field). Keys whose fragment cannot be
 * read are collected as errors and skipped. Progress is `98 * keysDone / keyCount` percent, like Lucene.
 *
 * @param fragmentOf the OSIS fragment of a key; a seam for tests, production reads the book.
 */
internal class FtsIndexBuilder(
    private val book: Book,
    private val policy: IndexPolicy,
    private val fragmentOf: (Book, Key) -> Element = { b, k -> BookData(b, k).getOsisFragment(false) },
) {
    /**
     * Writes the index to [target] (created fresh) and returns the keys that could not be indexed. The store is closed
     * on every path. [isCancelled] is polled after every key; true aborts with [IndexBuildCancelled], leaving [target]
     * for the caller to delete.
     */
    fun build(target: Path, job: Progress, isCancelled: () -> Boolean): List<Key> {
        val errors = ArrayList<Key>()
        val store = FtsIndexStore.create(target)
        try {
            val root = book.getGlobalKeyList()
            val size = root.getCardinality()
            val includeStrongs = book.bookMetaData.hasFeature(FeatureType.STRONGS_NUMBERS) && policy.isStrongsIndexed
            val state = State(size)
            store.beginBuild()
            walk(store, root, job, errors, includeStrongs, state, isCancelled)
            store.writeMetadata(metadata(size))
            store.commitBuild()
        } finally {
            store.close()
        }
        return errors
    }

    private class State(val size: Int) {
        var done = 0
        var nextRow = 1L
        var percent = 0
        var oldRoot = ""
    }

    private fun walk(store: FtsIndexStore, key: Key, job: Progress, errors: MutableList<Key>, includeStrongs: Boolean,
                     s: State, isCancelled: () -> Boolean) {
        val analyzer = BookAnalyzers.forBook(book)
        for (sub in key) {
            if (isCancelled()) throw IndexBuildCancelled()
            // A tree key (general book) iterates itself first and then every descendant, depth first (KeyIterator), so
            // no recursion is needed: Lucene's recursion on canHaveChildren() never terminated for such a key. The
            // iterated root itself is the nameless container and has no text.
            if (sub === key) continue

            val osis = try { fragmentOf(book, sub) } catch (e: BookException) { errors.add(sub); continue }
            val text = if (sub is Verse && sub.verse == 0) "" else OSISUtil.getCanonicalText(osis)
            val strongs = if (includeStrongs) StrongsTokens.fromOsis(osis) else emptyList()
            if (text.isNotEmpty() || strongs.isNotEmpty()) {
                store.addEntry(s.nextRow++, sub.getOsisRef(), analyzer.analyze(text), strongs)
            }

            val rootName = sub.getRootName()
            if (rootName != s.oldRoot) { s.oldRoot = rootName; job.sectionName = rootName }
            s.done++
            val percent = if (s.size <= 0) 0 else minOf(WORK_ESTIMATE, WORK_ESTIMATE * s.done / s.size)
            if (percent != s.percent) { s.percent = percent; job.work = percent }
        }
    }

    /**
     * Index identity. The source fingerprint is `keys=<cardinality of the global key list>;version=<conf Version>`: cheap
     * to compute, and it changes when a module update adds or removes entries or bumps its version. Staleness checks
     * ([FtsIndexManager.needsReindexing]) compare the book version, not the fingerprint, which is for diagnostics.
     */
    private fun metadata(keyCount: Int): FtsIndexMetadata {
        val version = book.bookMetaData.getProperty(SwordBookMetaData.KEY_VERSION)
        return FtsIndexMetadata(
            schemaVersion = FTS_SCHEMA_VERSION,
            analyzerId = BookAnalyzers.analyzerId(book.language.code),
            codec = SearchTokenCodec.ID,
            bookInitials = book.initials,
            bookVersion = version,
            sourceFingerprint = "keys=$keyCount;version=${version ?: ""}",
        )
    }

    private companion object {
        /** The share of the work spent on indexing; the rest is cleanup (Lucene's `WORK_ESTIMATE`). */
        const val WORK_ESTIMATE = 98
    }
}
