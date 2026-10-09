package org.crosswire.common.compress

import okio.EOFException
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZlibRoundTripTest {
    private val plain: ByteArray = buildString { for (i in 0 until 20) append("In the beginning $i KSword zlib. ") }.encodeToByteArray()

    /** `plain` deflated on the JVM by DeflaterOutputStream(Deflater()). */
    private val vector: ByteArray = intArrayOf(
        0x78, 0x9c, 0x85, 0xd0, 0xbb, 0x09, 0x80, 0x30, 0x14, 0x86, 0xd1, 0x55, 0xee, 0x04, 0xe2, 0xef, 
        0xdb, 0x11, 0x82, 0xa5, 0x1b, 0x04, 0x43, 0x0c, 0xc8, 0x15, 0x44, 0x10, 0x9c, 0xde, 0xde, 0xe6, 
        0xab, 0x4f, 0x77, 0x82, 0xdb, 0xbd, 0x27, 0x8b, 0x29, 0x17, 0xf7, 0xe2, 0xd9, 0x6a, 0x5b, 0xd6, 
        0xe7, 0xbc, 0x36, 0x7b, 0x8f, 0x12, 0x2b, 0x0b, 0x7f, 0x17, 0x78, 0x03, 0xde, 0x82, 0x77, 0xe0, 
        0x3d, 0xf8, 0x00, 0x3e, 0x82, 0x4f, 0xe0, 0x33, 0xfd, 0x60, 0x20, 0x0d, 0x8a, 0x0a, 0x45, 0x87, 
        0xa2, 0x44, 0xd1, 0xa2, 0xa8, 0x51, 0xf4, 0x28, 0x8a, 0xd4, 0x6f, 0xf2, 0x03, 0x39, 0xcc, 0xda, 
        0xad,
    ).map { it.toByte() }.toByteArray()

    @Test fun inflatesJvmProducedVector() {
        assertEquals(650, plain.size)
        for (size in intArrayOf(plain.size, 1, 7, 2048, 2 * plain.size)) {
            assertContentEquals(plain, Zlib.inflate(vector, size), "size=$size")
        }
    }

    @Test fun deflateRoundTrips() {
        for (n in intArrayOf(0, 1, 100, 70_000)) {
            val data = ByteArray(n) { ((it * 31) xor (it shr 5)).toByte() }
            assertContentEquals(data, Zlib.inflate(Zlib.deflate(data), 4096), "n=$n")
        }
        assertContentEquals(plain, Zlib.inflate(Zlib.deflate(plain), 100))
    }

    @Test fun trailingGarbageIsIgnored() {
        assertContentEquals(plain, Zlib.inflate(vector + byteArrayOf(9, 8, 7, 6, 5), 100))
    }

    @Test fun truncatedStreamIsEofException() {
        assertFailsWith<EOFException> { Zlib.inflate(vector.copyOf(vector.size - 4), 100) }
        assertFailsWith<EOFException> { Zlib.inflate(ByteArray(0), 100) }
    }

    @Test fun corruptHeaderIsIoException() {
        assertFailsWith<IOException> { Zlib.inflate(byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x11), 100) }
    }

    @Test fun nonPositiveExpectedSizeIsIllegalArgument() {
        assertFailsWith<IllegalArgumentException> { Zlib.inflate(vector, 0) }
        assertFailsWith<IllegalArgumentException> { Zlib.inflate(vector, -5) }
    }
}
