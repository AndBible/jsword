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
    fun size(): Long = handle.size()

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

    override fun close() = handle.close()

    companion object {
        private val log = LoggerFactory.getLogger("org.crosswire.jsword.book.sword.SwordUtil")

        fun open(path: Path, fs: FileSystem = FileSystem.SYSTEM): RandomAccessSource =
            RandomAccessSource(fs.openReadOnly(path))
    }
}
