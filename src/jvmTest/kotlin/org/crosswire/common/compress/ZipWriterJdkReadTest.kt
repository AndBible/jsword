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
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDateTime
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZipWriterJdkReadTest {
    private val bodies = linkedMapOf(
        "db/bookmarks.sqlite3" to ByteArray(100_000) { (it * 31 % 251).toByte() },
        "AndBibleBackupManifest.json" to "{\"v\":1}".encodeToByteArray(),
        "mods.d/kjv.conf" to "[KJV]\nDescription=ä\n".encodeToByteArray(),
        "ä/𝔄.txt" to "unicode".encodeToByteArray(),
        "empty" to ByteArray(0),
    )

    /** 2025-06-15 12:34:56 as a 32-bit MS-DOS value. */
    private val dos = (((2025 - 1980) shl 9) or (6 shl 5) or 15).toLong() shl 16 or
        ((12 shl 11) or (34 shl 5) or (56 / 2)).toLong()

    private fun write(bodies: Map<String, ByteArray>, threshold: Long = 0xFFFFFFFFL): ByteArray {
        val out = Buffer()
        ZipWriter(out, threshold).use { zip ->
            for ((n, b) in bodies) {
                if (b.size % 2 == 0) zip.putEntry(n, Buffer().write(b), dos) else zip.putEntry(n, dos) { it.write(b) }
            }
        }
        return out.readByteArray()
    }

    private fun readWithZipInputStream(zip: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zip)).use { zis ->
            while (true) {
                val e = zis.nextEntry ?: break
                result[e.name] = zis.readBytes()
            }
        }
        return result
    }

    private fun readWithZipFile(zip: ByteArray): Map<String, ByteArray> {
        val f = File.createTempFile("zipwriter", ".zip")
        try {
            f.writeBytes(zip)
            ZipFile(f).use { zf ->
                return zf.entries().asSequence().associate { it.name to zf.getInputStream(it).readBytes() }
            }
        } finally { f.delete() }
    }

    private fun assertSame(expected: Map<String, ByteArray>, actual: Map<String, ByteArray>) {
        assertEquals(expected.keys.toList(), actual.keys.toList())
        for ((k, v) in expected) assertContentEquals(v, actual.getValue(k), k)
    }

    @Test fun zipInputStreamReadsAllEntries() = assertSame(bodies, readWithZipInputStream(write(bodies)))

    @Test fun zipFileReadsAllEntries() = assertSame(bodies, readWithZipFile(write(bodies)))

    @Test fun forcedZip64IsReadByZipFileAndZipInputStream() {
        val three = linkedMapOf(
            "a" to ByteArray(1000) { (it % 7).toByte() },
            "b" to ByteArray(1000) { (it * 13 % 256).toByte() },
            "c" to ByteArray(1001) { 5 },
        )
        val zip = write(three, threshold = 10)
        assertSame(three, readWithZipFile(zip))
        assertSame(three, readWithZipInputStream(zip))

        // Metadata: a wrong ZIP64 extra field order would still read the bodies, so check the values.
        val streamed = HashMap<String, ZipEntry>()
        ZipInputStream(ByteArrayInputStream(zip)).use { zis ->
            while (true) {
                val e = zis.nextEntry ?: break
                zis.readBytes()
                streamed[e.name] = e
            }
        }
        val f = File.createTempFile("zipwriter", ".zip")
        try {
            f.writeBytes(zip)
            ZipFile(f).use { zf ->
                for ((name, body) in three) {
                    val e = zf.getEntry(name)
                    assertEquals(body.size.toLong(), e.size, "$name size")
                    assertEquals(CRC32().apply { update(body) }.value, e.crc, "$name crc")
                    assertTrue(e.compressedSize in 1 until body.size, "$name compressedSize ${e.compressedSize}")
                    assertEquals(streamed.getValue(name).compressedSize, e.compressedSize, "$name compressedSize vs descriptor")
                    assertEquals(LocalDateTime.of(2025, 6, 15, 12, 34, 56), e.timeLocal, "$name time")
                }
            }
        } finally { f.delete() }
    }

    @Test fun constructorRejectsThresholdThatCannotFitIn32Bits() {
        assertFailsWith<IllegalArgumentException> { ZipWriter(Buffer(), 0x1_0000_0000L) }
        assertFailsWith<IllegalArgumentException> { ZipWriter(Buffer(), 0L) }
    }

    @Test fun sixtyFiveThousandFiveHundredThirtyFiveEntriesGoZip64() {
        val out = Buffer()
        ZipWriter(out).use { zip -> repeat(0xFFFF) { zip.putEntry("e$it") { } } }
        val zip = out.readByteArray()
        val sig = byteArrayOf(0x50, 0x4b, 0x06, 0x06)
        assertTrue((0..zip.size - 4).any { i -> (0 until 4).all { zip[i + it] == sig[it] } }, "ZIP64 EOCD present")
        val f = File.createTempFile("zipwriter", ".zip")
        try {
            f.writeBytes(zip)
            ZipFile(f).use { zf ->
                assertEquals(0xFFFF, zf.size())
                val names = zf.entries().asSequence().map { it.name }.toList()
                assertEquals("e0", names.first())
                assertEquals("e65534", names.last())
                assertEquals(0L, zf.getEntry("e65534").size)
            }
        } finally { f.delete() }
    }

    @Test fun bodyIsCompressed() {
        val zip = write(mapOf("a" to ByteArray(1 shl 20) { 'a'.code.toByte() }))
        assertTrue(zip.size < 10 * 1024, "archive is ${zip.size} bytes")
    }

    @Test fun failureInsideWriteLeavesNoCentralDirectory() {
        val out = Buffer()
        var closed = false
        val tracking = object : okio.ForwardingSink(out) {
            override fun close() { closed = true; super.close() }
        }
        val zip = ZipWriter(tracking)
        assertFailsWith<IllegalStateException> {
            zip.putEntry("x") { it.writeUtf8("partial"); throw IllegalStateException("boom") }
        }
        assertFailsWith<IllegalStateException> { zip.putEntry("y") { it.writeUtf8("late") } }
        zip.close()
        assertTrue(closed, "sink closed")
        val bytes = out.readByteArray()
        val eocd = byteArrayOf(0x50, 0x4b, 0x05, 0x06)
        val found = (0..bytes.size - 4).any { i -> (0 until 4).all { bytes[i + it] == eocd[it] } }
        assertFalse(found)
    }
}
