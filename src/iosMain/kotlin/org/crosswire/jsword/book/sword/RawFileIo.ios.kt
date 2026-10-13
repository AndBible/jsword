package org.crosswire.jsword.book.sword

import okio.*
import org.crosswire.common.util.*
import platform.Foundation.*

internal actual fun rawFileCreate(location: StorageLocation): Boolean = NativeLexicalFiles.create(location.spelling)
internal actual fun rawFileNumber(value: Long, newline: Boolean): String {
    val negative = value < 0
    val number = NSNumberFormatter().apply {
        locale = NSLocale.currentLocale
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = false
        // Formatter width includes the sign; DecimalFormat minimumIntegerDigits does not.
        minimumIntegerDigits = if (negative) 6uL else 7uL
        maximumFractionDigits = 0u
        negativePrefix = "-"
        negativeSuffix = ""
    }.stringFromNumber(NSNumber(longLong = value)) ?: error("Unable to format raw file number")
    return number + if (newline) "\r\n" else ""
}
// iOS's platform-default filename charset is UTF-8. The legacy limit is bytes, not characters.
internal actual fun rawFileDecodeName(data: ByteArray): String = KCharset.decode(data, 0, 7, "UTF-8")
internal actual fun rawFileAppend(location: StorageLocation, ordinal: Long, charset: String?) {
    NativeLexicalFiles.sink(location.spelling, append = true).buffer().use {
        it.write(KCharset.encode(rawFileNumber(ordinal, true), charset))
    }
}
internal actual fun rawFileWrite(location: StorageLocation, data: ByteArray) {
    NativeLexicalFiles.sink(location.spelling).buffer().use { it.write(data) }
}
internal actual fun rawFileRead(location: StorageLocation, log: Logger): ByteArray {
    try {
        val length = NativeLexicalFiles.length(location.spelling).toInt()
        val data = ByteArray(length)
        NativeLexicalFiles.handle(location.spelling, false).use { handle ->
            // BufferedInputStream bypasses its 8192-byte buffer for a large request.
            // A small request fills once, then copies; neither branch retries a short read.
            val count = when {
                length == 0 -> 0
                length >= 8192 -> handle.read(0, data, 0, length)
                else -> {
                    val buffer = ByteArray(8192)
                    val read = handle.read(0, buffer, 0, buffer.size)
                    if (read < 0) -1 else minOf(read, length).also { buffer.copyInto(data, 0, 0, it) }
                }
            }
            if (count != length) {
                log.error("Read data is not of appropriate size of {} bytes!", length.toString())
                throw IOException("data is not $length bytes long")
            }
        }
        return data
    } catch (error: FileNotFoundException) {
        log.error("Could not read text data file, file not found: {}", storageName(location), error)
        throw error
    }
}
internal actual fun rawFileWriteInc(location: StorageLocation?, data: ByteArray, log: Logger) {
    try {
        NativeLexicalFiles.sink((location ?: throw NullPointerException()).spelling).use { it.write(Buffer().write(data), data.size.toLong()) }
    } catch (error: FileNotFoundException) {
        log.error("Error on writing to incfile, file should exist already!", error)
    }
}
internal actual fun rawFilePopulate(location: StorageLocation, count: Int) {
    NativeLexicalFiles.sink(location.spelling).buffer().use { sink ->
        for (i in 0 until count) sink.write(ByteArray(6))
    }
}
