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
import okio.GzipSource
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
 * the stream silently. A bad CRC or ISIZE, or a truncated stream, throws [IOException].
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

/**
 * Decodes member after member with okio's [GzipSource], which does the header, deflate, CRC and ISIZE work.
 *
 * Two things in okio are worked around. `GzipSource` reads ahead of its member, so a member cannot be cut
 * off the front of a longer stream; it is fed one byte per read from [pending], which keeps [pending] right
 * after the member, and its closing check for leftover input (which consumes one byte and fails with
 * [LEFTOVER_MESSAGE]) tells where the member ended: that byte is put back. And it rejects any bytes after
 * a member, where `GZIPInputStream` decodes a following member or silently ignores non-gzip bytes; the
 * header of a following member is therefore checked here before it is handed over.
 */
private class GunzipSource(private val upstream: BufferedSource) : Source {
    private var member: Source? = null
    private var first = true
    private var done = false
    private var lastDelivered: Byte = 0

    /** Bytes pulled from [upstream] and not yet consumed by a member. */
    private val pending = Buffer()

    private val feeder = object : Source {
        override fun read(sink: Buffer, byteCount: Long): Long {
            if (byteCount == 0L) return 0
            if (!request(1L)) return -1
            lastDelivered = pending.readByte()
            sink.writeByte(lastDelivered.toInt())
            return 1
        }
        override fun timeout(): Timeout = upstream.timeout()
        override fun close() {}
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount < 0: $byteCount" }
        if (byteCount == 0L) return 0
        while (!done) {
            val current = member ?: run {
                if (!first && !startFollowingMember()) { done = true; return -1 }
                first = false
                GzipSource(feeder).also { member = it }
            }
            val n = try {
                current.read(sink, byteCount)
            } catch (e: IOException) {
                if (e.message != LEFTOVER_MESSAGE) throw e
                // The member is complete and its closing check consumed the first byte after it.
                val rest = pending.readByteArray()
                pending.writeByte(lastDelivered.toInt())
                pending.write(rest)
                member = null
                continue
            }
            if (n == -1L) { member = null; done = true; return -1 }
            return n
        }
        return -1
    }

    /**
     * True when a valid header of another member is next; its bytes are left in [pending] for the member.
     * Anything else, including a truncated header, ends the stream silently as in `GZIPInputStream`.
     */
    private fun startFollowingMember(): Boolean {
        if (!request(1L)) return false
        val consumed = Buffer()
        val valid = try {
            readHeader(consumed)
            true
        } catch (_: IOException) {
            false
        }
        if (!valid) return false
        val rest = pending.readByteArray()
        pending.write(consumed, consumed.size)
        pending.write(rest)
        return true
    }

    private fun request(count: Long): Boolean {
        while (pending.size < count) if (upstream.read(pending, 8192L) == -1L) return false
        return true
    }

    /** Consumes one gzip header from [pending], copying its bytes to [consumed]. */
    private fun readHeader(consumed: Buffer) {
        val header = Crc32()
        fun byte(): Int {
            if (!request(1L)) throw EOFException("truncated gzip header")
            val value = pending.readByte()
            consumed.writeByte(value.toInt())
            header.update(byteArrayOf(value))
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
            val expected = byte() or (byte() shl 8)
            // The stored CRC16 covers the header bytes before it, which byte() has just also added.
            val actual = Crc32().apply { update(consumed.copy().readByteArray().let { it.copyOf(it.size - 2) }) }.value
            if (expected != (actual and 0xFFFF).toInt()) throw IOException("Corrupt GZIP header")
        }
    }

    override fun timeout(): Timeout = upstream.timeout()

    override fun close() {
        member?.close()
        member = null
        upstream.close()
    }

    private companion object {
        /** okio's message when input remains after a member's trailer. */
        const val LEFTOVER_MESSAGE = "gzip finished without exhausting source"
        const val FEXTRA = 4
        const val FNAME = 8
        const val FCOMMENT = 16
        const val FHCRC = 2
    }
}
