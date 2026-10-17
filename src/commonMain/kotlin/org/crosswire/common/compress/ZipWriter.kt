/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.compress

import okio.Buffer
import okio.BufferedSink
import okio.Closeable
import okio.Deflater
import okio.DeflaterSink
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import okio.use

/**
 * A streaming ZIP writer producing the same layout as `java.util.zip.ZipOutputStream` for DEFLATED
 * entries of unknown size: general-purpose flag `0x0808` (data descriptor, UTF-8 names), local header
 * CRC and sizes zero, a data descriptor with signature after the body, and ZIP64 records once a size,
 * offset or the directory reaches [zip64Threshold]. Memory is O(entries): bodies are deflated straight
 * into [sink]. The data descriptor uses 8-byte sizes only when a size really exceeds 32 bits, because
 * readers such as `ZipInputStream` decide that from the real sizes, not from [zip64Threshold] (which
 * only lowers the point where the central directory and end records go ZIP64, for tests). [dosDateTime] is the 32-bit MS-DOS value (date in the high 16 bits, time in the low 16).
 *
 * If a body writer throws, the exception propagates and [close] then closes the sink without writing a
 * central directory, so the broken archive cannot be mistaken for a complete one.
 */
class ZipWriter(sink: Sink, internal val zip64Threshold: Long = 0xFFFFFFFFL) : Closeable {
    private class Entry(
        val name: ByteArray, val dosDateTime: Long, val offset: Long,
        var crc: Long = 0, var compressedSize: Long = 0, var size: Long = 0,
    )

    init {
        require(zip64Threshold in 1..0xFFFFFFFFL) { "zip64Threshold must fit in 32 bits: $zip64Threshold" }
    }

    private val counting = CountingSink(sink)
    private val out: BufferedSink = counting.buffer()
    private val entries = ArrayList<Entry>()
    private var failed = false
    private var closed = false

    /** Offset of the next byte in the archive, including bytes still buffered. */
    private val position: Long get() = counting.count + out.buffer.size

    fun putEntry(name: String, dosDateTime: Long = 0L, write: (BufferedSink) -> Unit) {
        check(!closed) { "closed" }
        check(!failed) { "a previous entry failed" }
        try {
            val nameBytes = name.encodeToByteArray()
            require(nameBytes.size <= 0xFFFF) { "entry name too long" }
            val entry = Entry(nameBytes, dosDateTime, position)
            writeLocalHeader(entry)
            writeBody(entry, write)
            writeDataDescriptor(entry)
            entries += entry
        } catch (t: Throwable) {
            failed = true
            throw t
        }
    }

    /** Copies all of [source] into a new entry and closes it. */
    fun putEntry(name: String, source: Source, dosDateTime: Long = 0L) {
        source.use { s -> putEntry(name, dosDateTime) { it.writeAll(s) } }
    }

    private fun writeLocalHeader(e: Entry) = out.apply {
        writeIntLe(0x04034b50)
        writeShortLe(20) // version needed
        writeShortLe(FLAGS)
        writeShortLe(METHOD_DEFLATED)
        writeIntLe(e.dosDateTime.toInt())
        writeIntLe(0) // crc, sizes follow in the data descriptor
        writeIntLe(0)
        writeIntLe(0)
        writeShortLe(e.name.size)
        writeShortLe(0) // extra length
        write(e.name)
    }

    private fun writeBody(e: Entry, write: (BufferedSink) -> Unit) {
        val compressed = CountingSink(NonClosingSink(out))
        val deflater = Deflater(-1, true)
        try {
            val checked = ChecksumSink(DeflaterSink(compressed, deflater))
            val body = checked.buffer()
            write(body)
            body.close() // flushes, finishes the deflater and closes it
            e.crc = checked.crc.value
            e.size = checked.count
            e.compressedSize = compressed.count
        } finally {
            deflater.end()
        }
    }

    private fun writeDataDescriptor(e: Entry) = out.apply {
        writeIntLe(0x08074b50)
        writeIntLe(e.crc.toInt())
        if (e.compressedSize > MAX_32 || e.size > MAX_32) {
            writeLongLe(e.compressedSize)
            writeLongLe(e.size)
        } else {
            writeIntLe(e.compressedSize.toInt())
            writeIntLe(e.size.toInt())
        }
    }

    /** Writes the central directory and end records, then closes the sink. */
    override fun close() {
        if (closed) return
        closed = true
        try {
            if (!failed) writeCentralDirectory()
        } finally {
            out.close()
        }
    }

    private fun writeCentralDirectory() {
        val cdOffset = position
        for (e in entries) writeCentralRecord(e)
        val cdSize = position - cdOffset
        val count = entries.size.toLong()
        val zip64 = count >= 0xFFFF || cdSize >= zip64Threshold || cdOffset >= zip64Threshold
        out.apply {
            if (zip64) {
                val zip64EocdOffset = position
                writeIntLe(0x06064b50)
                writeLongLe(44)
                writeShortLe(45)
                writeShortLe(45)
                writeIntLe(0)
                writeIntLe(0)
                writeLongLe(count)
                writeLongLe(count)
                writeLongLe(cdSize)
                writeLongLe(cdOffset)
                writeIntLe(0x07064b50)
                writeIntLe(0)
                writeLongLe(zip64EocdOffset)
                writeIntLe(1)
            }
            writeIntLe(0x06054b50)
            writeShortLe(0)
            writeShortLe(0)
            writeShortLe(if (zip64) 0xFFFF else count.toInt())
            writeShortLe(if (zip64) 0xFFFF else count.toInt())
            writeIntLe(if (zip64) -1 else cdSize.toInt())
            writeIntLe(if (zip64) -1 else cdOffset.toInt())
            writeShortLe(0)
        }
    }

    private fun writeCentralRecord(e: Entry) {
        val bigSize = e.size >= zip64Threshold
        val bigCompressed = e.compressedSize >= zip64Threshold
        val bigOffset = e.offset >= zip64Threshold
        val extraLength = (if (bigSize) 8 else 0) + (if (bigCompressed) 8 else 0) + (if (bigOffset) 8 else 0)
        val version = if (extraLength > 0) 45 else 20
        out.apply {
            writeIntLe(0x02014b50)
            writeShortLe(version) // made by
            writeShortLe(version) // needed
            writeShortLe(FLAGS)
            writeShortLe(METHOD_DEFLATED)
            writeIntLe(e.dosDateTime.toInt())
            writeIntLe(e.crc.toInt())
            writeIntLe(if (bigCompressed) -1 else e.compressedSize.toInt())
            writeIntLe(if (bigSize) -1 else e.size.toInt())
            writeShortLe(e.name.size)
            writeShortLe(if (extraLength > 0) extraLength + 4 else 0)
            writeShortLe(0) // comment
            writeShortLe(0) // disk
            writeShortLe(0) // internal attributes
            writeIntLe(0) // external attributes
            writeIntLe(if (bigOffset) -1 else e.offset.toInt())
            write(e.name)
            if (extraLength > 0) {
                writeShortLe(1)
                writeShortLe(extraLength)
                if (bigSize) writeLongLe(e.size)
                if (bigCompressed) writeLongLe(e.compressedSize)
                if (bigOffset) writeLongLe(e.offset)
            }
        }
    }

    private companion object {
        const val MAX_32 = 0xFFFFFFFFL
        const val FLAGS = 0x0808
        const val METHOD_DEFLATED = 8
    }
}

/** Counts bytes passed to [delegate]. */
private class CountingSink(private val delegate: Sink) : Sink {
    var count = 0L
        private set

    override fun write(source: Buffer, byteCount: Long) {
        delegate.write(source, byteCount)
        count += byteCount
    }

    override fun flush() = delegate.flush()
    override fun timeout(): Timeout = delegate.timeout()
    override fun close() = delegate.close()
}

/** Lets a [DeflaterSink] finish without closing the archive sink underneath it. */
private class NonClosingSink(private val delegate: Sink) : Sink {
    override fun write(source: Buffer, byteCount: Long) = delegate.write(source, byteCount)
    override fun flush() = delegate.flush()
    override fun timeout(): Timeout = delegate.timeout()
    override fun close() = delegate.flush()
}

/** Tracks the CRC-32 and length of the bytes passing through to [delegate]. */
private class ChecksumSink(private val delegate: Sink) : Sink {
    val crc = Crc32()
    var count = 0L
        private set
    private val scratch = ByteArray(8192)

    override fun write(source: Buffer, byteCount: Long) {
        var remaining = byteCount
        val piece = Buffer()
        while (remaining > 0) {
            val n = minOf(remaining, scratch.size.toLong()).toInt()
            val read = source.read(scratch, 0, n)
            check(read > 0) { "source exhausted" }
            crc.update(scratch, 0, read)
            piece.write(scratch, 0, read)
            delegate.write(piece, read.toLong())
            remaining -= read
            count += read
        }
    }

    override fun flush() = delegate.flush()
    override fun timeout(): Timeout = delegate.timeout()
    override fun close() = delegate.close()
}
