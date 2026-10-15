package org.crosswire.common.util

import org.crosswire.jsword.book.install.sword.HttpsSwordInstaller
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpsProbeTransportRoutingTest {
    private val events = mutableListOf<String>()
    private fun fakeBody(length: Int, modified: Long) = object : HttpBody {
        override val status: Int get() { events += "status"; return 200 }
        override val contentLength: Int get() { events += "length"; return length }
        override val lastModified: Long get() { events += "modified"; return modified }
        override fun prepareRead() { events += "prepare" }
        override fun read(buf: ByteArray): Int { events += "read"; return -1 }
        override fun close() { events += "close" }
    }

    private fun <T> withTransport(transport: HttpTransport, block: () -> T): T {
        val old = WebResource.transport
        val oldTimeout = WebResource.getTimeout()
        WebResource.transport = transport
        try { return block() } finally {
            WebResource.transport = old
            WebResource.setTimeout(oldTimeout)
        }
    }

    private fun recording(length: Int = 42, modified: Long = 1234L) = object : HttpTransport {
        override fun head(uri: KUri, timeoutMs: Int): HttpHead { events += "head"; return HttpHead(200, null, 0, 0) }
        override fun get(uri: KUri, timeoutMs: Int): HttpBody {
            events += "get $uri timeout=$timeoutMs"
            return fakeBody(length, modified)
        }
    }

    @Test fun httpsLastModifiedReadsHeadersThroughTheTransportAndCloses() = withTransport(recording()) {
        WebResource.setTimeout(8765)
        assertEquals(1234L, NetUtil.getLastModified(URI("https://example.org/a")))
        assertEquals(listOf("get https://example.org/a timeout=8765", "modified", "close"), events)
    }

    @Test fun httpsSizeReadsContentLengthThroughTheTransportAndCloses() = withTransport(recording()) {
        WebResource.setTimeout(8765)
        assertEquals(42, NetUtil.getSize(URI("https://example.org/a")))
        assertEquals(listOf("get https://example.org/a timeout=8765", "length", "close"), events)
    }

    @Test fun installerIndexLastUpdatedUsesTheCatalogUri() = withTransport(recording(modified = 777L)) {
        WebResource.setTimeout(8765)
        val installer = object : HttpsSwordInstaller() { init { host = "example.org"; catalogDirectory = "/cat" } }
        assertEquals(777L, installer.indexLastUpdated())
        assertEquals(listOf("get https://example.org/cat/mods.d.tar.gz timeout=8765", "modified", "close"), events)
    }

    @Test fun metadataFailureClosesBodyAndPropagatesFromNetUtil() = withTransport(object : HttpTransport {
        override fun head(uri: KUri, timeoutMs: Int): HttpHead = error("HEAD must not be used")
        override fun get(uri: KUri, timeoutMs: Int): HttpBody = object : HttpBody {
            override val status: Int get() = error("status must not be read")
            override val contentLength: Int get() = error("length must not be read")
            override val lastModified: Long get() { events += "modified"; throw IllegalStateException("metadata") }
            override fun prepareRead() = error("body must not be prepared")
            override fun read(buf: ByteArray): Int = error("body must not be read")
            override fun close() { events += "close" }
        }
    }) {
        assertFailsWith<IllegalStateException> { NetUtil.getLastModified(URI("https://example.org/a")) }
        assertEquals(listOf("modified", "close"), events)
    }

    @Test fun installerMetadataFailureClosesBodyAndReturnsZero() = withTransport(object : HttpTransport {
        override fun head(uri: KUri, timeoutMs: Int): HttpHead = error("HEAD must not be used")
        override fun get(uri: KUri, timeoutMs: Int): HttpBody = object : HttpBody {
            override val status: Int get() = error("status must not be read")
            override val contentLength: Int get() = error("length must not be read")
            override val lastModified: Long get() { events += "modified"; throw IllegalStateException("metadata") }
            override fun prepareRead() = error("body must not be prepared")
            override fun read(buf: ByteArray): Int = error("body must not be read")
            override fun close() { events += "close" }
        }
    }) {
        val installer = object : HttpsSwordInstaller() { init { host = "example.org"; catalogDirectory = "/cat" } }
        assertEquals(0L, installer.indexLastUpdated())
        assertEquals(listOf("modified", "close"), events)
    }

    @Test fun installerIndexLastUpdatedIsZeroOnTransportFailure() = withTransport(object : HttpTransport {
        override fun head(uri: KUri, timeoutMs: Int): HttpHead = throw okio.IOException("x")
        override fun get(uri: KUri, timeoutMs: Int): HttpBody = throw okio.IOException("down")
    }) {
        val installer = object : HttpsSwordInstaller() { init { host = "example.org"; catalogDirectory = "/cat" } }
        assertEquals(0L, installer.indexLastUpdated())
    }
}
