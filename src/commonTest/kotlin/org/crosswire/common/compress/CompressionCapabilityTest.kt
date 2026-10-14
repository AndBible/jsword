package org.crosswire.common.compress

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.*
import org.crosswire.jsword.book.BookException
import org.crosswire.ksword.fixtures.isJvm

@OptIn(ExperimentalEncodingApi::class)
class CompressionCapabilityTest {
    private val text = "In the beginning\nK4 portable compression capability\n".encodeToByteArray()

    @Test fun bzip2DecodesValidPayloadOnJvmAndIsTypedUnsupportedOnNative() = verify(
        CompressorType.BZIP2,
        "QlpoOTFBWSZTWWaXho4AAARdgAAQQAAEAAAoOufcICAAQRT0J6anpNtUZ6hQNNDIyYhiArUdk/OY9RUn4bxny1ZN7aKbpdZUAGr4kM44LuSKcKEgzS8NHA==")

    @Test fun xzDecodesValidPayloadOnJvmAndIsTypedUnsupportedOnNative() = verify(
        CompressorType.XZ,
        "/Td6WFoAAATm1rRGAgAhARYAAAB0L+WjAQAzSW4gdGhlIGJlZ2lubmluZwpLNCBwb3J0YWJsZSBjb21wcmVzc2lvbiBjYXBhYmlsaXR5CgB4QcINxLQaZgABTDSXQf5dH7bzfQEAAAAABFla")

    private fun verify(type: CompressorType, validPayload: String) {
        val bytes = Base64.decode(validPayload)
        if (isJvm) {
            assertContentEquals(text, type.getCompressor(bytes).uncompress(), "$type valid external payload")
            val compressed = type.getCompressor(text).compress()
            assertContentEquals(text, type.getCompressor(compressed).uncompress(), "$type JVM round trip")
        } else {
            val error = assertFailsWith<BookException> { type.getCompressor(bytes).uncompress() }
            assertEquals("unsupported compression on this platform: $type", error.message)
        }
    }
}
