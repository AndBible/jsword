package org.crosswire.common.util

import okio.Buffer
import okio.buffer
import java.io.File
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import kotlin.test.*

/** The abort path must expose the real legacy orphan local header, not finalize a plausible empty cache. */
class StoredZipFailureContractTest {
    private fun call(writer: StoredZipWriter.Writer, name: String, vararg args: Any) {
        val method = writer.javaClass.methods.singleOrNull { it.name.startsWith(name + "$") && it.parameterCount == args.size }
            ?: throw AssertionError("Missing production Writer.$name")
        try { method.invoke(writer, *args) } catch (error: java.lang.reflect.InvocationTargetException) { throw error.cause!! }
    }

    @Test fun completedPendingEntryMatchesExistingStoredBytesExactly() {
        val expected = Buffer(); StoredZipWriter.write(expected, listOf("first" to byteArrayOf(1), "second" to byteArrayOf(2)))
        val output = Buffer(); val writer = StoredZipWriter.Writer(output)
        writer.entry("first", byteArrayOf(1))
        call(writer, "beginEntry", "second"); call(writer, "completeEntry", byteArrayOf(2)); writer.finish()
        assertContentEquals(expected.readByteArray(), output.readByteArray())
    }

    @Test fun abortedPendingEntryMatchesRealSeekableOrphanHeaderAndNeverAddsEndRecord() {
        val root = java.nio.file.Files.createTempDirectory("orphan-zip").toFile()
        try {
            val reference = File(root, "legacy.zip")
            val legacy = ZipArchiveOutputStream(reference)
            legacy.putArchiveEntry(ZipArchiveEntry("later.conf"))
            assertFailsWith<java.io.IOException> { legacy.close() }
            val actual = Buffer(); val writer = StoredZipWriter.Writer(actual)
            call(writer, "beginEntry", "later.conf"); call(writer, "abortEntry"); writer.finish()
            val want = reference.readBytes(); val got = actual.readByteArray()
            assertEquals(want.size, got.size)
            // The File writer stamps the current DOS time and nothing else. Zeroing any other
            // byte would hide a drifted version, flag, size or ZIP64 extra.
            repeat(4) { want[10 + it] = 0 }
            assertContentEquals(want, got)
            assertFalse((0..got.size - 4).any { i -> got[i] == 80.toByte() && got[i + 1] == 75.toByte() && got[i + 2] == 5.toByte() && got[i + 3] == 6.toByte() })
        } finally { root.deleteRecursively() }
    }

    @Test fun abortWriteFailurePropagatesAndDoesNotPublishAFalseEndRecord() {
        val output = object : okio.Sink {
            override fun timeout() = okio.Timeout.NONE
            override fun flush() = Unit
            override fun close() = Unit
            override fun write(source: Buffer, byteCount: Long) { throw java.io.IOException("disk full") }
        }
        val buffered = output.buffer()
        val writer = StoredZipWriter.Writer(buffered)
        call(writer, "beginEntry", "later.conf")
        assertEquals("disk full", assertFailsWith<java.io.IOException> { call(writer, "abortEntry") }.message)
        // A failed abort still leaves the writer unfinished; finish must not manufacture an EOCD.
        writer.finish()
    }
}
