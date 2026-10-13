package org.crosswire.common.util

import java.io.File
import java.net.URI
import java.net.URISyntaxException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeUtilityJvmCharacterizationTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun nullPropertyStoreTruncatesAfterOpeningButMissingParentWins() {
        val file = tmp.newFile("properties").apply { writeText("old") }
        assertFailsWith<NullPointerException> { netStoreProperties(null, file.toURI(), null) }
        assertEquals(0L, file.length())
        assertFailsWith<java.io.FileNotFoundException> {
            netStoreProperties(null, File(tmp.root, "missing/child").toURI(), null)
        }
    }

    @Test fun propertyLoadUsesDecodedLiteralFilePathAndPreservesEscapes() {
        val file = File(tmp.root, "literal\\name space.properties").apply {
            writeText("escaped\\ key=caf\\u00e9\n", Charsets.ISO_8859_1)
        }
        assertEquals(mapOf("escaped key" to "café"), netLoadProperties(file.toURI()).toMap())
        assertTrue(netCanRead(URI("http://example.org" + file.toURI().rawPath)))
        assertTrue(netCanWrite(file.toURI()))
        assertFailsWith<java.io.FileNotFoundException> { netLoadProperties(File(tmp.root, "absent").toURI()) }
    }

    @Test fun temporaryPrefixAndNullSuffixFollowFileContract() {
        assertFailsWith<IllegalArgumentException> { netTemporaryUri("ab", null) }
        val uri = netTemporaryUri("abc", null)
        try {
            assertTrue(File(uri).exists())
            assertTrue(File(uri).name.startsWith("abc"))
            assertTrue(File(uri).name.endsWith(".tmp"))
        } finally { File(uri).delete() }
    }

    @Test fun syntaxClassifierDoesNotSwallowUnrelatedArgumentFailure() {
        val syntax = assertFailsWith<URISyntaxException> { URI("http://[invalid]/") }
        assertTrue(netUriSyntaxFailure(syntax))
        assertFalse(netUriSyntaxFailure(IllegalArgumentException("unrelated")))
        assertFalse(netUriSyntaxFailure(IllegalStateException("unrelated")))
        assertEquals(null, NetMalformedURLException().message)
        assertEquals("invalid", NetMalformedURLException("invalid").message)
    }

    @Test fun indexFilterNullOnlyFailsWhenAnEntryIsVisited() {
        val file = tmp.newFile("index")
        assertEquals(emptyList(), netListByIndexFile(file.toURI(), null).toList())
        file.writeText("one\n")
        assertFailsWith<NullPointerException> { netListByIndexFile(file.toURI(), null) }
        val failure = IllegalStateException("filter failure")
        val filter = object : URIFilter { override fun accept(name: String?): Boolean = throw failure }
        assertEquals(failure, assertFailsWith<IllegalStateException> { netListByIndexFile(file.toURI(), filter) })
        file.writeText("two\n")
        assertEquals(listOf("two"), netListByIndexFile(file.toURI(), DefaultURIFilter()).toList())
    }
}
