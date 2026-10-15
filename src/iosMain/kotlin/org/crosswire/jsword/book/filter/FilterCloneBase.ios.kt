package org.crosswire.jsword.book.filter

actual abstract class FilterCloneBase actual constructor() {
    protected actual fun shallowFilterClone(): Any = portableFilterClone()
    protected actual open fun portableFilterClone(): Any =
        throw UnsupportedOperationException("Host subclass must provide a portable clone")
}
