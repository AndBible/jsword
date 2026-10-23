package org.crosswire.jsword.book.install.sword

import kotlinx.coroutines.runBlocking
import org.crosswire.common.util.HttpBody
import org.crosswire.common.util.HttpHead
import org.crosswire.common.util.HttpTransport
import org.crosswire.common.util.KUri
import org.crosswire.common.util.WebResource
import org.crosswire.jsword.book.Book
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** An https repository on a non-443 port keeps its port in catalog and package URIs. */
class HttpsSwordInstallerPortTest {
    private val savedTransport = WebResource.transport

    @AfterTest fun restore() { WebResource.transport = savedTransport }

    private fun book(initials: String): Book =
        Proxy.newProxyInstance(Book::class.java.classLoader, arrayOf(Book::class.java)) { _, m, _ ->
            when (m.name) {
                "getInitials" -> initials
                "toString" -> initials
                else -> error("Unexpected call: ${m.name}")
            }
        } as Book

    private fun installer(host: String) = HttpsSwordInstaller().apply {
        setHost(host)
        setPackageDirectory("/packages")
        setCatalogDirectory("/")
    }

    /** Records the URI of the catalog HEAD request. */
    private fun catalogUri(i: HttpsSwordInstaller): String {
        var seen: KUri? = null
        WebResource.transport = object : HttpTransport {
            override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead { seen = uri; return HttpHead(200, null, 0, 1L) }
            override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody = error("unexpected get")
        }
        runBlocking { i.indexLastUpdated() }
        return seen.toString()
    }

    @Test fun portKeptInCatalogAndPackageUris() {
        val i = installer("10.0.2.2:8443")
        assertEquals("https://10.0.2.2:8443/mods.d.tar.gz", catalogUri(i))
        assertEquals("https://10.0.2.2:8443/packages/KJV.zip", i.toRemoteURI(book("KJV")).toString())
    }

    @Test fun defaultPortUrisUnchanged() {
        val i = installer("example.org")
        assertEquals("https://example.org/mods.d.tar.gz", catalogUri(i))
        assertEquals("https://example.org/packages/KJV.zip", i.toRemoteURI(book("KJV")).toString())
    }
}
