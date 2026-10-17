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
import okio.IOException
import okio.buffer
import okio.use
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZipReaderJdkParityTest {
    private val bodies = linkedMapOf(
        "a.bin" to ByteArray(70_000) { (it * 17 % 253).toByte() },
        "b/c.txt" to "hello".encodeToByteArray(),
        "ä/𝔄.txt" to "unicode".encodeToByteArray(),
        "empty" to ByteArray(0),
    )

    private fun jdk(zip: ByteArray): List<Pair<String, ByteArray>> {
        val result = mutableListOf<Pair<String, ByteArray>>()
        ZipInputStream(ByteArrayInputStream(zip), Charsets.UTF_8).use { zis ->
            while (true) {
                val e = zis.nextEntry ?: break
                result += e.name to zis.readBytes()
            }
        }
        return result
    }

    private fun ours(zip: ByteArray): List<Pair<String, ByteArray>> {
        val result = mutableListOf<Pair<String, ByteArray>>()
        ZipReader(Buffer().write(zip)).use { r ->
            while (true) {
                val e = r.nextEntry() ?: break
                result += e.name to r.entry.buffer().readByteArray()
            }
        }
        return result
    }

    private fun assertParity(zip: ByteArray) {
        val expected = jdk(zip)
        val actual = ours(zip)
        assertEquals(expected.map { it.first }, actual.map { it.first })
        for (i in expected.indices) assertContentEquals(expected[i].second, actual[i].second, expected[i].first)
    }

    @Test fun parityOnZipOutputStreamDeflatedWithDescriptor() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> for ((n, b) in bodies) { z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
        assertParity(out.toByteArray())
        assertEquals(bodies.size, ours(out.toByteArray()).size)
    }

    @Test fun parityOnStoredEntriesWithSizes() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            for ((n, b) in bodies) {
                val e = ZipEntry(n)
                e.method = ZipEntry.STORED
                e.size = b.size.toLong()
                e.compressedSize = b.size.toLong()
                e.crc = CRC32().also { it.update(b) }.value
                z.putNextEntry(e); z.write(b); z.closeEntry()
            }
        }
        assertParity(out.toByteArray())
        assertEquals(bodies.keys.toList(), ours(out.toByteArray()).map { it.first })
    }

    @Test fun parityOnDirectoryEntries() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("d/")); z.closeEntry()
            z.putNextEntry(ZipEntry("d/e/")); z.closeEntry()
            z.putNextEntry(ZipEntry("d/e/f.txt")); z.write("x".encodeToByteArray()); z.closeEntry()
        }
        val zip = out.toByteArray()
        assertParity(zip)
        ZipReader(Buffer().write(zip)).use { r ->
            assertEquals(true, r.nextEntry()!!.isDirectory)
            assertEquals(true, r.nextEntry()!!.isDirectory)
            assertEquals(false, r.nextEntry()!!.isDirectory)
        }
    }

    @Test fun parityOnForcedZip64Archive() {
        val out = Buffer()
        ZipWriter(out, 1L).use { z -> for ((n, b) in bodies) z.putEntry(n) { it.write(b) } }
        assertParity(out.readByteArray())
    }

    @Test fun parityWhenTheDescriptorHasNoSignature() {
        val b = "payload payload payload".encodeToByteArray()
        val packed = Buffer().also { o ->
            val d = okio.Deflater(-1, true)
            try { okio.DeflaterSink(o, d).buffer().use { it.write(b) } } finally { d.end() }
        }.readByteArray()
        val descriptor = Buffer().writeIntLe(crcOf(b).toInt()).writeIntLe(packed.size).writeIntLe(b.size).readByteArray()
        val zip = rawLocalEntry("a".encodeToByteArray(), 8, 8, 0, 0, 0, packed, trailer = descriptor + ByteArray(8))
        assertParity(zip)
    }

    @Test fun cp437NameThrowsWhereJdkThrows() {
        val zip = rawLocalEntry(byteArrayOf(0x81.toByte()), 0, 0, 0, 0, 0, ByteArray(0))
        assertFailsWith<IllegalArgumentException> { jdk(zip) }
        val e = assertFailsWith<ZipFormatException> { ours(zip) }
        assertEquals("MALFORMED", e.message)
    }

    @Test fun storedWithDescriptorRejectedLikeJdk() {
        val body = "abc".encodeToByteArray()
        val zip = rawLocalEntry("a".encodeToByteArray(), 8, 0, 0, 0, 0, body)
        assertFailsWith<ZipException> { jdk(zip) }
        assertFailsWith<ZipFormatException> { ours(zip) }
    }

    @Test fun parityOnStoredEntryWithSizesInTheLocalZip64Extra() {
        val body = "zip64 stored".encodeToByteArray()
        val extra = Buffer().writeShortLe(1).writeShortLe(16).writeLongLe(body.size.toLong())
            .writeLongLe(body.size.toLong()).readByteArray()
        val zip = rawLocalEntry("a".encodeToByteArray(), 0, 0, crcOf(body), 0xFFFFFFFFL, 0xFFFFFFFFL, body, extra)
        assertParity(zip)
        val onlyCompressed = rawLocalEntry("a".encodeToByteArray(), 0, 0, crcOf(body), 0xFFFFFFFFL, body.size.toLong(), body, extra)
        assertParity(onlyCompressed)
    }
}
