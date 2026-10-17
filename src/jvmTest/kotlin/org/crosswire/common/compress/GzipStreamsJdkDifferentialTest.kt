package org.crosswire.common.compress

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals

/** The JDK is the oracle for [GzipStreams]. */
class GzipStreamsJdkDifferentialTest {
    private val random = Random(42)
    private val sizes = listOf(0, 1, 65_535, 3 shl 20)

    private fun jdkGzip(bytes: ByteArray) = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(bytes) } }.toByteArray()
    private fun jdkGunzip(bytes: ByteArray) = GZIPInputStream(ByteArrayInputStream(bytes)).readBytes()

    @Test fun gunzipMatchesGzipInputStreamOnJdkOutput() {
        for (size in sizes) {
            val incompressible = ByteArray(size).also { random.nextBytes(it) }
            val compressible = ByteArray(size) { (it % 13).toByte() }
            for (input in listOf(incompressible, compressible)) {
                assertContentEquals(input, GzipStreams.gunzip(jdkGzip(input)), "size $size")
            }
        }
    }

    @Test fun jdkGzipInputStreamReadsOurOutput() {
        for (size in sizes) {
            val input = ByteArray(size).also { random.nextBytes(it) }
            assertContentEquals(input, jdkGunzip(GzipStreams.gzip(input)), "size $size")
        }
    }

    @Test fun gunzipMatchesJdkOnMultiMemberAndTrailingBytes() {
        val multi = GzipStreams.gzip("ab".encodeToByteArray()) + GzipStreams.gzip("cd".encodeToByteArray())
        val trailing = GzipStreams.gzip("ab".encodeToByteArray()) + byteArrayOf(0, 1, 2)
        val magicThenGarbage = GzipStreams.gzip("ab".encodeToByteArray()) + byteArrayOf(0x1f, 0x8b.toByte(), 9, 9, 9)
        val bigMulti = jdkGzip(ByteArray(200_000).also { random.nextBytes(it) }) + jdkGzip(ByteArray(70_000) { 1 }) + byteArrayOf(5)
        for (input in listOf(multi, trailing, magicThenGarbage, bigMulti)) {
            assertContentEquals(jdkGunzip(input), GzipStreams.gunzip(input))
        }
    }

    @Test fun gunzipMatchesJdkOnFollowingMemberWithHeaderCrc() {
        val first = GzipStreams.gzip("ab".encodeToByteArray())
        val body = GzipStreams.gzip("cd".encodeToByteArray()).let { it.copyOfRange(10, it.size) }
        val head = byteArrayOf(0x1f, 0x8b.toByte(), 8, 2, 0, 0, 0, 0, 0, 0xff.toByte())
        val goodCrc = CRC32().apply { update(head) }.value.toInt()
        val withGood = head + byteArrayOf(goodCrc.toByte(), (goodCrc shr 8).toByte()) + body
        val withBad = head + byteArrayOf((goodCrc + 1).toByte(), (goodCrc shr 8).toByte()) + body
        for (input in listOf(first + withGood, first + withBad)) {
            assertContentEquals(jdkGunzip(input), GzipStreams.gunzip(input))
        }
    }

    @Test fun gunzipRejectsFirstMemberHeaderLikeJdk() {
        val plain = GzipStreams.gzip("xyz".encodeToByteArray())
        val head = plain.copyOf(10).also { it[3] = 2 }
        val body = plain.copyOfRange(10, plain.size)
        val badMethod = plain.copyOf().also { it[2] = 7 }
        val badHeaderCrc = head + byteArrayOf(0, 0) + body // CRC16 of the header is not 0
        for (input in listOf(badMethod, badHeaderCrc, byteArrayOf(), byteArrayOf(0x1f, 0x8b.toByte(), 8))) {
            kotlin.test.assertFailsWith<java.io.IOException> { jdkGunzip(input) }
            kotlin.test.assertFailsWith<okio.IOException> { GzipStreams.gunzip(input) }
        }
    }

    @Test fun crc32MatchesJavaUtilZipCrc32() {
        repeat(20) {
            val data = ByteArray(random.nextInt(100_000)).also { random.nextBytes(it) }
            val ours = Crc32().apply { update(data) }
            val jdk = CRC32().apply { update(data) }
            kotlin.test.assertEquals(jdk.value, ours.value)
        }
    }
}
