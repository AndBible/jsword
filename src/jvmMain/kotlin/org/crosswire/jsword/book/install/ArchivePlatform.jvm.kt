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
import java.util.zip.DataFormatException

// Lexical File spelling must never round-trip through okio.Path on Unix.
internal fun archiveRead(path: String): ByteArray = File(path).readBytes()
internal fun archiveSink(path: String): BufferedSink = File(path).outputStream().sink().buffer()
internal fun archiveGzipSource(path: String): BufferedSource = GzipCompressorInputStream(File(path).inputStream()).source().buffer()
internal fun archiveLowercase(name: String): String = name.lowercase(Locale.getDefault())
internal fun archiveZipName(bytes: ByteArray, utf8: Boolean): String = String(bytes, Charsets.UTF_8).replace('\\', '/')

/** Descriptor entries have no local compressed size; raw inflater EOF locates their descriptor. */
internal fun archiveDeflateLength(bytes: ByteArray, offset: Int): Int {
    val inflater = Inflater(true)
    try {
        inflater.setInput(bytes, offset, bytes.size - offset)
        val buffer = ByteArray(4096)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && !inflater.finished()) throw java.io.EOFException("Truncated ZIP deflate")
        }
        return bytes.size - offset - inflater.remaining
    } catch (error: DataFormatException) { throw java.util.zip.ZipException(error.message) }
    finally { inflater.end() }
}

internal fun archiveInflateSource(bytes: ByteArray): okio.Source = java.util.zip.InflaterInputStream(bytes.inputStream(), Inflater(true)).source()

internal fun archiveExtract(dest: String, name: String, directory: Boolean, source: () -> okio.Source) {
    val root = File(dest)
    val target = File(root, name)
    val canonicalRoot = root.canonicalFile.toPath()
    if (!target.canonicalFile.toPath().startsWith(canonicalRoot)) throw IOException("Archive entry escapes destination: $name")
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
