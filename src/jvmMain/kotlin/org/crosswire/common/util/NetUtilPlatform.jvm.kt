package org.crosswire.common.util

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.JarURLConnection
import java.net.URISyntaxException

internal actual typealias NetMalformedURLException = java.net.MalformedURLException
internal actual fun netUriSyntaxFailure(error: Throwable): Boolean = error is URISyntaxException
internal actual fun netCanRead(uri: KUri): Boolean = File(uri.path).canRead()
internal actual fun netCanWrite(uri: KUri): Boolean = File(uri.path).canWrite()
internal actual fun netCheckResource(uri: KUri) { uri.toURL().openStream().close() }
internal actual fun netUrlSize(uri: KUri): Int = uri.toURL().openConnection().contentLength
private const val TEMP_SUFFIX = "tmp"
internal actual fun netLoadProperties(uri: KUri): PropertyMap {
    var ins: InputStream? = null
    try {
        ins = NetUtilJvm.getInputStream(uri)
        val prop = PropertyMap()
        PropertyMapJvm.load(prop, ins)
        ins.close()
        return prop
    } finally {
        IOUtil.close(ins)
    }
}

internal actual fun netStoreProperties(properties: PropertyMap?, uri: KUri, title: String?) {
    var out: OutputStream? = null

    try {
        out = NetUtilJvm.getOutputStream(uri)
        val temp = PropertyMap()
        temp.putAll(properties!!)
        PropertyMapJvm.store(temp, out, title)
    } finally {
        IOUtil.close(out)
    }
}

internal actual fun netTemporaryUri(prefix: String, suffix: String?): KUri {
    val tempFile = File.createTempFile(prefix, suffix)
    return NetUtilJvm.getURI(tempFile)
}

internal actual fun netListByIndexFile(index: KUri, filter: URIFilter?): Array<String> {
    var `in`: InputStream? = null
    try {
        `in` = NetUtilJvm.getInputStream(index)
        val text = `in`.readBytes().decodeToString()
        return IndexFiles.parse(text) { filter!!.accept(it) }.toTypedArray()
    } finally {
        IOUtil.close(`in`)
    }
}

internal actual fun netUrlLastModified(uri: KUri): Long {
        val urlConnection = uri.toURL().openConnection()
        var time = urlConnection.lastModified

        // If it were a jar then time contains the last modified date of the jar.
        if (urlConnection is JarURLConnection) {
            // form is jar:file:.../xxx.jar!.../filename.ext
            val jarConnection: JarURLConnection = urlConnection
            val jarEntry = jarConnection.jarEntry
            time = jarEntry.time
        }

        return time
}
