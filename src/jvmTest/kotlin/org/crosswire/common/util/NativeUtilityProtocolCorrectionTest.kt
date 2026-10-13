package org.crosswire.common.util

import java.io.File
import java.net.URI
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class NativeUtilityProtocolCorrectionTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun uppercaseFileResourceAndPropertyReadsUseUrlConversion() {
        val file = tmp.newFile("properties").apply { writeText("key=value\n") }
        val uri = URI("FILE:" + file.toURI().rawPath)
        assertEquals(uri.toURL().openConnection().contentLength, netUrlSize(uri))
        netCheckResource(uri)
        assertEquals("value", netLoadProperties(uri)["key"])
    }

    @Test fun relativeResourceCheckMatchesDirectUrlConversionFailure() {
        val uri = URI("relative.properties")
        assertEquals(runCatching { uri.toURL().openStream().close() }.exceptionOrNull()!!::class,
            runCatching { netCheckResource(uri) }.exceptionOrNull()!!::class)
    }

    @Test fun relativePropertyLoadMatchesDirectInputBoundaryFailure() {
        val uri = URI("relative.properties")
        assertEquals(runCatching { NetUtilJvm.getInputStream(uri) }.exceptionOrNull()!!::class,
            runCatching { netLoadProperties(uri) }.exceptionOrNull()!!::class)
    }

    @Test fun relativeSizeMatchesDirectUrlConnectionFailure() {
        val uri = URI("relative.properties")
        assertEquals(runCatching { uri.toURL().openConnection().contentLength }.exceptionOrNull()!!::class,
            runCatching { netUrlSize(uri) }.exceptionOrNull()!!::class)
    }

    @Test fun relativeModifiedMatchesDirectUrlConnectionFailure() {
        val uri = URI("relative.properties")
        assertEquals(runCatching { uri.toURL().openConnection().lastModified }.exceptionOrNull()!!::class,
            runCatching { netUrlLastModified(uri) }.exceptionOrNull()!!::class)
    }

    @Test fun temporaryNulAndPathSuffixFailBeforeCreatingInvalidFiles() {
        for ((prefix, suffix) in listOf("abc\u0000tail" to ".tmp", "abc" to "\u0000.tmp", "abc" to "/child")) {
            val reference = runCatching { File.createTempFile(prefix, suffix) }
            val actual = runCatching { netTemporaryUri(prefix, suffix) }
            try {
                assertTrue(reference.isFailure)
                assertEquals(reference.exceptionOrNull()!!::class, actual.exceptionOrNull()!!::class)
            } finally {
                reference.getOrNull()?.delete()
                actual.getOrNull()?.let { File(it).delete() }
            }
        }
    }
}
