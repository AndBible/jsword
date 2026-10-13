package org.crosswire.jsword.book.sword

internal actual interface TreeNodeSerializable

actual abstract class TreeNodeCloneBase actual constructor() {
    protected actual fun shallowTreeClone(): Any = portableTreeClone()
    protected actual open fun portableTreeClone(): Any =
        throw UnsupportedOperationException("Host subclass must provide a portable clone")
}
