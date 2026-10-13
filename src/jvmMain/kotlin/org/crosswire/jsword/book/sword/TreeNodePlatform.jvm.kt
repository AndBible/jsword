package org.crosswire.jsword.book.sword

internal typealias TreeNodeSerializable = java.io.Serializable

/** Non-serializable base leaves TreeNode's serialized descriptor unchanged. */
abstract class TreeNodeCloneBase : Cloneable {
    protected fun shallowTreeClone(): Any = super.clone()
    protected open fun portableTreeClone(): Any =
        throw UnsupportedOperationException("Host subclass must provide a portable clone")
}
