package org.crosswire.common.util

import okio.Buffer
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import okio.buffer
import okio.openZip
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals

class StoredZipWriterTest {
    @Test fun readsStoredUtf8EntriesAndEmptyZipWithOkio() {
        for (entries in listOf(emptyList(), listOf("z.conf" to "hello".encodeToByteArray(), "mods.d/ä.conf" to byteArrayOf(0, 1, -1)))) {
            val bytes = Buffer()
            StoredZipWriter.write(bytes, entries)
            val fs = FakeFileSystem()
            val path = "/cache.zip".toPath()
            fs.write(path) { write(bytes.readByteArray()) }
            val zip = fs.openZip(path)
            run {
                for ((name, body) in entries) {
                    assertEquals(body.toList(), zip.source("/$name".toPath()).buffer().use { it.readByteArray().toList() })
                    assertEquals(body.size.toLong(), zip.metadata("/$name".toPath()).size)
                }
                assertEquals(entries.size, zip.listRecursively("/".toPath()).count { !zip.metadata(it).isDirectory })
            }
        }
    }

    @Test fun writesCorrectCrcAndUtf8Flag() {
        val output = Buffer()
        StoredZipWriter.write(output, listOf("ä.conf" to "123456789".encodeToByteArray()))
        val bytes = output.readByteArray()
        fun int(offset: Int): Long = (0..3).fold(0L) { acc, i -> acc or ((bytes[offset + i].toLong() and 255) shl (8 * i)) }
        assertEquals(0xcbf43926L, int(14))
        assertEquals(8, bytes[7].toInt())
        assertEquals(0, bytes[8].toInt())
        assertEquals(9, int(18))
        assertEquals(9, int(22))
    }
}
