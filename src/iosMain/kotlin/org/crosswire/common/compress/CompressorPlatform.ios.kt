package org.crosswire.common.compress

import okio.Buffer
import okio.use
import org.crosswire.jsword.book.BookException

internal actual fun platformCompress(type: CompressorType, input: ByteArray): ByteArray =
    throw BookException("unsupported compression on this platform: $type")

internal actual fun platformUncompress(type: CompressorType, input: ByteArray, expectedLength: Int): ByteArray =
    throw BookException("unsupported compression on this platform: $type")

internal actual fun platformGzipUncompress(input: ByteArray, expectedLength: Int): ByteArray {
    require(expectedLength >= 0) { "Negative initial size: $expectedLength" }
    return LegacyGzipSource(Buffer().write(input), ::NativeRawInflater).use { source ->
        val output = Buffer()
        while (source.read(output, 8192) != -1L) Unit
        output.readByteArray()
    }
}
