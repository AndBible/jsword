package org.crosswire.jsword.book.install

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.crosswire.common.util.IOUtil
import org.crosswire.common.util.IOUtilJvm
import org.crosswire.jsword.book.install.sword.HttpSwordInstaller
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** These tests run the real pre-port entry points; the cache is frozen before replacement. */
class ArchiveLegacyCharacterizationTest {
    private class Installer : HttpSwordInstaller() {
        fun expand(dir: File, input: File) = unpack(dir.toURI(), input.toURI())
    }

    @Test fun siblingPrefixDirectoryIsStillRejected() {
        val tmp = Files.createTempDirectory("archive-sibling").toFile()
        try {
            val dest = File(tmp, "dest").apply { mkdirs() }
            File(tmp, "dest-evil").mkdirs()
            val error = kotlin.runCatching {
                archiveExtract(dest.path, "../dest-evil/f", false) { okio.Buffer().writeUtf8("x") }
            }.exceptionOrNull()
            assertTrue(error is java.io.IOException, "expected IOException, got $error")
            assertTrue(error.message!!.contains("escapes destination"), error.message)
            assertTrue(!File(tmp, "dest-evil/f").exists())
        } finally { tmp.deleteRecursively() }
    }

    @Test fun saveActualLegacyCache() {
        val root = Files.createTempDirectory("archive-legacy-cache").toFile()
        try {
            val input = File("jsword/src/jvmTest/resources/k4-archives/mods.d.tar.gz")
                .let { if (it.exists()) it else File("src/jvmTest/resources/k4-archives/mods.d.tar.gz") }
            val cache = File(input.parentFile, "legacy-mods.d.zip")
            ZipFile(cache).use { zip ->
                val entries = zip.entries.toList()
                assertEquals(428, entries.size)
                assertTrue(entries.all { it.method == 8 })
            }
            val frozen = File(input.parentFile, "legacy-mods.d.zip")
            if (!frozen.exists()) cache.copyTo(frozen)
            println("LEGACY CACHE ${cache.length()} bytes; frozen=${frozen.absolutePath}")
        } finally { root.deleteRecursively() }
    }

    @Test fun pinGzipConsumptionAndErrors() {
        val bytes = tarGzip(listOf("mods.d/test.conf" to "[Test]\n".toByteArray()))
        val cases = linkedMapOf(
            "valid" to bytes,
            "trailing" to bytes + byteArrayOf(1, 2, 3),
            "second-member" to bytes + bytes,
            "truncated-trailer" to bytes.copyOf(bytes.size - 8),
            "corrupt-crc" to bytes.copyOf().apply { this[size - 8] = (this[size - 8].toInt() xor 1).toByte() },
        )
        for ((name, input) in cases) {
            val root = Files.createTempDirectory("archive-gzip-$name").toFile()
            try {
                val source = File(root, "in.gz").apply { writeBytes(input) }
                val result = runCatching { Installer().expand(root, source) }
                println("LEGACY GZIP $name: ${result.exceptionOrNull()?.javaClass?.name ?: "success"}")
                if (name in listOf("valid", "trailing", "second-member")) assertTrue(result.isSuccess)
            } finally { root.deleteRecursively() }
        }
    }

    @Test fun pinIncludeExcludeCaseAndLocale() {
        val root = Files.createTempDirectory("archive-filter").toFile()
        val previous = Locale.getDefault()
        try {
            val zip = File(root, "input.zip")
            zip(zip, listOf("Mods.D/Mixed.conf", "MODULES/UPPER", "Images/Icon", "other"))
            for (locale in listOf(Locale.US, Locale.forLanguageTag("tr-TR"))) {
                Locale.setDefault(locale)
                for (include in listOf(true, false)) {
                    val dest = File(root, "${locale.language}-$include").apply { mkdirs() }
                    IOUtilJvm.unpackZip(zip, dest, include, "mods.d", "modules", "images")
                    val names = dest.walkTopDown().filter { it.isFile }.map { it.relativeTo(dest).path }.toList().sorted()
                    val included = if (locale.language == "tr") listOf("MODULES/UPPER", "Mods.D/Mixed.conf") else listOf("Images/Icon", "MODULES/UPPER", "Mods.D/Mixed.conf")
                    assertEquals(if (include) included.sorted() else (listOf("Mods.D/Mixed.conf", "MODULES/UPPER", "Images/Icon", "other") - included.toSet()).sorted(), names)
                }
                val upper = File(root, "${locale.language}-upper").apply { mkdirs() }
                IOUtilJvm.unpackZip(zip, upper, true, "MODS.D")
                assertTrue(upper.listFiles()!!.isEmpty())
            }
        } finally { Locale.setDefault(previous); root.deleteRecursively() }
    }

    @Test fun pinOldZipSlipInsideOwnedRoot() {
        val root = Files.createTempDirectory("archive-slip").toFile()
        try {
            val zip = File(root, "input.zip")
            zip(zip, listOf("../escaped"))
            val dest = File(root, "dest").apply { mkdirs() }
            LegacyArchiveReference.unpackZip(zip, dest)
            assertTrue(File(root, "escaped").exists())
        } finally { root.deleteRecursively() }
    }

    companion object {
        fun tarGzip(entries: List<Pair<String, ByteArray>>): ByteArray {
            val output = ByteArrayOutputStream()
            TarArchiveOutputStream(GZIPOutputStream(output)).use { tar ->
                entries.forEach { (name, bytes) ->
                    tar.putArchiveEntry(TarArchiveEntry(name, true).apply { size = bytes.size.toLong() })
                    tar.write(bytes)
                    tar.closeArchiveEntry()
                }
            }
            return output.toByteArray()
        }

        fun zip(file: File, names: List<String>) {
            ZipArchiveOutputStream(file).use { zip ->
                names.forEach { name ->
                    zip.putArchiveEntry(ZipArchiveEntry(name))
                    zip.write(name.toByteArray())
                    zip.closeArchiveEntry()
                }
            }
        }
    }
}
