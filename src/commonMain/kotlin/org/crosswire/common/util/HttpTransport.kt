package org.crosswire.common.util

/**
 * Host-owned suspend download port (D3, K5b D4); the JVM default retains HttpsURLConnection behavior and runs its
 * blocking calls interruptibly, so cancelling the calling coroutine aborts the transfer.
 */
interface HttpTransport {
    /** HTTP HEAD. Throws okio.IOException when no response arrives. */
    suspend fun head(uri: KUri, timeoutMs: Int): HttpHead

    /**
     * Returns a lazy request. Metadata/status access may establish the connection and fail.
     * WebResource reads contentLength (only with a meter), then status, then prepareRead.
     * This ordering preserves URLConnection's metadata suppression of connection failures.
     */
    suspend fun get(uri: KUri, timeoutMs: Int): HttpBody
}

class HttpHead(val status: Int, val reason: String?, val contentLength: Int, val lastModified: Long)

interface HttpBody : okio.Closeable {
    /** HTTP status. May establish the connection (suspends while it does). */
    suspend fun status(): Int

    /** Content-Length, -1 when absent. May establish the connection. */
    suspend fun contentLength(): Int

    /** Last-Modified in epoch millis; 0 when absent (URLConnection semantics). May establish the connection. */
    suspend fun lastModified(): Long = 0L

    /** Establish the input stream before opening the destination; may throw on HTTP errors. */
    suspend fun prepareRead()

    /**
     * Reads up to buf.size bytes; -1 at EOF. Timed-out reads throw TransportCancelledException; cancelling the
     * calling coroutine ends the read with a CancellationException.
     */
    suspend fun read(buf: ByteArray): Int
}

/** A read interrupted or timed out (JVM InterruptedIOException, including SocketTimeoutException). */
class TransportCancelledException(cause: Throwable? = null) : okio.IOException(null, cause)

expect fun defaultHttpTransport(): HttpTransport?
