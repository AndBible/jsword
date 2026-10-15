package org.crosswire.common.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.crosswire.common.progress.JobManager
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Cancelling the coroutine that runs WebResource.copy unblocks a host read, closes the body and propagates. */
class WebResourceCoroutineCancellationTest {
    @Test fun coroutineCancellationUnblocksHostReadAndClosesBody() = runBlocking {
        val old = WebResource.transport
        val directory = Files.createTempDirectory("k5b-cancel").toFile()
        val entered = CompletableDeferred<Unit>()
        var closed = false
        WebResource.transport = object : HttpTransport {
            override suspend fun head(uri: KUri, timeoutMs: Int) = HttpHead(200, null, 4, 0)
            override suspend fun get(uri: KUri, timeoutMs: Int) = object : HttpBody {
                override suspend fun contentLength() = 4
                override suspend fun status() = 200
                override suspend fun prepareRead() = Unit
                override suspend fun read(buf: ByteArray): Int {
                    entered.complete(Unit)
                    awaitCancellation()
                }
                override fun close() { closed = true }
            }
        }
        val meter = JobManager.createJob("blocked-host", "blocked-host") { }
        var failure: Throwable? = null
        try {
            val worker = launch(Dispatchers.IO) {
                try { WebResource(KUri("https://fake/body")).copy(directory.resolve("body").toURI(), meter) }
                catch (ex: Throwable) { failure = ex; throw ex }
            }
            withTimeout(2000) { entered.await() }
            worker.cancelAndJoin()
            assertTrue(failure is CancellationException, "got $failure")
            assertEquals(true, closed)
        } finally {
            WebResource.transport = old
            directory.deleteRecursively()
            meter.done()
        }
    }
}
