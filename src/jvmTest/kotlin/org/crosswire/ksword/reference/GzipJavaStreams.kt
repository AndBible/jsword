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
package org.crosswire.ksword.reference

import org.crosswire.common.compress.Compressor

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

import org.apache.commons.compress.compressors.CompressorInputStream
import org.apache.commons.compress.compressors.CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils

/**
 * GZip manages the compression and uncompression of GZip data.
 *
 * Create a GZip that is capable of transforming the input.
 * @param input to compress or uncompress.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class GzipJavaStreams(private val input: InputStream) {
    /* (non-Javadoc)
     * @see org.crosswire.common.compress.Compressor#compress()
     */
    @Throws(IOException::class)
    fun compress(): ByteArrayOutputStream {
        val bin = BufferedInputStream(input)
        val bos = ByteArrayOutputStream()
        val out: CompressorOutputStream = GzipCompressorOutputStream(bos)
        IOUtils.copy(bin, out)
        bin.close()
        out.flush()
        out.close()
        return bos
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.compress.Compressor#uncompress()
     */
    @Throws(IOException::class)
    fun uncompress(): ByteArrayOutputStream {
        return uncompress(Compressor.BUF_SIZE)
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.compress.Compressor#uncompress(int)
     */
    @Throws(IOException::class)
    fun uncompress(expectedLength: Int): ByteArrayOutputStream {
        val out = ByteArrayOutputStream(expectedLength)
        val cin: CompressorInputStream = GzipCompressorInputStream(input)
        IOUtils.copy(cin, out)
        cin.close()
        out.flush()
        out.close()
        return out
    }
}
