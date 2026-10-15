package org.crosswire.common.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

/**
 * JVM default: preserve the legacy HTTPS cast, global proxy handling and lazy metadata access.
 *
 * Every blocking call runs on Dispatchers.IO through [runInterruptible], and cancelling the calling coroutine also
 * disconnects the connection: blocking socket reads ignore Thread.interrupt, so the disconnect is what actually
 * unblocks a read that waits for the next byte.
 */
class UrlConnectionHttpTransport internal constructor(
    private val openHttpsConnection: (KUri) -> HttpsURLConnection
) : HttpTransport {
    constructor() : this({ uri -> uri.toURL().openConnection() as HttpsURLConnection })

    private fun connection(uri: KUri, timeoutMs: Int) = openHttpsConnection(uri).apply {
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
    }

    override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead {
        val connection = runInterruptible(Dispatchers.IO) { connection(uri, timeoutMs) }
        return cancelled(connection) {
            connection.requestMethod = "HEAD"
            val status = connection.responseCode
            if (status == 200) HttpHead(status, null, connection.contentLength, connection.lastModified)
            else HttpHead(status, connection.responseMessage, 0, 0)
        }
    }

    override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody {
        val connection = runInterruptible(Dispatchers.IO) { connection(uri, timeoutMs) }
        return object : HttpBody {
            private var input: InputStream? = null
            override suspend fun contentLength(): Int = cancelled(connection) { connection.contentLength }
            override suspend fun lastModified(): Long = cancelled(connection) { connection.lastModified }
            override suspend fun status(): Int = cancelled(connection) { connection.responseCode }
            override suspend fun prepareRead() { cancelled(connection) { input = connection.inputStream } }
            override suspend fun read(buf: ByteArray): Int = cancelled(connection) { checkNotNull(input).read(buf) }
            override fun close() {
                try {
                    input?.close()
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    /**
     * Runs [block] on Dispatchers.IO. A cancelled calling coroutine interrupts the thread and disconnects
     * [connection], and the call then ends with a CancellationException; an InterruptedIOException (including a read
     * timeout) maps to TransportCancelledException as before.
     */
    private suspend fun <T> cancelled(connection: HttpsURLConnection, block: () -> T): T =
        withContext(Dispatchers.IO) {
            val running = AtomicBoolean(true)
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } catch (e: CancellationException) {
                    if (running.get()) connection.disconnect()
                    throw e
                }
            }
            try {
                runInterruptible { block() }
            } catch (error: Throwable) {
                // A disconnect or interrupt caused by our own cancellation surfaces as an arbitrary IOException.
                currentCoroutineContext().ensureActive()
                if (error is InterruptedIOException) throw TransportCancelledException(error)
                throw error
            } finally {
                running.set(false)
                watcher.cancel()
            }
        }
}

actual fun defaultHttpTransport(): HttpTransport? = UrlConnectionHttpTransport()
