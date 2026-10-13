package org.crosswire.common.util

import okio.BufferedSink
import okio.IOException

/** Writes non-ZIP64 STORED archives without timestamps or data descriptors. Does not close [sink]. */
object StoredZipWriter {
    fun write(sink: BufferedSink, entries: List<Pair<String, ByteArray>>) {
        val writer = Writer(sink)
        entries.forEach { (name, body) -> writer.entry(name, body) }
        writer.finish()
    }

    /** Incremental cache writer; retains directory metadata only, never previous entry bodies. */
    internal class Writer(private val sink: BufferedSink) {
        private class Record(val name: ByteArray, val size: Int, val crc: Int, val flags: Int, val offset: Long)
        private val records = ArrayList<Record>()
        private var offset = 0L
        private var pending: String? = null
        private var aborted = false

        internal fun beginEntry(name: String) {
            check(pending == null && !aborted)
            if (records.size == 65535) throw IOException("Too many ZIP entries")
            if (name.encodeToByteArray().size > 65535) throw IOException("ZIP name too long")
            pending = name
        }

        internal fun completeEntry(body: ByteArray) {
            val name = checkNotNull(pending)
            pending = null
            // Stored, not the seekable writer's rewritten provisional header: that close keeps method 8
            // and a ZIP64 extra while the body stays raw, which a sequential reader rejects as a stored block.
            entry(name, body)
        }

        /** Publish the commons-compress 1.12 seekable File writer's orphan header only after a failed body read.
         * Unknown size with Zip64Mode.AsNeeded writes version 45, method 8, flag 0x800 (UTF-8, no data-descriptor bit),
         * CRC 0, both sizes 0xffffffff, and a 20-byte ZIP64 extra of two zero sizes. Only the DOS time is zero here.
         * Normal output remains STORED; an aborted archive has no central directory or EOCD.
         * Header visibility during an in-progress read is deliberately not emulated.
         */
        internal fun abortEntry() {
            val name = checkNotNull(pending).encodeToByteArray()
            pending = null
            aborted = true
            int(0x04034b50); short(45); short(0x800); short(8); short(0); short(0)
            int(0); int(0xffffffffL); int(0xffffffffL); short(name.size); short(20)
            sink.write(name)
            short(1); short(16); sink.writeLongLe(0); sink.writeLongLe(0)
            sink.flush()
        }
        private fun short(value: Int) { sink.writeShortLe(value) }
        private fun int(value: Long) {
            if (value !in 0..0xffffffffL) throw IOException("ZIP64 required")
            sink.writeIntLe(value.toInt())
        }
        fun entry(name: String, body: ByteArray) {
            if (records.size == 65535) throw IOException("Too many ZIP entries")
            val encoded = name.encodeToByteArray()
            if (encoded.size > 65535) throw IOException("ZIP name too long")
            val record = Record(encoded, body.size, crc32(body), if (name.any { it.code > 127 }) 0x800 else 0, offset)
            records.add(record)
            int(0x04034b50); short(20); short(record.flags); short(0); short(0); short(0)
            int(record.crc.toLong() and 0xffffffffL); int(body.size.toLong()); int(body.size.toLong())
            short(encoded.size); short(0); sink.write(encoded); sink.write(body)
            offset += 30 + encoded.size.toLong() + body.size
            sink.flush()
        }
        fun finish() {
            if (aborted) return
            check(pending == null)
            val start = offset
            for (record in records) {
                int(0x02014b50); short(20); short(20); short(record.flags); short(0); short(0); short(0)
                int(record.crc.toLong() and 0xffffffffL); int(record.size.toLong()); int(record.size.toLong())
                short(record.name.size); short(0); short(0); short(0); short(0); int(0); int(record.offset)
                sink.write(record.name)
                offset += 46 + record.name.size
            }
            int(0x06054b50); short(0); short(0); short(records.size); short(records.size)
            int(offset - start); int(start); short(0)
            sink.flush()
        }
    }

    private fun crc32(bytes: ByteArray): Int {
        var crc = -1
        for (byte in bytes) {
            crc = crc xor (byte.toInt() and 255)
            repeat(8) { crc = (crc ushr 1) xor (if (crc and 1 != 0) 0xedb88320.toInt() else 0) }
        }
        return crc.inv()
    }
}
