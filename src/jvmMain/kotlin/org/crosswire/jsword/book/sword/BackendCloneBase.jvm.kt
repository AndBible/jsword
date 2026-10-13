package org.crosswire.jsword.book.sword

/** JVM object cloning preserves fields introduced by unknown host subclasses. */
abstract class BackendCloneBase : Cloneable {
    protected fun shallowBackendClone(): Any = super.clone()

    protected open fun portableBackendClone(): Any =
        throw UnsupportedOperationException("Host subclass must provide a portable clone")
}
