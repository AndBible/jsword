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
import okio.use
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ZipWriterTest {
    private fun archive(dos: Long = 0L, vararg entries: Pair<String, String>): ByteArray {
        val out = Buffer()
        ZipWriter(out).use { zip -> for ((n, b) in entries) zip.putEntry(n, dos) { it.writeUtf8(b) } }
        return out.readByteArray()
    }

    private fun le16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
    private fun le32(b: ByteArray, o: Int) = le16(b, o).toLong() or (le16(b, o + 2).toLong() shl 16)

    /** Offsets of the central directory records, found through the plain EOCD at the end. */
    private fun centralRecords(zip: ByteArray): List<Int> {
        val eocd = zip.size - 22
        assertEquals(0x06054b50L, le32(zip, eocd))
        val count = le16(zip, eocd + 10)
        var p = le32(zip, eocd + 16).toInt()
        val result = mutableListOf<Int>()
        repeat(count) {
            assertEquals(0x02014b50L, le32(zip, p))
            result += p
            p += 46 + le16(zip, p + 28) + le16(zip, p + 30) + le16(zip, p + 32)
        }
        return result
    }

    @Test fun writesNoBytesBeforeFirstEntryOtherThanTheLocalHeader() {
        val zip = archive(0L, "a.txt" to "hello")
        assertContentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04), zip.copyOf(4))
    }

    @Test fun centralDirectoryListsEntriesInOrder() {
        val zip = archive(0L, "one.txt" to "1", "dir/two.txt" to "22", "three" to "")
        val names = centralRecords(zip).map { p ->
            assertEquals(8, le16(zip, p + 10))
            assertEquals(0x0808, le16(zip, p + 8))
            zip.decodeToString(p + 46, p + 46 + le16(zip, p + 28))
        }
        assertEquals(listOf("one.txt", "dir/two.txt", "three"), names)
    }

    @Test fun dosDateTimeIsWrittenToLocalAndCentralHeaders() {
        val zip = archive(0x5A2B6C3DL, "a.txt" to "x")
        // Local header: time at 10, date at 12 (the 32-bit value is date<<16 | time).
        assertEquals(0x5A2B6C3DL, le32(zip, 10))
        assertEquals(0x5A2B6C3DL, le32(zip, centralRecords(zip).single() + 12))
    }

    @Test fun emptyArchiveIsJustEocd() {
        val out = Buffer()
        ZipWriter(out).close()
        val zip = out.readByteArray()
        assertEquals(22, zip.size)
        assertEquals(0x06054b50L, le32(zip, 0))
    }
}
