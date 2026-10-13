package org.crosswire.common.util

/** Host-owned download port (D3); the JVM default retains HttpsURLConnection behavior. */
interface HttpTransport {
    /** HTTP HEAD. Throws okio.IOException when no response arrives. */
    fun head(uri: KUri, timeoutMs: Int): HttpHead

    /**
     * Returns a lazy request. Metadata/status access may establish the connection and fail.
     * WebResource reads contentLength (only with a meter), then status, then prepareRead.
     * This ordering preserves URLConnection's metadata suppression of connection failures.
     */
    fun get(uri: KUri, timeoutMs: Int): HttpBody
}

class HttpHead(val status: Int, val reason: String?, val contentLength: Int, val lastModified: Long)

interface HttpBody : okio.Closeable {
    val status: Int
    val contentLength: Int

    /** Establish the input stream before opening the destination; may throw on HTTP errors. */
    fun prepareRead()

    /** Reads up to buf.size bytes; -1 at EOF. Interrupted/timed-out reads throw TransportCancelledException. */
    fun read(buf: ByteArray): Int
}

/** A read interrupted or timed out (JVM InterruptedIOException, including SocketTimeoutException). */
class TransportCancelledException(cause: Throwable? = null) : okio.IOException(null, cause)
