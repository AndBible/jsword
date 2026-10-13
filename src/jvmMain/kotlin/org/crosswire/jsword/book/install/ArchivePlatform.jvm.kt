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
internal fun archiveRead(path: String): ByteArray = File(path).readBytes()
internal fun archiveSink(path: String): BufferedSink = File(path).outputStream().sink().buffer()
internal fun archiveGzipSource(path: String): BufferedSource = GzipCompressorInputStream(File(path).inputStream()).source().buffer()
internal fun archiveLowercase(name: String): String = name.lowercase(Locale.getDefault())
internal fun archiveZipName(bytes: ByteArray, utf8: Boolean): String = String(bytes, Charsets.UTF_8).replace('\\', '/')

/** Raw-DEFLATE EOF reports consumed input without copying or decoding a selected body twice. */
internal fun archiveDescriptorSource(bytes: ByteArray, offset: Int, consumed: (Int) -> Unit): okio.Source {
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

internal fun archiveUnsupportedZipFeature(name: String, method: Int, encrypted: Boolean): Nothing {
    val entry = org.apache.commons.compress.archivers.zip.ZipArchiveEntry(name)
    entry.method = method
    val flags = org.apache.commons.compress.archivers.zip.GeneralPurposeBit()
    flags.useEncryption(encrypted)
    entry.generalPurposeBit = flags
    if (encrypted) throw org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException(
        org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException.Feature.ENCRYPTION,entry)
    val known = org.apache.commons.compress.archivers.zip.ZipMethod.getMethodByCode(method)
    if (known != null) throw org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException(known,entry)
    throw org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException(
        org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException.Feature.METHOD,entry)
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
