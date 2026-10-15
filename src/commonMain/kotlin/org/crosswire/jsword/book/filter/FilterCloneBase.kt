package org.crosswire.jsword.book.filter

/** Preserve host subclass fields on JVM; portable filters supply their own copy. */
expect abstract class FilterCloneBase() {
    protected fun shallowFilterClone(): Any
    protected open fun portableFilterClone(): Any
}
