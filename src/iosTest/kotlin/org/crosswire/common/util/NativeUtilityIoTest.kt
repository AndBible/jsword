@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*
import okio.IOException
import org.crosswire.jsword.book.sword.StorageLocation
import org.crosswire.jsword.book.sword.storageChild
import org.crosswire.jsword.book.sword.storageRead
import org.crosswire.jsword.book.sword.rawFileWrite

class NativeUtilityIoTest {
    private fun temporary(block: (StorageLocation) -> Unit) = memScoped {
        val template = (platform.Foundation.NSTemporaryDirectory() + "ksword-util-XXXXXX").cstr.getPointer(this)
        val root = StorageLocation(mkdtemp(template)!!.toKString())
        try { block(root) } finally {
            NativeLexicalFiles.list(root.spelling)?.forEach { unlink(NativeLexicalFiles.child(root.spelling, it)) }
            rmdir(root.spelling)
        }
    }

    @Test fun propertiesUseRealLiteralFilesAndTruncateBeforeNullFailure() = temporary { root ->
        val file = storageChild(root, "literal\\name space.properties")
        rawFileWrite(file, "escaped\\ key=caf\\u00e9\n".encodeToByteArray())
        val uri = KUri("file", null, null, -1, file.spelling, null, null)
        assertEquals("caf\u00e9", netLoadProperties(uri)["escaped key"])
        assertTrue(netCanRead(uri)); assertTrue(netCanWrite(uri))
        assertFailsWith<NullPointerException> { netStoreProperties(null, uri, null) }
        assertContentEquals(byteArrayOf(), storageRead(file))
        val properties = PropertyMap().apply { put("key", "value\n\u00e4") }
        netStoreProperties(properties, uri, "comment")
        assertEquals(properties.toMap(), netLoadProperties(uri).toMap())
        assertFailsWith<IOException> {
            netStoreProperties(null, KUri("file", null, null, -1, root.spelling + "/absent/child", null, null), null)
        }
    }

    @Test fun syntaxClassifierOnlyRecognizesActualUriSyntaxFailures() {
        val syntax = assertFails { KUri("http://[invalid]/") }
        assertTrue(netUriSyntaxFailure(syntax))
        assertFalse(netUriSyntaxFailure(IllegalArgumentException("unrelated")))
        assertFalse(netUriSyntaxFailure(IllegalStateException("unrelated")))
        assertEquals(null, NetMalformedURLException().message)
        assertEquals("invalid", NetMalformedURLException("invalid").message)
    }

    @Test fun remoteInputIsSuspendOnlyAndRemoteMetadataUsesHead() = kotlinx.coroutines.test.runTest {
        val previous = WebResource.transport
        val requests = mutableListOf<String>()
        WebResource.transport = object : HttpTransport {
            override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead {
                requests.add("HEAD")
                return HttpHead(200, "OK", 19, 1234L)
            }
            override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody {
                requests.add("GET")
                throw IllegalStateException("remote bodies are read through WebResource, never as a blocking Source")
            }
        }
        try {
            val uri = KUri("https://example.org/properties")
            // K5b D4: a blocking Source over a suspend body does not exist; these fail without a request.
            assertFailsWith<IOException> { netLoadProperties(uri) }
            assertFailsWith<IOException> { netCheckResource(uri) }
            assertFailsWith<IOException> { netListByIndexFile(uri, null) }
            assertEquals(19, netHttpsSize(uri)); assertEquals(1234L, netHttpsLastModified(uri))
            assertEquals(listOf("HEAD", "HEAD"), requests)
            assertFailsWith<IOException> { netStoreProperties(PropertyMap(), uri, null) }
            assertEquals(2, requests.size, "output must not silently use a GET transport")
        } finally { WebResource.transport = previous }
    }

    @Test fun missingHostAndOtherProtocolsFailWithoutFilesystemFallback() {
        val previous = WebResource.transport
        WebResource.transport = null
        try {
            for (uri in listOf(KUri("https://example.org/file"), KUri("ftp://example.org/file"))) {
                assertFailsWith<IOException> { netLoadProperties(uri) }
                assertFailsWith<IOException> { netCheckResource(uri) }
                assertFailsWith<IOException> { netStoreProperties(null, uri, null) }
            }
        } finally { WebResource.transport = previous }
    }
}
