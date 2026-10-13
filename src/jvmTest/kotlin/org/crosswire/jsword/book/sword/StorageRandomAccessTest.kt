package org.crosswire.jsword.book.sword

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class StorageRandomAccessTest {
    @Test fun nullReceiverOrderingMatchesLegacy() {
        LegacySwordIo.writeNextRAF(null, null)
        SwordUtil.writeNextRAF(null, null)
        assertThrows(NullPointerException::class.java) { SwordUtil.writeNextRAF(null, byteArrayOf()) }
        assertThrows(NullPointerException::class.java) { SwordUtil.writeRAF(null, 0, null) }
        assertThrows(NullPointerException::class.java) { SwordUtil.readRAF(null, 0, 0) }
        assertThrows(NullPointerException::class.java) { SwordUtil.readUntilRAF(null, 0) }
    }

    @Test fun emptyAndNullWritesRetainLegacyNoopAndSeekOrdering() {
        val file = File.createTempFile("empty-write", ".bin")
        try {
            val raf = java.io.RandomAccessFile(file, "r")
            val src = storageOpenRandomAccess(StorageLocation(file.path), false)
            try {
                for (data in listOf(byteArrayOf(), null)) {
                    LegacySwordIo.writeNextRAF(raf, data)
                    SwordUtil.writeNextRAF(src, data)
                    LegacySwordIo.writeRAF(raf, 7, data)
                    SwordUtil.writeRAF(src, 7, data)
                    assertEquals(raf.filePointer, src.position())
                    assertThrows(IOException::class.java) { LegacySwordIo.writeRAF(raf, -1, data) }
                    assertThrows(IOException::class.java) { SwordUtil.writeRAF(src, -1, data) }
                }
                raf.close(); src.close()
                for (data in listOf(byteArrayOf(), null)) {
                    LegacySwordIo.writeNextRAF(raf, data)
                    SwordUtil.writeNextRAF(src, data)
                    assertThrows(IOException::class.java) { LegacySwordIo.writeRAF(raf, 7, data) }
                    assertThrows(IOException::class.java) { SwordUtil.writeRAF(src, 7, data) }
                }
            } finally { raf.close(); src.close() }
        } finally { file.delete() }
    }

    @Test fun lexicalOpenAndCursorMatchLegacyFile() {
        val root = Files.createTempDirectory("lexical-raf").toFile()
        try {
            val literal = File(root, "literal\\file")
            literal.writeBytes(byteArrayOf(1, 2, 0, 3, 4))
            val decoy = File(root, "literal/file").apply { parentFile.mkdirs(); writeBytes(byteArrayOf(9)) }
            storageOpenRandomAccess(StorageLocation(literal.path), false).use { src ->
                assertArrayEquals(byteArrayOf(1), SwordUtil.readRAF(src, 0, 1))
                assertEquals(1L, src.position())
                assertArrayEquals(byteArrayOf(2, 0), SwordUtil.readUntilRAF(src, 0))
                assertEquals(3L, src.position())
                assertArrayEquals(byteArrayOf(3, 4), SwordUtil.readNextRAF(src, 8))
                assertEquals(5L, src.position())
                assertArrayEquals(byteArrayOf(), SwordUtil.readUntilRAF(src, 0))
                assertArrayEquals(byteArrayOf(), SwordUtil.readRAF(src, 2, -1))
                for (size in listOf(0, -1, 1)) {
                    assertThrows(IOException::class.java) { SwordUtil.readRAF(src, -1, size) }
                    assertEquals(2L, src.position())
                }
                assertThrows(IOException::class.java) { SwordUtil.writeRAF(src, 0, byteArrayOf(9)) }
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
