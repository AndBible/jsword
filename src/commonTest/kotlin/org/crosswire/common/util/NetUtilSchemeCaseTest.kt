package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/** Scheme matching is case-insensitive for both network schemes, so no mixed-case URL reaches the local-file helpers. */
class NetUtilSchemeCaseTest {
    private class Recording : HttpTransport {
        val requests = mutableListOf<String>()
        override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead {
            requests += "HEAD $uri"
            return HttpHead(200, "OK", 21, 4321L)
        }
        override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody {
            requests += "GET $uri"
            throw okio.IOException("metadata only through HEAD in this fake")
        }
    }

    @Test fun mixedCaseHttpGoesThroughTheTransportHead() = runTest {
        val saved = WebResource.transport
        val transport = Recording()
        WebResource.transport = transport
        try {
            for (scheme in listOf("http", "HTTP", "Http")) {
                transport.requests.clear()
                val uri = KUri("$scheme://example.org/a")
                assertEquals(21, NetUtil.getSize(uri), scheme)
                assertEquals(4321L, NetUtil.getLastModified(uri), scheme)
                assertEquals(2, transport.requests.size, scheme)
                assertEquals(true, transport.requests.all { it.startsWith("HEAD ") }, scheme)
            }
        } finally { WebResource.transport = saved }
    }

    /** Every request fails with an IOException, which NetUtil turns into 0; the JVM probes with GET, iOS with HEAD. */
    private class Failing : HttpTransport {
        val requests = mutableListOf<String>()
        override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead { requests += "HEAD $uri"; throw okio.IOException("down") }
        override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody { requests += "GET $uri"; throw okio.IOException("down") }
    }

    @Test fun mixedCaseHttpsNeverFallsIntoTheLocalHelpers() = runTest {
        val saved = WebResource.transport
        val transport = Failing()
        WebResource.transport = transport
        try {
            for (scheme in listOf("HTTPS", "Https")) {
                transport.requests.clear()
                assertEquals(0, NetUtil.getSize(KUri("$scheme://example.org/a")), scheme)
                assertEquals(1, transport.requests.size, scheme)
            }
        } finally { WebResource.transport = saved }
    }
}
