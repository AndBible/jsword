package org.crosswire.jsword.index.fts

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.index.AbstractIndex
import org.crosswire.jsword.index.analysis.BookAnalyzers
import org.crosswire.jsword.passage.AbstractPassage
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.NoSuchVerseException
import org.crosswire.jsword.passage.VerseFactory
import org.crosswire.jsword.versification.system.Versifications

/**
 * A searchable view of one book's FTS5 index file. The connection is used by one caller at a time (calls are
 * serialized), and [close] is idempotent; a [find] after [close] fails with the baseline "Search failed." error.
 *
 * Task 11 supplies the query evaluator; until then [find] handles a single plain term only (see [find]).
 */
class FtsIndex(private val book: Book, private val store: FtsIndexStore) : AbstractIndex() {
    private val lock = SynchronizedObject()
    private var closed = false

    /**
     * Finds the entries containing [search], analyzed with the book's analyzer.
     *
     * Results mirror `LuceneIndex.find`: each match's osisRef becomes a verse of the book's versification, added to
     * `book.createEmptyKeyList()` with event suppression, in canonical (build) order. A non-verse key (dictionary,
     * general book) fails the whole search with a "Search failed." [BookException] wrapping the
     * [NoSuchVerseException], exactly as the baseline `VerseCollector` does (those books stay unsearchable, K6 review
     * focus 3). A null [search] returns an empty key list.
     *
     * Only one plain term is supported here: one that analyzes to a single token is matched as that token, several
     * tokens (a term the analyzer splits) as a phrase, none as an empty result. Any query syntax or whitespace throws
     * [UnsupportedSearchSyntaxException]; so does a ranked [searchModifier] (`"ranked"`).
     */
    @Throws(BookException::class)
    override fun find(search: String?): Key {
        val results = book.createEmptyKeyList()
        if (search == null) return results
        if (searchModifier?.isRanked == true) throw UnsupportedSearchSyntaxException("ranked")
        val bad = search.firstOrNull { it.isWhitespace() || it in SYNTAX }
        if (bad != null) throw UnsupportedSearchSyntaxException(bad.toString())

        val tokens = BookAnalyzers.forBook(book).analyze(search)
        if (tokens.isEmpty()) return results
        val expression = if (tokens.size == 1) SearchTokenCodec.encode(tokens[0].text)
        else tokens.joinToString(" ", "\"", "\"") { SearchTokenCodec.encode(it.text) }

        val refs = synchronized(lock) {
            if (closed) throw BookException(JSMsg.gettext("Search failed."))
            try {
                store.match(expression).sorted().map { store.osisRef(it) }
            } catch (e: Exception) {
                throw BookException(JSMsg.gettext("Search failed."), e)
            }
        }

        val v11n = Versifications.instance().getVersification(book.bookMetaData.getProperty("Versification"))
        val passage = results as? AbstractPassage
        passage?.raiseEventSuppresion()
        passage?.raiseNormalizeProtection()
        try {
            for (ref in refs) {
                val key = try {
                    VerseFactory.fromString(v11n, ref) ?: throw NoSuchVerseException(ref)
                } catch (e: NoSuchVerseException) {
                    throw BookException(JSMsg.gettext("Search failed."), e)
                }
                results.addAll(key)
            }
        } finally {
            passage?.lowerNormalizeProtection()
            passage?.lowerEventSuppressionAndTest()
        }
        return results
    }

    @Throws(NoSuchKeyException::class)
    override fun getKey(name: String?): Key = book.getKey(name)

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            store.close()
        }
    }

    private companion object {
        /** Characters of the Lucene query language this stage does not evaluate. */
        const val SYNTAX = "\"*?~:^()[]{}+-!&|\\/"
    }
}
