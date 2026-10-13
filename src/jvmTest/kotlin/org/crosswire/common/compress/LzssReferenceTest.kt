package org.crosswire.common.compress

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import org.crosswire.ksword.reference.LzssJavaStreams

/** Independent frozen stream reference; remains JVM-only after Tasks 16-17. */
class LzssReferenceTest {
    private fun compressed(input: ByteArray) = LZSS(input).compress()
    private fun expanded(input: ByteArray, length: Int) = LZSS(input).uncompress(length)

    @Test fun corpusMatchesFrozenReference() {
        val random = Random(0x4c5a5353)
        repeat(2000) { case ->
            val length = when (case) { 0 -> 0; 1 -> 20000; else -> random.nextInt(20001) }
            val input = ByteArray(length)
            if (case % 2 == 0) random.nextBytes(input)
            else {
                val text = "In the beginning God created the heaven and the earth. ".encodeToByteArray()
                input.indices.forEach { input[it] = text[it % text.size] }
            }
            val old = LzssJavaStreams(ByteArrayInputStream(input)).compress().toByteArray()
            assertContentEquals(old, compressed(input), "compressed case $case length $length")
            val oldExpanded = LzssJavaStreams(ByteArrayInputStream(old)).uncompress(length).toByteArray()
            assertContentEquals(oldExpanded, expanded(old, length), "expanded case $case")
            assertContentEquals(input, oldExpanded, "reference round trip $case")
        }
    }

    @Test fun fixtureBlocksMatchFrozenReference() {
        val root = File("src/commonTest/resources/k4-modules/space dir/ä/modules/K4ZTextChapterLzss")
        for (testament in listOf("ot", "nt")) {
            val blocks = File(root, "$testament.czz").readBytes()
            val index = ByteBuffer.wrap(File(root, "$testament.czs").readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            while (index.remaining() >= 12) {
                val offset = index.int
                val compressedSize = index.int
                val length = index.int
                val block = blocks.copyOfRange(offset, offset + compressedSize)
                val old = LzssJavaStreams(ByteArrayInputStream(block)).uncompress(length).toByteArray()
                assertContentEquals(old, expanded(block, length))
                assertContentEquals(LzssJavaStreams(ByteArrayInputStream(old)).compress().toByteArray(), compressed(old))
            }
        }
    }

    @Test fun truncatedAndLengthHandlingMatchesReference() {
        for (input in listOf(byteArrayOf(), byteArrayOf(1), byteArrayOf(0, 0), byteArrayOf(1, 65), byteArrayOf(0, 0, 0))) {
            for (length in listOf(0, 1, 100)) {
                assertContentEquals(LzssJavaStreams(ByteArrayInputStream(input)).uncompress(length).toByteArray(), expanded(input, length))
            }
        }
    }
}
