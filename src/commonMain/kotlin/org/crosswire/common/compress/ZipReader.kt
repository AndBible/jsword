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
import okio.Closeable
import okio.EOFException
import okio.IOException
import okio.Source
import okio.Timeout
import kotlin.text.CharacterCodingException

/** The archive is not a readable ZIP (bad name encoding, unsupported feature, CRC or size mismatch, corrupt data). */
class ZipFormatException(message: String) : IOException(message)

/**
 * A streaming ZIP reader over local file headers, with the semantics of `ZipInputStream(UTF-8)`: entries come
 * in archive order and the central directory is never read ([nextEntry] returns null when it, or the end of
 * the input, is reached). Memory is O(1) in entry size.
 *
 * - Names are UTF-8 whatever the language-encoding flag says; invalid UTF-8 is a [ZipFormatException]
 *   ("MALFORMED"). Name text is returned raw (no `\` to `/` rewriting).
 * - DEFLATED entries are inflated with or without a data descriptor (its signature is optional; its sizes
 *   are 8 bytes when the local header carries a ZIP64 extra). STORED entries need sizes in the local header;
 *   a STORED entry with a data descriptor, an encrypted entry or another method is a [ZipFormatException].
 * - CRC and sizes are verified when an entry is exhausted (also while [nextEntry] skips an unread rest).
 *
 * Native inflater state is released whenever an entry ends, fails, is skipped or the reader is closed.
 */
class ZipReader(private val upstream: Source) : Closeable {
    class Entry(val name: String, val isDirectory: Boolean)

    private val pending = Buffer()
    private var pulled = 0L
    private var current: Body? = null
    private var done = false
    private var closed = false

    /**
     * The first failure, rethrown by every later [nextEntry] or read: after a failed header or entry the reader
     * cannot know where the next record starts, so it refuses to continue.
     */
    private var failure: IOException? = null

    /** The body of the entry returned by the last [nextEntry]. Closing it does not close the archive. */
    val entry: Source
        get() = checkNotNull(current) { "no current entry" }

    /** Moves to the next entry, skipping what is left of the current one; null at the central directory or the end. */
    fun nextEntry(): Entry? {
        check(!closed) { "closed" }
        failure?.let { throw it }
        try {
            return readNextEntry()
        } catch (e: IOException) {
            failure = e
            current?.release()
            throw e
        }
    }

    private fun readNextEntry(): Entry? {
        current?.let { body ->
            try {
                body.skipRest()
            } finally {
                body.release()
                current = null
            }
        }
        if (done) return null
        if (!request(1L)) { done = true; return null }
        require(4L, "truncated ZIP header")
        if (pending.readIntLe() != LOCAL_HEADER) {
            // Central directory, end record or anything else that is not a local header ends the entries.
            done = true
            return null
        }
        require(26L, "truncated ZIP local header")
        pending.skip(2) // version needed
        val flags = pending.readShortLe().toInt() and 0xFFFF
        val method = pending.readShortLe().toInt() and 0xFFFF
        pending.skip(4) // modification time
        val crc = pending.readIntLe().toLong() and MAX_32
        var compressedSize = pending.readIntLe().toLong() and MAX_32
        var size = pending.readIntLe().toLong() and MAX_32
        val nameLength = pending.readShortLe().toInt() and 0xFFFF
        val extraLength = pending.readShortLe().toInt() and 0xFFFF
        require((nameLength + extraLength).toLong(), "truncated ZIP local header")
        val nameBytes = pending.readByteArray(nameLength.toLong())
        val extra = pending.readByteArray(extraLength.toLong())

        if (flags and FLAG_ENCRYPTED != 0) throw fail("encrypted ZIP entry not supported")
        val hasDescriptor = flags and FLAG_DESCRIPTOR != 0
        if (method != METHOD_STORED && method != METHOD_DEFLATED) throw fail("invalid compression method")
        if (hasDescriptor && method != METHOD_DEFLATED) throw fail("only DEFLATED entries can have EXT descriptor")
        val name = try {
            nameBytes.decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            throw fail("MALFORMED")
        }

        val zip64 = zip64Extra(extra)
        if (zip64 != null && !hasDescriptor) {
            // Only the fields the header marked with 0xFFFFFFFF are present, uncompressed size first.
            if (zip64.size >= 16) {
                // A full local ZIP64 extra holds both sizes, uncompressed first.
                if (size == MAX_32) size = le64(zip64, 0)
                if (compressedSize == MAX_32) compressedSize = le64(zip64, 8)
            } else {
                var at = 0
                if (size == MAX_32 && at + 8 <= zip64.size) { size = le64(zip64, at); at += 8 }
                if (compressedSize == MAX_32 && at + 8 <= zip64.size) compressedSize = le64(zip64, at)
            }
        }
        if (method == METHOD_STORED && compressedSize != size) throw fail("invalid entry size")

        val start = position
        current = Body(method, hasDescriptor, zip64 != null, crc, compressedSize, size, start)
        return Entry(name, name.endsWith("/"))
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            current?.release()
            current = null
        } finally {
            upstream.close()
        }
    }

    /** Stream offset of the next unconsumed byte. */
    private val position: Long get() = pulled - pending.size

    private fun fail(message: String) = ZipFormatException(message)

    private fun refill(): Boolean {
        val n = upstream.read(pending, 8192L)
        if (n == -1L) return false
        pulled += n
        return true
    }

    private fun request(count: Long): Boolean {
        while (pending.size < count) if (!refill()) return false
        return true
    }

    private fun require(count: Long, message: String) {
        if (!request(count)) throw EOFException(message)
    }

    /** The payload of the ZIP64 extended information extra field (id 1), or null if there is none. */
    private fun zip64Extra(extra: ByteArray): ByteArray? {
        var p = 0
        while (p + 4 <= extra.size) {
            val id = (extra[p].toInt() and 0xFF) or ((extra[p + 1].toInt() and 0xFF) shl 8)
            val length = (extra[p + 2].toInt() and 0xFF) or ((extra[p + 3].toInt() and 0xFF) shl 8)
            if (p + 4 + length > extra.size) return null
            if (id == EXTRA_ZIP64) return extra.copyOfRange(p + 4, p + 4 + length)
            p += 4 + length
        }
        return null
    }

    private fun le64(bytes: ByteArray, at: Int): Long {
        var v = 0L
        for (i in 7 downTo 0) v = (v shl 8) or (bytes[at + i].toLong() and 0xFF)
        return v
    }

    private inner class Body(
        private val method: Int,
        private val hasDescriptor: Boolean,
        private val zip64: Boolean,
        private val headerCrc: Long,
        private val headerCompressedSize: Long,
        private val headerSize: Long,
        private val start: Long,
    ) : Source {
        private val crc = Crc32()
        private val scratch = ByteArray(8192)
        private var inflater: RawInflater? = null
        private var stream: RawInflateStream? = null
        private var produced = 0L
        private var finished = false

        init {
            if (method == METHOD_DEFLATED) {
                val raw = newRawInflater()
                inflater = raw
                stream = RawInflateStream(raw, pending) { refill() }
            }
        }

        override fun read(sink: Buffer, byteCount: Long): Long {
            require(byteCount >= 0) { "byteCount < 0: $byteCount" }
            check(!closed) { "closed" }
            failure?.let { throw it }
            if (finished) return -1
            if (byteCount == 0L) return 0
            try {
                val n = if (method == METHOD_STORED) readStored(byteCount) else readDeflated(byteCount)
                if (n == -1) {
                    verify()
                    finished = true
                    release()
                    return -1
                }
                crc.update(scratch, 0, n)
                produced += n
                sink.write(scratch, 0, n)
                return n.toLong()
            } catch (e: Throwable) {
                release()
                // Only corrupt deflate data is an invalid archive; other I/O errors keep their identity.
                val error = if (e is RawDeflateException) ZipFormatException(e.message ?: "invalid file") else e
                if (error is IOException) failure = error
                throw error
            }
        }

        private fun readStored(byteCount: Long): Int {
            val left = headerSize - produced
            if (left == 0L) return -1
            if (pending.size == 0L && !refill()) throw EOFException("truncated ZIP entry")
            val n = minOf(byteCount, left, pending.size, scratch.size.toLong()).toInt()
            return pending.read(scratch, 0, n)
        }

        private fun readDeflated(byteCount: Long): Int =
            stream!!.read(scratch, 0, minOf(byteCount, scratch.size.toLong()).toInt())

        /** Checks CRC and sizes against the header, or the data descriptor that follows the data. */
        private fun verify() {
            val compressed = position - start
            var expectedCrc = headerCrc
            var expectedCompressed = headerCompressedSize
            var expectedSize = headerSize
            var wide = false
            if (hasDescriptor) {
                require(4L, "truncated ZIP data descriptor")
                var first = pending.readIntLe()
                if (first == DESCRIPTOR_SIGNATURE) {
                    require(4L, "truncated ZIP data descriptor")
                    first = pending.readIntLe()
                }
                expectedCrc = first.toLong() and MAX_32
                wide = zip64 || compressed > MAX_32 || produced > MAX_32
                if (wide) {
                    require(16L, "truncated ZIP data descriptor")
                    expectedCompressed = pending.readLongLe()
                    expectedSize = pending.readLongLe()
                } else {
                    require(8L, "truncated ZIP data descriptor")
                    expectedCompressed = pending.readIntLe().toLong() and MAX_32
                    expectedSize = pending.readIntLe().toLong() and MAX_32
                }
            }
            if (expectedCrc != crc.value) throw fail("invalid entry CRC")
            if (expectedSize != produced) throw fail("invalid entry size")
            if (expectedCompressed != compressed) throw fail("invalid entry compressed size")
        }

        /** Reads and verifies the rest of the entry, which is how the end of a DEFLATED body is found. */
        fun skipRest() {
            val sink = Buffer()
            while (read(sink, 8192L) != -1L) sink.clear()
        }

        fun release() {
            stream = null
            val raw = inflater
            inflater = null
            raw?.close()
        }

        override fun timeout(): Timeout = upstream.timeout()

        /** Does nothing: the entry is skipped by [nextEntry] and the archive closed by [ZipReader.close]. */
        override fun close() {}
    }

    private companion object {
        const val MAX_32 = 0xFFFFFFFFL
        const val LOCAL_HEADER = 0x04034b50
        const val DESCRIPTOR_SIGNATURE = 0x08074b50
        const val FLAG_ENCRYPTED = 1
        const val FLAG_DESCRIPTOR = 8
        const val METHOD_STORED = 0
        const val METHOD_DEFLATED = 8
        const val EXTRA_ZIP64 = 1
    }
}
