package org.crosswire.jsword.book.install

import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.source
import okio.sink
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.crosswire.jsword.JSMsg
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.util.Locale
import java.util.zip.Inflater

// Lexical File spelling must never round-trip through okio.Path on Unix.
internal actual fun archiveRead(path: String): ByteArray = File(path).readBytes()
internal actual fun archiveSink(path: String): BufferedSink = File(path).outputStream().sink().buffer()
internal actual fun archiveGzipSource(path: String): okio.Source = GzipCompressorInputStream(File(path).inputStream()).source()
internal actual fun archiveLowercase(name: String): String = name.lowercase(Locale.getDefault())
internal actual fun archiveZipName(bytes: ByteArray, utf8: Boolean): String = String(bytes, Charsets.UTF_8).replace('\\', '/')

/** Raw-DEFLATE EOF reports consumed input without copying or decoding a selected body twice. */
internal actual fun archiveDescriptorSource(bytes: ByteArray, offset: Int, consumed: (Int) -> Unit): okio.Source {
    val input = java.io.ByteArrayInputStream(bytes, offset, bytes.size - offset)
    val inflater = Inflater(true)
    return object : java.util.zip.InflaterInputStream(input, inflater) {
        override fun read(buffer: ByteArray, off: Int, len: Int): Int {
            val count = super.read(buffer, off, len)
            if (count == -1) consumed(bytes.size - offset - input.available() - inflater.remaining)
            return count
        }
        override fun close() { try { super.close() } finally { inflater.end() } }
    }.source()
}

/** Error-only reopen retains the actual central entry attached to the legacy exception. */
internal actual fun archiveUnsupportedZipFeature(archiveSpelling: String, centralIndex: Int): Nothing {
    org.apache.commons.compress.archivers.zip.ZipFile(File(archiveSpelling)).use { zip ->
        val entries = zip.entries
        repeat(centralIndex) { entries.nextElement() }
        val entry = entries.nextElement()
        zip.getInputStream(entry).use { }
        throw IOException("ZIP feature changed while opening archive")
    }
}

/** Sequential inflater fills may read beyond the body; only that case drains on advance. */
internal actual fun archiveCacheSource(bytes: ByteArray, offset: Int, compressedSize: Int): Pair<okio.Source, () -> Unit> {
    val inflater = Inflater(true)
    var fed = 0
    val source = object : okio.Source {
        override fun timeout() = okio.Timeout.NONE
        override fun close() = inflater.end()
        override fun read(sink: okio.Buffer, byteCount: Long): Long {
            if (byteCount == 0L) return 0
            val output = ByteArray(minOf(byteCount,4096).toInt())
            while (true) {
                if (inflater.needsInput()) {
                    val count = minOf(512,bytes.size-offset-fed)
                    if (count == 0) throw IOException("Truncated ZIP file")
                    inflater.setInput(bytes,offset+fed,count); fed += count
                }
                val count = try { inflater.inflate(output) }
                catch (error: java.util.zip.DataFormatException) { throw java.util.zip.ZipException(error.message).apply { initCause(error) } }
                if (count > 0) { sink.write(output,0,count); return count.toLong() }
                if (inflater.finished()) return -1
                if (!inflater.needsInput()) throw java.util.zip.ZipException("Invalid ZIP deflate")
            }
        }
    }
    return source to {
        if (fed > compressedSize) {
            val scratch = okio.Buffer()
            while (source.read(scratch,4096) != -1L) scratch.clear()
        }
    }
}

internal actual fun archiveInflateSource(bytes: ByteArray): okio.Source = java.util.zip.InflaterInputStream(bytes.inputStream(), Inflater(true)).source()

internal actual fun archiveExtract(dest: String, name: String, directory: Boolean, source: () -> okio.Source) {
    val root = File(dest)
    val target = File(root, name)
    // String comparison, not java.nio.file.Path: that package needs Android API 26 and minSdk is 23.
    val canonicalRoot = root.canonicalPath
    val canonicalTarget = target.canonicalPath
    if (canonicalTarget != canonicalRoot && !canonicalTarget.startsWith(canonicalRoot + File.separator)) throw IOException("Archive entry escapes destination: $name")
    val parent = target.parentFile
    if (!parent.isDirectory && !parent.mkdirs()) {
        throw MalformedURLException(JSMsg.gettext("The URL {0} could not be created as a directory.", parent.toString()))
    }
    if (!directory) {
        val output = target.outputStream()
        val input = source()
        val buffer = okio.Buffer()
        while (true) {
            val count = input.read(buffer, 4096)
            if (count == -1L) break
            output.write(buffer.readByteArray(count))
        }
        output.close()
        input.close()
    }
}

internal actual fun archivePublish(dest: String, from: String, to: String) {
    val root = File(dest)
    if (!File(root, from).renameTo(File(root, to))) throw IOException("Cannot move $from to $to")
}

internal actual fun archiveDiscard(dest: String, name: String) {
    runCatching { File(File(dest), name).delete() }
}
