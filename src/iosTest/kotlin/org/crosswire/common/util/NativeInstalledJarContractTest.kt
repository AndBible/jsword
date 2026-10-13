@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*

class NativeInstalledJarContractTest {
    @Test fun jarKeepsRawNameAndExtendedTimestampUnlikeCommonsUnicodeLookup() = memScoped {
        val template = (platform.Foundation.NSTemporaryDirectory() + "ksword-jar-extra-XXXXXX").cstr.getPointer(this)
        val root = mkdtemp(template)!!.toKString(); val file = "$root/input.jar"
        try {
            val buffer = okio.Buffer()
            StoredZipWriter.write(buffer, listOf("legacy.properties" to "key=value\n".encodeToByteArray()))
            val original = buffer.readByteArray()
            val central = (0..original.size - 4).first { i -> original[i] == 80.toByte() && original[i + 1] == 75.toByte() && original[i + 2] == 1.toByte() && original[i + 3] == 2.toByte() }
            val end = original.size - 22
            // Independently checked CRC32; a central UnicodePath overrides commons only.
            val unicode = okio.Buffer().writeShortLe(0x7075).writeShortLe(23).writeByte(1).writeIntLe(0x1e9e4a40)
                .writeUtf8("unicode.properties").readByteArray()
            val timestamp = okio.Buffer().writeShortLe(0x5455).writeShortLe(5).writeByte(1).writeIntLe(1600000001).readByteArray()
            val extra = unicode + timestamp
            val nameEnd = central + 46 + "legacy.properties".length
            val fixture = original.copyOfRange(0, nameEnd) + extra + original.copyOfRange(nameEnd, original.size)
            fixture[central + 30] = extra.size.toByte()
            val movedEnd = end + extra.size
            val centralSize = 46 + "legacy.properties".length + extra.size
            repeat(4) { fixture[movedEnd + 12 + it] = (centralSize ushr (8 * it)).toByte() }
            NativeLexicalFiles.sink(file).use { sink -> val bytes = okio.Buffer().write(fixture); sink.write(bytes, bytes.size) }
            val uri = KUri("jar:file:$file!/legacy.properties")
            assertEquals("value", NetUtil.loadProperties(uri)["key"])
            assertEquals(1600000001000L, netUrlLastModified(uri))
            assertFailsWith<okio.FileNotFoundException> { netCheckResource(KUri("jar:file:$file!/unicode.properties")) }
            assertContentEquals("key=value\n".encodeToByteArray(), IOUtil.getZipEntry("$file!unicode.properties"))
        } finally { unlink(file); rmdir(root) }
    }

    @Test fun installedJarUsesEntryBodyCentralSizeAndDosTimestamp() = memScoped {
        val template = (platform.Foundation.NSTemporaryDirectory() + "ksword-jar-XXXXXX").cstr.getPointer(this)
        val root = mkdtemp(template)!!.toKString()
        val file = "$root/input.jar"
        try {
            val bytes = okio.Buffer()
            StoredZipWriter.write(bytes, listOf("exact.properties" to "key=value\n".encodeToByteArray()))
            val fixture = bytes.readByteArray()
            // 2020-01-02 03:04:06, interpreted in the current default time zone like JarEntry.
            val central = (0..fixture.size - 4).first { i -> fixture[i] == 80.toByte() && fixture[i + 1] == 75.toByte() && fixture[i + 2] == 1.toByte() && fixture[i + 3] == 2.toByte() }
            fixture[central + 12] = 0x83.toByte(); fixture[central + 13] = 0x18
            fixture[central + 14] = 0x22; fixture[central + 15] = 0x50
            NativeLexicalFiles.sink(file).use { sink -> val buffer = okio.Buffer().write(fixture); sink.write(buffer, buffer.size) }
            val uri = KUri("jar:file:$file!/exact.properties")
            netCheckResource(uri)
            assertEquals("value", NetUtil.loadProperties(uri)["key"])
            assertEquals(10, netUrlSize(uri))
            val date = alloc<tm>().apply { tm_year = 120; tm_mon = 0; tm_mday = 2; tm_hour = 3; tm_min = 4; tm_sec = 6; tm_isdst = -1 }
            assertEquals(mktime(date.ptr) * 1000L, netUrlLastModified(uri))
            assertFailsWith<okio.FileNotFoundException> { netCheckResource(KUri("jar:file:$file!/missing")) }
            assertEquals(-1, netUrlSize(KUri("jar:file:$file!/missing")))
            assertFailsWith<NetMalformedURLException> { netCheckResource(KUri("jar:file:$file!missing")) }
            assertFailsWith<okio.IOException> { IOUtil.getZipEntry("$root/missing.zip") }
            assertContentEquals("key=value\n".encodeToByteArray(), IOUtil.getZipEntry("$file!!exact.properties!ignored"))
        } finally { unlink(file); rmdir(root) }
    }
}
