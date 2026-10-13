@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.jsword.book.install

import kotlinx.cinterop.*
import okio.*
import org.crosswire.common.util.*
import platform.posix.*
import kotlin.test.*

class NativeArchiveContractTest {
    private fun temp(block: (String) -> Unit) = memScoped {
        val template = (platform.Foundation.NSTemporaryDirectory() + "ksword-archive-XXXXXX").cstr.getPointer(this)
        val root = mkdtemp(template)!!.toKString()
        try { block(root) } finally {
            fun remove(path: String) { NativeLexicalFiles.list(path)?.forEach { remove("$path/$it") }; unlink(path); rmdir(path) }
            remove(root)
        }
    }
    private fun write(path: String, bytes: ByteArray) = NativeLexicalFiles.sink(path).use { output ->
        val buffer = Buffer().write(bytes); output.write(buffer, buffer.size)
    }

    @Test fun visitorPrefixAdvanceFailureAndCallbackFailureCloseExactlyOnce() = temp { root ->
        for (suffix in listOf(200, 5000)) for (callbackFails in listOf(false, true)) {
            val size = 6 + suffix
            val payload = byteArrayOf(0, size.toByte(), (size ushr 8).toByte(), size.inv().toByte(), (size.inv() ushr 8).toByte()) +
                "prefix".encodeToByteArray() + ByteArray(suffix) { 32 } + byteArrayOf(7)
            val buffer = Buffer(); StoredZipWriter.write(buffer, listOf("first.conf" to payload))
            val bytes = buffer.readByteArray().apply { this[8] = 8; this[22] = 6; this[23] = 0 }
            val path = "$root/input.zip"; write(path, bytes)
            var closed = 0; var visited = false
            val callbackError = IllegalStateException("callback")
            val result = runCatching { SwordArchive.visitConfZipSpelling(path, { source ->
                object : ForwardingSource(source) { override fun close() { closed++; super.close() } }
            }) { _, body ->
                assertEquals("prefix", body.decodeToString()); visited = true
                if (callbackFails) throw callbackError
            } }
            assertTrue(visited); assertEquals(1, closed)
            when {
                callbackFails -> assertSame(callbackError, result.exceptionOrNull())
                suffix == 200 -> assertTrue(result.exceptionOrNull() is IOException)
                else -> assertTrue(result.isSuccess)
            }
        }
    }

    @Test fun extractionChecksContainmentAndOutputBeforeSourceAndClosesOnFailure() = temp { root ->
        NativeLexicalFiles.mkdirs("$root/dest"); NativeLexicalFiles.mkdirs("$root/outside")
        symlink("$root/outside", "$root/dest/link")
        var opened = false
        assertFailsWith<IOException> { archiveExtract("$root/dest", "link/escape", false) { opened = true; Buffer() } }
        assertFalse(opened); assertFalse(NativeLexicalFiles.exists("$root/outside/escape"))
        // JDK 17 File.getCanonicalFile resolves the link before a later "..", so both escape dest.
        // The review's claim that they land in dest/outside and dest/escaped was lexical collapse, not the JVM check.
        for (name in listOf("link/../outside", "link/sub/../../escaped")) {
            opened = false
            assertFailsWith<IOException>(name) { archiveExtract("$root/dest", name, false) { opened = true; Buffer().write(byteArrayOf(1)) } }
            assertFalse(opened, name)
        }
        assertTrue(NativeLexicalFiles.list("$root/outside").isNullOrEmpty())
        assertFalse(NativeLexicalFiles.exists("$root/escaped"))
        assertFalse(NativeLexicalFiles.exists("$root/dest/outside"))
        assertFalse(NativeLexicalFiles.exists("$root/dest/escaped"))
        var closed = 0
        val error = IOException("read")
        val source = object : Source {
            override fun timeout() = Timeout.NONE
            override fun read(sink: Buffer, byteCount: Long): Long = throw error
            override fun close() { closed++ }
        }
        assertSame(error, runCatching { archiveExtract("$root/dest", "literal\\case", false) { source } }.exceptionOrNull())
        assertEquals(1, closed); assertTrue(NativeLexicalFiles.exists("$root/dest/literal\\case"))
        NativeLexicalFiles.mkdirs("$root/dest/blocked")
        assertFailsWith<FileNotFoundException> { archiveExtract("$root/dest", "blocked", false) { opened = true; Buffer() } }
        assertFalse(opened)
        val closeError = IOException("source close")
        val failing = object : Source {
            override fun timeout() = Timeout.NONE
            override fun read(sink: Buffer, byteCount: Long): Long = throw error
            override fun close() { throw closeError }
        }
        val failure = runCatching { archiveExtract("$root/dest", "close-error", false) { failing } }.exceptionOrNull()
        assertSame(error, failure)
        assertTrue(failure!!.suppressedExceptions.contains(closeError))
    }

    @Test fun selectedUnsupportedDuplicateCarriesCentralAndLocalMetadataAndEncryptionPriority() = temp { root ->
        val buffer = Buffer(); StoredZipWriter.write(buffer, listOf("same" to byteArrayOf(1), "same" to byteArrayOf(2, 3)))
        val bytes = buffer.readByteArray()
        val central = (0..bytes.size - 4).filter { i -> bytes[i] == 80.toByte() && bytes[i + 1] == 75.toByte() && bytes[i + 2] == 1.toByte() && bytes[i + 3] == 2.toByte() }.last()
        bytes[central + 8] = 1; bytes[central + 10] = 99
        val path = "$root/input.zip"; write(path, bytes)
        val error = assertFailsWith<NativeUnsupportedZipFeatureException> { archiveUnsupportedZipFeature(path, 1) }
        assertEquals("encryption", error.feature)
        assertEquals(1, error.entry.centralIndex); assertEquals(2L, error.entry.size)
        assertEquals(99, error.entry.method); assertEquals(0, error.entry.localMethod)
        assertContentEquals("same".encodeToByteArray(), error.entry.rawName)
    }
}
