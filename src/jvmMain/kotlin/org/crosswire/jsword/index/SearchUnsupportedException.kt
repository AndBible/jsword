package org.crosswire.jsword.index

import org.crosswire.jsword.book.BookException

/**
 * Thrown on platforms without a search index implementation (iOS until K6 replaces Lucene with FTS5).
 * It is a [BookException] so every existing search error path (`Book.find`, index creation) handles it.
 */
class SearchUnsupportedException(message: String = "Search is not supported on this platform yet") : BookException(message)
