@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.cinterop.*
import okio.*
import platform.posix.*

internal actual class NetMalformedURLException actual constructor(message: String?) : IOException(message) {
    actual constructor() : this(null)
}

internal actual fun netUriSyntaxFailure(error: Throwable): Boolean = error is KUriSyntaxException
internal actual fun netCanRead(uri: KUri): Boolean = NativeLexicalFiles.canRead(uri.getPath()!!)
internal actual fun netCanWrite(uri: KUri): Boolean = NativeLexicalFiles.canWrite(uri.getPath()!!)

private fun transport(uri: KUri): HttpTransport {
    if (urlScheme(uri) != "http" && urlScheme(uri) != "https")
        throw NetMalformedURLException("Unsupported Native URL protocol: ${uri.getScheme()}")
    return WebResource.transport ?: throw IOException("No HttpTransport set")
}

private fun urlScheme(uri: KUri): String {
    if (!uri.isAbsolute()) throw IllegalArgumentException("URI is not absolute")
    return uri.getScheme()!!.lowercase()
}

private fun localUrlPath(uri: KUri): String {
    val authority = uri.rawAuthority()
    if (!authority.isNullOrEmpty() && !authority.equals("localhost", ignoreCase = true))
        throw IOException("Native remote file URL is unsupported: $authority")
    return uri.getPath()!!
}

/** jar:file is a resource URL, not IOUtil's literal bang-split spelling. */
private fun jar(uri: KUri): Pair<NativeZipArchive, String> {
    val spec = uri.toString().substringAfter(':').substringBefore('#')
    val split = spec.indexOf("!/")
    if (split < 0) throw NetMalformedURLException("no !/ in spec")
    val inner = try { KUri(spec.substring(0, split)) } catch (error: KUriSyntaxException) {
        throw NetMalformedURLException(error.message)
    }
    if (urlScheme(inner) != "file") throw NetMalformedURLException("Native jar URL requires file: ${inner.getScheme()}")
    return NativeZipArchive(localUrlPath(inner)) to KUriParts.decode(spec.substring(split + 2))
}

private fun jarEntry(zip: NativeZipArchive, name: String): CentralZipArchive.Entry =
    zip.jarEntry(name) ?: throw FileNotFoundException("JAR entry $name not found")

private fun input(uri: KUri, directFile: Boolean = false): Source {
    if (directFile && uri.getScheme() == "file") return NativeLexicalFiles.source(uri.getPath()!!)
    if (urlScheme(uri) == "file") return NativeLexicalFiles.source(localUrlPath(uri))
    if (urlScheme(uri) == "jar") {
        val (zip, name) = jar(uri)
        if (name.isEmpty()) throw IOException("no entry name specified")
        return zip.source(jarEntry(zip, name))
    }
    // Remote bodies are suspend-only (D4): they are read through WebResource, never as a blocking Source.
    transport(uri)
    throw IOException("Remote URL input requires the suspend WebResource API: $uri")
}

internal actual fun netCheckResource(uri: KUri) { input(uri).close() }
internal actual fun netUrlSize(uri: KUri): Int =
    if (urlScheme(uri) == "file") {
        val path = localUrlPath(uri)
        if (!NativeLexicalFiles.exists(path)) 0 else NativeLexicalFiles.length(path).let {
            if (it > Int.MAX_VALUE) -1 else it.toInt()
        }
    } else if (urlScheme(uri) == "jar") {
        try {
            val (zip, name) = jar(uri)
            zip.jarEntry(name)?.size?.let { if (it > Int.MAX_VALUE) -1 else it.toInt() } ?: -1
        } catch (error: NetMalformedURLException) { throw error }
        catch (_: IOException) { -1 }
    } else {
        transport(uri) // unsupported schemes and a missing transport keep their exceptions
        throw IllegalStateException("A remote size is suspend-only; use NetUtil.getSize: $uri")
    }

internal actual suspend fun netHttpsSize(uri: KUri): Int =
    transport(uri).head(uri, WebResource.getTimeout()).contentLength

internal actual fun netLoadProperties(uri: KUri): PropertyMap {
    var source: BufferedSource? = null
    try {
        uri.getScheme()!! // The direct JVM properties boundary dereferences a relative URI's null scheme.
        source = input(uri, directFile = true).buffer()
        val bytes = source.readByteArray()
        val properties = PropertyMap().apply { load(bytes) }
        source.close()
        return properties
    } finally { IOUtil.close(source) }
}

internal actual fun netStoreProperties(properties: PropertyMap?, uri: KUri, title: String?) {
    var sink: BufferedSink? = null
    try {
        if (uri.getScheme()!! != "file") throw IOException("Native URL output is unsupported: ${uri.getScheme()}")
        sink = NativeLexicalFiles.sink(uri.getPath()!!).buffer()
        val copy = PropertyMap().apply { putAll(properties!!) }
        sink.write(copy.store(title))
        sink.flush()
    } finally { IOUtil.close(sink) }
}

internal actual fun netTemporaryUri(prefix: String, suffix: String?): KUri = memScoped {
    require(prefix.length >= 3) { "Prefix string too short" }
    // File.createTempFile uses the platform temp directory; URI cache is for downloaded resources only.
    val directory = platform.Foundation.NSTemporaryDirectory()
    val name = NativeLexicalFiles.name(NativeLexicalFiles.spelling(prefix))
    val ending = suffix ?: ".tmp"
    if ('\u0000' in name || '\u0000' in ending) throw IOException("Invalid temporary file name")
    val nameMax = pathconf(directory, _PC_NAME_MAX).let { if (it > 0) it.toInt() else 255 }
    while (true) {
        val random = (arc4random().toULong() shl 32 or arc4random().toULong()).toString()
        var prefixLength = name.length
        var randomLength = random.length
        var suffixLength = ending.length
        fun excess() = prefixLength + randomLength + suffixLength - nameMax
        fun shorten(length: Int, amount: Int, minimum: Int) = minOf(length, maxOf(minimum, length - amount))
        if (excess() > 0) prefixLength = shorten(prefixLength, excess(), 3)
        val suffixExcess = excess()
        if (suffixExcess > 0) {
            suffixLength = shorten(suffixLength, suffixExcess, if (ending.startsWith('.')) 4 else 0)
            suffixLength = shorten(suffixLength, suffixExcess, 3)
        }
        if (excess() > 0 && excess() <= randomLength - 5)
            randomLength = shorten(randomLength, excess(), 5)
        val candidate = NativeLexicalFiles.spelling(name.take(prefixLength) + random.take(randomLength) + ending.take(suffixLength))
        if (candidate != NativeLexicalFiles.name(candidate)) throw IOException("Invalid temporary file name")
        val path = NativeLexicalFiles.child(directory, candidate)
        val fd = open(path, O_RDWR or O_CREAT or O_EXCL, 438)
        if (fd >= 0) {
            close(fd)
            return@memScoped KUri("file", null, null, -1, path, null, null)
        }
        if (errno != EEXIST) throw IOException("Could not create temporary file: $path")
    }
    throw IOException("Could not create temporary file")
}

internal actual fun netListByIndexFile(index: KUri, filter: URIFilter?): Array<String> {
    var source: BufferedSource? = null
    try {
        index.getScheme()!!
        source = input(index, directFile = true).buffer()
        return IndexFiles.parse(source.readByteArray().decodeToString()) { filter!!.accept(it) }.toTypedArray()
    } finally { IOUtil.close(source) }
}

internal actual fun netUrlLastModified(uri: KUri): Long =
    if (urlScheme(uri) == "file") memScoped {
        val path = localUrlPath(uri)
        val info = alloc<stat>()
        if ('\u0000' in path || stat(path, info.ptr) != 0) 0L
        else info.st_mtimespec.tv_sec * 1000L + info.st_mtimespec.tv_nsec / 1_000_000L
    } else if (urlScheme(uri) == "jar") {
        val (zip, name) = jar(uri)
        jarEntry(zip, name).let { it.jarModifiedMillis ?: archiveEntryTime(it) }
    } else {
        transport(uri)
        throw IllegalStateException("A remote modification time is suspend-only; use NetUtil.getLastModified: $uri")
    }

internal actual suspend fun netHttpsLastModified(uri: KUri): Long =
    transport(uri).head(uri, WebResource.getTimeout()).lastModified
