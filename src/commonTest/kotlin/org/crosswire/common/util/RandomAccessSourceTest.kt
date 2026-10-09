package org.crosswire.common.util

import okio.Path.Companion.toPath
import okio.use
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class RandomAccessSourceTest {
    private val fs = FakeFileSystem()
    private val content = ByteArray(100) { it.toByte() }

    private fun source(bytes: ByteArray = content): RandomAccessSource {
        val p = "/tmp/ras.bin".toPath()
        fs.createDirectories(p.parent!!)
        fs.write(p) { write(bytes) }
        return RandomAccessSource.open(p, fs)
    }

    @Test fun normalRead() = source().use {
        assertEquals(100L, it.size())
        assertContentEquals(content.copyOfRange(10, 30), it.read(10, 20))
        assertContentEquals(content, it.read(0, 100))
    }

    @Test fun zeroAndNegativeSizeAreEmpty() = source().use {
        assertEquals(0, it.read(5, 0).size)
        assertEquals(0, it.read(5, -3).size)
    }

    @Test fun offsetAtOrBeyondEndIsEmpty() = source().use {
        assertEquals(0, it.read(100, 5).size)
        assertEquals(0, it.read(1000, 5).size)
    }

    @Test fun readPastEndIsShortened() = source().use {
        assertContentEquals(content.copyOfRange(90, 100), it.read(90, 50))
        assertContentEquals(content.copyOfRange(99, 100), it.read(99, Int.MAX_VALUE))
    }

    @Test fun zeroSizeAtOffsetBeyondEndIsEmpty() = source().use {
        assertEquals(0, it.read(500, 0).size)
    }

    @Test fun emptyFile() = source(ByteArray(0)).use {
        assertEquals(0L, it.size())
        assertEquals(0, it.read(0, 10).size)
    }

    @Test fun largeReadAcrossInternalChunks() {
        val big = ByteArray(300_000) { (it * 31).toByte() }
        source(big).use { assertContentEquals(big.copyOfRange(1000, 299_000), it.read(1000, 298_000)) }
    }
}
