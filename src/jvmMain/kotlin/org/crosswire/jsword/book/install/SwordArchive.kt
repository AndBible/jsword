package org.crosswire.jsword.book.install

import okio.Buffer
import okio.buffer
import okio.Path
import org.crosswire.common.util.LoggerFactory
import org.crosswire.common.util.StoredZipWriter
import org.crosswire.common.util.TarReader
import okio.IOException

/** Archive operations shared by the installer and its JVM File adapters. */
object SwordArchive {
    private val log = LoggerFactory.getLogger(org.crosswire.jsword.book.install.sword.AbstractSwordInstaller::class)

    fun tarGzConfsToZip(tarGz: Path, zip: Path) = tarGzConfsToZipSpelling(tarGz.toString(), zip.toString())
    internal fun tarGzConfsToZipSpelling(tarGz: String, zip: String) {
        val sink = archiveSink(zip)
        val writer = StoredZipWriter.Writer(sink)
        try {
        TarReader(archiveGzipSource(tarGz)).use { reader ->
            while (true) {
                val entry = reader.next() ?: break
                if (entry.isDirectory) continue
                if (!safeName(entry.name)) { log.error("Unsafe archive entry: {}", entry.name); continue }
                if (!entry.name.endsWith(".conf")) { log.error("Not a SWORD config file: {}", entry.name); continue }
                if (entry.size == 0L) { log.error("Empty entry: {}", entry.name); continue }
                writer.entry(entry.name, reader.readEntry())
            }
            reader.consumeLegacyBlockPadding()
        }
        } finally {
            try { writer.finish() } finally { sink.close() }
        }
    }

    fun readConfZip(zip: Path): List<Pair<String, ByteArray>> = readConfZipSpelling(zip.toString())
    internal fun readConfZipSpelling(zip: String): List<Pair<String, ByteArray>> = readZip(archiveRead(zip)).mapNotNull { entry ->
        when {
            entry.directory -> null
            !safeName(entry.name) -> { log.error("Unsafe archive entry: {}", entry.name); null }
            !entry.name.endsWith(".conf") -> { log.error("Not a SWORD config file: {}", entry.name); null }
            else -> entry.bytes.let { body ->
                if (body.isEmpty()) { log.error("Empty entry: {}", entry.name); null }
                else entry.name to body
            }
        }
    }

    /** Boolean is legacy INCLUDE/EXCLUDE polarity, not output lowercasing. */
    fun unpackZip(zip: Path, dest: Path, include: Boolean, vararg dirs: String) =
        unpackZipSpelling(zip.toString(), dest.toString(), include, *dirs)

    internal fun unpackZipSpelling(zip: String, dest: String, include: Boolean, vararg dirs: String) {
        for (entry in readZip(archiveRead(zip))) {
            if (!safeName(entry.name)) throw IOException("Unsafe archive entry: ${entry.name}")
            if (dirs.isNotEmpty()) {
                val matches = dirs.any { archiveLowercase(entry.name).startsWith(it) }
                if (matches != include) continue
            }
            archiveExtract(dest, entry.name, entry.directory) { entry.source() }
        }
    }

    private fun safeName(name: String): Boolean = name.isNotEmpty() && !name.startsWith('/') &&
        !name.startsWith('\\') && !(name.length > 1 && name[1] == ':') &&
        name.replace('\\', '/').split('/').none { it == ".." } && '\u0000' !in name

    private class ZipEntry(val name: String, val directory: Boolean, val source: () -> okio.Source) {
        val bytes: ByteArray get() = source().buffer().use { it.readByteArray() }
    }

    /** Central directory drives iteration, not pathname lookup (which loses duplicate names). */
    private fun readZip(bytes: ByteArray): List<ZipEntry> {
        fun need(offset: Int, count: Int) { if (offset < 0 || count < 0 || offset > bytes.size - count) throw okio.EOFException("Truncated ZIP") }
        fun u16(offset: Int): Int { need(offset, 2); return (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8) }
        fun u32(offset: Int): Long { need(offset, 4); return (0..3).fold(0L) { value, i -> value or ((bytes[offset+i].toLong() and 255) shl (8*i)) } }
        fun u64(offset: Int): Long { val low = u32(offset); val high = u32(offset+4); if (high > 0x7fffffffL) throw IOException("ZIP64 size overflow"); return low or (high shl 32) }
        fun bounded(value: Long): Int { if (value > Int.MAX_VALUE || value < 0) throw IOException("ZIP64/large ZIP unsupported"); return value.toInt() }
        var end = bytes.size - 22
        val min = maxOf(0, bytes.size - 65557)
        while (end >= min && (u32(end) != 0x06054b50L || end + 22 + u16(end + 20) != bytes.size)) end--
        if (end < min) throw IOException("Missing ZIP end record")
        if (u16(end+4) != 0 || u16(end+6) != 0 || u16(end+8) != u16(end+10)) throw IOException("Multi-disk ZIP unsupported")
        var count = u16(end+10)
        var centralSize = bounded(u32(end+12).let { if (it == 0xffffffffL) 0 else it })
        var start = bounded(u32(end+16).let { if (it == 0xffffffffL) 0 else it })
        if (end >= 20 && u32(end-20) == 0x07064b50L) {
            if (u32(end-16) != 0L || u32(end-4) != 1L) throw IOException("Multi-disk ZIP64 unsupported")
            val record = bounded(u64(end-12)); need(record,56)
            if (u32(record) != 0x06064b50L || u32(record+16) != 0L || u32(record+20) != 0L || u64(record+24) != u64(record+32)) throw IOException("Invalid ZIP64 end record")
            count = bounded(u64(record+32)); centralSize = bounded(u64(record+40)); start = bounded(u64(record+48))
        } else if (count == 65535 || u32(end+12) == 0xffffffffL || u32(end+16) == 0xffffffffL) throw IOException("Missing ZIP64 record")
        if (start > end || centralSize > end - start) throw IOException("Invalid central directory")
        var cursor = start
        return buildList {
            repeat(count) {
                need(cursor, 46)
                if (u32(cursor) != 0x02014b50L) throw IOException("Invalid ZIP central header")
                val flags = u16(cursor+8); val method = u16(cursor+10)
                if (flags and 1 != 0 || method !in listOf(0, 8)) throw IOException("Encrypted/unsupported ZIP")
                val crc = u32(cursor+16); var compressedValue = u32(cursor+20); var sizeValue = u32(cursor+24)
                val zip64Sizes = compressedValue == 0xffffffffL || sizeValue == 0xffffffffL
                val nameSize = u16(cursor+28); val extraSize = u16(cursor+30); val commentSize = u16(cursor+32)
                if (u16(cursor+34) != 0) throw IOException("Multi-disk ZIP unsupported")
                need(cursor+46, nameSize+extraSize+commentSize)
                val nameBytes = bytes.copyOfRange(cursor+46, cursor+46+nameSize)
                val name = archiveZipName(nameBytes, flags and 0x800 != 0)
                var localValue = u32(cursor+42)
                var extra = cursor+46+nameSize
                val extraEnd = extra+extraSize
                while (extra < extraEnd) {
                    need(extra,4); val tag = u16(extra); val length = u16(extra+2)
                    if (length > extraEnd-extra-4) throw IOException("Invalid ZIP extra field")
                    if (tag == 1) {
                        var field = extra+4
                        fun next64(): Long { if (field+8 > extra+4+length) throw IOException("Truncated ZIP64 field"); val value=u64(field); field+=8; return value }
                        if (sizeValue == 0xffffffffL) sizeValue=next64()
                        if (compressedValue == 0xffffffffL) compressedValue=next64()
                        if (localValue == 0xffffffffL) localValue=next64()
                    }
                    extra += 4+length
                }
                val compressed = bounded(compressedValue); val size = bounded(sizeValue)
                val local = bounded(localValue); need(local,30)
                if (u32(local) != 0x04034b50L || u16(local+6) != flags || u16(local+8) != method) throw IOException("ZIP local/central mismatch")
                val localName = u16(local+26); val localExtra = u16(local+28)
                need(local+30, localName+localExtra)
                if (!bytes.copyOfRange(local+30,local+30+localName).contentEquals(nameBytes)) throw IOException("ZIP name mismatch")
                if (flags and 8 == 0 && (u32(local+14) != crc || (u32(local+18) != 0xffffffffL && u32(local+18) != compressed.toLong()) || (u32(local+22) != 0xffffffffL && u32(local+22) != size.toLong()))) throw IOException("ZIP size mismatch")
                val data = local+30+localName+localExtra; need(data, compressed)
                if (data.toLong()+compressed > start) throw IOException("ZIP body overlaps central directory")
                if (flags and 8 != 0) {
                    var descriptor = data + compressed
                    if (u32(descriptor) == 0x08074b50L) descriptor += 4
                    val descriptorSize = if (zip64Sizes) 20 else 12
                    val descriptorCompressed = if (zip64Sizes) u64(descriptor+4) else u32(descriptor+4)
                    val descriptorUncompressed = if (zip64Sizes) u64(descriptor+12) else u32(descriptor+8)
                    if (descriptor.toLong() + descriptorSize > start || u32(descriptor) != crc ||
                        descriptorCompressed != compressed.toLong() || descriptorUncompressed != size.toLong()) {
                        throw IOException("ZIP data descriptor mismatch")
                    }
                }
                add(ZipEntry(name, name.endsWith('/')) {
                    val payload = bytes.copyOfRange(data,data+compressed)
                    if (method == 0) Buffer().write(payload) else archiveInflateSource(payload)
                })
                cursor += 46+nameSize+extraSize+commentSize
            }
            if (cursor != start+centralSize) throw IOException("ZIP central directory size mismatch")
        }
    }

    private fun crc32(bytes: ByteArray): Long {
        var crc = -1
        for (byte in bytes) {
            crc = crc xor (byte.toInt() and 255)
            repeat(8) { crc = (crc ushr 1) xor if (crc and 1 != 0) 0xedb88320.toInt() else 0 }
        }
        return crc.inv().toLong() and 0xffffffffL
    }
}
