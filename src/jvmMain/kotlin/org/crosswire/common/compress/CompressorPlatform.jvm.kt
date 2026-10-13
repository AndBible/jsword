package org.crosswire.common.compress

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.apache.commons.compress.compressors.CompressorInputStream
import org.apache.commons.compress.compressors.CompressorOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils

/** Plain JVM helpers until Task 16 supplies expect/actual declarations. */
internal fun platformCompress(type: CompressorType, input: ByteArray): ByteArray {
    val bin = BufferedInputStream(ByteArrayInputStream(input))
    val bos = ByteArrayOutputStream()
    val out: CompressorOutputStream = when (type) {
        CompressorType.BZIP2 -> BZip2CompressorOutputStream(bos)
        CompressorType.XZ -> XZCompressorOutputStream(bos)
        else -> error("Not a platform compressor: $type")
    }
    IOUtils.copy(bin, out)
    bin.close()
    out.flush()
    out.close()
    return bos.toByteArray()
}

internal fun platformUncompress(type: CompressorType, input: ByteArray, expectedLength: Int): ByteArray {
    val out = ByteArrayOutputStream(expectedLength)
    val cin: CompressorInputStream = when (type) {
        CompressorType.BZIP2 -> BZip2CompressorInputStream(ByteArrayInputStream(input))
        CompressorType.XZ -> XZCompressorInputStream(ByteArrayInputStream(input))
        else -> error("Not a platform compressor: $type")
    }
    IOUtils.copy(cin, out)
    cin.close()
    out.flush()
    out.close()
    return out.toByteArray()
}

/** Retains first-member-only gzip, trailing-byte acceptance and legacy IOException classes. */
internal fun platformGzipUncompress(input: ByteArray, expectedLength: Int): ByteArray {
    val out = ByteArrayOutputStream(expectedLength)
    val cin: CompressorInputStream = GzipCompressorInputStream(ByteArrayInputStream(input))
    IOUtils.copy(cin, out)
    cin.close()
    out.flush()
    out.close()
    return out.toByteArray()
}
