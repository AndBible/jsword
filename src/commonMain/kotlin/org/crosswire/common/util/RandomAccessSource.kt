package org.crosswire.common.util

import okio.Closeable
import okio.FileHandle
import okio.FileSystem
import okio.Path

/**
 * Positional reads from a file with the semantics of `SwordUtil.readRAF`: a zero or negative size or an offset at or
 * beyond the end yields an empty array, a read past the end is shortened. Problems are logged like `SwordUtil` does,
 * under the `SwordUtil` logger name so existing log configuration keeps applying.
 *
 * Difference from `SwordUtil.readRAF`: that method seeks first, so a negative offset throws `IOException` there even
 * when the size is 0; here a size of 0 returns an empty array before the offset is looked at, and a negative offset
 * with a positive size fails in the underlying [FileHandle] read.
 */
class RandomAccessSource(private val handle: FileHandle) : Closeable {
    private var cursor = 0L
    private var closed = false

    fun size(): Long = handle.size()

    internal fun position(): Long {
        checkOpen()
        return cursor
    }

    internal fun seek(offset: Long) {
        checkOpen()
        if (offset < 0) throw okio.IOException("Negative seek offset")
        cursor = offset
    }

    internal fun readAtCursor(offset: Long, size: Int): ByteArray {
        seek(offset)
        return readNext(size)
    }

    internal fun readNext(size: Int): ByteArray {
        checkOpen()
        // Legacy readNextRAF checks the handle even for a zero-sized request.
        handle.size()
        return read(cursor, size).also { cursor += it.size }
    }

    internal fun readUntil(stopByte: Byte): ByteArray {
        val offset = position()
        var count = 0
        val byte = ByteArray(1)
        var next: Int
        do {
            val n = handle.read(cursor, byte, 0, 1)
            next = if (n < 0) -1 else byte[0].toInt() and 0xff
            if (n > 0) cursor++
            count++
        } while (next != -1 && next != stopByte.toInt())
        return readAtCursor(offset, count)
    }

    internal fun writeAtCursor(offset: Long, data: ByteArray?) {
        seek(offset)
        writeNext(data)
    }

    internal fun writeNext(data: ByteArray?) {
        // RAF direct writes of null/empty data are no-ops even on closed/read-only files.
        // writeAtCursor still seeks first, so its closed/negative seek errors are retained.
        if (data == null || data.isEmpty()) return
        checkOpen()
        if (!handle.readWrite) throw okio.IOException("Bad file descriptor")
        handle.write(cursor, data, 0, data.size)
        cursor += data.size
    }

    private fun checkOpen() {
        if (closed) throw okio.IOException("Stream Closed")
    }

    fun read(offset: Long, size: Int): ByteArray {
        // It is common to have an entry that points to nothing. That is the equivalent of an empty string.
        if (size == 0) return ByteArray(0)
        val length = handle.size()
        if (size < 0) {
            log.error("Nothing to read at offset = {} returning empty because negative size={}", offset.toString(), size.toString())
            return ByteArray(0)
        }
        if (offset >= length) {
            log.error("Attempt to read beyond end. offset={} size={} but raf.length={}", offset.toString(), size.toString(), length.toString())
            return ByteArray(0)
        }
        var n = size
        if (offset + size > length) {
            log.error("Need to reduce size to avoid EOFException. offset={} size={} but raf.length={}", offset.toString(), size.toString(), length.toString())
            n = (length - offset).toInt()
        }
        val result = ByteArray(n)
        var done = 0
        while (done < n) {
            val r = handle.read(offset + done, result, done, n - done)
            if (r <= 0) throw okio.EOFException("Unexpected end of file at offset ${offset + done}")
            done += r
        }
        return result
    }

    override fun close() {
        closed = true
        handle.close()
    }

    companion object {
        private val log = LoggerFactory.getLogger("org.crosswire.jsword.book.sword.SwordUtil")

        fun open(path: Path, fs: FileSystem = FileSystem.SYSTEM): RandomAccessSource =
            RandomAccessSource(fs.openReadOnly(path))
    }
}
