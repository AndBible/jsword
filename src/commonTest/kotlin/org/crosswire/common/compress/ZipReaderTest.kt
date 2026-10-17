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
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A hand-built local file record (plus [trailer] bytes), for archives other tools would write. */
internal fun rawLocalEntry(
    name: ByteArray, flags: Int, method: Int, crc: Long, compressedSize: Long, size: Long,
    data: ByteArray, extra: ByteArray = ByteArray(0), trailer: ByteArray = ByteArray(0),
): ByteArray = Buffer().apply {
    writeIntLe(0x04034b50)
    writeShortLe(20)
    writeShortLe(flags)
    writeShortLe(method)
    writeIntLe(0)
    writeIntLe(crc.toInt())
    writeIntLe(compressedSize.toInt())
    writeIntLe(size.toInt())
    writeShortLe(name.size)
    writeShortLe(extra.size)
    write(name)
    write(extra)
    write(data)
    write(trailer)
}.readByteArray()

internal fun crcOf(bytes: ByteArray): Long = Crc32().apply { update(bytes) }.value

class ZipReaderTest {
    private val bodies = linkedMapOf(
        "db/bookmarks.sqlite3" to ByteArray(100_000) { ((it.toLong() * it * 2654435761L) shr 13).toByte() },
        "manifest.json" to "{\"v\":1}".encodeToByteArray(),
        "ä/𝔄.txt" to "unicode".encodeToByteArray(),
        "dir/" to ByteArray(0),
        "empty" to ByteArray(0),
    )

    private fun archive(): ByteArray {
        val out = Buffer()
        ZipWriter(out).use { zip -> for ((n, b) in bodies) zip.putEntry(n) { it.write(b) } }
        return out.readByteArray()
    }

    private fun readAll(zip: ByteArray): List<Triple<String, Boolean, ByteArray>> {
        val result = mutableListOf<Triple<String, Boolean, ByteArray>>()
        ZipReader(Buffer().write(zip)).use { reader ->
            while (true) {
                val e = reader.nextEntry() ?: break
                result += Triple(e.name, e.isDirectory, reader.entry.buffer().readByteArray())
            }
        }
        return result
    }

    @Test fun readsWhatZipWriterWrites() {
        val read = readAll(archive())
        assertEquals(bodies.keys.toList(), read.map { it.first })
        for ((i, b) in bodies.values.withIndex()) assertContentEquals(b, read[i].third)
        assertEquals(listOf(false, false, false, true, false), read.map { it.second })
    }

    @Test fun emptyArchiveHasNoEntries() {
        val out = Buffer()
        ZipWriter(out).close()
        assertEquals(0, readAll(out.readByteArray()).size)
        assertEquals(0, readAll(ByteArray(0)).size)
    }

    @Test fun crcMismatchThrowsZipFormatException() {
        val zip = archive()
        val sig = byteArrayOf(0x50, 0x4b, 0x07, 0x08)
        var at = -1
        for (i in 0 until zip.size - 4) if (zip.copyOfRange(i, i + 4).contentEquals(sig)) { at = i; break }
        assertTrue(at > 0)
        zip[at + 4] = (zip[at + 4].toInt() xor 0x55).toByte()
        assertFailsWith<ZipFormatException> { readAll(zip) }
    }

    @Test fun storedCrcAndSizeAreVerified() {
        val body = "stored body".encodeToByteArray()
        val ok = rawLocalEntry("a".encodeToByteArray(), 0, 0, crcOf(body), body.size.toLong(), body.size.toLong(), body)
        assertContentEquals(body, readAll(ok).single().third)
        val badCrc = rawLocalEntry("a".encodeToByteArray(), 0, 0, crcOf(body) xor 1, body.size.toLong(), body.size.toLong(), body)
        assertFailsWith<ZipFormatException> { readAll(badCrc) }
    }

    @Test fun truncatedEntryThrows() {
        val zip = archive()
        for (cut in listOf(10, 30, 100, 50_000, zip.size / 2)) {
            assertFailsWith<IOException>("cut at $cut") { readAll(zip.copyOf(cut)) }
        }
    }

    @Test fun truncatedStoredEntryAndHeaderThrow() {
        val body = ByteArray(100) { it.toByte() }
        val zip = rawLocalEntry("a".encodeToByteArray(), 0, 0, crcOf(body), 100, 100, body)
        assertFailsWith<IOException> { readAll(zip.copyOf(zip.size - 1)) }
        assertFailsWith<IOException> { readAll(zip.copyOf(20)) }
        assertFailsWith<IOException> { readAll(zip.copyOf(2)) }
    }

    @Test fun nextEntrySkipsUnreadBody() {
        ZipReader(Buffer().write(archive())).use { reader ->
            assertEquals("db/bookmarks.sqlite3", reader.nextEntry()!!.name)
            reader.entry.buffer().readByteArray(10) // read only a little of the big body
            assertEquals("manifest.json", reader.nextEntry()!!.name)
            assertEquals("{\"v\":1}", reader.entry.buffer().readUtf8())
            assertEquals("ä/𝔄.txt", reader.nextEntry()!!.name) // skipped unread
            assertNotNull(reader.nextEntry())
            assertNotNull(reader.nextEntry())
            assertNull(reader.nextEntry())
            assertNull(reader.nextEntry())
        }
    }

    @Test fun entryWithoutDescriptorEndingTheInputIsRead() {
        // A deflated entry with known sizes and nothing after it (no central directory).
        val body = "no descriptor".encodeToByteArray()
        val packed = Buffer().also { out ->
            val d = okio.Deflater(-1, true)
            try { okio.DeflaterSink(out, d).buffer().use { it.write(body) } } finally { d.end() }
        }.readByteArray()
        val zip = rawLocalEntry("a".encodeToByteArray(), 0, 8, crcOf(body), packed.size.toLong(), body.size.toLong(), packed)
        assertContentEquals(body, readAll(zip).single().third)
    }

    @Test fun invalidDeflateDataIsAZipFormatException() {
        val zip = rawLocalEntry("a".encodeToByteArray(), 0, 8, 0, 4, 4, byteArrayOf(-1, -1, -1, -1))
        assertFailsWith<ZipFormatException> { readAll(zip) }
    }

    @Test fun absurdHeaderLengthsEndInAnExceptionNotAHang() {
        val zip = Buffer().apply {
            writeIntLe(0x04034b50); writeShortLe(20); writeShortLe(0); writeShortLe(0); writeIntLe(0)
            writeIntLe(0); writeIntLe(0); writeIntLe(0); writeShortLe(0xFFFF); writeShortLe(0xFFFF)
            write(ByteArray(10))
        }.readByteArray()
        assertFailsWith<IOException> { readAll(zip) }
    }

    @Test fun unsupportedMethodAndEncryptionAreRejected() {
        val name = "a".encodeToByteArray()
        assertFailsWith<ZipFormatException> { readAll(rawLocalEntry(name, 0, 12, 0, 0, 0, ByteArray(0))) }
        assertFailsWith<ZipFormatException> { readAll(rawLocalEntry(name, 1, 0, 0, 0, 0, ByteArray(0))) }
    }

    @Test fun entryIsUnavailableBeforeNextEntry() {
        ZipReader(Buffer()).use { r ->
            assertFailsWith<IllegalStateException> { r.entry }
            assertFalse(r.nextEntry() != null)
        }
    }

    private fun flipFirstDescriptorCrc(zip: ByteArray) {
        val sig = byteArrayOf(0x50, 0x4b, 0x07, 0x08)
        var at = -1
        for (i in 0 until zip.size - 4) if (zip.copyOfRange(i, i + 4).contentEquals(sig)) { at = i; break }
        assertTrue(at > 0)
        zip[at + 4] = (zip[at + 4].toInt() xor 0x55).toByte()
    }

    @Test fun readerRefusesToContinueAfterABadCrc() {
        val zip = archive()
        flipFirstDescriptorCrc(zip)
        ZipReader(Buffer().write(zip)).use { r ->
            assertNotNull(r.nextEntry())
            val body = r.entry.buffer()
            assertFailsWith<ZipFormatException> { body.readByteArray() }
            assertFailsWith<ZipFormatException> { r.entry.buffer().readByteArray() } // read after failure
            assertFailsWith<ZipFormatException> { r.nextEntry() }
            assertFailsWith<ZipFormatException> { r.nextEntry() }
        }
    }

    @Test fun readerRefusesToContinueAfterATruncatedDeflateBody() {
        val zip = archive()
        ZipReader(Buffer().write(zip.copyOf(zip.size / 2))).use { r ->
            assertNotNull(r.nextEntry())
            assertFailsWith<IOException> { r.entry.buffer().readByteArray() }
            assertFailsWith<IOException> { r.nextEntry() }
        }
    }

    @Test fun skippingABadEntryFailsWithoutANullPointerException() {
        val zip = archive()
        flipFirstDescriptorCrc(zip)
        ZipReader(Buffer().write(zip)).use { r ->
            assertNotNull(r.nextEntry())
            assertFailsWith<ZipFormatException> { r.nextEntry() } // skipRest verifies
            assertFailsWith<ZipFormatException> { r.nextEntry() }
        }
    }

    @Test fun retriedNextEntryAfterAHeaderErrorRethrows() {
        val zip = rawLocalEntry(byteArrayOf(0x81.toByte()), 0, 0, 0, 0, 0, ByteArray(0))
        ZipReader(Buffer().write(zip)).use { r ->
            assertFailsWith<ZipFormatException> { r.nextEntry() }
            assertFailsWith<ZipFormatException> { r.nextEntry() }
        }
    }

    @Test fun upstreamIoErrorsKeepTheirIdentity() {
        val zip = archive()
        val failing = object : okio.Source {
            val data = Buffer().write(zip.copyOf(30_000))
            override fun read(sink: Buffer, byteCount: Long): Long {
                if (data.size == 0L) throw IOException("disk error")
                return data.read(sink, minOf(byteCount, data.size))
            }
            override fun timeout() = okio.Timeout.NONE
            override fun close() {}
        }
        ZipReader(failing).use { r ->
            assertNotNull(r.nextEntry())
            val e = assertFailsWith<IOException> { r.entry.buffer().readByteArray() }
            assertEquals("disk error", e.message)
            assertFalse(e is ZipFormatException)
        }
    }

    private fun zip64Extra(vararg values: Long) = Buffer().apply {
        writeShortLe(1); writeShortLe(values.size * 8)
        for (v in values) writeLongLe(v)
    }.readByteArray()

    private fun deflate(body: ByteArray) = Buffer().also { out ->
        val d = okio.Deflater(-1, true)
        try { okio.DeflaterSink(out, d).buffer().use { it.write(body) } } finally { d.end() }
    }.readByteArray()

    @Test fun storedSizesAreResolvedFromTheLocalZip64Extra() {
        val body = "zip64 stored".encodeToByteArray()
        val n = body.size.toLong()
        val name = "a".encodeToByteArray()
        val both = rawLocalEntry(name, 0, 0, crcOf(body), 0xFFFFFFFFL, 0xFFFFFFFFL, body, zip64Extra(n, n))
        assertContentEquals(body, readAll(both).single().third)
        // Only the compressed size is marked: it is still the second value of a full extra.
        val onlyCompressed = rawLocalEntry(name, 0, 0, crcOf(body), 0xFFFFFFFFL, n, body, zip64Extra(n, n))
        assertContentEquals(body, readAll(onlyCompressed).single().third)
        val onlySize = rawLocalEntry(name, 0, 0, crcOf(body), n, 0xFFFFFFFFL, body, zip64Extra(n, n))
        assertContentEquals(body, readAll(onlySize).single().third)
        // A short extra holding just the one marked field.
        val shortExtra = rawLocalEntry(name, 0, 0, crcOf(body), n, 0xFFFFFFFFL, body, zip64Extra(n))
        assertContentEquals(body, readAll(shortExtra).single().third)
    }

    @Test fun deflatedEntryWithZip64ExtraReadsEightByteDescriptor() {
        // More lenient than ZipInputStream, which may reject this commons-compress style layout.
        val body = "deflated zip64 descriptor".encodeToByteArray()
        val packed = deflate(body)
        val name = "a".encodeToByteArray()
        for (signature in listOf(true, false)) {
            val descriptor = Buffer().apply {
                if (signature) writeIntLe(0x08074b50)
                writeIntLe(crcOf(body).toInt()); writeLongLe(packed.size.toLong()); writeLongLe(body.size.toLong())
            }.readByteArray()
            val zip = rawLocalEntry(name, 8, 8, 0, 0, 0, packed, zip64Extra(), descriptor + ByteArray(8))
            assertContentEquals(body, readAll(zip).single().third, "signature=$signature")
        }
    }

    @Test fun wrongEightByteDescriptorSizeIsRejected() {
        val body = "deflated".encodeToByteArray()
        val packed = deflate(body)
        val descriptor = Buffer().writeIntLe(0x08074b50).writeIntLe(crcOf(body).toInt())
            .writeLongLe(packed.size.toLong()).writeLongLe(body.size + 1L).readByteArray()
        val zip = rawLocalEntry("a".encodeToByteArray(), 8, 8, 0, 0, 0, packed, zip64Extra(), descriptor)
        assertFailsWith<ZipFormatException> { readAll(zip) }
    }
}
