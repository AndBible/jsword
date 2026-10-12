package org.crosswire.jsword.book.sword

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class StorageRandomAccessTest {
    @Test fun lexicalOpenAndCursorMatchLegacyFile() {
        val root = Files.createTempDirectory("lexical-raf").toFile()
        try {
            val literal = File(root, "literal\\file")
            literal.writeBytes(byteArrayOf(1, 2, 0, 3, 4))
            val decoy = File(root, "literal/file").apply { parentFile.mkdirs(); writeBytes(byteArrayOf(9)) }
            storageOpenRandomAccess(StorageLocation(literal.path), false).use { src ->
                assertArrayEquals(byteArrayOf(1), src.readAtCursor(0, 1))
                assertEquals(1L, src.position())
                assertArrayEquals(byteArrayOf(2, 0), src.readUntil(0))
                assertEquals(3L, src.position())
                assertArrayEquals(byteArrayOf(3, 4), src.readNext(8))
                assertEquals(5L, src.position())
                assertArrayEquals(byteArrayOf(), src.readUntil(0))
                assertArrayEquals(byteArrayOf(), src.readAtCursor(2, -1))
                for (size in listOf(0, -1, 1)) {
                    assertThrows(IOException::class.java) { src.readAtCursor(-1, size) }
                    assertEquals(2L, src.position())
                }
                assertThrows(IOException::class.java) { src.writeAtCursor(0, byteArrayOf(9)) }
                assertEquals(0L, src.position())
            }
            val closed = storageOpenRandomAccess(StorageLocation(literal.path), false)
            closed.close()
            closed.close()
            assertThrows(IOException::class.java) { closed.readAtCursor(0, 0) }
            assertThrows(IOException::class.java) { closed.position() }
            assertArrayEquals(byteArrayOf(9), decoy.readBytes())
            val created = File(root, "new\\file")
            storageOpenRandomAccess(StorageLocation(created.path), true).use { src ->
                assertTrue(created.exists())
                src.writeAtCursor(2, byteArrayOf(7, 8))
                assertEquals(4L, src.position())
                src.writeAtCursor(1, null)
                assertEquals(1L, src.position())
                assertArrayEquals(byteArrayOf(0, 0, 7, 8), created.readBytes())
            }
        } finally { root.deleteRecursively() }
    }
}
