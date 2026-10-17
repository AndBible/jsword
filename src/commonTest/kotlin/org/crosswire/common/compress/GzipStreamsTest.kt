package org.crosswire.common.compress

import okio.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Behaviour of the common GZIP streams and CRC32. */
class GzipStreamsTest {
    private fun pseudoRandom(size: Int): ByteArray {
        var state = 987654321
        return ByteArray(size) { state = state * 1664525 + 1013904223; (state ushr 24).toByte() }
    }

    @Test fun gzipThenGunzipRoundTripsEmptySmallAndOneMiB() {
        val text = "In the beginning".encodeToByteArray()
        for (input in listOf(byteArrayOf(), byteArrayOf(7), pseudoRandom(1 shl 20), ByteArray(1 shl 20) { text[it % text.size] })) {
            assertContentEquals(input, GzipStreams.gunzip(GzipStreams.gzip(input)))
        }
    }

    @Test fun gunzipRejectsBadCrc() {
        val bytes = GzipStreams.gzip("hello world".encodeToByteArray())
        bytes[bytes.size - 8] = (bytes[bytes.size - 8].toInt() xor 0x55).toByte()
        assertFailsWith<IOException> { GzipStreams.gunzip(bytes) }
    }

    @Test fun gunzipRejectsBadIsize() {
        val bytes = GzipStreams.gzip("hello world".encodeToByteArray())
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0x01).toByte()
        assertFailsWith<IOException> { GzipStreams.gunzip(bytes) }
    }

    @Test fun gunzipRejectsTruncatedStream() {
        val bytes = GzipStreams.gzip(pseudoRandom(5000))
        for (cut in listOf(bytes.size - 1, bytes.size - 8, bytes.size / 2, 5, 0)) {
            assertFailsWith<IOException>("cut at $cut") { GzipStreams.gunzip(bytes.copyOf(cut)) }
        }
    }

    @Test fun gunzipRejectsNonGzipInput() {
        assertFailsWith<IOException> { GzipStreams.gunzip(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)) }
    }

    @Test fun gunzipDecodesTwoConcatenatedMembers() {
        val both = GzipStreams.gzip("ab".encodeToByteArray()) + GzipStreams.gzip("cd".encodeToByteArray())
        assertEquals("abcd", GzipStreams.gunzip(both).decodeToString())
    }

    @Test fun gunzipIgnoresTrailingNonGzipBytes() {
        val bytes = GzipStreams.gzip("ab".encodeToByteArray()) + byteArrayOf(0, 1, 2)
        assertEquals("ab", GzipStreams.gunzip(bytes).decodeToString())
    }

    @Test fun gunzipSkipsOptionalHeaderFields() {
        // FEXTRA | FNAME | FCOMMENT, hand-built header around a raw gzip body.
        val plain = GzipStreams.gzip("xyz".encodeToByteArray())
        val header = byteArrayOf(0x1f, 0x8b.toByte(), 8, 0x1c, 0, 0, 0, 0, 0, 0xff.toByte(),
            2, 0, 9, 9, // extra: length 2
            'n'.code.toByte(), 0, 'c'.code.toByte(), 0)
        assertEquals("xyz", GzipStreams.gunzip(header + plain.copyOfRange(10, plain.size)).decodeToString())
    }

    @Test fun crc32OfCheckStringIs0xCBF43926() {
        val crc = Crc32()
        crc.update("123456789".encodeToByteArray())
        assertEquals(0xCBF43926L, crc.value)
    }

    @Test fun crc32UpdateIsIncrementalAndHonoursOffset() {
        val data = pseudoRandom(1000)
        val whole = Crc32().apply { update(data) }
        val parts = Crc32().apply { update(data, 0, 400); update(data, 400, 600) }
        assertEquals(whole.value, parts.value)
        assertEquals(0L, Crc32().value)
    }
}
