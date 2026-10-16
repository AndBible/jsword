package org.crosswire.common.util

import org.crosswire.common.progress.JobManager
import org.crosswire.jsword.book.install.DownloadCancelledException
import org.crosswire.jsword.book.install.DownloadException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import okio.FileSystem
import okio.Path
import org.crosswire.ksword.fixtures.withTestDirectory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertSame

/** Portable transport ordering; the original JVM thread-lifetime test stays on the JVM. */
class WebResourceTransportTest {
    private fun fixture(block: suspend (Path, MutableList<String>, HttpBody) -> Unit) = runTest { withTestDirectory { directory ->
        val old = WebResource.transport
        val events = mutableListOf<String>()
        val body = object : HttpBody {
            override suspend fun contentLength(): Int { events.add("length"); return 0 }
            override suspend fun status(): Int { events.add("status"); return 200 }
            override suspend fun prepareRead() { events.add("prepare") }
            var read = false
            override suspend fun read(buf: ByteArray): Int {
                events.add("read")
                if (read) return -1
                read = true
                "body".encodeToByteArray().copyInto(buf)
                return 4
            }
            override fun close() { events.add("close") }
        }
        try {
            WebResource.transport = object : HttpTransport {
                override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead { events.add("head"); return HttpHead(200, "OK", 4, 123) }
                override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody { events.add("get"); return body }
            }
            block(directory, events, body)
        } finally {
            WebResource.transport = old
        }
    } }

    @Test fun injectedTransportUsesLegacyOrderAndHeadForZeroOnly() = fixture { directory, events, _ ->
        val job = JobManager.createJob("k4")
        try {
            WebResource(KUri("https://fake/body")).copy(pathUri(directory / "body"), job)
            assertEquals("body", FileSystem.SYSTEM.read(directory / "body") { readUtf8() })
            assertEquals(4, job.totalWork)
            assertEquals(listOf("get", "length", "head", "status", "prepare", "read", "read", "close"), events)
        } finally { job.done() }
    }

    @Test fun preparationFailureDoesNotTruncateOutputAndClosesBody() = fixture { directory, events, body ->
        val failure = okio.IOException("input")
        WebResource.transport = object : HttpTransport {
            override suspend fun head(uri: KUri, timeoutMs: Int) = HttpHead(200, null, 4, 0)
            override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody = object : HttpBody by body {
                override suspend fun prepareRead() { events.add("prepare-failure"); throw failure }
            }
        }
        val dest = directory / "body"
        FileSystem.SYSTEM.write(dest) { writeUtf8("keep") }
        val error = assertFailsWith<DownloadException> { WebResource(KUri("https://fake/body")).copy(pathUri(dest)) }
        assertEquals(200, error.statusCode)
        assertSame(failure, error.cause)
        assertEquals("keep", FileSystem.SYSTEM.read(dest) { readUtf8() })
        assertEquals(listOf("status", "prepare-failure", "close"), events)
    }

    @Test fun cancellationAfterPartialReadClosesInputBeforeOutputFlush() = fixture { directory, events, body ->
        val dest = directory / "body"
        WebResource.transport = object : HttpTransport {
            override suspend fun head(uri: KUri, timeoutMs: Int) = HttpHead(200, null, 4, 0)
            override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody = object : HttpBody by body {
                var read = false
                override suspend fun read(buf: ByteArray): Int {
                    if (read) throw TransportCancelledException()
                    read = true
                    "body".encodeToByteArray().copyInto(buf)
                    return 4
                }
                override fun close() {
                    assertEquals(4L, FileSystem.SYSTEM.metadata(dest).size, "legacy writes must reach output before the next read")
                    events.add("close")
                }
            }
        }
        assertFailsWith<DownloadCancelledException> { WebResource(KUri("https://fake/body")).copy(pathUri(dest)) }
        assertEquals("body", FileSystem.SYSTEM.read(dest) { readUtf8() })
        assertTrue(events.contains("close"))
    }
}
