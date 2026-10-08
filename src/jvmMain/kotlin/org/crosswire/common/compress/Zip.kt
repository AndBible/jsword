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
 * © CrossWire Bible Society, 2007 - 2016
 *
 */
package org.crosswire.common.compress

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Zip manages the compression and uncompression of Zip files.
 *
 * Create a Zip that is capable of transforming the input.
 * @param input to compress or uncompress.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Zip(input: InputStream) : AbstractCompressor(input) {
    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.common.compress.Compressor#compress()
     */
    @Throws(IOException::class)
    override fun compress(): ByteArrayOutputStream {
        val bin = BufferedInputStream(input)
        val bos = ByteArrayOutputStream()
        val out = DeflaterOutputStream(bos, Deflater(), Compressor.BUF_SIZE)
        val buf = ByteArray(Compressor.BUF_SIZE)

        var count = bin.read(buf)
        while (count != -1) {
            out.write(buf, 0, count)
            count = bin.read(buf)
        }
        bin.close()
        out.flush()
        out.close()
        return bos
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.common.compress.Compressor#uncompress()
     */
    @Throws(IOException::class)
    override fun uncompress(): ByteArrayOutputStream {
        return uncompress(Compressor.BUF_SIZE)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.common.compress.Compressor#uncompress(int)
     */
    @Throws(IOException::class)
    override fun uncompress(expectedLength: Int): ByteArrayOutputStream {
        val out = ByteArrayOutputStream(expectedLength)
        val zin = InflaterInputStream(input, Inflater(), expectedLength)
        val buf = ByteArray(expectedLength)

        var count = zin.read(buf)
        while (count != -1) {
            out.write(buf, 0, count)
            count = zin.read(buf)
        }
        zin.close()
        out.flush()
        out.close()
        return out
    }
}
