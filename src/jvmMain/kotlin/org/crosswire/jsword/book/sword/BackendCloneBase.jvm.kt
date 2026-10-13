package org.crosswire.jsword.book.sword

/** JVM object cloning preserves fields introduced by unknown host subclasses. */
actual abstract class BackendCloneBase actual constructor() : Cloneable {
    protected actual fun shallowBackendClone(): Any = super.clone()

    protected actual open fun portableBackendClone(): Any =
        throw UnsupportedOperationException("Host subclass must provide a portable clone")
}
