package org.crosswire.jsword.book

import org.crosswire.common.activate.Activatable
import org.crosswire.jsword.index.IndexStatusListener
import org.crosswire.jsword.passage.Key

actual interface Book : Activatable, Comparable<Book> {
    actual val initials: String
    actual var bookMetaData: BookMetaData
    actual fun getKey(name: String?): Key
    actual fun addIndexStatusListener(li: IndexStatusListener?)
    actual fun removeIndexStatusListener(li: IndexStatusListener?)
}

actual interface BookDriver {
    actual fun getDriverName(): String
}
