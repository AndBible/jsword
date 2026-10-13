package org.crosswire.common.util

import okio.Path.Companion.toOkioPath
import okio.use
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.jsword.book.sword.LegacySwordIo
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.Random

/** [RandomAccessSource.read] must equal [SwordUtil.readRAF] for the same file, offset and size. */
class RandomAccessSourceParityTest {
    @Test fun sameAsReadRaf() {
        val f = File.createTempFile("ras", ".bin")
        try {
            val data = ByteArray(1000).also { Random(5).nextBytes(it) }
            f.writeBytes(data)
            val cases = listOf(
                0L to 0, 0L to -1, 10L to -5, 0L to 1000, 0L to 1, 999L to 1, 999L to 10, 1000L to 1,
                1000L to 0, 1500L to 3, 500L to 600, 250L to 500, 0L to 2000, 7L to Int.MAX_VALUE,
            )
            RandomAccessFile(f, "r").use { raf ->
                RandomAccessSource.open(f.toOkioPath()).use { src ->
                    for ((off, size) in cases) {
                        assertArrayEquals("off=$off size=$size", LegacySwordIo.readRAF(raf, off, size), src.read(off, size))
                    }
                }
            }
        } finally {
            f.delete()
        }
    }
}
