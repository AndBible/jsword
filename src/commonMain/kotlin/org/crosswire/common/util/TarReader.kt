package org.crosswire.common.util

import okio.BufferedSource
import okio.Closeable
import okio.IOException

/** Sequential ustar reader, including GNU long names and per-entry PAX paths. */
class TarReader(private val source: BufferedSource) : Closeable {
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
            val count = source.read(scratch, minOf(left, 4096))
            if (count == -1L) break
            scratch.clear()
            left -= count
        }
    }

    fun next(): Entry? {
        if (ended) return null
        source.skip(remaining + padding)
        consumed += remaining + padding
        remaining = 0
        padding = 0
        var longName: String? = null
        var paxPath: String? = null
        while (true) {
            val header = source.readByteArray(512)
            consumed += 512
            if (header.all { it == 0.toByte() }) {
                val second = source.readByteArray(512)
                consumed += 512
                if (second.any { it != 0.toByte() }) throw IOException("Invalid tar terminator")
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
