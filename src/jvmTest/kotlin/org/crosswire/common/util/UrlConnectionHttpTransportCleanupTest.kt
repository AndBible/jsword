package org.crosswire.common.util

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UrlConnectionHttpTransportCleanupTest {
    private class RecordingConnection(uri: URI, private val events: MutableList<String>) : HttpsURLConnection(uri.toURL()) {
        var metadataFailure: IOException? = null
        var closeFailure: IOException? = null
        var bodyPrepared = false
        var disconnected = false
        var connectTimeoutSeen = -1
        var readTimeoutSeen = -1

        override fun connect() { events += "connect" }
        override fun disconnect() { events += "disconnect"; disconnected = true }
        override fun usingProxy(): Boolean = false
        override fun getCipherSuite(): String? = null
        override fun getLocalCertificates(): Array<java.security.cert.Certificate>? = null
        override fun getServerCertificates(): Array<java.security.cert.Certificate>? = null
        override fun getPeerPrincipal(): java.security.Principal? = null
        override fun getLocalPrincipal(): java.security.Principal? = null
        override fun getLastModified(): Long {
            events += "modified"
            metadataFailure?.let { throw it }
            return 1234L
        }
        override fun getInputStream(): java.io.InputStream {
            events += "input"
            bodyPrepared = true
            return object : ByteArrayInputStream(byteArrayOf(1)) {
                override fun close() {
                    events += "stream-close"
                    closeFailure?.let { throw it }
                }
            }
        }
        override fun setConnectTimeout(timeout: Int) { connectTimeoutSeen = timeout; super.setConnectTimeout(timeout) }
        override fun setReadTimeout(timeout: Int) { readTimeoutSeen = timeout; super.setReadTimeout(timeout) }
    }

    private fun transport(connection: RecordingConnection) = UrlConnectionHttpTransport { uri ->
        assertEquals("https", uri.scheme)
        connection
    }

    @Test fun metadataOnlyCloseDisconnectsWithoutPreparingBody() = runTest {
        val events = mutableListOf<String>()
        val connection = RecordingConnection(URI("https://example.org/a"), events)
        val body = transport(connection).get(URI("https://example.org/a"), 4321)
        assertEquals(1234L, body.lastModified())
        body.close()
        assertEquals(listOf("modified", "disconnect"), events)
        assertFalse(connection.bodyPrepared)
        assertTrue(connection.disconnected)
    }

    @Test fun metadataFailureStillDisconnects() = runTest {
        val events = mutableListOf<String>()
        val connection = RecordingConnection(URI("https://example.org/a"), events).apply {
            metadataFailure = IOException("headers failed")
        }
        val body = transport(connection).get(URI("https://example.org/a"), 4321)
        try {
            assertFailsWith<IOException> { body.lastModified() }
        } finally {
            body.close()
        }
        assertEquals(listOf("modified", "disconnect"), events)
        assertTrue(connection.disconnected)
    }

    @Test fun streamCloseFailureStillDisconnects() = runTest {
        val events = mutableListOf<String>()
        val connection = RecordingConnection(URI("https://example.org/a"), events).apply {
            closeFailure = IOException("stream close failed")
        }
        val body = transport(connection).get(URI("https://example.org/a"), 4321)
        body.prepareRead()
        assertFailsWith<IOException> { body.close() }
        assertEquals(listOf("input", "stream-close", "disconnect"), events)
        assertTrue(connection.disconnected)
    }

    @Test fun configuredTimeoutIsAppliedToHttpsConnection() = runTest {
        val events = mutableListOf<String>()
        val connection = RecordingConnection(URI("https://example.org/a"), events)
        val body = transport(connection).get(URI("https://example.org/a"), 4321)
        assertEquals(4321, connection.connectTimeoutSeen)
        assertEquals(4321, connection.readTimeoutSeen)
        body.close()
    }
}
