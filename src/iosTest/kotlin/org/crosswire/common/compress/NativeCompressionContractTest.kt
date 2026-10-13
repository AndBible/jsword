package org.crosswire.common.compress

import org.crosswire.jsword.book.BookException
import kotlin.test.*

class NativeCompressionContractTest {
    private val gzip = hex("1f8b08000000000000ffcb48cdc9c9070086a6103605000000")
    private fun hex(value: String) = ByteArray(value.length / 2) { value.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    @Test fun gzipUsesFirstMemberAcceptsTrailingBytesAndRejectsCorruptTrailer() {
        for (bytes in listOf(gzip, gzip + gzip, gzip + byteArrayOf(7))) {
            assertEquals("hello", CompressorType.GZIP.getCompressor(bytes).uncompress(0).decodeToString())
        }
        assertFailsWith<okio.IOException> { CompressorType.GZIP.getCompressor(gzip.copyOf().apply { this[size - 8] = 0 }).uncompress(1) }
        assertFailsWith<okio.EOFException> { CompressorType.GZIP.getCompressor(gzip.copyOf(gzip.size - 8)).uncompress(1) }
        assertFailsWith<IllegalArgumentException> { CompressorType.GZIP.getCompressor(byteArrayOf()).uncompress(-1) }
    }

    @Test fun unsupportedCodecsAreTypedInBothDirections() {
        for (type in listOf(CompressorType.BZIP2, CompressorType.XZ)) {
            assertFailsWith<BookException> { type.getCompressor(byteArrayOf(42)).compress() }
            assertFailsWith<BookException> { type.getCompressor(byteArrayOf(42)).uncompress(1) }
        }
    }
}
