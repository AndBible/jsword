package org.crosswire.common.compress

import kotlin.test.Test
import kotlin.test.assertContentEquals

/** The optional XZ runtime must decode real payloads, not merely match a missing-class failure. */
class XzFunctionalContractTest {
    @Test fun realXzPayloadsRoundTripWithLengthAsCapacity() {
        val inputs = listOf(byteArrayOf(), byteArrayOf(42), ByteArray(18000) { (it % 251).toByte() })
        for (input in inputs) {
            val encoded = CompressorType.XZ.getCompressor(input).compress()
            assertContentEquals(byteArrayOf(0xfd.toByte(), 0x37, 0x7a, 0x58, 0x5a, 0), encoded.copyOf(6))
            for (capacity in listOf(0, 1, input.size + 11)) {
                assertContentEquals(input, CompressorType.XZ.getCompressor(encoded).uncompress(capacity))
            }
        }
    }
}
