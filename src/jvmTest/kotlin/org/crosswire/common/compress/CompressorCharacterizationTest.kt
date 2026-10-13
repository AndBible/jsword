package org.crosswire.common.compress

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CompressorCharacterizationTest {
    private fun compress(type: CompressorType, bytes: ByteArray) = type.getCompressor(bytes).compress()
    private fun expand(type: CompressorType, bytes: ByteArray, length: Int) = type.getCompressor(bytes).uncompress(length)
    private fun reference(type: CompressorType, bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val sink = when(type) {
            CompressorType.GZIP -> GzipCompressorOutputStream(out)
            CompressorType.BZIP2 -> BZip2CompressorOutputStream(out)
            CompressorType.XZ -> XZCompressorOutputStream(out)
            else -> error("not a platform reference")
        }
        sink.write(bytes); sink.flush(); sink.close()
        return out.toByteArray()
    }
    @Test fun gzipBytesMatchLegacyCorpus() {
        val random = java.util.Random(0x475a4950)
        val lengths = listOf(0, 1, 2, 2047, 2048, 2049, 8191, 8192, 8193, 20000) + List(100) { random.nextInt(20001) }
        for (length in lengths) {
            val bytes = ByteArray(length).also { random.nextBytes(it) }
            assertContentEquals(reference(CompressorType.GZIP, bytes), compress(CompressorType.GZIP, bytes), "gzip random length $length")
            val text = ByteArray(length) { "In the beginning"[it % 16].code.toByte() }
            assertContentEquals(reference(CompressorType.GZIP, text), compress(CompressorType.GZIP, text), "gzip repetitive length $length")
        }
    }
    @Test fun supportedBytesAndLengths() {
        for (type in listOf(CompressorType.ZIP, CompressorType.GZIP, CompressorType.BZIP2)) {
            for (bytes in listOf(byteArrayOf(), byteArrayOf(42), "In the beginning".repeat(300).encodeToByteArray())) {
                val expected = if (type == CompressorType.ZIP) Zlib.deflate(bytes) else reference(type, bytes)
                assertContentEquals(expected, compress(type, bytes), "$type compressed")
                // ZIP's zero-sized inflater buffer is rejected even for empty payloads.
                for (length in listOf(1, 17, bytes.size + 10)) assertContentEquals(bytes, expand(type, expected, length), "$type length $length")
                assertEquals(IllegalArgumentException::class, runCatching { expand(type, expected, -1) }.exceptionOrNull()!!::class)
                if (type != CompressorType.ZIP) assertContentEquals(bytes, expand(type, expected, 0))
            }
        }
    }
    @Test fun gzipFramingAndErrorsMatchFrozenReference() {
        val encoded = reference(CompressorType.GZIP, "hello".encodeToByteArray())
        val cases = listOf(byteArrayOf(), byteArrayOf(1, 2, 3), encoded, encoded + encoded, encoded + byteArrayOf(1, 2, 3)) +
            (0 until encoded.size).map { encoded.copyOf(it) } +
            listOf(encoded.copyOf().also { it[it.size - 8] = (it[it.size - 8].toInt() xor 1).toByte() })
        for ((index, bytes) in cases.withIndex()) {
            val old = runCatching { org.crosswire.ksword.reference.GzipJavaStreams(ByteArrayInputStream(bytes)).uncompress(1).toByteArray() }
            val current = runCatching { expand(CompressorType.GZIP, bytes, 1) }
            assertEquals(old.exceptionOrNull()?.javaClass, current.exceptionOrNull()?.javaClass, "gzip case $index")
            if (old.isSuccess) assertContentEquals(old.getOrThrow(), current.getOrThrow(), "gzip case $index")
        }
    }
    @Test fun xzOptionalRuntimeOutcomeIsExplicit() {
        val bytes = byteArrayOf(42)
        val expected = runCatching { reference(CompressorType.XZ, bytes) }
        val current = runCatching { compress(CompressorType.XZ, bytes) }
        assertEquals(expected.exceptionOrNull()?.javaClass, current.exceptionOrNull()?.javaClass)
        if (expected.isSuccess) {
            assertContentEquals(expected.getOrThrow(), current.getOrThrow())
            assertContentEquals(bytes, expand(CompressorType.XZ, current.getOrThrow(), bytes.size))
        } else {
            assertEquals(NoClassDefFoundError::class, expected.exceptionOrNull()!!::class)
        }
    }
    @Test fun malformedInputsRetainExceptionClasses() {
        for (type in listOf(CompressorType.GZIP, CompressorType.BZIP2)) {
            for (bytes in listOf(byteArrayOf(), byteArrayOf(1, 2, 3))) {
                val old = runCatching {
                    val stream = when(type) {
                        CompressorType.GZIP -> GzipCompressorInputStream(ByteArrayInputStream(bytes))
                        CompressorType.BZIP2 -> BZip2CompressorInputStream(ByteArrayInputStream(bytes))
                        else -> XZCompressorInputStream(ByteArrayInputStream(bytes))
                    }
                    stream.use { it.readBytes() }
                }.exceptionOrNull()!!
                assertEquals(old::class, runCatching { expand(type, bytes, 1) }.exceptionOrNull()!!::class, "$type malformed")
            }
        }
    }
}
