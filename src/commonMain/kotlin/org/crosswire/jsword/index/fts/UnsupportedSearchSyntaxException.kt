package org.crosswire.jsword.index.fts

import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookException

/**
 * A search used query syntax the FTS5 index does not support (K6-3): a `field:` other than `content:`/`strong:`, `^`
 * boosts, `[a TO b]` and `{a TO b}` term ranges, a quoted phrase `strong:"a b"`, a fuzzy `strong:term~`, or ranked
 * search ([org.crosswire.jsword.index.search.SearchModifier.isRanked]). [syntax] names the offending construct.
 */
class UnsupportedSearchSyntaxException(val syntax: String) :
    BookException(JSMsg.gettext("Unsupported search syntax: {0}", syntax))
