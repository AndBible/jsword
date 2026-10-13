package org.crosswire.jsword.book.sword

internal actual typealias TreeNodeSerializable = java.io.Serializable

/** Non-serializable base leaves TreeNode's serialized descriptor unchanged. */
actual abstract class TreeNodeCloneBase actual constructor() : Cloneable {
    protected actual fun shallowTreeClone(): Any = super.clone()
    protected actual open fun portableTreeClone(): Any =
        throw UnsupportedOperationException("Host subclass must provide a portable clone")
}
