package org.crosswire.common.compress

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import kotlin.test.*

/** Pins framing and validation before the Native streaming implementation is introduced. */
class GzipBoundaryCharacterizationTest {
    private fun gzip(body: ByteArray): ByteArray = ByteArrayOutputStream().also { out ->
        GzipCompressorOutputStream(out).use { it.write(body) }
    }.toByteArray()

    @Test fun optionalHeaderFieldsIncludingUncheckedHeaderCrcRemainAccepted() {
        val body = "first member".encodeToByteArray()
        val plain = gzip(body)
        val header = plain.copyOf(10).apply { this[3] = 0x1e }
        val optional = byteArrayOf(3, 0, 1, 2, 3) + "name\u0000comment\u0000".encodeToByteArray() + byteArrayOf(0, 0)
        val bytes = header + optional + plain.copyOfRange(10, plain.size)
        assertContentEquals(body, CompressorType.GZIP.getCompressor(bytes).uncompress(0))
        assertContentEquals(body, CompressorType.GZIP.getCompressor(bytes + plain + byteArrayOf(7)).uncompress(1))
        for (length in 10 until 10 + optional.size) {
            val failure = runCatching { CompressorType.GZIP.getCompressor(bytes.copyOf(length)).uncompress(1) }.exceptionOrNull()
            assertTrue(failure?.javaClass == java.io.EOFException::class.java, "optional truncation $length")
        }
    }

    @Test fun crcFailurePrecedesMissingSizeAndNegativeCapacityPrecedesMalformedHeader() {
        val plain = gzip("body".encodeToByteArray())
        val crc = plain.copyOf(plain.size - 4).apply { this[size - 4] = (this[size - 4].toInt() xor 1).toByte() }
        val failure = assertFailsWith<java.io.IOException> { CompressorType.GZIP.getCompressor(crc).uncompress(1) }
        assertEquals(java.io.IOException::class.java, failure.javaClass)
        assertEquals("Gzip-compressed data is corrupt (CRC32 error)", failure.message)
        assertFailsWith<IllegalArgumentException> { CompressorType.GZIP.getCompressor(byteArrayOf()).uncompress(-1) }
        val size = plain.copyOf().apply { this[this.size - 4] = (this[this.size - 4].toInt() xor 1).toByte() }
        assertEquals("Gzip-compressed data is corrupt(uncompressed size mismatch)",
            assertFailsWith<java.io.IOException> { CompressorType.GZIP.getCompressor(size).uncompress(1) }.message)
    }

    @Test fun requestedBoundaryDoesNotDecodeOrValidateUnreadSuffix() {
        val body = ByteArray(20000) { (it % 251).toByte() }
        val corrupt = gzip(body).apply { this[size - 8] = (this[size - 8].toInt() xor 1).toByte() }
        GzipCompressorInputStream(ByteArrayInputStream(corrupt)).use { input ->
            val first = ByteArray(10240)
            assertEquals(first.size, input.read(first))
            assertContentEquals(body.copyOf(first.size), first)
            assertFailsWith<java.io.IOException> { input.readBytes() }
        }
    }

    @Test fun actualArchiveSourceReturnsOnlyRequestedBytesAndValidatesAtMemberEnd() {
        val root = java.nio.file.Files.createTempDirectory("gzip-boundary").toFile()
        try {
            val body = ByteArray(20000) { (it % 251).toByte() }
            val bytes = gzip(body).apply { this[size - 8] = (this[size - 8].toInt() xor 1).toByte() }
            val file = java.io.File(root, "in.gz").apply { writeBytes(bytes) }
            org.crosswire.jsword.book.install.archiveGzipSource(file.path).use { source ->
                val buffer = okio.Buffer()
                var remaining = 10240L
                while (remaining > 0) {
                    val count = source.read(buffer, remaining)
                    assertTrue(count in 1..remaining)
                    remaining -= count
                }
                assertContentEquals(body.copyOf(10240), buffer.readByteArray())
                assertFailsWith<java.io.IOException> { while (source.read(buffer, 10240) != -1L) buffer.clear() }
            }
        } finally { root.deleteRecursively() }
    }
}
