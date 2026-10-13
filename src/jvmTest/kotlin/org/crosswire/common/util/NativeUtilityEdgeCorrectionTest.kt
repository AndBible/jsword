package org.crosswire.common.util

import java.io.File
import java.util.Properties
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class NativeUtilityEdgeCorrectionTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun emptyContinuationThenCommentMatchesIndependentProperties() {
        assertEquals("17", System.getProperty("java.specification.version"))
        for (text in listOf("\\\n#comment\nnext=value\n", "\\\r\n !comment\nnext=value\n", "\\", "\\\n", "\\\n\n", "\\\n   ", "x=one\\\n#two")) {
            val bytes = text.toByteArray(Charsets.ISO_8859_1)
            val reference = Properties().apply { load(bytes.inputStream()) }
            assertEquals(reference.entries.associate { it.key as String to it.value as String },
                LegacyPropertyText.entries(bytes).toMap(), "input=${text.replace("\n", "<LF>")}")
        }
    }

    @Test fun danglingBackslashOnlyAtEofMatchesIndependentProperties() {
        assertEquals("17", System.getProperty("java.specification.version"))
        val bytes = "\\".encodeToByteArray()
        val reference = Properties().apply { load(bytes.inputStream()) }
        assertEquals(reference.entries.associate { it.key as String to it.value as String }, LegacyPropertyText.entries(bytes).toMap())
    }

    @Test fun missingFileUrlConnectionSizeMatchesDirectJdkReference() {
        val uri = File(tmp.root, "missing").toURI()
        assertEquals(uri.toURL().openConnection().contentLength, netUrlSize(uri))
    }

    @Test fun temporaryPrefixTrailingSlashAndLongNamesFollowDirectFileAuthority() {
        for (prefix in listOf("abc/", "x".repeat(300))) {
            val reference = runCatching { File.createTempFile(prefix, null) }
            val actual = runCatching { netTemporaryUri(prefix, null) }
            try {
                if (reference.isSuccess) {
                    assertTrue(actual.isSuccess, actual.exceptionOrNull().toString())
                    val expectedName = reference.getOrThrow().name
                    val actualName = File(actual.getOrThrow()).name
                    assertTrue(expectedName.endsWith(".tmp"))
                    assertTrue(actualName.endsWith(".tmp"))
                    for (name in listOf(expectedName, actualName)) {
                        if (prefix.startsWith('x')) {
                            val retained = name.takeWhile { it == 'x' }.length
                            val digits = name.substring(retained).removeSuffix(".tmp")
                            assertEquals(255, name.length)
                            assertTrue(retained >= 3)
                            assertTrue(digits.length in 1..20 && digits.all { it in '0'..'9' })
                            assertNotNull(digits.toULongOrNull())
                            assertEquals(255 - digits.length - 4, retained)
                        } else {
                            assertTrue(name.startsWith("abc"))
                            assertNotNull(name.removePrefix("abc").removeSuffix(".tmp").toULongOrNull())
                        }
                    }
                } else {
                    assertEquals(reference.exceptionOrNull()!!::class, actual.exceptionOrNull()!!::class)
                }
            } finally {
                reference.getOrNull()?.delete()
                actual.getOrNull()?.let { File(it).delete() }
            }
        }
    }
}
