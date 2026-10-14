package org.crosswire.common.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LZSSTest {
    @Test fun testCore() {
        val ringSize = 4096
        val ringWrap = ringSize - 1
        val threshold = 3
        for (i in -1 until ringSize - 1) assertEquals(i + 1, (i + 1) and ringWrap)
        for (i in -1 until ringSize - 1) assertEquals(i + 1, (i + ringSize + 1) and ringWrap)
        var mask: Byte = 1
        for (i in 7 downTo 1) {
            mask = (mask.toInt() shl 1).toByte()
            assertTrue(mask.toInt() != 0)
        }
        mask = (mask.toInt() shl 1).toByte()
        assertEquals(0, mask.toInt())
        val pos = 0x0fff
        val len = 0x000f + threshold
        val lowPart = pos.toByte()
        val highPart = (((pos shr 4) and 0xf0) or (len - threshold)).toByte()
        assertEquals(0xff.toByte(), lowPart)
        assertEquals(0xff.toByte(), highPart)
        assertEquals(pos, (lowPart.toInt() and 0xff) or ((highPart.toInt() and 0xf0) shl 4))
        assertEquals(len, ((highPart.toInt() and 0x0f) + threshold).toShort().toInt())
    }

    @Test fun testCompression() {
        val compressed = LZSS(compressionTestCorpus()).compress()
        val result = LZSS(compressed).uncompress().decodeToString()
        assertTrue(Regex("^          \r?\nThe First Book of Moses, called Genesis").containsMatchIn(result), "round trip LZSS uncompression")
    }
}
