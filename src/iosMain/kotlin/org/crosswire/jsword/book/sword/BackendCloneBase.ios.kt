package org.crosswire.jsword.book.sword

actual abstract class BackendCloneBase actual constructor() {
    protected actual fun shallowBackendClone(): Any = portableBackendClone()
    protected actual open fun portableBackendClone(): Any =
        throw UnsupportedOperationException("Host subclass must provide a portable clone")
}
