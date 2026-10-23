@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.jsword.book.install

import kotlinx.cinterop.*
import okio.*
import org.crosswire.common.compress.LegacyGzipSource
import org.crosswire.common.compress.LegacyRawZipSource
import org.crosswire.common.compress.NativeRawInflater
import org.crosswire.common.util.*
import org.crosswire.jsword.JSMsg
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.currentLocale
import platform.Foundation.lowercaseStringWithLocale
import platform.posix.*

internal actual fun archiveRead(path: String): ByteArray = NativeLexicalFiles.read(path)
internal actual fun archiveSink(path: String): BufferedSink = NativeLexicalFiles.sink(path).buffer()
internal actual fun archiveGzipSource(path: String): Source = LegacyGzipSource(NativeLexicalFiles.source(path), ::NativeRawInflater)
internal actual fun archiveLowercase(name: String): String = (name as NSString).lowercaseStringWithLocale(NSLocale.currentLocale)
internal actual fun archiveZipName(bytes: ByteArray, utf8: Boolean): String = KCharset.decode(bytes, 0, bytes.size, "UTF-8").replace('\\', '/')

internal actual fun archiveDescriptorSource(bytes: ByteArray, offset: Int, consumed: (Int) -> Unit): Source {
    val raw = LegacyRawZipSource(bytes, offset, ::NativeRawInflater, "Unexpected end of ZLIB input stream")
    return object : ForwardingSource(raw) {
        override fun read(sink: Buffer, byteCount: Long): Long = super.read(sink, byteCount).also { if (it == -1L) consumed(raw.consumed) }
    }
}
internal actual fun archiveCacheSource(bytes: ByteArray, offset: Int, compressedSize: Int): Pair<Source, () -> Unit> {
    val raw = LegacyRawZipSource(bytes, offset, ::NativeRawInflater, "Truncated ZIP file")
    fun truncated(error: EOFException): Nothing = throw IOException(error.message)
    val source = object : ForwardingSource(raw) {
        override fun read(sink: Buffer, byteCount: Long): Long = try { super.read(sink, byteCount) } catch (error: EOFException) { truncated(error) }
    }
    return source to { try { raw.advance(compressedSize) } catch (error: EOFException) { truncated(error) } }
}
internal actual fun archiveInflateSource(bytes: ByteArray): Source = LegacyRawZipSource(bytes, 0, ::NativeRawInflater, "Unexpected end of ZLIB input stream")

/** Unsupported errors carry the selected duplicate's central AND local metadata, not a pathname lookup. */
internal class NativeUnsupportedZipFeatureException(val feature: String, val entry: CentralZipArchive.Entry) : IOException(
    if (feature == "encryption") "unsupported feature encryption used in entry ${entry.name}"
    // ZipMethod.getMethodByCode maps only the known codes; 11 is absent, so commons uses Feature.METHOD.
    else if (entry.method == 11) "unsupported feature compression method used in entry ${entry.name}"
    else zipMethodName(entry.method)?.let { "unsupported feature method '$it' used in entry ${entry.name}" }
        ?: "unsupported feature compression method used in entry ${entry.name}"
) {
    val time: Long get() = archiveEntryTime(entry)
}

private fun zipMethodName(method: Int): String? = when (method) {
    0 -> "STORED"; 1 -> "UNSHRINKING"; 2 -> "EXPANDING_LEVEL_1"; 3 -> "EXPANDING_LEVEL_2"
    4 -> "EXPANDING_LEVEL_3"; 5 -> "EXPANDING_LEVEL_4"; 6 -> "IMPLODING"; 7 -> "TOKENIZATION"
    8 -> "DEFLATED"; 9 -> "ENHANCED_DEFLATED"; 10 -> "PKWARE_IMPLODING"; 12 -> "BZIP2"
    14 -> "LZMA"; 96 -> "JPEG"; 97 -> "WAVPACK"; 98 -> "PPMD"; 99 -> "AES_ENCRYPTED"
    else -> null
}

internal actual fun archiveUnsupportedZipFeature(archiveSpelling: String, centralIndex: Int): Nothing {
    val zip = CentralZipArchive(NativeLexicalFiles.read(archiveSpelling), ::archiveZipName)
    val entry = zip.entries[centralIndex]
    throw NativeUnsupportedZipFeatureException(if (entry.flags and 1 != 0) "encryption" else "method", entry)
}

/** File.getCanonicalFile equivalence: resolve each existing symlink before applying a later "..".
 * A link whose text is ".." is resolved too. The literal spelling used for the actual write is unchanged.
 */
private fun canonical(path: String): String = canonical(path, ArrayDeque())

private fun canonical(path: String, seen: ArrayDeque<String>): String {
    if ('\u0000' in path) throw IOException("Invalid file path")
    val absolute = NativeLexicalFiles.absolute(NativeLexicalFiles.spelling(path))
    val parts = absolute.split('/')
    var resolved = if (absolute.startsWith('/')) "/" else ""
    for (part in parts) {
        if (part.isEmpty() || part == ".") continue
        if (part == "..") { resolved = NativeLexicalFiles.parent(resolved) ?: "/"; continue }
        val next = if (resolved == "/") "/$part" else if (resolved.isEmpty()) part else "$resolved/$part"
        val link = symlinkTarget(next)
        resolved = if (link == null) next else {
            if (seen.any { it == next }) throw IOException("Symlink loop: $path")
            seen.addLast(next)
            try { canonical(if (link.startsWith('/')) link else NativeLexicalFiles.child(resolved, link), seen) }
            finally { seen.removeLast() }
        }
    }
    return if (resolved.isEmpty()) "/" else resolved
}

/** The link text, or null when the path is not a symlink. A dangling link is still a link. */
private fun symlinkTarget(path: String): String? = memScoped {
    val limit = 4096
    val buffer = allocArray<ByteVar>(limit + 1)
    val count = readlink(path, buffer, limit.toULong())
    if (count < 0) {
        if (errno == EINVAL || errno == ENOENT || errno == ENOTDIR) null
        else throw IOException("readlink $path: ${strerror(errno)?.toKString()}")
    } else if (count >= limit) throw IOException("Symlink target too long: $path")
    else {
        buffer[count.toInt()] = 0
        buffer.toKString()
    }
}

internal actual fun archiveExtract(dest: String, name: String, directory: Boolean, source: () -> Source) {
    val target = NativeLexicalFiles.child(NativeLexicalFiles.spelling(dest), name)
    val root = canonical(dest)
    val actual = canonical(target)
    if (actual != root && !actual.startsWith(if (root == "/") root else "$root/")) throw IOException("Archive entry escapes destination: $name")
    val parent = NativeLexicalFiles.parent(target) ?: throw IOException("Missing archive entry parent: $name")
    if (!NativeLexicalFiles.directory(parent) && !NativeLexicalFiles.mkdirs(parent))
        throw NetMalformedURLException(JSMsg.gettext("The URL {0} could not be created as a directory.", parent))
    if (!directory) {
        // Truncation/open errors precede source creation and unsupported-feature checks.
        val output = NativeLexicalFiles.sink(target)
        var input: Source? = null
        var failure: Throwable? = null
        try {
            input = source()
            val buffer = Buffer()
            while (true) {
                val count = input.read(buffer, 4096)
                if (count == -1L) break
                output.write(buffer, count)
            }
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            var closeFailure: Throwable? = null
            try { output.close() } catch (error: Throwable) { closeFailure = error }
            try { input?.close() } catch (error: Throwable) {
                if (closeFailure == null) closeFailure = error else closeFailure.addSuppressed(error)
            }
            closeFailure?.let { if (failure == null) throw it else failure.addSuppressed(it) }
        }
    }
}

internal actual fun archivePublish(dest: String, from: String, to: String) {
    val source = NativeLexicalFiles.child(NativeLexicalFiles.spelling(dest), from)
    val target = NativeLexicalFiles.child(NativeLexicalFiles.spelling(dest), to)
    if (!NativeLexicalFiles.move(source, target)) throw IOException("Cannot move $from to $to")
}

internal actual fun archiveExists(dest: String, name: String): Boolean =
    NativeLexicalFiles.exists(NativeLexicalFiles.child(NativeLexicalFiles.spelling(dest), name))

internal actual fun archiveDiscard(dest: String, name: String) {
    runCatching { unlink(NativeLexicalFiles.child(NativeLexicalFiles.spelling(dest), name)) }
}
