package org.crosswire.jsword.index.fts

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.index.AbstractIndex
import org.crosswire.jsword.index.Index
import org.crosswire.jsword.index.RegexSearchBudget
import org.crosswire.jsword.index.RegexSearchResult
import org.crosswire.jsword.index.RegexSearchableIndex
import org.crosswire.jsword.index.analysis.BookAnalyzers
import org.crosswire.jsword.index.fts.query.FtsQueryEvaluator
import org.crosswire.jsword.index.fts.query.LuceneSyntaxParser
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
 * Queries are parsed by [LuceneSyntaxParser] and evaluated by [FtsQueryEvaluator] (K6 task 11).
 */
class FtsIndex internal constructor(internal val book: Book, private val store: FtsIndexStore) : AbstractIndex(), RegexSearchableIndex {
    private val lock = SynchronizedObject()
    private var closed = false

    /**
     * Set by [FtsIndexManager] before it closes this index because a rebuild or [FtsIndexManager.closeAllIndexes]
     * replaced it: a [find] that reaches the closed index (a caller holding it or the book's cached searcher while the
     * swap happened) is answered by the current index instead of failing. Null after a delete: the find fails.
     */
    internal var successor: (() -> Index)? = null

    /**
     * Finds the entries containing [search], analyzed with the book's analyzer.
     *
     * Results mirror `LuceneIndex.find`: each match's osisRef becomes a verse of the book's versification, added to
     * `book.createEmptyKeyList()` with event suppression, in canonical (build) order. A non-verse key (dictionary,
     * general book) fails the whole search with a "Search failed." [BookException] wrapping the
     * [NoSuchVerseException], exactly as the baseline `VerseCollector` does (those books stay unsearchable, K6 review
     * focus 3). A null [search] returns an empty key list.
     *
     * [search] is Lucene 3.6.2 classic query syntax (see [LuceneSyntaxParser]). Invalid syntax fails with a
     * "Search failed." [BookException] whose cause is a [org.crosswire.jsword.index.fts.query.QueryParseException] (the baseline's `ParseException`);
     * syntax outside the K6 query language (see [UnsupportedSearchSyntaxException]) and a ranked [searchModifier]
     * (`"ranked"`) throw [UnsupportedSearchSyntaxException].
     */
    @Throws(BookException::class)
    override fun find(search: String?): Key {
        val results = book.createEmptyKeyList()
        if (search == null) return results
        if (searchModifier?.isRanked == true) throw UnsupportedSearchSyntaxException("ranked")
        val analyzer = BookAnalyzers.forBook(book)
        val node = try {
            LuceneSyntaxParser.parse(search, analyzer)
        } catch (e: UnsupportedSearchSyntaxException) {
            throw e
        } catch (e: Exception) { // QueryParseException, or an analyzer failure: never an untyped crash
            throw BookException(JSMsg.gettext("Search failed."), e)
        }

        var forward: (() -> Index)? = null
        val refs = synchronized(lock) {
            if (closed) {
                forward = successor ?: throw BookException(JSMsg.gettext("Search failed."))
                return@synchronized emptyList()
            }
            try {
                FtsQueryEvaluator(store, analyzer).evaluate(node).sorted().map { store.osisRef(it) }
            } catch (e: UnsupportedSearchSyntaxException) {
                throw e
            } catch (e: Exception) {
                throw BookException(JSMsg.gettext("Search failed."), e)
            }
        }

        forward?.let { next -> return next().also { it.searchModifier = searchModifier }.find(search) }

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

    /**
     * Scans the stored entry texts in build (canonical) order for [regex], skipping rows outside [restriction]
     * before matching. The [budget] is checked before every row: when it is exhausted the scan stops with
     * `truncated = true`; reaching [maxResults] stops it with `truncated = false`. Closed/successor forwarding and
     * error wrapping are as in [find]; an index closed in the middle of the scan forwards the whole scan.
     *
     * The rows are read [REGEX_BATCH_ROWS] at a time under the index lock, and matched outside it. Android's ICU regex
     * engine matches a `toString()` copy natively, so [BudgetedText] cannot interrupt a runaway pattern there: such a
     * match then must not block [close], a rebuild or a [find] (the caller stops waiting for it instead, see
     * `SearchControl.getMultiRegexResults`). A row whose match overflows the stack counts as no match.
     */
    @Throws(BookException::class)
    override fun findRegex(regex: Regex, restriction: Key?, maxResults: Int, budget: RegexSearchBudget): RegexSearchResult {
        val v11n = Versifications.instance().getVersification(book.bookMetaData.getProperty("Versification"))
        val hits = ArrayList<String>()
        var truncated = false
        var afterRowId = Long.MIN_VALUE
        scan@ while (true) {
            var forward: (() -> Index)? = null
            val batch = synchronized(lock) {
                if (closed) {
                    forward = successor ?: throw BookException(JSMsg.gettext("Search failed."))
                    return@synchronized emptyList()
                }
                try {
                    store.entryTexts(afterRowId, REGEX_BATCH_ROWS)
                } catch (e: Exception) {
                    throw BookException(JSMsg.gettext("Search failed."), e)
                }
            }
            forward?.let { next ->
                val idx = next().also { it.searchModifier = searchModifier }
                return (idx as? RegexSearchableIndex ?: throw BookException(JSMsg.gettext("Search failed.")))
                    .findRegex(regex, restriction, maxResults, budget)
            }
            if (batch.isEmpty()) break
            for (row in batch) {
                if (hits.size >= maxResults) break@scan
                if (budget.exhausted) { truncated = true; break@scan }
                val matched = try {
                    val inRestriction = restriction == null ||
                        restriction.contains(VerseFactory.fromString(v11n, row.osisRef) ?: throw NoSuchVerseException(row.osisRef))
                    inRestriction && regex.containsMatchIn(BudgetedText(row.text, budget))
                } catch (_: BudgetExhaustedException) {
                    truncated = true
                    break@scan
                } catch (e: Exception) {
                    throw BookException(JSMsg.gettext("Search failed."), e)
                } catch (e: Throwable) {
                    if (!isStackOverflow(e)) throw e
                    log.warn("Regex match overflowed the stack in {} {}: counted as no match", book.initials, row.osisRef)
                    false
                }
                if (matched) hits += row.osisRef
            }
            afterRowId = batch.last().rowId
        }

        val results = book.createEmptyKeyList()
        val passage = results as? AbstractPassage
        passage?.raiseEventSuppresion()
        passage?.raiseNormalizeProtection()
        try {
            for (ref in hits) {
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
        return RegexSearchResult(results, truncated)
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
        /** Rows read under the lock per step of a regex scan (a few hundred verses: kilobytes of text). */
        const val REGEX_BATCH_ROWS = 512
        val log: Logger = LoggerFactory.getLogger(FtsIndex::class)
    }
}

/**
 * True for the JVM's `StackOverflowError` (a deeply recursive match), without a JVM-only type in common code.
 * Kotlin/Native has no catchable stack overflow, so there this is never true.
 */
private fun isStackOverflow(e: Throwable): Boolean = e::class.simpleName == "StackOverflowError"

private class BudgetExhaustedException : RuntimeException()

/**
 * The text a regex scans, polling [budget] every [CHECK_INTERVAL] character reads so that a catastrophically
 * backtracking pattern stops at the deadline instead of running for minutes. The JVM and the Kotlin/Native regex
 * engines read their input through [get]; Android's ICU engine copies it with [toString] and is not interrupted.
 */
private class BudgetedText(private val text: String, private val budget: RegexSearchBudget) : CharSequence {
    private var reads = 0
    override val length: Int get() = text.length
    override fun get(index: Int): Char {
        if (++reads % CHECK_INTERVAL == 0 && budget.exhausted) throw BudgetExhaustedException()
        return text[index]
    }
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = text.subSequence(startIndex, endIndex)
    override fun toString(): String = text

    private companion object { const val CHECK_INTERVAL = 1024 }
}
