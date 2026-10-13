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
        org.crosswire.common.util.IOUtilJvm.unpackZip(archive, new)
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
        val actual=runCatching { org.crosswire.common.util.IOUtilJvm.unpackZip(file,new) }
        println("LATE DEFLATE old=${reference.exceptionOrNull()?.javaClass?.name} new=${actual.exceptionOrNull()?.javaClass?.name} oldtree=${old.listFiles()!!.map { it.name to it.length() }}")
        assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass)
        assertTrue(File(old,"later.conf").length()>4096)
        assertEquals(tree(old),tree(new))
        val filtered=File(root,"filtered").apply { mkdirs() }
        org.crosswire.common.util.IOUtilJvm.unpackZip(file,filtered,true,"first")
        assertEquals(mapOf("first.conf" to listOf<Byte>(1)),tree(filtered))
    }

    private class CacheInstaller(private val root: File) : org.crosswire.jsword.book.install.sword.HttpSwordInstaller() {
        override fun getCachedIndexDir() = root.toURI()
        override fun getCachedIndexFile() = File(root,"mods.d.tar.gz").toURI()
        fun load() = loadCachedIndex()
        fun snapshot() = entries.values.map { it.initials }.sorted()
    }

    @Test fun actualInstallerRetainsEarlierBooksOnLaterTailFailure() = temp { root ->
        val body = "[First]\nDescription=First book\nModDrv=RawText\nDataPath=./modules/texts/rawtext/first/\nLang=en\n".toByteArray()
        val buffer = okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(buffer,listOf("first.conf" to body))
        val file = File(root,"mods.d.zip").apply { writeBytes(buffer.readByteArray().let { it.copyOf(it.size-1) }) }
        File(root,"mods.d.tar.gz").writeBytes(byteArrayOf(1))
        val expected = mutableListOf<String>()
        val reference = runCatching { LegacyArchiveReference.loadConfZip(file) { name,bytes ->
            val metadata = org.crosswire.jsword.book.sword.SwordBookMetaData(bytes,file.path+'!'+name)
            metadata.setDriver(org.crosswire.jsword.book.sword.SwordBookDriver.instance())
            if (metadata.isSupported) expected.add(metadata.initials)
        } }
        val installer = CacheInstaller(root)
        val actual = runCatching { installer.load() }
        assertEquals(listOf("First"),expected)
        assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass)
        assertEquals(expected,installer.snapshot())
    }

    @Test fun actualVisitorClosesOwnedSourceOnEveryExitWithoutAdvancingCallbackFailure() = temp { root ->
        val callbackError = IllegalStateException("callback")
        val closeError = IOException("close")
        for (mode in listOf("success","callback","read","advance")) for (failClose in listOf(false,true)) {
            val body = "prefix".toByteArray(); val size = body.size
            val payload = byteArrayOf(0,(size+200).toByte(),0,(size+200).inv().toByte(),0xff.toByte()) + body + ByteArray(200) { 32 } + byteArrayOf(7)
            val buffer = okio.Buffer()
            org.crosswire.common.util.StoredZipWriter.write(buffer,listOf("a.conf" to if (mode == "success") body else payload))
            val bytes = buffer.readByteArray()
            if (mode != "success") {
                bytes[8]=8
                repeat(4) { bytes[22+it] = (size ushr (8*it)).toByte() }
                if (mode == "read") bytes[30+"a.conf".length]=7
            }
            val file = File(root,"input.zip").apply { writeBytes(bytes) }
            var closed = 0; var visited = false
            val result = runCatching { SwordArchive.visitConfZipSpelling(file.path, { source ->
                object : okio.ForwardingSource(source) {
                    override fun close() { closed++; super.close(); if (failClose) throw closeError }
                }
            }) { _,_ -> visited = true; if (mode == "callback") throw callbackError } }
            assertEquals(1,closed,"mode=$mode/failClose=$failClose")
            if (mode == "callback") assertEquals(callbackError,result.exceptionOrNull())
            if (mode in listOf("read","advance")) assertTrue(result.exceptionOrNull() is java.util.zip.ZipException)
            if (mode == "success") assertEquals(if (failClose) closeError else null,result.exceptionOrNull())
            assertEquals(mode != "read",visited)
            if (failClose && mode != "success") assertTrue(result.exceptionOrNull()!!.suppressed.contains(closeError))
        }
    }

    @Test fun installerGnuAndPaxExtensionsFollowRawReadBoundaries() = temp { root ->
        for (pax in listOf(false,true)) {
            val name = "mods.d/"+"a".repeat(140)+".conf"
            val record = "path=$name\n"
            var length = record.length+3
            while ("$length $record".length != length) length = "$length $record".length
            val extension = if (pax) org.crosswire.common.util.TarReaderTest.tarRecord("pax","$length $record".toByteArray(),'x')
                else org.crosswire.common.util.TarReaderTest.tarRecord("././@LongLink",(name+'\u0000').toByteArray(),'L')
            val tar = extension + org.crosswire.common.util.TarReaderTest.tarRecord("short.conf",byteArrayOf(1)) +
                org.crosswire.common.util.TarReaderTest.tarRecord("ignored.txt",byteArrayOf(2)) +
                org.crosswire.common.util.TarReaderTest.tarRecord("last.conf",byteArrayOf(3)) + ByteArray(1024)
            val gzip = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(tar) } }.toByteArray()
            val input = File(root,"input.gz").apply { writeBytes(gzip) }
            val old = File(root,"old").apply { deleteRecursively(); mkdirs() }; val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
            val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
            assertTrue(reference.isSuccess)
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,"pax=$pax")
            val expected = confs(File(old,"mods.d.zip"))
            assertEquals(listOf(name,"last.conf"),expected.map { it.first })
            assertEquals(expected,confs(File(new,"mods.d.zip")),"pax=$pax")
        }
    }

    @Test fun installerIncompleteGnuBodyFailsLikeFrozenLegacy() = incompleteInstallerExtension('L')

    @Test fun installerIncompletePaxBodyFailsLikeFrozenLegacy() = incompleteInstallerExtension('x')

    @Test fun installerTerminalPaxPathFailsLikeFrozenLegacy() = terminalInstallerPax("25 path=mods.d/long.conf\n")

    @Test fun installerTerminalPaxCommentFailsLikeFrozenLegacy() = terminalInstallerPax("13 comment=a\n")

    private fun terminalInstallerPax(record: String) = temp { root ->
        val body = record.toByteArray()
        val fullPadding = (512-body.size%512)%512
        val endings = listOf("no-padding" to byteArrayOf(), "one-padding" to ByteArray(1),
            "short-padding" to ByteArray(100), "full-padding" to ByteArray(fullPadding),
            "one-terminator" to ByteArray(fullPadding+512), "two-terminators" to ByteArray(fullPadding+1024))
        val mismatches = mutableListOf<String>()
        for (earlier in listOf(false,true)) for ((ending,tail) in endings) {
            val extension = org.crosswire.common.util.TarReaderTest.tarRecord("extension",body,'x').copyOf(512+body.size)
            val prefix = if (earlier) org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/first.conf",byteArrayOf(1)) else byteArrayOf()
            val tar = prefix+extension+tail
            val gzip = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(tar) } }.toByteArray()
            val input = File(root,"input.gz").apply { writeBytes(gzip) }
            assertEquals(tar.toList(),GZIPInputStream(input.inputStream()).use { it.readBytes().toList() })
            val old = File(root,"old").apply { deleteRecursively(); mkdirs() }; val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
            val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
            val context = "record=${record.trim()}/earlier=$earlier/ending=$ending"
            // JVM-generated dereference messages are characterized, not part of the portable contract.
            println("TERMINAL PAX $context old=${reference.exceptionOrNull()} new=${actual.exceptionOrNull()}")
            val expected = if (earlier) listOf("mods.d/first.conf" to listOf<Byte>(1)) else emptyList()
            runCatching {
                assertTrue(reference.exceptionOrNull()?.javaClass == NullPointerException::class.java,context)
                assertEquals(expected,confs(File(old,"mods.d.zip")),context)
                assertEquals(expected,confs(File(new,"mods.d.zip")),context)
                assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,context)
            }.exceptionOrNull()?.let { mismatches.add("$context: ${it.message}") }
        }
        assertTrue(mismatches.isEmpty(),"Terminal PAX failure/cache contract was not preserved: $mismatches")
    }

    @Test fun installerTerminalPaxTerminatorRetainsBlockPaddingErrorOrder() = temp { root ->
        val mismatches = mutableListOf<String>()
        for (record in listOf("25 path=mods.d/long.conf\n","13 comment=a\n")) for (earlier in listOf(false,true)) {
            val prefix = if (earlier) org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/first.conf",byteArrayOf(1)) else byteArrayOf()
            val extension = org.crosswire.common.util.TarReaderTest.tarRecord("extension",record.toByteArray(),'x')
            val tar = (prefix+extension+ByteArray(1024)).copyOf(10240)
            val gzip = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(tar) } }.toByteArray()
            for (variant in listOf("valid","truncated","crc")) {
                val bytes = when (variant) {
                    "truncated" -> gzip.copyOf(gzip.size-8)
                    "crc" -> gzip.copyOf().apply { this[size-8] = (this[size-8].toInt() xor 1).toByte() }
                    else -> gzip
                }
                val input = File(root,"input.gz").apply { writeBytes(bytes) }
                val old = File(root,"old").apply { deleteRecursively(); mkdirs() }; val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
                val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
                val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
                val context = "record=${record.trim()}/earlier=$earlier/block-padding/$variant"
                println("TERMINAL PAX $context old=${reference.exceptionOrNull()} new=${actual.exceptionOrNull()}")
                val expected = if (earlier) listOf("mods.d/first.conf" to listOf<Byte>(1)) else emptyList()
                runCatching {
                    val failureClass = when (variant) {
                        "valid" -> NullPointerException::class.java
                        "truncated" -> java.io.EOFException::class.java
                        else -> IOException::class.java
                    }
                    assertTrue(reference.exceptionOrNull()?.javaClass == failureClass,context)
                    assertEquals(expected,confs(File(old,"mods.d.zip")),context)
                    assertEquals(expected,confs(File(new,"mods.d.zip")),context)
                    assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,context)
                }.exceptionOrNull()?.let { mismatches.add("$context: ${it.message}") }
            }
        }
        assertTrue(mismatches.isEmpty(),"Terminal PAX block-padding error order was not preserved: $mismatches")
    }

    @Test fun installerIgnoredPaxWithFollowingEntryStillMatchesFrozenLegacy() = temp { root ->
        val tar = org.crosswire.common.util.TarReaderTest.tarRecord("extension","13 comment=a\n".toByteArray(),'x') +
            org.crosswire.common.util.TarReaderTest.tarRecord("short.conf",byteArrayOf(1)) +
            org.crosswire.common.util.TarReaderTest.tarRecord("ignored.txt",byteArrayOf(2)) +
            org.crosswire.common.util.TarReaderTest.tarRecord("last.conf",byteArrayOf(3)) + ByteArray(1024)
        val gzip = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(tar) } }.toByteArray()
        val input = File(root,"input.gz").apply { writeBytes(gzip) }
        val old = File(root,"old").apply { mkdirs() }; val new = File(root,"new").apply { mkdirs() }
        LegacyArchiveReference.unpack(old.toURI(),input.toURI())
        SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath())
        val expected = listOf("short.conf" to listOf<Byte>(1),"last.conf" to listOf<Byte>(3))
        assertEquals(expected,confs(File(old,"mods.d.zip")))
        assertEquals(expected,confs(File(new,"mods.d.zip")))
    }

    private fun incompleteInstallerExtension(type: Char) = temp { root ->
        val completeBody = if (type == 'L') "mods.d/long.conf\u0000".toByteArray() else "25 path=mods.d/long.conf\n".toByteArray()
        val mismatches = mutableListOf<String>()
        for (earlier in listOf(false,true)) for (partial in listOf(false,true)) {
            val body = if (partial) completeBody else byteArrayOf()
            // The checksum and declared size remain valid; only the extension body is short.
            val extension = org.crosswire.common.util.TarReaderTest.tarRecord("extension",body+byteArrayOf(0),type).copyOf(512+body.size)
            val prefix = if (earlier) org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/first.conf",byteArrayOf(1)) else byteArrayOf()
            val tar = prefix+extension
            val gzip = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(tar) } }.toByteArray()
            val input = File(root,"input.gz").apply { writeBytes(gzip) }
            assertEquals(tar.toList(),GZIPInputStream(input.inputStream()).use { it.readBytes().toList() })
            val old = File(root,"old").apply { deleteRecursively(); mkdirs() }; val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
            val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
            val context = "type=$type/earlier=$earlier/partial=$partial"
            println("INCOMPLETE EXTENSION $context old=${reference.exceptionOrNull()} new=${actual.exceptionOrNull()}")
            val expected = if (earlier) listOf("mods.d/first.conf" to listOf<Byte>(1)) else emptyList()
            assertEquals(expected,confs(File(old,"mods.d.zip")),context)
            assertEquals(expected,confs(File(new,"mods.d.zip")),context)
            assertTrue(reference.exceptionOrNull()?.javaClass == IOException::class.java,context)
            if (reference.exceptionOrNull()?.javaClass != actual.exceptionOrNull()?.javaClass) mismatches.add(context)
        }
        assertTrue(mismatches.isEmpty(),"Legacy IOException was not preserved: $mismatches")
    }

    @Test fun installerCompleteGnuExtensionShortPaddingStillMatchesFrozenLegacy() = temp { root ->
        for (padding in listOf(0,1,100)) {
            val body = "mods.d/long.conf\u0000".toByteArray()
            val extension = org.crosswire.common.util.TarReaderTest.tarRecord("extension",body,'L').copyOf(512+body.size+padding)
            val tar = org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/first.conf",byteArrayOf(1))+extension
            val gzip = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(tar) } }.toByteArray()
            val input = File(root,"input.gz").apply { writeBytes(gzip) }
            val old = File(root,"old").apply { deleteRecursively(); mkdirs() }; val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
            val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
            val context = "GNU/padding=$padding"
            println("COMPLETE EXTENSION $context old=${reference.exceptionOrNull()} new=${actual.exceptionOrNull()}")
            assertTrue(reference.isSuccess,context)
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,context)
            assertEquals(listOf("mods.d/first.conf" to listOf<Byte>(1)),confs(File(old,"mods.d.zip")),context)
            assertEquals(confs(File(old,"mods.d.zip")),confs(File(new,"mods.d.zip")),context)
        }
    }

    @Test fun unsupportedDuplicateEntryRetainsActualCentralMetadata() = temp { root ->
        val file = File(root,"input.zip")
        org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(file).use { zip ->
            for (index in 0..1) {
                val entry = org.apache.commons.compress.archivers.zip.ZipArchiveEntry("duplicate.conf")
                entry.time = 1600000000000L+index*2000; entry.comment = "central-$index"; entry.unixMode = 420+index
                zip.putArchiveEntry(entry); zip.write(ByteArray(index+1) { (index+65).toByte() }); zip.closeArchiveEntry()
            }
        }
        val bytes = file.readBytes()
        val central = (0..bytes.size-4).filter { bytes[it] == 80.toByte() && bytes[it+1] == 75.toByte() && bytes[it+2] == 1.toByte() && bytes[it+3] == 2.toByte() }.last()
        bytes[central+10]=99; file.writeBytes(bytes)
        val old = File(root,"old").apply { mkdirs() }; val new = File(root,"new").apply { mkdirs() }
        val reference = runCatching { LegacyArchiveReference.unpackZip(file,old) }.exceptionOrNull() as org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException
        val actual = runCatching { SwordArchive.unpackZip(file.path.toPath(),new.path.toPath(),true) }.exceptionOrNull() as org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException
        assertEquals(2,reference.entry.size)
        assertEquals(reference.entry.size,actual.entry.size)
        assertEquals(reference.entry.compressedSize,actual.entry.compressedSize)
        assertEquals(reference.entry.crc,actual.entry.crc)
        assertEquals(reference.entry.time,actual.entry.time)
        assertEquals(reference.entry.comment,actual.entry.comment)
        assertEquals(reference.entry.externalAttributes,actual.entry.externalAttributes)
        assertEquals(reference.entry.rawName.toList(),actual.entry.rawName.toList())
        assertEquals(reference.message,actual.message)
        assertEquals(tree(old),tree(new))
    }

    @Test fun installerCompleteBodyShortPaddingMatchesOldWithoutHidingGzipErrors() = temp { root ->
        val complete = org.crosswire.common.util.TarReaderTest.tarRecord("a.conf",byteArrayOf(1)).copyOf(513)
        for (padding in listOf(0,1,100,510)) for (variant in listOf("valid","truncated","crc")) {
            val gzip = java.io.ByteArrayOutputStream().also { out -> java.util.zip.GZIPOutputStream(out).use { it.write(complete+ByteArray(padding)) } }.toByteArray()
            val input = File(root,"input.gz").apply { writeBytes(when (variant) { "truncated" -> gzip.copyOf(gzip.size-8); "crc" -> gzip.copyOf().apply { this[size-8] = (this[size-8].toInt() xor 1).toByte() }; else -> gzip }) }
            val old = File(root,"old").apply { deleteRecursively(); mkdirs() }; val new = File(root,"new").apply { deleteRecursively(); mkdirs() }
            val reference = runCatching { LegacyArchiveReference.unpack(old.toURI(),input.toURI()) }
            val actual = runCatching { SwordArchive.tarGzConfsToZip(input.path.toPath(),File(new,"mods.d.zip").path.toPath()) }
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,"padding=$padding/variant=$variant")
            if (variant == "valid") assertTrue(reference.isSuccess)
            assertEquals(confs(File(old,"mods.d.zip")),confs(File(new,"mods.d.zip")))
        }
    }

    @Test fun actualCallerBoundedPrefixBeforeMalformedSuffixAndThrowingCallback() = temp { root ->
        val body = "[First]\nDescription=First book\nModDrv=RawText\nDataPath=./modules/texts/rawtext/first/\nLang=en\n".toByteArray()
        val length = body.size
        for (suffixSize in listOf(200,5000)) {
        val blockSize = length+suffixSize
        val payload = byteArrayOf(0,blockSize.toByte(),(blockSize ushr 8).toByte(),blockSize.inv().toByte(),(blockSize.inv() ushr 8).toByte()) + body + ByteArray(suffixSize) { 32 } + byteArrayOf(7)
        val buffer = okio.Buffer()
        org.crosswire.common.util.StoredZipWriter.write(buffer,listOf("first.conf" to payload))
        val bytes = buffer.readByteArray()
        val central = (0..bytes.size-4).first { bytes[it] == 80.toByte() && bytes[it+1] == 75.toByte() && bytes[it+2] == 1.toByte() && bytes[it+3] == 2.toByte() }
        bytes[8]=8; bytes[central+10]=8
        repeat(4) { bytes[22+it]=(length ushr (8*it)).toByte(); bytes[central+24+it]=(length ushr (8*it)).toByte() }
        val file = File(root,"mods.d.zip").apply { writeBytes(bytes) }
        File(root,"mods.d.tar.gz").writeBytes(byteArrayOf(1))
        val expected = mutableListOf<String>()
        val reference = runCatching { LegacyArchiveReference.loadConfZip(file) { _,data -> expected.add(data.decodeToString()) } }
        val actualContents = mutableListOf<String>()
        val actual = runCatching { SwordArchive.visitConfZipSpelling(file.path) { _,data -> actualContents.add(data.decodeToString()) } }
        assertEquals(listOf(body.decodeToString()),expected)
        assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass)
        assertEquals(expected,actualContents)
        val installer = CacheInstaller(root); val installed = runCatching { installer.load() }
        assertEquals(reference.exceptionOrNull()?.javaClass,installed.exceptionOrNull()?.javaClass)
        assertEquals(listOf("First"),installer.snapshot())
        val callbackError = IllegalStateException("metadata callback")
        val oldThrow = runCatching { LegacyArchiveReference.loadConfZip(file) { _,_ -> throw callbackError } }
        val newThrow = runCatching { SwordArchive.visitConfZipSpelling(file.path) { _,_ -> throw callbackError } }
        assertEquals(callbackError,oldThrow.exceptionOrNull())
        assertEquals(callbackError,newThrow.exceptionOrNull())
        }
    }

    @Test fun actualInstallerLateBodyAndEarlierMetadataErrorOrder() = temp { root ->
        val valid = "[First]\nDescription=First book\nModDrv=RawText\nDataPath=./modules/texts/rawtext/first/\nLang=en\n".toByteArray()
        for (badFirst in listOf(false,true)) {
            val dir = File(root,"case-$badFirst").apply { mkdirs() }
            val file = File(dir,"mods.d.zip")
            org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(file).use { zip ->
                zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("first.conf")); zip.write(if (badFirst) byteArrayOf(0) else valid); zip.closeArchiveEntry()
                zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("later.conf")); zip.write(ByteArray(100) { 65 }); zip.closeArchiveEntry()
            }
            val bytes = file.readBytes()
            val local = (0..bytes.size-4).filter { bytes[it] == 80.toByte() && bytes[it+1] == 75.toByte() && bytes[it+2] == 3.toByte() && bytes[it+3] == 4.toByte() }.last()
            val name = (bytes[local+26].toInt() and 255) or ((bytes[local+27].toInt() and 255) shl 8)
            val extra = (bytes[local+28].toInt() and 255) or ((bytes[local+29].toInt() and 255) shl 8)
            bytes[local+30+name+extra] = 7; file.writeBytes(bytes)
            File(dir,"mods.d.tar.gz").writeBytes(byteArrayOf(1))
            val expected = mutableListOf<String>()
            val reference = runCatching { LegacyArchiveReference.loadConfZip(file) { path,body ->
                val metadata = org.crosswire.jsword.book.sword.SwordBookMetaData(body,file.path+'!'+path)
                metadata.setDriver(org.crosswire.jsword.book.sword.SwordBookDriver.instance())
                if (metadata.isSupported) expected.add(metadata.initials)
            } }
            val installer = CacheInstaller(dir); val actual = runCatching { installer.load() }
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,"badFirst=$badFirst")
            assertEquals(expected,installer.snapshot(),"badFirst=$badFirst")
            if (!badFirst) assertEquals(listOf("First"),expected)
            println("CALLER ORDER badFirst=$badFirst exception=${reference.exceptionOrNull()} retained=$expected")
        }
    }

    @Test fun actualCacheCallerDeclaredSizeAndDescriptorsMatch() = temp { root ->
        for (declared in listOf(0,3,20)) {
            val file = File(root,"input.zip")
            org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(file).use { zip ->
                zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("a.conf")); zip.write("payload".toByteArray()); zip.closeArchiveEntry()
            }
            val bytes = file.readBytes(); repeat(4) { bytes[22+it] = (declared ushr (8*it)).toByte() }; file.writeBytes(bytes)
            val expected = mutableListOf<Pair<String,List<Byte>>>()
            val reference = runCatching { LegacyArchiveReference.loadConfZip(file) { name,body -> expected.add(name to body.toList()) } }
            val actual = runCatching { buildList { SwordArchive.visitConfZipSpelling(file.path) { name,body -> add(name to body.toList()) } } }
            assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass,"declared=$declared")
            if (reference.isSuccess) assertEquals(expected,actual.getOrThrow(),"declared=$declared")
        }
        val out = java.io.ByteArrayOutputStream()
        org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(out).use { zip -> zip.putArchiveEntry(org.apache.commons.compress.archivers.zip.ZipArchiveEntry("a.conf")); zip.write(byteArrayOf(1)); zip.closeArchiveEntry() }
        val file = File(root,"descriptor.zip").apply { writeBytes(out.toByteArray()) }
        val reference = runCatching { LegacyArchiveReference.loadConfZip(file) { _,_ -> } }
        val actual = runCatching { SwordArchive.visitConfZipSpelling(file.path) { _,_ -> } }
        assertEquals(reference.exceptionOrNull()?.javaClass,actual.exceptionOrNull()?.javaClass)
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
            val selectedOld = runCatching { LegacyArchiveReference.unpackZip(file,old) }
            val selectedNew = runCatching { SwordArchive.unpackZip(file.path.toPath(),new.path.toPath(),true) }
            assertEquals(selectedOld.exceptionOrNull()?.javaClass,selectedNew.exceptionOrNull()?.javaClass)
            val oldError = selectedOld.exceptionOrNull() as org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException
            val newError = selectedNew.exceptionOrNull() as org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException
            assertEquals(oldError.message,newError.message)
            assertEquals(oldError.feature,newError.feature)
            assertEquals(oldError.entry.name,newError.entry.name)
            assertEquals(oldError.entry.method,newError.entry.method)
            assertEquals(oldError.entry.generalPurposeBit.usesEncryption(),newError.entry.generalPurposeBit.usesEncryption())
            assertEquals(oldError.entry.size,newError.entry.size)
            assertEquals(oldError.entry.compressedSize,newError.entry.compressedSize)
            assertEquals(oldError.entry.crc,newError.entry.crc)
            assertEquals(oldError.entry.time,newError.entry.time)
            assertEquals(oldError.entry.rawName.toList(),newError.entry.rawName?.toList())
            assertEquals(oldError.entry.rawFlag,newError.entry.rawFlag)
            assertEquals(oldError.entry.platform,newError.entry.platform)
            assertEquals(oldError.entry.internalAttributes,newError.entry.internalAttributes)
            assertEquals(oldError.entry.externalAttributes,newError.entry.externalAttributes)
            assertEquals(oldError.entry.comment,newError.entry.comment)
            assertEquals(oldError.entry.centralDirectoryExtra.toList(),newError.entry.centralDirectoryExtra.toList())
            assertEquals(oldError.entry.localFileDataExtra.toList(),newError.entry.localFileDataExtra.toList())
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
