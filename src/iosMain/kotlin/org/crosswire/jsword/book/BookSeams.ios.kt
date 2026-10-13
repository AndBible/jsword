package org.crosswire.jsword.book

import org.crosswire.common.activate.Activatable

actual interface Book : Activatable, Comparable<Book>

actual interface BookDriver {
    actual fun getDriverName(): String
}
