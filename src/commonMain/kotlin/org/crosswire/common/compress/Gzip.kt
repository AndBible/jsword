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
 * © CrossWire Bible Society, 2014 - 2016
 *
 */
package org.crosswire.common.compress

import okio.Buffer
import okio.GzipSink
import okio.use
import okio.IOException
import kotlin.Throws

/** Gzip block compressor; JVM decoding retains the legacy first-member contract. */
open class Gzip(input: ByteArray) : AbstractCompressor(input) {
    @Throws(IOException::class)
    override fun compress(): ByteArray {
        val output = Buffer()
        GzipSink(output).use { it.write(Buffer().write(input), input.size.toLong()) }
        return output.readByteArray().also {
            // Commons-compress's default OS is unknown, unlike Okio's FAT default.
            it[9] = 0xff.toByte()
        }
    }

    @Throws(IOException::class)
    override fun uncompress(): ByteArray = uncompress(Compressor.BUF_SIZE)

    @Throws(IOException::class)
    override fun uncompress(expectedLength: Int): ByteArray = platformGzipUncompress(input, expectedLength)
}
