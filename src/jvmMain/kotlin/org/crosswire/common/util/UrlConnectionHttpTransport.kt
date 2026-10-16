package org.crosswire.common.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

/**
 * Owner of the transport's blocking work, never tied to a caller: the blocking calls run here so that a caller that
 * is cancelled can return at once, and the detached disconnect jobs run here.
 */
private val transportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * JVM default: preserve the legacy HTTPS cast, global proxy handling and lazy metadata access.
 *
 * Cancellation. Each blocking call runs as a [kotlinx.coroutines.Deferred] in [transportScope] that the caller awaits.
 * When the caller is cancelled it gets its CancellationException immediately and a detached job disconnects the
 * connection, once per body. The disconnect is detached because the JDK makes it wait: `HttpURLConnection.disconnect()`
 * closes the input stream, which takes the `MeteredStream` lock that a blocked `read()` holds for the whole socket
 * read, and `Thread.interrupt()` does not wake a blocking socket read. So `disconnect()` returns only when the pending
 * read ends (data, EOF or the transport timeout) and then closes the socket.
 *
 * NOT promised: after a cancel the pool thread of the pending read and the socket stay occupied until that read ends
 * or times out, which is bounded by `timeoutMs`; the pending read may still write into the caller's buffer, so a
 * cancelled caller must not reuse it. `close()` of a body whose caller was cancelled does not touch the connection
 * (the detached job owns it), so it never blocks on the same lock.
 */
class UrlConnectionHttpTransport internal constructor(
    private val openHttpsConnection: (KUri) -> HttpsURLConnection
) : HttpTransport {
    constructor() : this({ uri -> uri.toURL().openConnection() as HttpsURLConnection })

    private fun connection(uri: KUri, timeoutMs: Int) = openHttpsConnection(uri).apply {
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
    }

    /** Whether the detached disconnect of one connection was started; it is started at most once. */
    private class Detach(private val connection: HttpsURLConnection) {
        private val started = AtomicBoolean(false)
        val isStarted: Boolean get() = started.get()

        fun start() {
            if (started.compareAndSet(false, true)) {
                transportScope.launch {
                    try { connection.disconnect() } catch (_: Exception) { /* nothing left to report */ }
                }
            }
        }
    }

    override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead {
        val connection = runInterruptible(Dispatchers.IO) { connection(uri, timeoutMs) }
        return cancelled(Detach(connection)) {
            connection.requestMethod = "HEAD"
            val status = connection.responseCode
            if (status == 200) HttpHead(status, null, connection.contentLength, connection.lastModified)
            else HttpHead(status, connection.responseMessage, 0, 0)
        }
    }

    override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody {
        val connection = runInterruptible(Dispatchers.IO) { connection(uri, timeoutMs) }
        val detach = Detach(connection)
        return object : HttpBody {
            private var input: InputStream? = null
            override suspend fun contentLength(): Int = cancelled(detach) { connection.contentLength }
            override suspend fun lastModified(): Long = cancelled(detach) { connection.lastModified }
            override suspend fun status(): Int = cancelled(detach) { connection.responseCode }
            override suspend fun prepareRead() { cancelled(detach) { input = connection.inputStream } }
            override suspend fun read(buf: ByteArray): Int = cancelled(detach) { checkNotNull(input).read(buf) }
            override fun close() {
                // After a cancel the detached job owns the connection: closing here would wait on the lock a
                // blocked read still holds.
                if (detach.isStarted) return
                try {
                    input?.close()
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    /**
     * Runs [block] as a deferred in [transportScope] and awaits it. A cancelled caller returns at once with its
     * CancellationException and starts the detached disconnect; an InterruptedIOException (including a read timeout)
     * maps to TransportCancelledException as before.
     */
    private suspend fun <T> cancelled(detach: Detach, block: () -> T): T {
        val work = transportScope.async { runInterruptible { block() } }
        try {
            return work.await()
        } catch (cancelled: CancellationException) {
            detach.start()
            work.cancel()
            throw cancelled
        } catch (error: InterruptedIOException) {
            throw TransportCancelledException(error)
        }
    }
}

actual fun defaultHttpTransport(): HttpTransport? = UrlConnectionHttpTransport()
