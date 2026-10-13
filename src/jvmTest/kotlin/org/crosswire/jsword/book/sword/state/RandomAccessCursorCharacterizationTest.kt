package org.crosswire.jsword.book.sword.state

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.jsword.book.sword.LegacySwordIo
import org.junit.Assert.*
import org.junit.Test

class RandomAccessCursorCharacterizationTest {
    @Test fun cursorDelimiterEofAndSeekFirstBehavior() {
        val file = File.createTempFile("cursor", ".bin")
        try {
            file.writeBytes(byteArrayOf(1, 2, 0, 3, 4))
            RandomAccessFile(file, "r").use { raf ->
                assertArrayEquals(byteArrayOf(1), LegacySwordIo.readRAF(raf, 0, 1))
                assertEquals(1L, raf.filePointer)
                assertArrayEquals(byteArrayOf(2, 0), LegacySwordIo.readUntilRAF(raf, 0))
                assertEquals(3L, raf.filePointer)
                assertArrayEquals(byteArrayOf(3, 4), LegacySwordIo.readNextRAF(raf, 8))
                assertEquals(5L, raf.filePointer)
                assertArrayEquals(byteArrayOf(), LegacySwordIo.readUntilRAF(raf, 0))
                assertEquals(5L, raf.filePointer)
                assertArrayEquals(byteArrayOf(3, 4), LegacySwordIo.readUntilRAF(raf, 3, 0))
                assertEquals(5L, raf.filePointer)
                assertArrayEquals(byteArrayOf(), LegacySwordIo.readRAF(raf, 2, -1))
                assertEquals(2L, raf.filePointer)
                for (size in listOf(0, -1, 1)) {
                    assertThrows(IOException::class.java) { LegacySwordIo.readRAF(raf, -1, size) }
                    assertEquals(2L, raf.filePointer)
                }
                assertThrows(IOException::class.java) { LegacySwordIo.writeRAF(raf, 0, byteArrayOf(9)) }
                assertEquals(0L, raf.filePointer)
            }
        } finally { file.delete() }
    }

    @Test fun writableCreationAndNullWriteRetainSeek() {
        val root = Files.createTempDirectory("cursor-create").toFile()
        val file = File(root, "literal\\file")
        try {
            RandomAccessFile(file, "rw").use { raf ->
                assertTrue(file.exists())
                LegacySwordIo.writeRAF(raf, 2, byteArrayOf(7, 8))
                assertEquals(4L, raf.filePointer)
                assertArrayEquals(byteArrayOf(0, 0, 7, 8), file.readBytes())
                LegacySwordIo.writeRAF(raf, 1, null)
                assertEquals(1L, raf.filePointer)
                LegacySwordIo.writeNextRAF(null, null)
                assertThrows(IOException::class.java) { LegacySwordIo.writeRAF(raf, -1, null) }
            }
        } finally { root.deleteRecursively() }
    }
}
