package org.crosswire.common.compress

import kotlin.test.Test
import kotlin.test.assertContentEquals

/** Move to commonTest in Tasks 16-17 together with the JVM-only subjects. */
class CompressorRoundTripTest {
    @Test fun fixedInputsRoundTrip() {
        var state = 123456789
        val random = ByteArray(3000) { state = state * 1664525 + 1013904223; (state ushr 24).toByte() }
        val text = "In the beginning".encodeToByteArray()
        val inputs = listOf(byteArrayOf(), byteArrayOf(42), ByteArray(5000) { text[it % text.size] }, random)
        for (type in listOf(CompressorType.ZIP, CompressorType.GZIP, CompressorType.LZSS)) {
            for (input in inputs) {
                val compressed: ByteArray = type.getCompressor(input).compress()
                if (type == CompressorType.ZIP && input.isEmpty()) {
                    kotlin.test.assertFailsWith<IllegalArgumentException> { type.getCompressor(compressed).uncompress(0) }
                    assertContentEquals(input, type.getCompressor(compressed).uncompress())
                } else {
                    val result: ByteArray = type.getCompressor(compressed).uncompress(input.size)
                    assertContentEquals(input, result, "$type length ${input.size}")
                }
            }
        }
    }
}
