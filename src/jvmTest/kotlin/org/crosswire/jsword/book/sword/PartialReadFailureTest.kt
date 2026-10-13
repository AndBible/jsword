package org.crosswire.jsword.book.sword

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import okio.FileHandle
import org.crosswire.common.util.RandomAccessSource
import org.junit.Assert.*
import org.junit.Test

class PartialReadFailureTest {
    @Test fun partialEofRetainsCursorForBothPublicReaders() = checkFailure(false)
    @Test fun partialIOExceptionRetainsCursorForBothPublicReaders() = checkFailure(true)

    private fun checkFailure(ioFailure: Boolean) {
        val file = File.createTempFile("partial-read", ".bin")
        try {
            for (seekFirst in listOf(false, true)) {
                file.writeBytes(byteArrayOf(10, 20, 30, 40, 50, 60))
                // readFully is the real JDK implementation: its first read consumes two bytes,
                // and its next read encounters a deterministic shrink or IO failure.
                val old = object : RandomAccessFile(file, "rw") {
                    var reads = 0
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (++reads == 2 && ioFailure) throw IOException("injected read failure")
                        val n = super.read(b, off, if (reads == 1) minOf(2, len) else len)
                        if (reads == 1 && !ioFailure) setLength(filePointer)
                        return n
                    }
                }
                old.use {
                    val physical = RandomAccessFile(file, "r")
                    var reads = 0
                    val source = RandomAccessSource(object : FileHandle(false) {
                        override fun protectedSize(): Long = 6
                        override fun protectedRead(offset: Long, b: ByteArray, off: Int, count: Int): Int {
                            if (++reads == 2 && ioFailure) throw IOException("injected read failure")
                            physical.seek(offset)
                            return physical.read(b, off, if (reads == 1) minOf(2, count) else count)
                        }
                        override fun protectedWrite(o: Long, b: ByteArray, a: Int, c: Int) = error("readonly")
                        override fun protectedResize(size: Long) = error("readonly")
                        override fun protectedFlush() = Unit
                        override fun protectedClose() = physical.close()
                    })
                    source.use { src ->
                        old.seek(1); src.seek(1)
                        assertThrows(IOException::class.java) {
                            if (seekFirst) LegacySwordIo.readRAF(old, 1, 5) else LegacySwordIo.readNextRAF(old, 5)
                        }
                        assertEquals("JDK readFully consumed two bytes", 3L, old.filePointer)
                        assertThrows(IOException::class.java) {
                            if (seekFirst) SwordUtil.readRAF(src, 1, 5) else SwordUtil.readNextRAF(src, 5)
                        }
                        assertEquals("failed source read retains consumed cursor", old.filePointer, src.position())
                        val expected = LegacySwordIo.readNextRAF(old, 1)
                        if (ioFailure) assertArrayEquals(expected, SwordUtil.readNextRAF(src, 1))
                        else {
                            assertArrayEquals(byteArrayOf(), expected)
                            // The size driver deliberately reports stale length: a subsequent EOF must not move cursor.
                            assertThrows(IOException::class.java) { SwordUtil.readNextRAF(src, 1) }
                            assertEquals(old.filePointer, src.position())
                        }
                    }
                }
            }
        } finally { file.delete() }
    }
}
