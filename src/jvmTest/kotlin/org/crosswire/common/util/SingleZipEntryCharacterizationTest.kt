package org.crosswire.common.util

import java.io.File
import java.util.zip.CRC32
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class SingleZipEntryCharacterizationTest {
    @get:Rule val tmp = TemporaryFolder()
    private val conf = "[Demo]\nDescription=Catalog demo\nModDrv=RawText\nDataPath=./modules/texts/rawtext/demo/\nLang=en\nAbout=Retained only after reload\n".encodeToByteArray()

    private fun archive(name: String, method: Int, duplicates: Boolean = false): File {
        val file = File(tmp.root, name)
        ZipArchiveOutputStream(file).use { zip ->
            fun entry(body: ByteArray) {
                val entry = ZipArchiveEntry("mods.d/demo.conf").apply {
                    this.method = method
                    size = body.size.toLong()
                    crc = CRC32().apply { update(body) }.value
                }
                zip.putArchiveEntry(entry); zip.write(body); zip.closeArchiveEntry()
            }
            entry(conf)
            if (duplicates) entry("second".encodeToByteArray())
        }
        return file
    }

    @Test fun centralLookupReadsStoredAndDeflatedCatalogAndMetadataReload() {
        for (method in listOf(ZipArchiveEntry.STORED, ZipArchiveEntry.DEFLATED)) {
            val file = archive("cache$method.zip", method)
            val spec = file.path + "!mods.d/demo.conf"
            assertContentEquals(conf, IOUtil.getZipEntry(spec))
            val metadata = SwordBookMetaData(conf, spec)
            metadata.reload()
            assertEquals("Retained only after reload", metadata.getProperty("About"))
        }
    }

    @Test fun exactNamesFirstCentralDuplicateAndAdditionalBangContract() {
        val file = archive("duplicates.zip", ZipArchiveEntry.STORED, true)
        assertContentEquals(conf, IOUtil.getZipEntry(file.path + "!mods.d/demo.conf"))
        assertContentEquals(conf, IOUtil.getZipEntry(file.path + "!mods.d/demo.conf!ignored"))
        assertFailsWith<NullPointerException> { IOUtil.getZipEntry(file.path + "!./mods.d/demo.conf") }
        assertFailsWith<NullPointerException> { IOUtil.getZipEntry(file.path + "!missing") }
        assertFailsWith<IndexOutOfBoundsException> { IOUtil.getZipEntry(file.path) }
    }
}
