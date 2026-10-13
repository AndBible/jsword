package org.crosswire.common.util

import okio.EOFException
import okio.IOException

/** Metadata-only central index. Bodies remain compressed and are opened only by selected consumers. */
internal class CentralZipArchive(private val bytes: ByteArray, private val decode: (ByteArray, Boolean) -> String) {
    internal class Entry(
        val centralIndex: Int, val name: String, val rawName: ByteArray, val flags: Int,
        val method: Int, val size: Long, val compressedSize: Long, val crc: Long,
        val dosTime: Int, val dosDate: Int, val platform: Int, val versionMadeBy: Int,
        val versionRequired: Int, val internalAttributes: Int, val externalAttributes: Long,
        val comment: String, val centralExtra: ByteArray, val localExtra: ByteArray,
        val rawCentralExtra: ByteArray, val rawLocalExtra: ByteArray, val localOffset: Int,
        val dataOffset: Int, val localFlags: Int, val localMethod: Int, val localName: ByteArray,
        val jarName: String, val jarModifiedMillis: Long?
    )
    val entries: List<Entry> = parse()
    fun entry(name: String): Entry? = entries.firstOrNull { it.name == name }
    fun jarEntry(name: String): Entry? = entries.lastOrNull { it.jarName == name }
    fun payload(entry: Entry): ByteArray = bytes.copyOfRange(entry.dataOffset, entry.dataOffset + bounded(entry.compressedSize))

    private fun need(offset: Int, count: Int) {
        if (offset < 0 || count < 0 || offset > bytes.size - count) throw EOFException("Truncated ZIP")
    }
    private fun u16(offset: Int): Int { need(offset, 2); return (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8) }
    private fun u32(offset: Int): Long { need(offset, 4); return (0..3).fold(0L) { value, i -> value or ((bytes[offset + i].toLong() and 255) shl (8 * i)) } }
    private fun u64(offset: Int): Long {
        val low = u32(offset); val high = u32(offset + 4)
        if (high > 0x7fffffff) throw IOException("ZIP64 size overflow")
        return low or (high shl 32)
    }
    private fun bounded(value: Long): Int {
        if (value < 0 || value > Int.MAX_VALUE) throw IOException("ZIP64/large ZIP unsupported")
        return value.toInt()
    }
    private fun slice(offset: Int, size: Int): ByteArray { need(offset, size); return bytes.copyOfRange(offset, offset + size) }

    private fun parse(): List<Entry> {
        var end = bytes.size - 22
        val min = maxOf(0, bytes.size - 65557)
        while (end >= min && (u32(end) != 0x06054b50L || end + 22 + u16(end + 20) != bytes.size)) end--
        if (end < min) throw IOException("Missing ZIP end record")
        if (u16(end + 4) != 0 || u16(end + 6) != 0 || u16(end + 8) != u16(end + 10)) throw IOException("Multi-disk ZIP unsupported")
        var count = u16(end + 10)
        var centralSize = bounded(u32(end + 12).let { if (it == 0xffffffffL) 0 else it })
        var start = bounded(u32(end + 16).let { if (it == 0xffffffffL) 0 else it })
        if (end >= 20 && u32(end - 20) == 0x07064b50L) {
            if (u32(end - 16) != 0L || u32(end - 4) != 1L) throw IOException("Multi-disk ZIP64 unsupported")
            val record = bounded(u64(end - 12)); need(record, 56)
            if (u32(record) != 0x06064b50L || u32(record + 16) != 0L || u32(record + 20) != 0L || u64(record + 24) != u64(record + 32)) throw IOException("Invalid ZIP64 end record")
            count = bounded(u64(record + 32)); centralSize = bounded(u64(record + 40)); start = bounded(u64(record + 48))
        } else if (count == 65535 || u32(end + 12) == 0xffffffffL || u32(end + 16) == 0xffffffffL) throw IOException("Missing ZIP64 record")
        if (start > end || centralSize > end - start) throw IOException("Invalid central directory")
        var cursor = start
        return buildList {
            repeat(count) { index ->
                need(cursor, 46)
                if (u32(cursor) != 0x02014b50L) throw IOException("Invalid ZIP central header")
                val flags = u16(cursor + 8); val method = u16(cursor + 10)
                var compressed = u32(cursor + 20); var size = u32(cursor + 24); var local = u32(cursor + 42)
                val nameSize = u16(cursor + 28); val extraSize = u16(cursor + 30); val commentSize = u16(cursor + 32)
                if (u16(cursor + 34) != 0) throw IOException("Multi-disk ZIP unsupported")
                need(cursor + 46, nameSize + extraSize + commentSize)
                val rawName = slice(cursor + 46, nameSize)
                val centralExtra = slice(cursor + 46 + nameSize, extraSize)
                var extra = cursor + 46 + nameSize
                val extraEnd = extra + extraSize
                while (extra + 4 <= extraEnd) {
                    val tag = u16(extra); val length = u16(extra + 2)
                    if (length > extraEnd - extra - 4) break
                    if (tag == 1) {
                        var field = extra + 4
                        fun next(): Long { if (field + 8 > extra + 4 + length) throw IOException("Truncated ZIP64 field"); return u64(field).also { field += 8 } }
                        if (size == 0xffffffffL) size = next()
                        if (compressed == 0xffffffffL) compressed = next()
                        if (local == 0xffffffffL) local = next()
                    }
                    extra += 4 + length
                }
                val localOffset = bounded(local); need(localOffset, 30)
                val localNameSize = u16(localOffset + 26); val localExtraSize = u16(localOffset + 28)
                val rawLocalExtra = slice(localOffset + 30 + localNameSize, localExtraSize)
                val data = localOffset + 30 + localNameSize + localExtraSize
                need(data, bounded(compressed))
                val normalized = mergeExtras(centralExtra, rawLocalExtra)
                val rawComment = slice(cursor + 46 + nameSize + extraSize, commentSize)
                val utf8 = flags and 0x800 != 0
                val name = if (!utf8) unicodeOverride(normalized.second, 0x7075, rawName) ?: decode(rawName, false) else decode(rawName, true)
                val comment = if (!utf8 && rawComment.isNotEmpty()) unicodeOverride(normalized.second, 0x6375, rawComment) ?: decode(rawComment, false) else decode(rawComment, utf8)
                add(Entry(index, name, rawName, flags, method, size, compressed,
                    u32(cursor + 16), u16(cursor + 12), u16(cursor + 14), u16(cursor + 4) ushr 8,
                    u16(cursor + 4), u16(cursor + 6), u16(cursor + 36), u32(cursor + 38),
                    comment,
                    normalized.first, normalized.second, centralExtra, rawLocalExtra, localOffset, data,
                    u16(localOffset + 6), u16(localOffset + 8), slice(localOffset + 30, localNameSize),
                    KCharset.decode(rawName, 0, rawName.size, "UTF-8"), jarTimestamp(centralExtra)))
                cursor += 46 + nameSize + extraSize + commentSize
            }
            if (cursor != start + centralSize) throw IOException("ZIP central directory size mismatch")
        }
    }

    /** JarEntry parses raw central time fields in order, without commons' local-field merging. */
    private fun jarTimestamp(extra: ByteArray): Long? {
        fun u16(at: Int) = (extra[at].toInt() and 255) or ((extra[at + 1].toInt() and 255) shl 8)
        fun u32(at: Int): Long = (0..3).fold(0L) { value, i -> value or ((extra[at + i].toLong() and 255) shl (8 * i)) }
        var at = 0
        var time: Long? = null
        while (at + 4 < extra.size) {
            val tag = u16(at); val size = u16(at + 2); at += 4
            if (size > extra.size - at) break
            if (tag == 0x5455 && size >= 5 && extra[at].toInt() and 1 != 0)
                time = u32(at + 1).toInt().toLong() * 1000L
            if (tag == 10 && size >= 32 && u16(at + 4) == 1 && u16(at + 6) == 24) {
                val ticks = u32(at + 8) or (u32(at + 12) shl 32)
                if (ticks != Long.MIN_VALUE) time = (ticks / 10L - 11644473600000000L) / 1000L
            }
            at += size
        }
        return time
    }

    private fun unicodeOverride(extra: ByteArray, wanted: Int, original: ByteArray): String? {
        var at = 0
        while (at + 4 <= extra.size) {
            val tag = (extra[at].toInt() and 255) or ((extra[at + 1].toInt() and 255) shl 8)
            val size = (extra[at + 2].toInt() and 255) or ((extra[at + 3].toInt() and 255) shl 8)
            if (size > extra.size - at - 4) break
            if (tag == wanted && size >= 5 && extra[at + 4] == 1.toByte()) {
                val expected = (0..3).fold(0L) { value, i -> value or ((extra[at + 5 + i].toLong() and 255) shl (8 * i)) }
                var crc = -1
                for (byte in original) {
                    crc = crc xor (byte.toInt() and 255)
                    repeat(8) { crc = (crc ushr 1) xor (if (crc and 1 != 0) 0xedb88320.toInt() else 0) }
                }
                if (expected == (crc.inv().toLong() and 0xffffffffL)) return decode(extra.copyOfRange(at + 9, at + 4 + size), true)
            }
            at += 4 + size
        }
        return null
    }

    /** Commons retains central field order, appending local-only fields to both representations. */
    private fun mergeExtras(central: ByteArray, local: ByteArray): Pair<ByteArray, ByteArray> {
        fun fields(bytes: ByteArray): List<Pair<Int, ByteArray>> {
            var at = 0
            return buildList {
                while (at + 4 <= bytes.size) {
                    val tag = (bytes[at].toInt() and 255) or ((bytes[at + 1].toInt() and 255) shl 8)
                    val size = (bytes[at + 2].toInt() and 255) or ((bytes[at + 3].toInt() and 255) shl 8)
                    if (size > bytes.size - at - 4) break
                    add(tag to bytes.copyOfRange(at + 4, at + 4 + size)); at += 4 + size
                }
                if (at < bytes.size) add(-1 to bytes.copyOfRange(at, bytes.size))
            }
        }
        val cf = fields(central); val lf = fields(local)
        val tags = (cf.map { it.first } + lf.map { it.first }).distinct()
        fun serialize(localSide: Boolean): ByteArray = okio.Buffer().apply {
            for (tag in tags) {
                val c = cf.lastOrNull { it.first == tag }?.second
                val l = lf.lastOrNull { it.first == tag }?.second
                var body = if (localSide) l ?: c!! else c ?: l!!
                // ZIP64 is one merged object. Parsing the local sizes adds those sizes to its central form.
                if (!localSide && tag == 1 && l != null && l.size >= 16) body = l.copyOf(16) + (c?.drop(16)?.toByteArray() ?: byteArrayOf())
                if (tag in listOf(0x7075, 0x6375) && l != null) body = l
                if (tag == 0x5455) {
                    val timestamp = l ?: c!!
                    body = if (localSide) timestamp else timestamp.copyOf(if (timestamp[0].toInt() and 1 != 0) 5 else 1)
                }
                if (!localSide && tag == 0x7875) body = byteArrayOf()
                if (tag >= 0) { writeShortLe(tag); writeShortLe(body.size) }
                write(body)
            }
        }.readByteArray()
        return serialize(false) to serialize(true)
    }
}
