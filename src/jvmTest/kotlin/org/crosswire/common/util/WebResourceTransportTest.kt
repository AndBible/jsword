package org.crosswire.common.util

import org.crosswire.common.progress.JobManager
import org.crosswire.jsword.book.install.DownloadCancelledException
import org.crosswire.jsword.book.install.DownloadException
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** JVM temporarily: WebResource stays JVM until Task 16. */
class WebResourceTransportTest {
    private fun fixture(block: (java.io.File, MutableList<String>, HttpBody) -> Unit) {
        val old = WebResource.transport
        val directory = Files.createTempDirectory("k4-fake").toFile()
        val events = mutableListOf<String>()
        val body = object : HttpBody {
            override val contentLength: Int get() { events.add("length"); return 0 }
            override val status: Int get() { events.add("status"); return 200 }
            override fun prepareRead() { events.add("prepare") }
            var read = false
            override fun read(buf: ByteArray): Int {
                events.add("read")
                if (read) return -1
                read = true
                "body".toByteArray().copyInto(buf)
                return 4
            }
            override fun close() { events.add("close") }
        }
        try {
            WebResource.transport = object : HttpTransport {
                override fun head(uri: KUri, timeoutMs: Int): HttpHead { events.add("head"); return HttpHead(200, "OK", 4, 123) }
                override fun get(uri: KUri, timeoutMs: Int): HttpBody { events.add("get"); return body }
            }
            block(directory, events, body)
        } finally {
            WebResource.transport = old
            directory.deleteRecursively()
        }
    }

    @Test fun injectedTransportUsesLegacyOrderAndHeadForZeroOnly() = fixture { directory, events, _ ->
        val job = JobManager.createJob("k4")
        try {
            WebResource(KUri("https://fake/body")).copy(directory.resolve("body").toURI(), job)
            assertEquals("body", directory.resolve("body").readText())
            assertEquals(4, job.totalWork)
            assertEquals(listOf("get", "length", "head", "status", "prepare", "read", "read", "close"), events)
        } finally { job.done() }
    }

    @Test fun preparationFailureDoesNotTruncateOutputAndClosesBody() = fixture { directory, events, body ->
        WebResource.transport = object : HttpTransport {
            override fun head(uri: KUri, timeoutMs: Int) = HttpHead(200, null, 4, 0)
            override fun get(uri: KUri, timeoutMs: Int): HttpBody = object : HttpBody by body {
                override fun prepareRead() { events.add("prepare-failure"); throw okio.IOException("input") }
            }
        }
        val dest = directory.resolve("body").apply { writeText("keep") }
        val error = assertFailsWith<DownloadException> { WebResource(KUri("https://fake/body")).copy(dest.toURI()) }
        assertEquals(200, error.statusCode)
        assertEquals("keep", dest.readText())
        assertEquals(listOf("status", "prepare-failure", "close"), events)
    }

    @Test fun cancellationAfterPartialReadClosesInputBeforeOutputFlush() = fixture { directory, events, body ->
        val dest = directory.resolve("body")
        WebResource.transport = object : HttpTransport {
            override fun head(uri: KUri, timeoutMs: Int) = HttpHead(200, null, 4, 0)
            override fun get(uri: KUri, timeoutMs: Int): HttpBody = object : HttpBody by body {
                var read = false
                override fun read(buf: ByteArray): Int {
                    if (read) throw TransportCancelledException()
                    read = true
                    "body".toByteArray().copyInto(buf)
                    return 4
                }
                override fun close() {
                    assertEquals(4, dest.length(), "legacy writes must reach output before the next read")
                    events.add("close")
                }
            }
        }
        assertFailsWith<DownloadCancelledException> { WebResource(KUri("https://fake/body")).copy(dest.toURI()) }
        assertEquals("body", dest.readText())
        assertTrue(events.contains("close"))
    }
}
