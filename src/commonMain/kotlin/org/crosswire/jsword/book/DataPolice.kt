package org.crosswire.jsword.book

import org.crosswire.jsword.passage.Key

/** Report source-data defects without moving the JVM book facade. */
expect class DataPolice {
    companion object {
        fun report(book: Book?, key: Key?, message: String?)
        var isReporting: Boolean
    }
}
