package org.crosswire.jsword.index.fts

import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookException

/**
 * A search used query syntax the FTS5 index does not support (K6-3): a `field:` other than `strong:`, `^` boosts,
 * `[a TO b]` ranges, or ranked search. [syntax] names the offending construct.
 */
class UnsupportedSearchSyntaxException(val syntax: String) :
    BookException(JSMsg.gettext("Unsupported search syntax: {0}", syntax))
