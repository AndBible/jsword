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
import okio.BufferedSource
import okio.GzipSink
import okio.IOException
import okio.EOFException
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import okio.use
import kotlin.Throws

/**
 * Common GZIP streams with `java.util.zip.GZIPInputStream` decoding semantics: concatenated members
 * are decoded in sequence, and bytes after a member that do not form another valid gzip header end
 * the stream silently (unlike the JDK we always look for a following member, which is identical on
 * in-memory and file streams). A bad CRC or ISIZE, or a truncated stream, throws [IOException].
 */
object GzipStreams {
    /** A sink that gzips (default level) what is written to it into [sink]; close it to finish. */
    fun gzip(sink: Sink): Sink = GzipSink(sink)

    /** A source that decodes the gzip data in [source]. */
    fun gunzip(source: Source): Source = GunzipSource(source.buffer())

    @Throws(IOException::class)
    fun gzip(bytes: ByteArray): ByteArray {
        val out = Buffer()
        gzip(out).buffer().use { it.write(bytes) }
        return out.readByteArray()
    }

    @Throws(IOException::class)
    fun gunzip(bytes: ByteArray): ByteArray = gunzip(Buffer().write(bytes)).buffer().use { it.readByteArray() }
}

private class GunzipSource(private val upstream: BufferedSource) : Source {
    private var inflater: RawInflater? = null
    private var stream: RawInflateStream? = null
    private val crc = Crc32()
    private val output = ByteArray(8192)
    private var first = true
    private var done = false

    /** Bytes pulled from [upstream] and not yet consumed. */
    private val pending = Buffer()

    override fun read(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount < 0: $byteCount" }
        if (byteCount == 0L) return 0
        try {
            while (!done) {
                val current = stream ?: startMember() ?: run { done = true; return -1 }
                val n = current.read(output, 0, minOf(byteCount, output.size.toLong()).toInt())
                if (n == -1) {
                    finishMember(current.totalWritten)
                    continue
                }
                crc.update(output, 0, n)
                sink.write(output, 0, n)
                return n.toLong()
            }
        } catch (e: Throwable) {
            releaseInflater()
            throw e
        }
        return -1
    }

    /**
     * Reads the next member header and starts inflating it; null when no further member follows. A first
     * member must be valid; for later ones an invalid or truncated header ends the stream silently, as
     * in `GZIPInputStream`.
     */
    private fun startMember(): RawInflateStream? {
        if (first) {
            readHeader()
        } else {
            if (!request(1L)) return null
            try {
                readHeader()
            } catch (_: IOException) {
                return null
            }
        }
        first = false
        crc.reset()
        val raw = newRawInflater().also { inflater = it }
        return RawInflateStream(raw, pending) { upstream.read(pending, 8192L) != -1L }.also { stream = it }
    }

    private fun finishMember(written: Long) {
        releaseInflater()
        require(8L)
        val expectedCrc = pending.readIntLe().toLong() and 0xFFFFFFFFL
        val expectedSize = pending.readIntLe().toLong() and 0xFFFFFFFFL
        if (expectedCrc != crc.value) throw IOException("gzip CRC mismatch")
        if (expectedSize != (written and 0xFFFFFFFFL)) throw IOException("gzip ISIZE mismatch")
    }

    private fun releaseInflater() {
        stream = null
        inflater?.close()
        inflater = null
    }

    private fun request(count: Long): Boolean {
        while (pending.size < count) if (upstream.read(pending, 8192L) == -1L) return false
        return true
    }

    private fun require(count: Long) {
        if (!request(count)) throw EOFException("truncated gzip data")
    }

    /** Consumes one gzip header from [pending]. */
    private fun readHeader() {
        val seen = Buffer()
        fun byte(): Int {
            require(1L)
            val value = pending.readByte()
            seen.writeByte(value.toInt())
            return value.toInt() and 0xFF
        }
        if (byte() != 0x1f || byte() != 0x8b) throw IOException("Not in GZIP format")
        if (byte() != 8) throw IOException("Unsupported compression method")
        val flags = byte()
        repeat(6) { byte() } // MTIME, XFL, OS
        if (flags and FEXTRA != 0) {
            val length = byte() or (byte() shl 8)
            repeat(length) { byte() }
        }
        if (flags and FNAME != 0) while (byte() != 0) { /* skip */ }
        if (flags and FCOMMENT != 0) while (byte() != 0) { /* skip */ }
        if (flags and FHCRC != 0) {
            val covered = Crc32().apply { update(seen.snapshot().toByteArray()) }.value
            val expected = byte() or (byte() shl 8)
            if (expected != (covered and 0xFFFF).toInt()) throw IOException("Corrupt GZIP header")
        }
    }

    override fun timeout(): Timeout = upstream.timeout()

    override fun close() {
        releaseInflater()
        upstream.close()
    }

    private companion object {
        const val FEXTRA = 4
        const val FNAME = 8
        const val FCOMMENT = 16
        const val FHCRC = 2
    }
}
