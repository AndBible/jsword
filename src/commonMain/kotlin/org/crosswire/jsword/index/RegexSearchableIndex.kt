package org.crosswire.jsword.index

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.passage.Key
import kotlin.time.TimeMark

/** Budget shared by every index a search scans; [isCancelled] is polled between rows. */
class RegexSearchBudget(val deadline: TimeMark, val isCancelled: () -> Boolean = { false }) {
    val exhausted: Boolean get() = isCancelled() || deadline.hasPassedNow()
}

/** Matches of a regex scan; [truncated] is true when the scan stopped early because its budget ran out. */
data class RegexSearchResult(val keys: Key, val truncated: Boolean)

/** An [Index] that can scan its stored entry texts with a regular expression. */
interface RegexSearchableIndex {
    /**
     * Entries whose text contains a match of [regex], in canonical order, limited to verses in [restriction]
     * (null = whole book) and to [maxResults]; stops early with truncated = true when [budget] is exhausted.
     */
    @Throws(BookException::class)
    fun findRegex(regex: Regex, restriction: Key?, maxResults: Int, budget: RegexSearchBudget): RegexSearchResult
}
