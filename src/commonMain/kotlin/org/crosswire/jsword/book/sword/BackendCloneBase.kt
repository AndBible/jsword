package org.crosswire.jsword.book.sword

expect abstract class BackendCloneBase() {
    protected fun shallowBackendClone(): Any
    protected open fun portableBackendClone(): Any
}
