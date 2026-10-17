package org.crosswire.common.compress

import java.util.zip.Inflater
import okio.Buffer
import kotlin.test.*

class LegacyRawZipSourceContractTest {
    private fun malformed(suffix: Int): ByteArray {
        val length = 6 + suffix
        return byteArrayOf(0, length.toByte(), (length ushr 8).toByte(), length.inv().toByte(), (length.inv() ushr 8).toByte()) +
            "prefix".encodeToByteArray() + ByteArray(suffix) { 32 } + byteArrayOf(7)
    }

    @Test fun prefixReadPreservesConditionalAdvanceAndDoesNotDecodeOnClose() {
        for (suffix in listOf(200, 5000)) {
            val compressed = malformed(suffix)
            LegacyRawZipSource(compressed + ByteArray(200), 0, ::JdkRawInflater, "Truncated ZIP file").use { source ->
                val output = Buffer()
                assertEquals(6L, source.read(output, 6))
                assertEquals("prefix", output.readUtf8())
                if (suffix == 200) assertFailsWith<RawDeflateException> { source.advance(compressed.size) }
                else source.advance(compressed.size)
            }
            LegacyRawZipSource(compressed, 0, ::JdkRawInflater, "Truncated ZIP file").use { source ->
                val output = Buffer()
                assertEquals(6L, source.read(output, 6))
                assertEquals("prefix", output.readUtf8())
                // Closing is the callback-failure branch: no implicit advance or suffix validation.
            }
        }
    }

    @Test fun descriptorConsumptionExcludesFollowingHeadersAndOutputIsChunkBounded() {
        val size = 5000
        val payload = byteArrayOf(1, size.toByte(), (size ushr 8).toByte(), size.inv().toByte(), (size.inv() ushr 8).toByte()) + ByteArray(size) { 65 }
        val bytes = byteArrayOf(42, 43) + payload + ByteArray(100) { 7 }
        LegacyRawZipSource(bytes, 2, ::JdkRawInflater, "Unexpected end of ZLIB input stream").use { source ->
            val output = Buffer()
            val reference = java.util.zip.InflaterInputStream(bytes.inputStream().apply { skip(2) }, Inflater(true))
            reference.use { input ->
                while (true) {
                    // InflaterInputStream fills 512 bytes, then returns at most one 8192-byte segment.
                    val chunk = ByteArray(8192)
                    val expected = input.read(chunk)
                    val actual = source.read(output, 10000)
                    assertEquals(expected.toLong(), actual)
                    if (expected == -1) break
                    assertTrue(actual in 1..8192)
                }
            }
            assertEquals(payload.size, source.consumed)
            assertContentEquals(ByteArray(size) { 65 }, output.readByteArray())
        }
    }
}
