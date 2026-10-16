package org.crosswire.jsword.index.fts

/** Names shared by the FTS5 search index and the code that locates its files (K6). */
object SearchFields {
    /** The Strong's number field of the query language (`strong:H430`), as in the baseline Lucene index. */
    const val STRONG = "strong"

    /** Writable project subdirectory holding the SQLite search indexes: `search/<driverName>/<initials>.sqlite`. */
    const val DIR_SEARCH = "search"

    /** Writable project subdirectory of the legacy Lucene indexes: `lucene/<driverName>/<initials>/`. */
    const val DIR_LEGACY_LUCENE = "lucene"
}
