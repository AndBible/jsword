/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.compress

import okio.Buffer
import okio.Deflater
import okio.DeflaterSink
import okio.buffer
import okio.use
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RawInflateStreamTest {
    private fun deflate(data: ByteArray): ByteArray {
        val out = Buffer()
        val deflater = Deflater(-1, true)
        try {
            DeflaterSink(out, deflater).buffer().use { it.write(data) }
        } finally {
            deflater.end()
        }
        return out.readByteArray()
    }

    private fun inflateAll(input: Buffer, bufferSize: Int = 8192): Pair<ByteArray, Long> {
        val inflater = newRawInflater()
        try {
            val stream = RawInflateStream(inflater, input) { false }
            val out = Buffer()
            val chunk = ByteArray(bufferSize)
            while (true) {
                val n = stream.read(chunk, 0, chunk.size)
                if (n == -1) break
                out.write(chunk, 0, n)
            }
            return out.readByteArray() to stream.totalWritten
        } finally {
            inflater.close()
        }
    }

    @Test fun streamEndingExactlyAtEndOfInputIsNotTruncated() {
        val data = ByteArray(50_000) { (it * 7 % 13).toByte() }
        // No trailer or other bytes follow the deflate data: the end must not be reported as truncation.
        for (size in listOf(1, 10, 8192, 50_000)) {
            val (inflated, written) = inflateAll(Buffer().write(deflate(data)), size)
            assertContentEquals(data, inflated)
            assertEquals(data.size.toLong(), written)
        }
    }

    @Test fun bytesAfterTheStreamArePushedBack() {
        val input = Buffer().write(deflate("hello".encodeToByteArray())).write(byteArrayOf(1, 2, 3))
        val (inflated, _) = inflateAll(input)
        assertEquals("hello", inflated.decodeToString())
        assertContentEquals(byteArrayOf(1, 2, 3), input.readByteArray())
    }

    @Test fun trulyTruncatedStreamStillThrows() {
        val packed = deflate(ByteArray(50_000) { (it * 31 % 251).toByte() })
        assertFailsWith<okio.IOException> { inflateAll(Buffer().write(packed.copyOf(packed.size / 2))) }
    }
}
