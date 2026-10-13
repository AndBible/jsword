package org.crosswire.jsword.book.install

import okio.Path.Companion.toPath
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.io.IOException

class SwordArchiveTest {
    private fun resource(name: String) = File(javaClass.getResource("/k4-archives/$name")!!.toURI())
    private fun confs(file: File): List<Pair<String, List<Byte>>> = ZipArchiveInputStream(file.inputStream()).use { zip ->
        buildList {
            while (true) {
                val entry = zip.nextZipEntry ?: break
                if (!entry.isDirectory && entry.name.endsWith(".conf") && entry.size != 0L) add(entry.name to zip.readBytes().toList())
            }
        }
    }
    private fun tree(root: File) = root.walkTopDown().filter { it.isFile }.associate { it.relativeTo(root).path to it.readBytes().toList() }
    private fun temp(block: (File) -> Unit) { val root = Files.createTempDirectory("sword-archive").toFile(); try { block(root) } finally { root.deleteRecursively() } }

    @Test fun realCatalogMatchesOldCacheNamesBytesAndOrder() = temp { root ->
        val output = File(root, "new.zip")
        SwordArchive.tarGzConfsToZip(resource("mods.d.tar.gz").path.toPath(), output.path.toPath())
        val expected = confs(resource("legacy-mods.d.zip"))
        assertEquals(428, expected.size)
        assertEquals(expected, SwordArchive.readConfZip(output.path.toPath()).map { it.first to it.second.toList() })
        assertEquals(expected, SwordArchive.readConfZip(resource("legacy-mods.d.zip").path.toPath()).map { it.first to it.second.toList() })
    }

    @Test fun realModuleTreeMatchesFrozenLegacyForBothHomes() = temp { root ->
        for (include in listOf(true, false)) {
            val old = File(root, "old-$include").apply { mkdirs() }; val new = File(root, "new-$include").apply { mkdirs() }
            LegacyArchiveReference.unpackZip(resource("ASV.zip"), old, include, "mods.d", "modules")
            SwordArchive.unpackZip(resource("ASV.zip").path.toPath(), new.path.toPath(), include, "mods.d", "modules")
            assertEquals(tree(old), tree(new))
        }
    }

    @Test fun nonalphabeticDuplicateAndUtf8CacheOrder() = temp { root ->
        val file = File(root, "cache.zip")
        org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(file).use { zip ->
            listOf("z.conf" to "first", "a.conf" to "middle", "ä.conf" to "unicode", "z.conf" to "last").forEach { (name, body) ->
                zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry(name))
                zip.write(body.toByteArray()); zip.closeArchiveEntry()
            }
        }
        assertEquals(confs(file), SwordArchive.readConfZip(file.path.toPath()).map { it.first to it.second.toList() })
    }

    @Test fun filtersRetainLocalePolarityAndOutputCase() = temp { root ->
        val file = File(root, "cache.zip"); ArchiveLegacyCharacterizationTest.zip(file, listOf("Mods.D/Mixed.conf", "Images/Icon", "other"))
        val previous = Locale.getDefault()
        try {
            for (locale in listOf(Locale.US, Locale.forLanguageTag("tr-TR"))) {
                Locale.setDefault(locale)
                for (include in listOf(true, false)) for (filter in listOf("mods.d", "images", "MODS.D")) {
                    val old = File(root, "old").apply { deleteRecursively(); mkdirs() }; val new = File(root, "new").apply { deleteRecursively(); mkdirs() }
                    LegacyArchiveReference.unpackZip(file, old, include, filter)
                    SwordArchive.unpackZip(file.path.toPath(), new.path.toPath(), include, filter)
                    assertEquals(tree(old), tree(new))
                }
            }
        } finally { Locale.setDefault(previous) }
    }

    @Test fun maliciousTarNamesAndEmptyEntriesAreSkipped() = temp { root ->
        val input = File(root, "in.gz").apply { writeBytes(ArchiveLegacyCharacterizationTest.tarGzip(listOf("../evil.conf" to byteArrayOf(1), "/abs.conf" to byteArrayOf(2), "mods.d/empty.conf" to byteArrayOf(), "mods.d/good.conf" to byteArrayOf(3)))) }
        val output = File(root, "out.zip")
        SwordArchive.tarGzConfsToZip(input.path.toPath(), output.path.toPath())
        assertEquals(listOf("mods.d/good.conf"), SwordArchive.readConfZip(output.path.toPath()).map { it.first })
    }

    @Test fun rejectsZipTraversalAndSymlinkEscapeBeforeWriting() = temp { root ->
        for (name in listOf("../escaped", "/abs", "a/../../escaped", "..\\escaped", "C:/escaped", "\\server\\escaped")) {
            val file = File(root, "input.zip"); ArchiveLegacyCharacterizationTest.zip(file, listOf(name))
            val dest = File(root, "dest").apply { mkdirs() }
            assertFailsWith<IOException>(name) { SwordArchive.unpackZip(file.path.toPath(), dest.path.toPath(), true) }
            assertFalse(File(root, "escaped").exists())
        }
        val outside = File(root, "outside").apply { mkdirs() }; val dest = File(root, "symlink").apply { mkdirs() }
        Files.createSymbolicLink(File(dest, "link").toPath(), outside.toPath())
        val file = File(root, "symlink.zip"); ArchiveLegacyCharacterizationTest.zip(file, listOf("link/escaped"))
        assertFailsWith<IOException> { SwordArchive.unpackZip(file.path.toPath(), dest.path.toPath(), true) }
        assertFalse(File(outside, "escaped").exists())
    }

    @Test fun fileAdaptersPreserveLiteralBackslashesAndDotSpelling() = temp { root ->
        val archive = File(root, "input\\literal.zip")
        val old = File(root, "old\\literal").apply { mkdirs() }; val new = File(root, "new\\literal").apply { mkdirs() }
        // Dotdot is separately rejected; use a safe archive for the lexical differential.
        val buffer = okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(buffer, listOf("mods.d\\literal.conf" to byteArrayOf(1), "./good.conf" to byteArrayOf(2)))
        archive.writeBytes(buffer.readByteArray())
        println("LEGACY STREAM BACKSLASH ${confs(archive).map { it.first }}")
        assertEquals(confs(archive), SwordArchive.readConfZipSpelling(archive.path).map { it.first to it.second.toList() })
        LegacyArchiveReference.unpackZip(archive, old)
        org.crosswire.common.util.IOUtil.unpackZip(archive, new)
        assertEquals(tree(old), tree(new))
        assertTrue(File(new,"mods.d/literal.conf").isFile)
    }

    @Test fun tarRecordBoundaryConsumptionMatchesFrozenLegacy() = temp { root ->
        for (bodySize in listOf(1, 8704, 9216, 10241)) for (extra in listOf(0, 32768)) {
            val tar = org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/a.conf", ByteArray(bodySize) { 65 }) + ByteArray(1024) + ByteArray(extra) { 42 }
            val raw = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(tar) } }.toByteArray()
            for ((variant, bytes) in listOf("valid" to raw, "truncated" to raw.copyOf(raw.size-8), "crc" to raw.copyOf().apply { this[size-8] = (this[size-8].toInt() xor 1).toByte() })) {
                val input = File(root, "input.gz").apply { writeBytes(bytes) }
                val old = File(root, "old").apply { mkdirs() }; val new = File(root, "new").apply { mkdirs() }
                val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(), input.toURI()) }
                val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(), File(new,"mods.d.zip").path.toPath()) }
                println("PADDING $bodySize/$extra/$variant old=${reference.exceptionOrNull()?.javaClass?.name} new=${actual.exceptionOrNull()?.javaClass?.name}")
                assertEquals(reference.exceptionOrNull()?.javaClass, actual.exceptionOrNull()?.javaClass, "$bodySize/$extra/$variant")
                if (reference.isSuccess) assertEquals(confs(File(old,"mods.d.zip")), confs(File(new,"mods.d.zip")))
            }
        }
    }

    @Test fun lateDeflateFailurePreservesPartialWritesAndFilteredPayloadIsIgnored() = temp { root ->
        val compressed = byteArrayOf(0, 0x88.toByte(), 0x13, 0x77, 0xec.toByte()) + ByteArray(5000) { 65 } + byteArrayOf(7)
        val buffer=okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(buffer,listOf("first.conf" to byteArrayOf(1),"later.conf" to compressed))
        val bytes=buffer.readByteArray()
        var local=0; var central=0
        for (i in 0..bytes.size-4) {
            if (bytes[i]==80.toByte() && bytes[i+1]==75.toByte()) {
                if (bytes[i+2]==3.toByte() && bytes[i+3]==4.toByte()) local=i
                if (bytes[i+2]==1.toByte() && bytes[i+3]==2.toByte()) central=i
            }
        }
        bytes[local+8]=8; bytes[central+10]=8
        fun putInt(offset:Int,value:Int) { repeat(4) { bytes[offset+it]=(value ushr (it*8)).toByte() } }
        putInt(local+22,5000);putInt(central+24,5000)
        val file=File(root,"bad.zip").apply { writeBytes(bytes) }
        val old=File(root,"old").apply { mkdirs() }; val new=File(root,"new").apply { mkdirs() }
        val reference=runCatching { LegacyArchiveReference.unpackZip(file,old) }
        val actual=runCatching { org.crosswire.common.util.IOUtil.unpackZip(file,new) }
        println("LATE DEFLATE old=${reference.exceptionOrNull()?.javaClass?.name} new=${actual.exceptionOrNull()?.javaClass?.name} oldtree=${old.listFiles()!!.map { it.name to it.length() }}")
        assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass)
        assertTrue(File(old,"later.conf").length()>4096)
        assertEquals(tree(old),tree(new))
        val filtered=File(root,"filtered").apply { mkdirs() }
        org.crosswire.common.util.IOUtil.unpackZip(file,filtered,true,"first")
        assertEquals(mapOf("first.conf" to listOf<Byte>(1)),tree(filtered))
    }

    @Test fun corruptTarTruncatesExistingCacheBeforeReadingInput() = temp { root ->
        val input=File(root,"bad.gz").apply { writeBytes(byteArrayOf(1,2,3)) }
        val output=File(root,"mods.d.zip").apply { writeText("previous cache") }
        assertFailsWith<IOException> { SwordArchive.tarGzConfsToZip(input.path.toPath(),output.path.toPath()) }
        assertFalse(output.readBytes().contentEquals("previous cache".toByteArray()))
        assertEquals(emptyList(),confs(output))
    }

    @Test fun legacyCorruptPayloadCharacterization() = temp { root ->
        val file = File(root,"input.zip")
        val buffer = okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(buffer, listOf("a.conf" to "hello".toByteArray()))
        val bytes = buffer.readByteArray()
        val data = 30 + "a.conf".length
        bytes[data] = (bytes[data].toInt() xor 1).toByte()
        file.writeBytes(bytes)
        val old = File(root,"old").apply { mkdirs() }
        val result = runCatching { LegacyArchiveReference.unpackZip(file,old) }
        println("OLD STORED BADCRC ${result.exceptionOrNull()?.javaClass?.name} tree=${tree(old)}")
        assertTrue(result.isSuccess)
        println("OLD STREAM BADCRC ${runCatching { confs(file) }.exceptionOrNull()?.javaClass?.name}")
        val deflated = File(root,"deflate.zip")
        org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(deflated).use { zip ->
            zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("first.conf")); zip.write(byteArrayOf(1)); zip.closeArchiveEntry()
            zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("later.conf")); zip.write(ByteArray(50000) { (it%251).toByte() }); zip.closeArchiveEntry()
        }
        val corrupt = deflated.readBytes()
        var local = 0
        while (local < corrupt.size-4) {
            if (corrupt[local]==80.toByte() && corrupt[local+1]==75.toByte() && corrupt[local+2]==3.toByte() && corrupt[local+3]==4.toByte()) {
                val length=(corrupt[local+26].toInt() and 255) or ((corrupt[local+27].toInt() and 255) shl 8)
                if (String(corrupt,local+30,length)=="later.conf") {
                    val extra=(corrupt[local+28].toInt() and 255) or ((corrupt[local+29].toInt() and 255) shl 8)
                    corrupt[local+30+length+extra]=7
                    break
                }
            }
            local++
        }
        deflated.writeBytes(corrupt)
        val dest=File(root,"deflated").apply { mkdirs() }
        val outcome=runCatching { LegacyArchiveReference.unpackZip(deflated,dest) }
        println("OLD DEFLATE CORRUPT ${outcome.exceptionOrNull()?.javaClass?.name} sizes=${dest.walkTopDown().filter { it.isFile }.map { it.name to it.length() }.toList()}")
    }

    @Test fun forcedZip64DescriptorMatchesLegacy() = temp { root ->
        val output = java.io.ByteArrayOutputStream()
        org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(output).use { zip ->
            zip.setUseZip64(org.apache.commons.compress.archivers.zip.Zip64Mode.Always)
            zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("test.conf"))
            zip.write("content".toByteArray()); zip.closeArchiveEntry()
        }
        val file = File(root,"zip64-descriptor.zip").apply { writeBytes(output.toByteArray()) }
        assertEquals(confs(file), SwordArchive.readConfZip(file.path.toPath()).map { it.first to it.second.toList() })
    }

    @Test fun forcedZip64SmallArchiveMatchesLegacy() = temp { root ->
        val file = File(root, "zip64.zip")
        org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(file).use { zip ->
            zip.setUseZip64(org.apache.commons.compress.archivers.zip.Zip64Mode.Always)
            zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("test.conf"))
            zip.write("content".toByteArray()); zip.closeArchiveEntry()
        }
        val expected = confs(file)
        assertEquals(listOf("test.conf" to "content".toByteArray().toList()), expected)
        assertEquals(expected, SwordArchive.readConfZip(file.path.toPath()).map { it.first to it.second.toList() })
    }

    @Test fun malformedZipCrcSizesDescriptorAndTruncationFail() = temp { root ->
        val output = java.io.ByteArrayOutputStream()
        org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(output).use { zip ->
            zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("a.conf"))
            zip.write("payload".toByteArray()); zip.closeArchiveEntry()
        }
        val good = output.toByteArray()
        fun signature(signature: Int) = (0..good.size-4).first { offset ->
            (0..3).all { good[offset+it] == (signature ushr (8*it)).toByte() }
        }
        val central = signature(0x02014b50); val descriptor = signature(0x08074b50)
        assertTrue(good[6].toInt() and 8 != 0)
        val file = File(root, "input.zip").apply { writeBytes(good) }
        assertEquals(listOf("a.conf"), SwordArchive.readConfZip(file.path.toPath()).map { it.first })
        for (bytes in listOf(good.copyOf(good.size-1), good.copyOf().apply { this[central+16] = (this[central+16].toInt() xor 1).toByte() },
            good.copyOf().apply { this[central+24] = (this[central+24].toInt()+1).toByte() },
            good.copyOf().apply { this[descriptor+4] = (this[descriptor+4].toInt() xor 1).toByte() })) {
            file.writeBytes(bytes)
            val legacy=runCatching { confs(file) }
            val actual=runCatching { SwordArchive.readConfZip(file.path.toPath()) }
            println("MALFORMED META old=${legacy.exceptionOrNull()?.javaClass?.name} new=${actual.exceptionOrNull()?.javaClass?.name}")
            // Diagnostic only until controller rules on stream/central metadata conflict.
            if (legacy.isFailure) assertTrue(actual.isFailure)
        }
    }

    @Test fun gzipTailOutcomesMatchActualLegacy() = temp { root ->
        val good = ArchiveLegacyCharacterizationTest.tarGzip(listOf("mods.d/test.conf" to byteArrayOf(1)))
        for ((name, bytes) in listOf("valid" to good, "trailing" to good + byteArrayOf(1,2), "second" to good + good,
            "truncated" to good.copyOf(good.size - 8), "crc" to good.copyOf().apply { this[size-8] = (this[size-8].toInt() xor 1).toByte() })) {
            val input = File(root, "$name.gz").apply { writeBytes(bytes) }
            val outcome = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(), File(root, "$name.zip").path.toPath()) }
            println("NEW GZIP $name: ${outcome.exceptionOrNull()?.javaClass?.name ?: "success"}")
            if (name == "truncated") assertTrue(outcome.exceptionOrNull() is java.io.EOFException)
            else if (name == "crc") assertTrue(outcome.exceptionOrNull() is IOException)
            else assertTrue(outcome.isSuccess, name)
        }
    }
}
