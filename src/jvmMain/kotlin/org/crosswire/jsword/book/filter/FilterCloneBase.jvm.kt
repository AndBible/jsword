package org.crosswire.jsword.book.filter

actual abstract class FilterCloneBase actual constructor() : Cloneable {
    protected actual fun shallowFilterClone(): Any = super.clone()
    protected actual open fun portableFilterClone(): Any =
        throw UnsupportedOperationException("Host subclass must provide a portable clone")
}

actual typealias FilterCloneMarker = Cloneable
