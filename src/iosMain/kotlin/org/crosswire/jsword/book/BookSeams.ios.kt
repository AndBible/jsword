package org.crosswire.jsword.book

actual interface Book : Comparable<Book>

actual interface BookDriver {
    actual fun getDriverName(): String
}
