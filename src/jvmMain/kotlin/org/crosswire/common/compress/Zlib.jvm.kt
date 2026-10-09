package org.crosswire.common.compress

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/** The stream code `Zip` used before it was moved here, unchanged, so the JDK's exceptions pass through. */
actual object Zlib {
    private const val BUF_SIZE = 2048

    actual fun inflate(input: ByteArray, expectedSize: Int): ByteArray {
        val out = ByteArrayOutputStream(expectedSize)
        val zin = InflaterInputStream(ByteArrayInputStream(input), Inflater(), expectedSize)
        val buf = ByteArray(expectedSize)

        var count = zin.read(buf)
        while (count != -1) {
            out.write(buf, 0, count)
            count = zin.read(buf)
        }
        zin.close()
        out.flush()
        out.close()
        return out.toByteArray()
    }

    actual fun deflate(input: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        val out = DeflaterOutputStream(bos, Deflater(), BUF_SIZE)
        out.write(input, 0, input.size)
        out.flush()
        out.close()
        return bos.toByteArray()
    }
}
