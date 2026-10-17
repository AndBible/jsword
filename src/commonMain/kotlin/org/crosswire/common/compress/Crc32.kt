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

/** Table-driven CRC-32 (IEEE 802.3, polynomial 0xEDB88320) as used by ZIP and GZIP. */
internal class Crc32 {
    private var crc = 0xFFFFFFFFL

    /** The checksum of everything seen so far, in `0..0xFFFFFFFF`. */
    val value: Long get() = crc xor 0xFFFFFFFFL

    fun update(bytes: ByteArray, offset: Int = 0, count: Int = bytes.size) {
        require(offset >= 0 && count >= 0 && offset + count <= bytes.size) { "range out of bounds" }
        var c = crc
        for (i in offset until offset + count) {
            c = TABLE[((c xor bytes[i].toLong()) and 0xFF).toInt()] xor (c ushr 8)
        }
        crc = c
    }

    fun reset() { crc = 0xFFFFFFFFL }

    private companion object {
        val TABLE = LongArray(256) { n ->
            var c = n.toLong()
            repeat(8) { c = if (c and 1L != 0L) 0xEDB88320L xor (c ushr 1) else c ushr 1 }
            c
        }
    }
}
