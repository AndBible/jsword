package org.crosswire.common.util

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cancelling the coroutine that reads a body mid-transfer must end the read and drop the connection (K5b D4).
 * The server speaks plain HTTP, so the connection handed to [UrlConnectionHttpTransport] is an
 * [HttpsURLConnection] shell that delegates to a real [HttpURLConnection] (real sockets, real blocking reads).
 *
 * The test fails if the read is not cancellable: without the transport's disconnect on cancellation the blocking
 * socket read ignores the thread interrupt, the read loop keeps receiving bytes (the server sends for 5 s), and the
 * join below exceeds its 1 s bound; if the connection were not dropped the server would not see the close.
 */
class UrlConnectionTransportCancelTest {
    private class DelegatingConnection(private val real: HttpURLConnection) : HttpsURLConnection(real.url) {
        override fun connect() = real.connect()
        override fun disconnect() = real.disconnect()
        override fun usingProxy(): Boolean = false
        override fun getCipherSuite(): String? = null
        override fun getLocalCertificates(): Array<java.security.cert.Certificate>? = null
        override fun getServerCertificates(): Array<java.security.cert.Certificate>? = null
        override fun getResponseCode(): Int = real.responseCode
        override fun getResponseMessage(): String? = real.responseMessage
        override fun getContentLength(): Int = real.contentLength
        override fun getLastModified(): Long = real.lastModified
        override fun getInputStream(): java.io.InputStream = real.inputStream
        override fun setRequestMethod(method: String) { real.requestMethod = method }
        override fun setConnectTimeout(timeout: Int) { real.connectTimeout = timeout }
        override fun setReadTimeout(timeout: Int) { real.readTimeout = timeout }
    }

    @Test fun cancellingTheReaderMidBodyStopsTheReadAndClosesTheConnection() = runTest {
        val serverSawClose = CountDownLatch(1)
        val serverBytesSent = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/slow") { exchange ->
            exchange.sendResponseHeaders(200, 0) // chunked: headers now, body later
            try {
                repeat(50) {
                    exchange.responseBody.write(1)
                    exchange.responseBody.flush()
                    serverBytesSent.incrementAndGet()
                    Thread.sleep(100)
                }
            } catch (_: java.io.IOException) {
                serverSawClose.countDown() // the client dropped the connection
            } finally {
                try { exchange.close() } catch (_: Exception) {}
            }
        }
        server.start()
        try {
            val uri = URI("http://127.0.0.1:${server.address.port}/slow")
            val transport = UrlConnectionHttpTransport { u -> DelegatingConnection(u.toURL().openConnection() as HttpURLConnection) }
            var received = 0
            var failure: Throwable? = null
            withContext(Dispatchers.Default) {
                val body = transport.get(uri, 10_000)
                try {
                    assertEquals(200, body.status())
                    body.prepareRead()
                    val reader = launch {
                        val buf = ByteArray(16)
                        try {
                            while (true) {
                                val n = body.read(buf)
                                if (n < 0) break
                                received += n
                            }
                        } catch (e: Throwable) { failure = e; throw e }
                    }
                    delay(300)
                    val cancelledAt = System.nanoTime()
                    withTimeout(1_000) { reader.cancelAndJoin() }
                    assertTrue(serverSawClose.await(1, TimeUnit.SECONDS), "the server must see the connection close")
                    val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - cancelledAt)
                    assertTrue(elapsedMs < 1_000, "close seen after $elapsedMs ms")
                } finally { body.close() }
            }
            assertTrue(received >= 1, "the reader must have been mid-body when cancelled")
            val ended = assertNotNull(failure, "the read loop must have ended by an exception")
            assertTrue(ended is TransportCancelledException || ended is CancellationException, "got $ended")
        } finally {
            server.stop(0)
        }
    }
}
