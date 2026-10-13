package org.crosswire.common.util

import java.io.File
import java.net.URI
import java.util.zip.CRC32
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class InstalledJarCharacterizationTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun archive(method: Int): File = File(tmp.root, "installed $method.jar").also { file ->
        ZipArchiveOutputStream(file).use { zip ->
            val body = "key=first\n".encodeToByteArray()
            val entry = ZipArchiveEntry("exact/space name.properties").apply {
                this.method = method; size = body.size.toLong()
                crc = CRC32().apply { update(body) }.value
                time = 1577934246000L
            }
            zip.putArchiveEntry(entry); zip.write(body); zip.closeArchiveEntry()
        }
        file.setLastModified(1700000000000L)
    }

    @Test fun installedStoredAndDeflatedJarUseExactCentralEntryBodySizeAndTime() {
        for (method in listOf(0, 8)) {
            val file = archive(method)
            val uri = URI("jar:${file.toURI()}!/exact/space%20name.properties")
            netCheckResource(uri)
            assertEquals("first", NetUtil.loadProperties(uri)["key"])
            assertEquals(10, netUrlSize(uri))
            val expectedTime = java.util.jar.JarFile(file).use { it.getJarEntry("exact/space name.properties").time }
            assertEquals(expectedTime, netUrlLastModified(uri))
            assertNotEquals(file.lastModified(), netUrlLastModified(uri))
        }
    }

    @Test fun missingEntryAndMalformedJarRetainPerOperationErrorPolicy() {
        val file = archive(0)
        val missing = URI("jar:${file.toURI()}!/missing")
        assertFailsWith<java.io.FileNotFoundException> { netCheckResource(missing) }
        assertFailsWith<java.io.FileNotFoundException> { netLoadProperties(missing) }
        assertEquals(-1, netUrlSize(missing))
        assertFailsWith<java.io.FileNotFoundException> { netUrlLastModified(missing) }
        assertEquals(-1, NetUtil.getSize(missing))
        val malformed = URI("jar:${file.toURI()}!missing")
        assertFailsWith<java.net.MalformedURLException> { netCheckResource(malformed) }
        assertEquals(0, NetUtil.getSize(malformed))
    }

    @Test fun jarIgnoresUnicodePathExtraAndPrefersExtendedCentralTimestamp() {
        val file = File(tmp.root, "extras.jar")
        ZipArchiveOutputStream(file).use { zip ->
            val entry = ZipArchiveEntry("legacy.properties").apply {
                time = 1577934246000L
                addExtraField(org.apache.commons.compress.archivers.zip.UnicodePathExtraField("unicode.properties", "legacy.properties".encodeToByteArray()))
                addExtraField(org.apache.commons.compress.archivers.zip.X5455_ExtendedTimestamp().apply {
                    modifyTime = org.apache.commons.compress.archivers.zip.ZipLong(1600000001L)
                })
            }
            zip.putArchiveEntry(entry); zip.write("key=value\n".encodeToByteArray()); zip.closeArchiveEntry()
        }
        val bytes = file.readBytes()
        bytes[7] = (bytes[7].toInt() and 0xf7).toByte()
        val central = (0..bytes.size - 4).first { i -> bytes[i] == 80.toByte() && bytes[i + 1] == 75.toByte() && bytes[i + 2] == 1.toByte() && bytes[i + 3] == 2.toByte() }
        bytes[central + 9] = (bytes[central + 9].toInt() and 0xf7).toByte(); file.writeBytes(bytes)
        val uri = URI("jar:${file.toURI()}!/legacy.properties")
        assertEquals("value", NetUtil.loadProperties(uri)["key"])
        assertEquals(1600000001000L, netUrlLastModified(uri))
        assertFailsWith<java.io.FileNotFoundException> { netCheckResource(URI("jar:${file.toURI()}!/unicode.properties")) }
        assertContentEquals("key=value\n".encodeToByteArray(), IOUtil.getZipEntry(file.path + "!unicode.properties"))
        val parsed = CentralZipArchive(bytes) { raw, _ -> String(raw, Charsets.UTF_8).replace('\\', '/') }
        assertNotNull(parsed.jarEntry("legacy.properties"))
        assertNull(parsed.jarEntry("unicode.properties"))
    }

    @Test fun jarTimestampExtraOrderAndSignedUnixSecondsMatchRealJdk() {
        fun field(tag: Int, body: ByteArray) = okio.Buffer().writeShortLe(tag).writeShortLe(body.size).write(body).readByteArray()
        fun unix(seconds: Int) = field(0x5455, okio.Buffer().writeByte(1).writeIntLe(seconds).readByteArray())
        fun ntfs(millis: Long) = field(10, okio.Buffer().writeIntLe(0).writeShortLe(1).writeShortLe(24)
            .writeLongLe((millis + 11644473600000L) * 10000).writeLongLe(Long.MIN_VALUE).writeLongLe(Long.MIN_VALUE).readByteArray())
        for ((index, case) in listOf(unix(-1) to -1000L, ntfs(1500000000123L) to 1500000000123L,
            (ntfs(1500000000123L) + unix(1600000001)) to 1600000001000L,
            (unix(1600000001) + ntfs(1500000000123L)) to 1500000000123L).withIndex()) {
            val buffer = okio.Buffer(); StoredZipWriter.write(buffer, listOf("time" to byteArrayOf(1)))
            val original = buffer.readByteArray()
            val central = (0..original.size - 4).first { i -> original[i] == 80.toByte() && original[i + 1] == 75.toByte() && original[i + 2] == 1.toByte() && original[i + 3] == 2.toByte() }
            val nameEnd = central + 50
            val bytes = original.copyOfRange(0, nameEnd) + case.first + original.copyOfRange(nameEnd, original.size)
            bytes[central + 30] = case.first.size.toByte()
            val end = bytes.size - 22; repeat(4) { bytes[end + 12 + it] = ((50 + case.first.size) ushr (8 * it)).toByte() }
            val file = File(tmp.root, "time$index.jar").apply { writeBytes(bytes) }
            assertEquals(case.second, netUrlLastModified(URI("jar:${file.toURI()}!/time")))
            val parsed = CentralZipArchive(bytes) { raw, _ -> raw.decodeToString() }
            val getter = parsed.jarEntry("time")!!::class.java.getMethod("getJarModifiedMillis")
            assertEquals(case.second, getter.invoke(parsed.jarEntry("time")))
        }
    }

    @Test fun duplicateJarLookupUsesActualJdkWinnerNotCommonsFirstEntry() {
        val file = File(tmp.root, "duplicates.jar")
        ZipArchiveOutputStream(file).use { zip ->
            for ((index, body) in listOf("key=first\n", "key=second\n").withIndex()) {
                val entry = ZipArchiveEntry("same.properties").apply { time = 1577934246000L + index * 2000 }
                zip.putArchiveEntry(entry); zip.write(body.encodeToByteArray()); zip.closeArchiveEntry()
            }
        }
        val uri = URI("jar:${file.toURI()}!/same.properties")
        val expected = java.util.jar.JarFile(file).use { jar ->
            val entry = jar.getJarEntry("same.properties")
            Triple(jar.getInputStream(entry).use { it.readBytes() }, entry.size.toInt(), entry.time)
        }
        assertContentEquals(expected.first, NetUtilJvm.getInputStream(uri).use { it.readBytes() })
        assertEquals(expected.second, netUrlSize(uri)); assertEquals(expected.third, netUrlLastModified(uri))
        assertEquals("second", NetUtil.loadProperties(uri)["key"])
        assertContentEquals("key=first\n".encodeToByteArray(), IOUtil.getZipEntry(file.path + "!same.properties"))
    }

    @Test fun literalSplitOpensArchiveBeforeMissingEntryIndexAndIgnoresEmptySeparators() {
        val file = archive(0)
        assertFailsWith<java.io.IOException> { IOUtil.getZipEntry(File(tmp.root, "missing.zip").path) }
        assertFailsWith<java.io.IOException> { IOUtil.getZipEntry(file.path + "!") }
        assertFailsWith<IndexOutOfBoundsException> { IOUtil.getZipEntry(file.path) }
        assertContentEquals("key=first\n".encodeToByteArray(), IOUtil.getZipEntry(file.path + "!!exact/space name.properties!ignored"))
    }
}
