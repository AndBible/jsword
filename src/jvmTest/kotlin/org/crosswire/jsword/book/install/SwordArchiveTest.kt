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
        assertEquals(listOf("../evil.conf", "/abs.conf", "mods.d/empty.conf", "mods.d/good.conf"), TarArchiveInputStream(GZIPInputStream(input.inputStream())).use { tar -> buildList { while (true) { val entry = tar.nextTarEntry ?: break; add(entry.name) } } })
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

    @Test fun extractionSkippedFeaturesMatchActualOld() = temp { root ->
        val buffer = okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(buffer,listOf("first.conf" to byteArrayOf(1),"excluded.bin" to byteArrayOf(2)))
        val good = buffer.readByteArray()
        val central = (0..good.size-4).filter { good[it] == 80.toByte() && good[it+1] == 75.toByte() && good[it+2] == 1.toByte() && good[it+3] == 2.toByte() }.last()
        for ((offset,value) in listOf(central+8 to 1,central+10 to 99)) {
            val file = File(root,"input.zip").apply { writeBytes(good.copyOf().apply { this[offset] = value.toByte() }) }
            val old = File(root,"old").apply { deleteRecursively(); mkdirs() }; val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpackZip(file,old,true,"first") }
            val actual = runCatching { SwordArchive.unpackZip(file.path.toPath(),new.path.toPath(),true,"first") }
            assertTrue(reference.isSuccess)
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass)
            assertEquals(tree(old),tree(new))
        }
    }

    @Test fun installerTarPermissiveHeaderEofMatchesActualOld() = temp { root ->
        val entry = org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/a.conf",byteArrayOf(1))
        for ((name,tail) in listOf("none" to byteArrayOf(),"short" to ByteArray(20),"one" to ByteArray(512),"short-second" to ByteArray(532),"nonzero-second" to (ByteArray(512)+ByteArray(512) { 42 }))) {
            val bytes = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(entry+tail) } }.toByteArray()
            val input = File(root,"input.gz").apply { writeBytes(bytes) }
            val old = File(root,"old-$name").apply { mkdirs() }; val new = File(root,"new-$name").apply { mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
            val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
            assertTrue(reference.isSuccess,name)
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,name)
            assertEquals(confs(File(old,"mods.d.zip")),confs(File(new,"mods.d.zip")),name)
        }
    }

    @Test fun cacheOutputOpensBeforeMissingInputAndFinalizesOnError() = temp { root ->
        val input = File(root,"missing.gz")
        for (blocked in listOf(false,true)) {
            val old = File(root,"old-$blocked").apply { mkdirs() }; val new = File(root,"new-$blocked").apply { mkdirs() }
            if (blocked) { File(old,"mods.d.zip").mkdir(); File(new,"mods.d.zip").mkdir() }
            else { File(old,"mods.d.zip").writeText("old cache"); File(new,"mods.d.zip").writeText("old cache") }
            val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
            val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass)
            assertTrue(reference.exceptionOrNull() is java.io.FileNotFoundException)
            if (blocked) {
                assertTrue(actual.exceptionOrNull()!!.message!!.contains("mods.d.zip"))
            } else assertEquals(confs(File(old,"mods.d.zip")),confs(File(new,"mods.d.zip")))
        }
    }

    @Test fun corruptTarOutputAndPartialCacheMatchFrozenLegacy() = temp { root ->
        val complete = org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/first.conf",byteArrayOf(1))
        val later = org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/later.conf",ByteArray(5000) { 65 })
        val truncated = complete + later.copyOf(512+4300)
        val partial = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(truncated) } }.toByteArray()
        for ((name,bytes) in listOf("bad" to byteArrayOf(1,2,3), "partial" to partial)) {
            val input = File(root,"$name.gz").apply { writeBytes(bytes) }
            val old = File(root,"old-$name").apply { mkdirs() }; val new = File(root,"new-$name").apply { mkdirs() }
            File(old,"mods.d.zip").writeText("previous cache"); File(new,"mods.d.zip").writeText("previous cache")
            val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
            val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,name)
            val oldContents = runCatching { confs(File(old,"mods.d.zip")) }
            val newContents = runCatching { confs(File(new,"mods.d.zip")) }
            println("PARTIAL TAR $name old=${oldContents.getOrNull()?.map { it.first to it.second.size }} new=${newContents.getOrNull()?.map { it.first to it.second.size }}")
            assertEquals(oldContents.exceptionOrNull()?.javaClass,newContents.exceptionOrNull()?.javaClass,name)
            if (oldContents.isSuccess) assertEquals(oldContents.getOrThrow(),newContents.getOrThrow(),name)
        }
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

    @Test fun malformedIgnoredCacheBodyMatchesSequentialLegacy() = temp { root ->
        for (descriptor in listOf(false,true)) {
            val file = File(root,"input.zip")
            fun write(zip: org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream) {
                zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("ignored.txt"))
                zip.write(ByteArray(5000) { 65 }); zip.closeArchiveEntry(); zip.close()
            }
            if (descriptor) {
                val out = java.io.ByteArrayOutputStream(); write(org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(out)); file.writeBytes(out.toByteArray())
            } else write(org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(file))
            val bytes = file.readBytes()
            val nameLength = (bytes[26].toInt() and 255) or ((bytes[27].toInt() and 255) shl 8)
            val extraLength = (bytes[28].toInt() and 255) or ((bytes[29].toInt() and 255) shl 8)
            bytes[30+nameLength+extraLength] = 7
            file.writeBytes(bytes)
            val reference = runCatching { confs(file) }
            val actual = runCatching { SwordArchive.readConfZip(file.path.toPath()) }
            println("IGNORED CACHE descriptor=$descriptor old=${reference.exceptionOrNull()} new=${actual.exceptionOrNull()}")
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,"descriptor=$descriptor")
            if (reference.isSuccess) assertEquals(emptyList(),actual.getOrThrow())
        }
    }

    @Test fun emptyCacheAndStoredLocalMetadataMatchLegacy() = temp { root ->
        val buffer = okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(buffer, emptyList())
        val file = File(root,"input.zip").apply { writeBytes(buffer.readByteArray()) }
        assertEquals(confs(file),SwordArchive.readConfZip(file.path.toPath()).map { it.first to it.second.toList() })
        val stored = okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(stored,listOf("a.conf" to "payload".toByteArray()))
        val good = stored.readByteArray()
        for (offset in listOf(6,8,14,18,22,30)) {
            file.writeBytes(good.copyOf().apply { this[offset] = (this[offset].toInt() xor 1).toByte() })
            val old = File(root,"old").apply { deleteRecursively(); mkdirs() }; val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpackZip(file,old) }
            val actual = runCatching { SwordArchive.unpackZip(file.path.toPath(),new.path.toPath(),true) }
            assertTrue(reference.isSuccess,"legacy offset=$offset ${reference.exceptionOrNull()}")
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,"offset=$offset")
            assertEquals(tree(old),tree(new),"offset=$offset")
        }
    }

    @Test fun extractionIgnoresLegacyAcceptedLocalAndDescriptorMetadata() = temp { root ->
        val output = java.io.ByteArrayOutputStream()
        org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(output).use { zip ->
            zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("a.conf"))
            zip.write("payload".toByteArray()); zip.closeArchiveEntry()
        }
        val good = output.toByteArray()
        fun signature(value: Int) = (0..good.size-4).first { offset -> (0..3).all { good[offset+it] == (value ushr (8*it)).toByte() } }
        val central = signature(0x02014b50); val descriptor = signature(0x08074b50)
        for (offset in listOf(14, 30, descriptor+4, descriptor+8, descriptor+12, central+16, central+24)) {
            val bytes = good.copyOf().apply { this[offset] = (this[offset].toInt() xor 1).toByte() }
            val file = File(root,"input.zip").apply { writeBytes(bytes) }
            val old = File(root,"old").apply { deleteRecursively(); mkdirs() }
            val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpackZip(file,old) }
            val actual = runCatching { SwordArchive.unpackZip(file.path.toPath(),new.path.toPath(),true) }
            assertTrue(reference.isSuccess,"legacy offset=$offset: ${reference.exceptionOrNull()}")
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,"offset=$offset")
            assertEquals(tree(old),tree(new),"offset=$offset")
        }
    }

    @Test fun cacheUsesLocalOrderWhileExtractionUsesCentralOrder() = temp { root ->
        val buffer = okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(buffer, listOf("z.conf" to byteArrayOf(1), "a.conf" to byteArrayOf(2)))
        val bytes = buffer.readByteArray()
        val central = (0..bytes.size-4).first { bytes[it] == 80.toByte() && bytes[it+1] == 75.toByte() && bytes[it+2] == 1.toByte() && bytes[it+3] == 2.toByte() }
        val length = 46 + "z.conf".length
        val first = bytes.copyOfRange(central, central+length)
        bytes.copyInto(bytes, central, central+length, central+2*length)
        first.copyInto(bytes, central+length)
        val file = File(root,"reordered.zip").apply { writeBytes(bytes) }
        assertEquals(listOf("a.conf", "z.conf"), org.apache.commons.compress.archivers.zip.ZipFile(file).use { zip -> java.util.Collections.list(zip.entries).map { it.name } })
        assertEquals(listOf("z.conf", "a.conf"), confs(file).map { it.first })
        assertEquals(confs(file), SwordArchive.readConfZip(file.path.toPath()).map { it.first to it.second.toList() })
        val old = File(root,"old").apply { mkdirs() }; val new = File(root,"new").apply { mkdirs() }
        LegacyArchiveReference.unpackZip(file,old)
        SwordArchive.unpackZip(file.path.toPath(),new.path.toPath(),true)
        assertEquals(tree(old),tree(new))
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
            assertEquals(legacy.exceptionOrNull()?.javaClass, actual.exceptionOrNull()?.javaClass)
            if (legacy.isSuccess) assertEquals(legacy.getOrThrow(), actual.getOrThrow().map { it.first to it.second.toList() })
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
