package org.crosswire.common.util

import java.io.File
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import kotlin.test.*

/** Real central/local disagreements protect Native lookup from installer local-header policy. */
class CentralZipArchiveContractTest {
    @Test fun exactFirstDuplicateLookupRetainsCentralSizeAndBothMetadataSets() {
        val root = java.nio.file.Files.createTempDirectory("central-zip").toFile()
        try {
            val file = File(root, "in.zip")
            ZipArchiveOutputStream(file).use { zip ->
                for (index in 0..1) {
                    val entry = ZipArchiveEntry("mods.d/demo.conf").apply {
                        comment = "entry-$index"; time = 1577934246000L + index * 2000
                        unixMode = 420 + index
                    }
                    zip.putArchiveEntry(entry); zip.write(ByteArray(index + 5) { (65 + index).toByte() }); zip.closeArchiveEntry()
                }
            }
            val bytes = file.readBytes()
            bytes[22] = 1 // Deliberate local declared-size disagreement, ignored by ZipFile.
            val parsed = CentralZipArchive(bytes) { raw, _ -> String(raw, Charsets.UTF_8).replace('\\', '/') }
            ZipFile(file).use { reference ->
                val entries = java.util.Collections.list(reference.entries)
                assertEquals(2, parsed.entries.size)
                for (index in entries.indices) {
                    val expected = entries[index]; val actual = parsed.entries[index]
                    assertEquals(index, actual.centralIndex)
                    assertEquals(expected.name, actual.name)
                    assertEquals(expected.size, actual.size)
                    assertEquals(expected.compressedSize, actual.compressedSize)
                    assertEquals(expected.crc, actual.crc)
                    assertEquals(expected.method, actual.method)
                    assertEquals(expected.rawFlag, actual.flags)
                    assertEquals(expected.platform, actual.platform)
                    assertEquals(expected.comment, actual.comment)
                    assertEquals(expected.internalAttributes, actual.internalAttributes)
                    assertEquals(expected.externalAttributes, actual.externalAttributes)
                    assertContentEquals(expected.rawName, actual.rawName)
                    assertContentEquals(expected.centralDirectoryExtra, actual.centralExtra)
                    assertContentEquals(expected.localFileDataExtra, actual.localExtra)
                }
                assertEquals(0, parsed.entry("mods.d/demo.conf")!!.centralIndex)
                assertNull(parsed.entry("./mods.d/demo.conf"))
                assertNull(parsed.entry("missing"))
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun knownExtraFieldsNormalizeAndUnicodeNamesFollowCommonsCentralLookup() {
        val root = java.nio.file.Files.createTempDirectory("central-extra").toFile()
        try {
            val file = File(root, "in.zip")
            val rawName = "legacy.conf".encodeToByteArray()
            val rawComment = "legacy comment".encodeToByteArray()
            ZipArchiveOutputStream(file).use { zip ->
                val entry = ZipArchiveEntry("legacy.conf").apply {
                    comment = "legacy comment"
                    addExtraField(org.apache.commons.compress.archivers.zip.UnicodePathExtraField("unicode.conf", rawName))
                    addExtraField(org.apache.commons.compress.archivers.zip.UnicodeCommentExtraField("unicode comment", rawComment))
                    addExtraField(org.apache.commons.compress.archivers.zip.X5455_ExtendedTimestamp().apply {
                        modifyTime = org.apache.commons.compress.archivers.zip.ZipLong(1577934246L)
                        accessTime = org.apache.commons.compress.archivers.zip.ZipLong(1577934247L)
                        createTime = org.apache.commons.compress.archivers.zip.ZipLong(1577934248L)
                    })
                    addExtraField(org.apache.commons.compress.archivers.zip.X7875_NewUnix().apply { setUID(1234); setGID(5678) })
                }
                zip.putArchiveEntry(entry); zip.write(byteArrayOf(1)); zip.closeArchiveEntry()
            }
            val bytes = file.readBytes()
            // Enable unicode extras: commons ignores them when the UTF8 flag is set.
            bytes[7] = (bytes[7].toInt() and 0xf7).toByte()
            val central = (0..bytes.size - 4).first { i -> bytes[i] == 80.toByte() && bytes[i + 1] == 75.toByte() && bytes[i + 2] == 1.toByte() && bytes[i + 3] == 2.toByte() }
            bytes[central + 9] = (bytes[central + 9].toInt() and 0xf7).toByte()
            file.writeBytes(bytes)
            val parsed = CentralZipArchive(bytes) { raw, _ -> String(raw, Charsets.UTF_8).replace('\\', '/') }
            ZipFile(file).use { reference ->
                val expected = reference.entries.nextElement(); val actual = parsed.entries.single()
                assertEquals(expected.name, actual.name); assertEquals(expected.comment, actual.comment)
                assertContentEquals(expected.centralDirectoryExtra, actual.centralExtra)
                assertContentEquals(expected.localFileDataExtra, actual.localExtra)
                assertEquals(0, parsed.entry(expected.name)!!.centralIndex)
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun skippedEntriesAreParsedWithoutPrematureUnsupportedFeatureValidation() {
        val buffer = okio.Buffer()
        StoredZipWriter.write(buffer, listOf("first" to byteArrayOf(1), "skip/" to byteArrayOf()))
        val bytes = buffer.readByteArray()
        val central = (0..bytes.size - 4).filter { i -> bytes[i] == 80.toByte() && bytes[i + 1] == 75.toByte() && bytes[i + 2] == 1.toByte() && bytes[i + 3] == 2.toByte() }.last()
        bytes[central + 8] = 1; bytes[central + 10] = 99
        val parsed = CentralZipArchive(bytes) { raw, _ -> raw.decodeToString().replace('\\', '/') }
        assertEquals(listOf("first", "skip/"), parsed.entries.map { it.name })
        assertEquals(99, parsed.entries.last().method)
        assertEquals(1, parsed.entries.last().flags)
    }
}
