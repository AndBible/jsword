package org.crosswire.common.compress

import org.crosswire.jsword.book.sword.SwordUtil
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.Random
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/** [Zlib] against `java.util.zip` used directly: the JDK is the reference. */
class ZlibDifferentialTest {
    private fun jdkDeflate(b: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        DeflaterOutputStream(bos, Deflater(), 2048).use { it.write(b) }
        return bos.toByteArray()
    }

    private fun jdkInflate(b: ByteArray, size: Int): ByteArray {
        val out = ByteArrayOutputStream()
        InflaterInputStream(ByteArrayInputStream(b), Inflater(), size).use { it.copyTo(out, size) }
        return out.toByteArray()
    }

    /** Half random, half repetitive so that the deflater has something to do. */
    private fun payload(n: Int, seed: Long): ByteArray {
        val r = Random(seed)
        val b = ByteArray(n)
        r.nextBytes(b)
        for (i in n / 2 until n) b[i] = (i % 7).toByte()
        return b
    }

    private val sizes = intArrayOf(0, 1, 100, 64 * 1024, 1024 * 1024)

    @Test fun inflateEqualsOriginalForAnyBufferSize() {
        for (n in sizes) {
            val data = payload(n, 42L + n)
            val z = jdkDeflate(data)
            for (expected in intArrayOf(maxOf(n, 1), 1, maxOf(2 * n, 1))) {
                assertArrayEquals("n=$n expected=$expected", data, Zlib.inflate(z, expected))
            }
        }
    }

    @Test fun deflateIsByteEqualToJdkAndRoundTrips() {
        for (n in sizes) {
            val data = payload(n, 7L + n)
            val d = Zlib.deflate(data)
            assertArrayEquals("n=$n", jdkDeflate(data), d)
            assertArrayEquals(data, jdkInflate(d, 4096))
        }
    }

    @Test fun trailingGarbageIsIgnoredLikeJdk() {
        val data = payload(5000, 1L)
        val z = jdkDeflate(data) + byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val expected = jdkInflate(z, 5000)
        assertArrayEquals(expected, Zlib.inflate(z, 5000))
        assertArrayEquals(data, expected)
    }

    private fun thrown(block: () -> Unit): Throwable? = try { block(); null } catch (t: Throwable) { t }

    @Test fun truncatedStreamThrowsSameClassAsJdk() {
        val z = jdkDeflate(payload(5000, 2L))
        val cut = z.copyOf(z.size - 4)
        val jdk = thrown { jdkInflate(cut, 5000) }
        val ours = thrown { Zlib.inflate(cut, 5000) }
        println("truncated: jdk=${jdk?.javaClass?.name}: ${jdk?.message}; ours=${ours?.javaClass?.name}")
        assertEquals(java.io.EOFException::class.java, jdk!!.javaClass)
        assertEquals(jdk.javaClass, ours!!.javaClass)
        assertEquals(jdk.message, ours.message)
        assertTrue(ours is okio.IOException)
    }

    @Test fun emptyInputThrowsSameClassAsJdk() {
        val jdk = thrown { jdkInflate(ByteArray(0), 100) }
        val ours = thrown { Zlib.inflate(ByteArray(0), 100) }
        println("empty: jdk=${jdk?.javaClass?.name}: ${jdk?.message}")
        assertEquals(jdk!!.javaClass, ours!!.javaClass)
    }

    @Test fun corruptHeaderThrowsSameClassAsJdk() {
        val bad = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9a.toByte(), 0xbc.toByte())
        val jdk = thrown { jdkInflate(bad, 100) }
        val ours = thrown { Zlib.inflate(bad, 100) }
        println("corrupt: jdk=${jdk?.javaClass?.name}: ${jdk?.message}; ours=${ours?.javaClass?.name}")
        assertEquals(java.util.zip.ZipException::class.java, jdk!!.javaClass)
        assertEquals(jdk.javaClass, ours!!.javaClass)
        assertEquals(jdk.message, ours.message)
        assertTrue(ours is okio.IOException)
    }

    @Test fun zeroExpectedSizeIsIllegalArgument() {
        val z = jdkDeflate(payload(10, 3L))
        val jdk = thrown { jdkInflate(z, 0) }
        val ours = thrown { Zlib.inflate(z, 0) }
        assertEquals(IllegalArgumentException::class.java, jdk!!.javaClass)
        assertEquals(IllegalArgumentException::class.java, ours!!.javaClass)
    }

    @Test fun realZTextBlocksInflateLikeJdk() {
        val dir = File(System.getProperty("user.home"), ".sword/modules/texts/ztext/kjv")
        assumeTrue("kjv zText module absent", File(dir, "nt.bzs").isFile && File(dir, "nt.bzz").isFile)
        var checked = 0
        for (testament in listOf("ot", "nt")) {
            RandomAccessFile(File(dir, "$testament.bzs"), "r").use { idx ->
                RandomAccessFile(File(dir, "$testament.bzz"), "r").use { dat ->
                    // The compressed-block index: 12-byte entries of start, compressed size, uncompressed size.
                    for (i in 0 until 10) {
                        val e = SwordUtil.readRAF(idx, 12L * i, 12)
                        if (e.size < 12) break
                        val start = SwordUtil.decodeLittleEndian32(e, 0)
                        val csize = SwordUtil.decodeLittleEndian32(e, 4)
                        val usize = SwordUtil.decodeLittleEndian32(e, 8)
                        val block = SwordUtil.readRAF(dat, start.toLong(), csize)
                        assertEquals(csize, block.size)
                        val expected = jdkInflate(block, usize)
                        assertEquals(usize, expected.size)
                        assertArrayEquals("$testament block $i", expected, Zlib.inflate(block, usize))
                        checked++
                    }
                }
            }
        }
        assertEquals(20, checked)
    }
}
