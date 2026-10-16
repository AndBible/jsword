package org.crosswire.common.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A caller cancelled while its connection is still being established (here: the TLS handshake, during which
 * `HttpURLConnection.http` is still null, so the first disconnect has no socket to close) must not leave the socket
 * to the garbage collector when the connection completes late.
 *
 * The test puts a delaying TCP front in front of a local TLS server: the client connects at once, but nothing answers
 * its handshake for [DELAY_MS]. The caller is cancelled during that delay. After the front starts forwarding, the
 * abandoned work may complete, and the front must see the client socket closed within a bound.
 *
 * Honest limit: this is the best bounded version, not a proven reproduction. Against the round 2 transport (one
 * disconnect) it also passes: on JDK 17 the socket was already closed during the handshake delay, so the second
 * disconnect that the reviewer asked for (`disconnect(); work.join(); disconnect()`) could not be shown to be
 * necessary here. The pure TCP-connect window (no socket yet) cannot be produced deterministically on loopback, where
 * the kernel completes the connect at once. The test guards the observable contract (no socket left open after a
 * cancel during connection set-up) and the second disconnect is defensive.
 */
class UrlConnectionLateConnectCancelTest {
    @Test fun aSocketThatConnectsAfterTheCancelIsStillClosed() = runTest {
        TestHttpsServer.withTrustAll {
            TestHttpsServer().use { target ->
                val targetPort = target.uri("/ok").port
                val clientClosed = CountDownLatch(1)
                val front = ServerSocket()
                front.bind(InetSocketAddress("127.0.0.1", 0))
                val splicer = thread(isDaemon = true) {
                    val client = front.accept()
                    Thread.sleep(DELAY_MS)
                    val upstream = Socket("127.0.0.1", targetPort)
                    val toClient = thread(isDaemon = true) {
                        try { upstream.getInputStream().copyTo(client.getOutputStream()) } catch (_: Exception) {}
                    }
                    try {
                        // client -> upstream until the client closes its socket
                        client.getInputStream().copyTo(upstream.getOutputStream())
                        clientClosed.countDown()
                    } catch (_: Exception) {
                        clientClosed.countDown() // a reset is a close too
                    } finally {
                        try { upstream.close() } catch (_: Exception) {}
                        toClient.join(1_000)
                        try { client.close() } catch (_: Exception) {}
                    }
                }
                try {
                    val uri = KUri("https://127.0.0.1:${front.localPort}/ok")
                    var failure: Throwable? = null
                    withContext(Dispatchers.Default) {
                        val body = UrlConnectionHttpTransport().get(uri, 15_000)
                        val reader = launch {
                            try { body.status() } catch (e: Throwable) { failure = e; throw e }
                        }
                        delay(500) // the handshake is pending: nothing answers for DELAY_MS
                        withTimeout(1_000) { reader.cancelAndJoin() } // the caller never waits for the connect
                        body.close()
                    }
                    assertTrue(failure is CancellationException, "got $failure")
                    assertTrue(
                        clientClosed.await(8, TimeUnit.SECONDS),
                        "the socket that connected after the cancel was never closed",
                    )
                } finally {
                    front.close()
                    splicer.join(2_000)
                }
            }
        }
    }

    private companion object {
        const val DELAY_MS = 2_000L
    }
}
