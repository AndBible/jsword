package org.crosswire.jsword.book.sword

expect abstract class TreeNodeCloneBase() {
    protected fun shallowTreeClone(): Any
    protected open fun portableTreeClone(): Any
}

internal expect interface TreeNodeSerializable
