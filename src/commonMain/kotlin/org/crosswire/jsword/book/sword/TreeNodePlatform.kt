package org.crosswire.jsword.book.sword

expect abstract class TreeNodeCloneBase() {
    protected open fun clone(): Any
    protected fun shallowTreeClone(): Any
    protected open fun portableTreeClone(): Any
}

internal expect interface TreeNodeSerializable
