package org.crosswire.ksword.fixtures

import okio.FileSystem
import okio.Path
import org.crosswire.common.util.NetUtil

/** Real, isolated files for portable storage tests; always remove even on assertion failure. */
internal fun withTestDirectory(block: (Path) -> Unit) {
    val fs = FileSystem.SYSTEM
    val root = NetUtil.toPath(NetUtil.getTemporaryURI("k4-port", null))
    fs.delete(root)
    fs.createDirectory(root)
    try { block(root) } finally { fs.deleteRecursively(root) }
}
