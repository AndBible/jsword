package org.crosswire.common.util

import okio.BufferedSource
import okio.Closeable
import okio.IOException
import okio.buffer

/** Sequential ustar reader, including GNU long names and per-entry PAX paths. */
class TarReader private constructor(private val source: BufferedSource, private val installerSource: okio.Source?) : Closeable {
    constructor(source: BufferedSource) : this(source,null)
    internal constructor(source: okio.Source) : this(source.buffer(),source)

    private fun readInstaller(buffer: okio.Buffer, count: Long): Long = (installerSource ?: source).read(buffer,count)
    class Entry(val name: String, val size: Long, val isDirectory: Boolean)
    private var remaining = 0L
    private var padding = 0L
    private var ended = false
    private var consumed = 0L

    /** Installer compatibility: commons-compress skips to its 10240-byte block boundary. */
    internal fun consumeLegacyBlockPadding() {
        var left = (10240 - consumed % 10240) % 10240
        val scratch = okio.Buffer()
        while (left > 0) {
            val count = readInstaller(scratch, minOf(left, 4096))
            if (count == -1L) break
            scratch.clear()
            left -= count
        }
    }

    fun next(): Entry? = next(false)

    private fun next(installer: Boolean): Entry? {
        if (ended) return null
        if (installer && remaining == 0L) {
            val scratch = okio.Buffer()
            var left = padding
            while (left > 0) {
                val count = readInstaller(scratch,left)
                if (count == -1L) break
                scratch.clear()
                consumed += count
                left -= count
            }
        } else if (installer) {
            val scratch = okio.Buffer()
            var left = remaining+padding
            while (left > 0) {
                val count = readInstaller(scratch,left)
                if (count == -1L) throw okio.EOFException("Truncated TAR archive")
                scratch.clear(); consumed += count; left -= count
            }
        } else {
            source.skip(remaining + padding)
            consumed += remaining + padding
        }
        remaining = 0
        padding = 0
        var longName: String? = null
        var paxPath: String? = null
        while (true) {
            fun record(): ByteArray? {
                if (!installer) return source.readByteArray(512).also { consumed += 512 }
                val buffer = okio.Buffer()
                while (buffer.size < 512) {
                    val count = readInstaller(buffer,512-buffer.size)
                    if (count == -1L) return null
                    consumed += count
                }
                return buffer.readByteArray()
            }
            val header = record() ?: run { ended = true; return null }
            if (header.all { it == 0.toByte() }) {
                val second = record()
                if (!installer && second!!.any { it != 0.toByte() }) throw IOException("Invalid tar terminator")
                ended = true
                return null
            }
            fun text(start: Int, count: Int): String {
                var end = start
                while (end < start + count && header[end] != 0.toByte()) end++
                return header.copyOfRange(start, end).decodeToString()
            }
            val sizeText = text(124, 12).trim { it <= ' ' }
            val size = sizeText.toLongOrNull(8) ?: throw IOException("Invalid tar size: $sizeText")
            if (size < 0) throw IOException("Negative tar size")
            remaining = size
            padding = (512 - size % 512) % 512
            val type = header[156].toInt().toChar()
            if (type == 'L' || type == 'x') {
                val body = readEntry()
                source.skip(padding)
                consumed += padding
                padding = 0
                if (type == 'L') longName = body.decodeToString().trimEnd('\u0000', '\n')
                else paxPath = paxPath(body) ?: paxPath
                continue
            }
            val prefix = text(345, 155)
            val name = text(0, 100)
            return Entry(paxPath ?: longName ?: if (prefix.isEmpty()) name else "$prefix/$name", size, type == '5')
        }
    }

    /** Installer's legacy single body read permits a short body before next-entry truncation. */
    internal fun readInstallerEntry(): ByteArray {
        if (remaining > Int.MAX_VALUE) throw IOException("Tar entry too large")
        val buffer = okio.Buffer()
        while (remaining > 0) {
            val count = readInstaller(buffer, remaining)
            if (count == -1L) break
            consumed += count
            remaining -= count
        }
        return buffer.readByteArray()
    }

    internal fun nextInstaller(): Entry? {
        val incomplete = remaining > 0
        try { return next(true) }
        catch (error: okio.EOFException) {
            if (incomplete) throw IOException("Truncated TAR archive", error)
            throw error
        }
    }

    /** Consumes only the unread current body, leaving block padding for next(). */
    fun readEntry(): ByteArray {
        if (remaining > Int.MAX_VALUE) throw IOException("Tar entry too large")
        val bytes = source.readByteArray(remaining)
        consumed += remaining
        remaining = 0
        return bytes
    }

    override fun close() = source.close()

    private fun paxPath(body: ByteArray): String? {
        var offset = 0
        var path: String? = null
        while (offset < body.size) {
            var space = offset
            while (space < body.size && body[space] != 32.toByte()) space++
            val length = body.copyOfRange(offset, space).decodeToString().toIntOrNull()
                ?: throw IOException("Invalid PAX record")
            if (length <= space - offset + 1 || length > body.size - offset) throw IOException("Invalid PAX length")
            val record = body.copyOfRange(space + 1, offset + length).decodeToString().trimEnd('\n')
            if (record.startsWith("path=")) path = record.substring(5)
            offset += length
        }
        return path
    }
}
