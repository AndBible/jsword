package org.crosswire.jsword.book.install

import java.io.File
import java.nio.file.Files
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import kotlin.test.*

class FailedCacheEvidenceTest {
    @Test fun walksFull32BitCompressedLengthBeforeExactOrphanHeader() = temp { root ->
        val random = java.util.Random(42)
        val large = ByteArray(100000).also { random.nextBytes(it) }
        val file = File(root, "large.zip")
        val writer = ZipArchiveOutputStream(file)
        writer.putArchiveEntry(ZipArchiveEntry("first.conf"))
        writer.write(large)
        writer.closeArchiveEntry()
        writer.putArchiveEntry(ZipArchiveEntry("later.conf"))
        assertFailsWith<java.io.IOException> { writer.close() }
        val bytes = file.readBytes()
        val compressedSize = (0..3).fold(0L) { value, i -> value or ((bytes[18 + i].toLong() and 255) shl (8 * i)) }
        assertTrue(compressedSize > 65535, "Fixture must exercise the high compressed-size bits")
        val orphan = File(root, "orphan.zip")
        val orphanWriter = ZipArchiveOutputStream(orphan)
        orphanWriter.putArchiveEntry(ZipArchiveEntry("later.conf"))
        assertFailsWith<java.io.IOException> { orphanWriter.close() }
        val expected = orphan.readBytes().apply { repeat(4) { this[10 + it] = 0 } }
        assertContentEquals(expected, FailedCacheEvidence.orphanHeader(bytes))
        val observation = FailedCacheEvidence.observe(file)
        assertEquals("first.conf", observation.entries.first().first)
        assertEquals(large.toList(), observation.entries.first().second)
        assertEquals("later.conf", observation.entries.last().first)
        assertEquals(java.io.IOException::class.java, observation.failureClass)
    }

    @Test fun readerRetainsPartialBodyAndAssertionRejectsSameFailureWithDifferentPrefix() = temp { root ->
        fun malformed(file: File, first: Byte) {
            val payload = byteArrayOf(0, 0x88.toByte(), 0x13, 0x77, 0xec.toByte()) + ByteArray(5000) { 65 } + byteArrayOf(7)
            val buffer = okio.Buffer()
            org.crosswire.common.util.StoredZipWriter.write(buffer, listOf("first.conf" to byteArrayOf(first), "later.conf" to payload))
            val bytes = buffer.readByteArray()
            val local = 30 + "first.conf".length + 1
            bytes[local + 8] = 8
            repeat(4) { bytes[local + 22 + it] = (5000 ushr (8 * it)).toByte() }
            file.writeBytes(bytes)
        }
        val reference = File(root, "reference.zip").also { malformed(it, 1) }
        val different = File(root, "different.zip").also { malformed(it, 2) }
        val observed = FailedCacheEvidence.observe(reference)
        assertEquals("first.conf" to listOf<Byte>(1), observed.entries.first())
        assertEquals("later.conf", observed.entries.last().first)
        assertTrue(observed.entries.last().second.size > 4096, "Partial failed entry must remain visible")
        assertNotNull(observed.failureClass)
        assertEquals(observed.failureClass, FailedCacheEvidence.observe(different).failureClass)
        assertFailsWith<AssertionError> { FailedCacheEvidence.assertMatches(reference, different, "different prefix") }
    }

    @Test fun orphanComparisonRejectsNonTimeHeaderDrift() = temp { root ->
        val reference = File(root, "reference.zip")
        val writer = ZipArchiveOutputStream(reference)
        writer.putArchiveEntry(ZipArchiveEntry("later.conf"))
        assertFailsWith<java.io.IOException> { writer.close() }
        val changed = File(root, "changed.zip").apply {
            writeBytes(reference.readBytes().apply { this[4] = (this[4].toInt() xor 1).toByte() })
        }
        assertEquals(FailedCacheEvidence.observe(reference), FailedCacheEvidence.observe(changed))
        assertFailsWith<AssertionError> { FailedCacheEvidence.assertMatches(reference, changed, "version drift") }
    }

    private fun temp(block: (File) -> Unit) {
        val root = Files.createTempDirectory("cache-evidence").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }
}
