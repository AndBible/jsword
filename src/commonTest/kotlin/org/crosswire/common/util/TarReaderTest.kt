package org.crosswire.common.util

import okio.Buffer
import okio.EOFException
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TarReaderTest {
    @Test fun readsNamesBodiesAndSkipsUnreadPadding() {
        val archive = tarRecord("dir/", ByteArray(0), '5') +
            tarRecord("first.conf", "hello".encodeToByteArray(), prefix = "mods.d") +
            tarRecord("last.conf", "last".encodeToByteArray()) + ByteArray(1024)
        TarReader(Buffer().write(archive)).use { reader ->
            assertTrue(reader.next()!!.isDirectory)
            assertEquals("mods.d/first.conf", reader.next()!!.name)
            assertEquals("last.conf", reader.next()!!.name)
            assertEquals("last", reader.readEntry().decodeToString())
            assertEquals(0, reader.readEntry().size)
            assertNull(reader.next())
        }
    }

    @Test fun readsGnuLongNameAndPaxPath() {
        val long = "mods.d/" + "a".repeat(140) + ".conf"
        val record = "path=$long\n"
        var length = record.encodeToByteArray().size + 3
        while ("$length $record".encodeToByteArray().size != length) length = "$length $record".encodeToByteArray().size
        val archive = tarRecord("././@LongLink", (long + '\u0000').encodeToByteArray(), 'L') +
            tarRecord("short", byteArrayOf(1)) + tarRecord("pax", "$length $record".encodeToByteArray(), 'x') +
            tarRecord("short2", byteArrayOf(2)) + ByteArray(1024)
        TarReader(Buffer().write(archive)).use { reader ->
            assertEquals(long, reader.next()!!.name)
            assertEquals(listOf<Byte>(1), reader.readEntry().toList())
            assertEquals(long, reader.next()!!.name)
            assertEquals(listOf<Byte>(2), reader.readEntry().toList())
            assertNull(reader.next())
        }
    }

    @Test fun rejectsTruncatedHeaderBodyPaddingAndTerminator() {
        for (bytes in listOf(ByteArray(20), tarRecord("a", ByteArray(513)).copyOf(1024),
            tarRecord("a", byteArrayOf(1)).copyOf(513), ByteArray(512))) {
            assertFailsWith<EOFException> {
                TarReader(Buffer().write(bytes)).use { reader -> while (reader.next() != null) reader.readEntry() }
            }
        }
    }

    companion object {
        fun tarRecord(name: String, body: ByteArray, type: Char = '0', prefix: String = ""): ByteArray {
            val header = ByteArray(512)
            fun text(offset: Int, value: String) { value.encodeToByteArray().copyInto(header, offset) }
            text(0, name); text(100, "0000644"); text(124, body.size.toString(8).padStart(11, '0'))
            header[156] = type.code.toByte(); text(257, "ustar"); text(345, prefix)
            for (i in 148..155) header[i] = 32
            text(148, header.sumOf { it.toInt() and 255 }.toString(8).padStart(6, '0') + "\u0000 ")
            return header + body + ByteArray((512 - body.size % 512) % 512)
        }
    }
}
