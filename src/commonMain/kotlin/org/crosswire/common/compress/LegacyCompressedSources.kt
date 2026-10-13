package org.crosswire.common.compress

import okio.Buffer
import okio.EOFException
import okio.IOException
import okio.Source
import okio.Timeout
import okio.buffer

/** A raw inflater owns only codec state; its current input stays alive until replaced or closed. */
internal interface RawInflater {
    val remaining: Int
    val finished: Boolean
    val needsInput: Boolean
    val totalWritten: Long
    fun setInput(bytes: ByteArray, offset: Int, count: Int)
    fun inflate(bytes: ByteArray, offset: Int, count: Int): Int
    fun close()
}

internal class RawDeflateException(message: String?) : IOException(message)

/** Commons 1.12's first-member gzip framing, with bounded output and late trailer validation. */
internal class LegacyGzipSource(raw: Source, factory: () -> RawInflater) : Source {
    private val input = raw.buffer()
    private var inflater: RawInflater? = null
    private var ended = false
    private var closed = false
    private var crc = -1
    private var fed = ByteArray(0)

    init {
        try {
            inflater = factory()
            val first = byteOrEof()
            val second = byteOrEof()
            if (first != 31 || second != 139) throw IOException("Input is not in the .gz format")
            val method = byte()
            if (method != 8) throw IOException("Unsupported compression method $method in the .gz header")
            val flags = byte()
            if (flags and 224 != 0) throw IOException("Reserved flags are set in the .gz header")
            repeat(6) { byte() }
            if (flags and 4 != 0) { val size = byte() or (byte() shl 8); repeat(size) { byte() } }
            if (flags and 8 != 0) while (byte() != 0) Unit
            if (flags and 16 != 0) while (byte() != 0) Unit
            // The legacy decoder consumes FHCRC without validating it.
            if (flags and 2 != 0) repeat(2) { byte() }
        } catch (error: Throwable) {
            try { close() } catch (closeError: Throwable) { error.addSuppressed(closeError) }
            throw error
        }
    }

    private fun byteOrEof(): Int = if (input.exhausted()) -1 else input.readByte().toInt() and 255
    private fun byte(): Int = byteOrEof().also { if (it < 0) throw EOFException() }

    override fun read(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "closed" }
        require(byteCount >= 0)
        if (byteCount == 0L) return 0
        if (ended) return -1
        val codec = inflater!!
        val output = ByteArray(minOf(byteCount, Int.MAX_VALUE.toLong()).toInt())
        var written = 0
        while (written < output.size) {
            if (codec.needsInput) {
                val scratch = Buffer()
                val count = input.read(scratch, 8192)
                if (count == -1L) throw EOFException()
                fed = scratch.readByteArray()
                codec.setInput(fed, 0, fed.size)
            }
            val count = try { codec.inflate(output, written, output.size - written) }
            catch (_: RawDeflateException) { throw IOException("Gzip-compressed data is corrupt") }
            for (index in written until written + count) {
                crc = crc xor (output[index].toInt() and 255)
                repeat(8) { crc = (crc ushr 1) xor (if (crc and 1 != 0) 0xedb88320.toInt() else 0) }
            }
            written += count
            if (codec.finished) {
                // Restore only the inflater's unread compressed input; no next-member decode.
                var at = fed.size - codec.remaining
                fun trailerByte(): Int = if (at < fed.size) fed[at++].toInt() and 255 else byte()
                fun trailerInt(): Long = (0..3).fold(0L) { value, index -> value or (trailerByte().toLong() shl (index * 8)) }
                if (trailerInt() != (crc.inv().toLong() and 0xffffffffL))
                    throw IOException("Gzip-compressed data is corrupt (CRC32 error)")
                if (trailerInt() != (codec.totalWritten and 0xffffffffL))
                    throw IOException("Gzip-compressed data is corrupt(uncompressed size mismatch)")
                codec.close()
                inflater = null
                ended = true
                if (written == 0) return -1
                break
            }
            if (count == 0 && !codec.needsInput) throw IOException("Gzip-compressed data is corrupt")
        }
        sink.write(output, 0, written)
        return written.toLong()
    }

    override fun timeout(): Timeout = input.timeout()
    override fun close() {
        if (closed) return
        closed = true
        try { inflater?.close() } finally { inflater = null; input.close() }
    }
}

/** Raw ZIP streams feed 512 compressed bytes and return at most one 8192-byte output segment. */
internal class LegacyRawZipSource(
    private val bytes: ByteArray,
    private val offset: Int,
    factory: () -> RawInflater,
    private val truncatedMessage: String
) : Source {
    private val inflater = factory()
    private var fed = 0
    private var closed = false
    val consumed: Int get() = fed - inflater.remaining

    override fun read(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "closed" }
        require(byteCount >= 0)
        if (byteCount == 0L) return 0
        if (inflater.finished) return -1
        // InflaterInputStream.source() returns one segment, up to 8192. The 4096 cap is the cache discard buffer, not this stream.
        val output = ByteArray(minOf(byteCount, 8192).toInt())
        while (true) {
            if (inflater.needsInput) {
                val count = minOf(512, bytes.size - offset - fed)
                if (count <= 0) throw EOFException(truncatedMessage)
                inflater.setInput(bytes, offset + fed, count)
                fed += count
            }
            val count = inflater.inflate(output, 0, output.size)
            if (count > 0) { sink.write(output, 0, count); return count.toLong() }
            if (inflater.finished) return -1
            if (!inflater.needsInput) throw RawDeflateException("Invalid ZIP deflate")
        }
    }

    fun advance(compressedSize: Int) {
        if (fed > compressedSize) {
            val scratch = Buffer()
            while (read(scratch, 4096) != -1L) scratch.clear()
        }
    }
    override fun timeout() = Timeout.NONE
    override fun close() { if (!closed) { closed = true; inflater.close() } }
}
