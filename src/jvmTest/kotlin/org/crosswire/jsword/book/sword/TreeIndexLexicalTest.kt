package org.crosswire.jsword.book.sword

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class TreeIndexLexicalTest {
    @Test fun treeUsesLegacyFileAbsoluteSpellingAndReadsActualNode() {
        val directory = Files.createTempDirectory("tree-lexical").toFile()
        try {
            val folder = java.io.File(directory, "literal\\name").apply { mkdir() }
            val path = java.io.File(folder, "./tree").path
            java.io.File("$path.idx").writeBytes(ByteArray(4))
            val data = ByteArray(12) { -1 } + "root".toByteArray() + byteArrayOf(0, 0, 0)
            java.io.File("$path.dat").writeBytes(data)
            val metadata = SwordBookMetaData(
                "[Tree]\nModDrv=RawGenBook\nDataPath=$path\n".toByteArray(), "tree.conf"
            ).apply { library = java.io.File("/").toURI() }
            val tree = TreeKeyIndex(metadata)
            try {
                val node = tree.getRoot()
                assertEquals("root", node.name)
                assertEquals(-1, node.parent)
                val method = TreeKeyIndex::class.java.getDeclaredMethod("getExpandedDataPath").apply { isAccessible = true }
                assertEquals(java.io.File(path).absolutePath, method.invoke(tree))
            } finally { tree.deactivate(null) }
        } finally { directory.deleteRecursively() }
    }
}
