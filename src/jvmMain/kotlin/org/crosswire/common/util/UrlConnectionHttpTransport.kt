package org.crosswire.common.util

import java.io.InputStream
import java.io.InterruptedIOException
import javax.net.ssl.HttpsURLConnection

/** JVM default: preserve the legacy HTTPS cast, global proxy handling and lazy metadata access. */
class UrlConnectionHttpTransport internal constructor(
    private val openHttpsConnection: (KUri) -> HttpsURLConnection
) : HttpTransport {
    constructor() : this({ uri -> uri.toURL().openConnection() as HttpsURLConnection })

    private fun connection(uri: KUri, timeoutMs: Int) = openHttpsConnection(uri).apply {
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
    }

    override fun head(uri: KUri, timeoutMs: Int): HttpHead = cancelled {
        val connection = connection(uri, timeoutMs)
        connection.requestMethod = "HEAD"
        val status = connection.responseCode
        if (status == 200) HttpHead(status, null, connection.contentLength, connection.lastModified)
        else HttpHead(status, connection.responseMessage, 0, 0)
    }

    override fun get(uri: KUri, timeoutMs: Int): HttpBody {
        val connection = connection(uri, timeoutMs)
        return object : HttpBody {
            private var input: InputStream? = null
            override val contentLength: Int get() = cancelled { connection.contentLength }
            override val lastModified: Long get() = cancelled { connection.lastModified }
            override val status: Int get() = cancelled { connection.responseCode }
            override fun prepareRead() { cancelled { input = connection.inputStream } }
            override fun read(buf: ByteArray): Int = cancelled { checkNotNull(input).read(buf) }
            override fun close() {
                try {
                    input?.close()
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    private inline fun <T> cancelled(block: () -> T): T = try {
        block()
    } catch (error: InterruptedIOException) {
        throw TransportCancelledException(error)
    }
}

actual fun defaultHttpTransport(): HttpTransport? = UrlConnectionHttpTransport()
