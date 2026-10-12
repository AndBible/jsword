package org.crosswire.jsword.book.sword

import okio.Path
import okio.Path.Companion.toPath

/** Lexical file location: Path projections must not erase JVM filename spelling before IO. */
internal data class StorageLocation(val spelling: String) {
    val path: Path get() = spelling.toPath()
    val name: String get() = storageName(this)
    val parent: StorageLocation? get() = storageParent(this)
    operator fun div(child: String): StorageLocation = storageChild(this, child)
    override fun toString(): String = spelling
}
