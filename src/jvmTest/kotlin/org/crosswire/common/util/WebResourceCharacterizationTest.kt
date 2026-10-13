package org.crosswire.common.util

import org.crosswire.common.progress.JobManager
import org.crosswire.jsword.book.install.DownloadCancelledException
import org.crosswire.jsword.book.install.DownloadException
import org.junit.Test
import java.net.URI
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WebResourceCharacterizationTest {
    private fun fixture(block: (TestHttpsServer, java.io.File) -> Unit) = TestHttpsServer.withTrustAll {
        val timeout = WebResource.getTimeout()
        val directory = Files.createTempDirectory("k4-download").toFile()
        try {
            TestHttpsServer().use { server -> block(server, directory) }
        } finally {
            WebResource.setTimeout(timeout)
            directory.deleteRecursively()
        }
    }

    @Test fun installExceptionKeepsJvmSerializedDescriptor() {
        val descriptor = java.io.ObjectStreamClass.lookup(org.crosswire.jsword.book.install.InstallException::class.java)
        println("InstallException descriptor UID=${descriptor.serialVersionUID}, fields=${descriptor.fields.toList()}")
        assertEquals(3258132440416335669L, descriptor.serialVersionUID)
        assertEquals(emptyList(), descriptor.fields.toList())
    }

    @Test fun copyWritesTheBodyAndSetsTotalWork() = fixture { server, directory ->
        val job = JobManager.createJob("k4")
        try {
            val dest = directory.resolve("body")
            WebResource(server.uri("/ok")).copy(dest.toURI(), job)
            assertEquals("k4-body", dest.readText())
            assertEquals(7, job.totalWork)
        } finally { job.done() }
    }

    @Test fun missingLengthDoesNotFallBackToHead() = fixture { server, directory ->
        val job = JobManager.createJob("k4")
        try {
            WebResource(server.uri("/chunked")).copy(directory.resolve("body").toURI(), job)
            assertEquals(-1, job.totalWork)
            assertEquals(0, server.heads.get())
        } finally { job.done() }
    }

    @Test fun notFoundIsDownloadExceptionWithStatus() = fixture { server, directory ->
        val error = assertFailsWith<DownloadException> {
            WebResource(server.uri("/missing")).copy(directory.resolve("body").toURI(), null)
        }
        assertEquals("Downloading ${server.uri("/missing")} failed with status code 404", error.message)
        assertTrue(!directory.resolve("body").exists())
    }

    @Test fun companionTimeoutNotConstructorControlsStalledRead() = fixture { server, directory ->
        WebResource.setTimeout(200)
        assertFailsWith<DownloadCancelledException> {
            WebResource(server.uri("/stall"), 10000).copy(directory.resolve("body").toURI(), null)
        }
    }

    @Test fun interruptDuringBlockedBodyEventuallyMapsTimeoutToCancellation() = fixture { server, directory ->
        WebResource.setTimeout(400)
        val failure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
        val worker = Thread {
            try {
                WebResource(server.uri("/stall")).copy(directory.resolve("body").toURI())
            } catch (error: Throwable) { failure.set(error) }
        }
        try {
            worker.start()
            assertTrue(server.stalled.await(2, java.util.concurrent.TimeUnit.SECONDS))
            worker.interrupt()
            worker.join(2000)
            assertTrue(!worker.isAlive, "blocked worker did not finish")
            assertTrue(failure.get() is DownloadCancelledException, failure.get().toString())
            assertEquals("k4-", directory.resolve("body").readText())
        } finally {
            worker.interrupt()
            worker.join(2000)
            check(!worker.isAlive)
        }
    }

    @Test fun refusedConnectionIsUnableToFind() = fixture { _, directory ->
        val error = assertFailsWith<LucidException> {
            WebResource(URI("https://127.0.0.1:1/x")).copy(directory.resolve("body").toURI(), null)
        }
        assertEquals("Unable to find: https://127.0.0.1:1/x", error.message)
    }

    @Test fun headMetadataAndRedirect() = fixture { server, directory ->
        assertEquals(7, WebResource(server.uri("/ok")).size)
        assertEquals(784887151000L, WebResource(server.uri("/lm")).lastModified)
        assertEquals(0, WebResource(server.uri("/missing")).size)
        val dest = directory.resolve("body")
        WebResource(server.uri("/redirect")).copy(dest.toURI())
        assertEquals("k4-body", dest.readText())
    }

    @Test fun constructorProxyIsIgnored() = fixture { server, directory ->
        val dest = directory.resolve("body")
        WebResource(server.uri("/ok"), "127.0.0.1", 1, 10000L).copy(dest.toURI())
        assertEquals("k4-body", dest.readText())
    }

    @Test fun defaultProxySelectorStillControlsConnection() = fixture { server, directory ->
        val previous = java.net.ProxySelector.getDefault()
        val selected = java.util.concurrent.atomic.AtomicInteger()
        try {
            java.net.ProxySelector.setDefault(object : java.net.ProxySelector() {
                override fun select(uri: URI): List<java.net.Proxy> {
                    selected.incrementAndGet()
                    return listOf(java.net.Proxy.NO_PROXY)
                }
                override fun connectFailed(uri: URI, address: java.net.SocketAddress, error: java.io.IOException) {
                    throw AssertionError("unexpected proxy failure", error)
                }
            })
            WebResource(server.uri("/ok"), "ignored", 1, 1L).copy(directory.resolve("body").toURI())
            assertEquals("k4-body", directory.resolve("body").readText())
            assertTrue(selected.get() > 0)
        } finally { java.net.ProxySelector.setDefault(previous) }
    }

    @Test fun dotComponentsRemainLexicalAndMissingParentFails() = fixture { server, directory ->
        directory.resolve("existing").mkdir()
        val dest = directory.resolve("existing/../body")
        WebResource(server.uri("/ok")).copy(dest.toURI())
        assertEquals("k4-body", directory.resolve("body").readText())
        assertFailsWith<DownloadException> {
            WebResource(server.uri("/ok")).copy(directory.resolve("missing/../other").toURI())
        }
        assertTrue(!directory.resolve("other").exists())
    }

    @Test fun metadataFailureStillUpdatesMeterBeforeStatusFailure() = fixture { server, directory ->
        val job = JobManager.createJob("k4")
        try {
            job.totalWork = 123
            assertFailsWith<LucidException> {
                WebResource(server.uri("/drop")).copy(directory.resolve("body").toURI(), job)
            }
            assertEquals(-1, job.totalWork)
        } finally { job.done() }
    }

    @Test fun plainHttpRetainsClassCastFailure() = fixture { _, directory ->
        assertFailsWith<ClassCastException> {
            WebResource(URI("http://127.0.0.1:1/x")).copy(directory.resolve("body").toURI())
        }
    }

    @Test fun proposedPathSinkCannotOpenLegacyBackslashFilename() = fixture { _, directory ->
        val dest = directory.resolve("a\\b")
        val projection = NetUtil.toPath(dest.toURI())
        assertEquals(directory.resolve("a/b").path, projection.toString())
        assertFailsWith<java.io.IOException> { okio.FileSystem.SYSTEM.sink(projection).close() }
        NetUtil.getOutputStream(dest.toURI()).use { it.write("legacy".toByteArray()) }
        assertEquals("legacy", dest.readText())
    }

    @Test fun lexicalBackslashOutputNameIsNotPathProjection() = fixture { server, directory ->
        val dest = directory.resolve("a\\b")
        WebResource(server.uri("/ok")).copy(dest.toURI())
        assertEquals("k4-body", dest.readText())
        assertTrue(!directory.resolve("a/b").exists())
    }
}
