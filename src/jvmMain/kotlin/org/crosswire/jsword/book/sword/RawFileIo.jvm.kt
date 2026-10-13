package org.crosswire.jsword.book.sword

import java.io.*
import org.crosswire.common.util.Logger

internal fun rawFileCreate(location: StorageLocation): Boolean = File(location.spelling).createNewFile()
internal fun rawFileNumber(value: Long, newline: Boolean): String = String.format(if (newline) "%07d\r\n" else "%07d", value)
internal fun rawFileDecodeName(data: ByteArray): String = String(data, 0, 7, java.nio.charset.Charset.defaultCharset())

internal fun rawFileAppend(location: StorageLocation, ordinal: Long, charset: String?) {
    var stream: BufferedOutputStream? = null
    try {
        stream = BufferedOutputStream(FileOutputStream(File(location.spelling), true))
        stream.write((rawFileNumber(ordinal, true) as java.lang.String).getBytes(charset))
    } finally { stream?.close() }
}

internal fun rawFileWrite(location: StorageLocation, data: ByteArray) {
    var stream: BufferedOutputStream? = null
    try {
        stream = BufferedOutputStream(FileOutputStream(File(location.spelling), false))
        stream.write(data)
    } finally { stream?.close() }
}

internal fun rawFileRead(location: StorageLocation, log: Logger): ByteArray {
    val file = File(location.spelling)
    var stream: BufferedInputStream? = null
    try {
        val len = file.length().toInt()
        val data = ByteArray(len)
        stream = BufferedInputStream(FileInputStream(file))
        if (stream.read(data) != len) {
            log.error("Read data is not of appropriate size of {} bytes!", len.toString())
            throw IOException("data is not $len bytes long")
        }
        return data
    } catch (e: FileNotFoundException) {
        log.error("Could not read text data file, file not found: {}", file.name, e)
        throw e
    } finally { stream?.close() }
}

internal fun rawFileWriteInc(location: StorageLocation?, data: ByteArray, log: Logger) {
    var stream: FileOutputStream? = null
    try {
        stream = FileOutputStream(location?.let { File(it.spelling) }, false)
        stream.write(data)
    } catch (e: FileNotFoundException) {
        log.error("Error on writing to incfile, file should exist already!", e)
    } finally { stream?.close() }
}

internal fun rawFilePopulate(location: StorageLocation, count: Int) {
    val stream = BufferedOutputStream(FileOutputStream(File(location.spelling), false))
    try { for (i in 0 until count) stream.write(ByteArray(6)) }
    finally { stream.close() }
}

/** Legacy File entry point split away from the portable backend. */
object RawFileBackendJvm {
    @JvmStatic
    @Throws(IOException::class)
    fun updateDataFile(backend: RawFileBackend, ordinal: Long, file: File) {
        rawFileAppend(StorageLocation(file.path), ordinal, backend.bookMetaData.bookCharset)
    }
}
