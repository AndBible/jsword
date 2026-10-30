package org.crosswire.jsword.index.fts

import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.passage.Key

/**
 * The policy for entries a build could not read (a corrupt entry in a module file). A few unreadable entries must not
 * make a module unsearchable: the index is published and the user is told which entries are missing. Above the
 * threshold the module is more broken than worth indexing: nothing is published and the previous index stays.
 * A published index with skipped entries is complete as far as the module can be read; it never triggers a rebuild.
 */
internal object IndexReadErrors {
    /** Unreadable entries tolerated whatever the size of the module. */
    const val MIN_TOLERATED_ERRORS = 5

    /** Unreadable entries tolerated for a large module, as a percentage of the entries processed. */
    const val TOLERATED_PERCENT = 1

    /** References listed when the index is published with skipped entries. */
    const val MAX_WARNED_REFERENCES = 10

    /** References listed when the build is refused. */
    const val MAX_REFUSAL_REFERENCES = 20

    /**
     * True when an index with [errors] unreadable entries out of [processed] may be published: at most
     * max([MIN_TOLERATED_ERRORS], [TOLERATED_PERCENT] percent). A module where every entry failed is never published.
     */
    fun isTolerable(errors: Int, processed: Int): Boolean {
        if (errors == 0) return true
        if (errors >= processed) return false
        return errors <= MIN_TOLERATED_ERRORS || errors * 100L <= TOLERATED_PERCENT.toLong() * processed
    }

    /** The user message: the first [limit] references, one per line, then "and N more" when the list was cut. */
    fun message(errors: List<Key>, limit: Int): String {
        val listed = errors.take(limit).joinToString("") { "$it\n" }
        val more = errors.size - limit
        val tail = if (more > 0) JSMsg.gettext("and {0} more", more) + "\n" else ""
        // TRANSLATOR: One or more verses could not be indexed due to errors in those verses. The list of them follows.
        return JSMsg.gettext("The following verses have errors and could not be indexed\n{0}", listed + tail)
    }
}
