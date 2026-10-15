package org.crosswire.ksword.k5b

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import java.io.ByteArrayOutputStream
import java.io.File

/** Deterministic SWORD catalog (`mods.d.tar.gz`) over the K4 fixture `.conf` files. */
object K5bCatalogFixture {
    fun bytes(confDir: File): ByteArray {
        val out = ByteArrayOutputStream()
        val gz = GzipCompressorOutputStream(out, GzipParameters().apply { modificationTime = 0 })
        TarArchiveOutputStream(gz).use { tar ->
            confDir.listFiles { f -> f.name.endsWith(".conf") }!!.sortedBy { it.name }.forEach { f ->
                val data = f.readBytes()
                val e = TarArchiveEntry("mods.d/" + f.name).apply {
                    size = data.size.toLong(); setModTime(0L); userId = 0; groupId = 0; mode = 0b110_100_100
                }
                tar.putArchiveEntry(e); tar.write(data); tar.closeArchiveEntry()
            }
        }
        return out.toByteArray()
    }

    /** The K4 fixture directory holding the 15 `.conf` files. */
    fun confDir(): File = File(checkNotNull(System.getProperty("ksword.fixturesDir")), "space dir/ä/mods.d")

    /** The committed fixture, as a test resource. */
    fun committedBytes(): ByteArray =
        checkNotNull(K5bCatalogFixture::class.java.getResourceAsStream("/k5b/mods.d.tar.gz")) { "missing k5b/mods.d.tar.gz" }.use { it.readBytes() }
}
