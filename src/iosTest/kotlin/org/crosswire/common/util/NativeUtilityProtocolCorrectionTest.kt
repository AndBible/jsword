@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*
import okio.use
import okio.IOException

class NativeUtilityProtocolCorrectionTest {
    @Test fun uppercaseFileAndMissingMetadataFollowUrlResourceContract() = memScoped {
        val template = (platform.Foundation.NSTemporaryDirectory() + "ksword-protocol-XXXXXX").cstr.getPointer(this)
        val fd = mkstemp(template)
        assertTrue(fd >= 0); close(fd)
        val path = template.toKString()
        try {
            NativeLexicalFiles.sink(path).use { sink ->
                val buffer = okio.Buffer().writeUtf8("key=value\n")
                sink.write(buffer, buffer.size)
            }
            val uri = KUri("FILE", null, null, -1, path, null, null)
            assertEquals(10, netUrlSize(uri)); netCheckResource(uri)
            assertEquals("value", netLoadProperties(uri)["key"])
            assertEquals(0, netUrlSize(KUri("file", null, null, -1, "$path-missing", null, null)))
            assertEquals(0L, netUrlLastModified(KUri("file", null, null, -1, "$path-missing", null, null)))
            val remote = KUri("file://remote.invalid$path")
            assertEquals("value", netLoadProperties(remote)["key"], "Direct lowercase file properties ignore authority")
            assertFailsWith<IOException> { netCheckResource(remote) }
            assertFailsWith<IOException> { netLoadProperties(KUri("FILE://remote.invalid$path")) }
        } finally { unlink(path) }
        Unit
    }

    @Test fun relativeUrisRetainArgumentFailureAndUppercaseHttpsUsesHost() {
        val relative = KUri("relative.properties")
        assertFailsWith<IllegalArgumentException> { netCheckResource(relative) }
        assertFailsWith<NullPointerException> { netLoadProperties(relative) }
        assertFailsWith<IllegalArgumentException> { netUrlSize(relative) }
        assertFailsWith<IllegalArgumentException> { netUrlLastModified(relative) }
        val previous = WebResource.transport
        var requests = 0
        WebResource.transport = object : HttpTransport {
            override fun get(uri: KUri, timeoutMs: Int): HttpBody = error("No GET expected")
            override fun head(uri: KUri, timeoutMs: Int): HttpHead {
                requests++
                return HttpHead(200, "OK", 7, 1234L)
            }
        }
        try {
            val uri = KUri("HTTPS://example.org/resource")
            assertEquals(7, netUrlSize(uri)); assertEquals(1234L, netUrlLastModified(uri))
            assertEquals(2, requests)
        } finally { WebResource.transport = previous }
    }

    @Test fun temporaryNamesNormalizeTrailingPrefixAndRejectInvalidComponents() {
        val uri = netTemporaryUri("abc/", null)
        try { assertTrue(NativeLexicalFiles.name(uri.getPath()!!).startsWith("abc")) }
        finally { unlink(uri.getPath()!!) }
        for ((prefix, suffix) in listOf("abc\u0000tail" to ".tmp", "abc" to "\u0000.tmp", "abc" to "/child")) {
            assertFailsWith<IOException> { netTemporaryUri(prefix, suffix) }
        }
        val long = netTemporaryUri("x".repeat(300), null)
        try {
            val name = NativeLexicalFiles.name(long.getPath()!!)
            val retained = name.takeWhile { it == 'x' }.length
            val digits = name.substring(retained).removeSuffix(".tmp")
            assertEquals(255, name.length)
            assertTrue(retained >= 3)
            assertTrue(digits.length in 1..20 && digits.all { it in '0'..'9' })
            assertNotNull(digits.toULongOrNull())
            assertEquals(255 - digits.length - 4, retained)
        }
        finally { unlink(long.getPath()!!) }
    }
}
