package org.crosswire.common.compress

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import okio.Buffer
import okio.Source
import okio.Timeout
import kotlin.test.*

/** Runs the same production framing algorithm used by Native, with a real JDK raw inflater. */
class LegacyGzipSourceContractTest {
    private fun gzip(body: ByteArray) = ByteArrayOutputStream().also { out ->
        GzipCompressorOutputStream(out).use { it.write(body) }
    }.toByteArray()

    private class JdkInflater : RawInflater {
        private val inflater = Inflater(true)
        override val remaining get() = inflater.remaining
        override val finished get() = inflater.finished()
        override val needsInput get() = inflater.needsInput()
        override val totalWritten get() = inflater.bytesWritten
        override fun setInput(bytes: ByteArray, offset: Int, count: Int) = inflater.setInput(bytes, offset, count)
        override fun inflate(bytes: ByteArray, offset: Int, count: Int): Int = try {
            inflater.inflate(bytes, offset, count)
        } catch (error: java.util.zip.DataFormatException) { throw RawDeflateException(error.message) }
        override fun close() = inflater.end()
    }

    @Test fun framingCorpusMatchesRealCommonsErrorsAndFirstMember() {
        val body = "hello".encodeToByteArray()
        val encoded = gzip(body)
        val optional = encoded.copyOf(10).apply { this[3] = 30 } + byteArrayOf(2, 0, 42, 43, 65, 0, 66, 0, 0, 0) + encoded.copyOfRange(10, encoded.size)
        val cases = listOf(encoded, encoded + encoded, encoded + byteArrayOf(7), optional) +
            (0 until optional.size).map { optional.copyOf(it) } +
            listOf(encoded.copyOf().apply { this[size - 8] = (this[size - 8].toInt() xor 1).toByte() },
                encoded.copyOf().apply { this[size - 4] = (this[size - 4].toInt() xor 1).toByte() },
                encoded.copyOf().apply { this[2] = 99 }, encoded.copyOf().apply { this[3] = 32 })
        for ((index, bytes) in cases.withIndex()) {
            val reference = runCatching { GzipCompressorInputStream(bytes.inputStream()).use { it.readBytes() } }
            val actual = runCatching { LegacyGzipSource(Buffer().write(bytes), ::JdkInflater).use { source ->
                val output = Buffer()
                while (source.read(output, 4096) != -1L) Unit
                output.readByteArray()
            } }
            assertEquals(reference.exceptionOrNull()?.javaClass, actual.exceptionOrNull()?.javaClass, "case $index")
            if (reference.isSuccess) assertContentEquals(reference.getOrThrow(), actual.getOrThrow(), "case $index")
            else assertEquals(reference.exceptionOrNull()?.message, actual.exceptionOrNull()?.message, "case $index")
        }
    }

    @Test fun boundedReadAndCloseNeverValidateUnreadSuffix() {
        val body = ByteArray(20000) { (it % 251).toByte() }
        val encoded = gzip(body).apply { this[size - 8] = (this[size - 8].toInt() xor 1).toByte() }
        var closed = 0
        val raw = object : Source {
            val buffer = Buffer().write(encoded)
            override fun read(sink: Buffer, byteCount: Long) = buffer.read(sink, byteCount)
            override fun timeout() = Timeout.NONE
            override fun close() { closed++ }
        }
        LegacyGzipSource(raw, ::JdkInflater).use { source ->
            val output = Buffer()
            assertEquals(512L, source.read(output, 512))
            assertContentEquals(body.copyOf(512), output.readByteArray())
            assertEquals(0L, source.read(output, 0))
            assertEquals(9728L, source.read(output, 9728))
            assertContentEquals(body.copyOfRange(512, 10240), output.readByteArray())
        }
        assertEquals(1, closed)
    }

    @Test fun realInstallerTarAndCacheWriterRetainPaddingFailureAndCompletedEntryBoundaries() {
        val root = java.nio.file.Files.createTempDirectory("portable-gzip-cache").toFile()
        try {
            val record = org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/first.conf", byteArrayOf(1))
            val pax = org.crosswire.common.util.TarReaderTest.tarRecord("extension", "13 comment=a\n".encodeToByteArray(), 'x')
            val cases = listOf("short" to record.copyOf(513), "full" to (record + ByteArray(1024)).copyOf(10240),
                "tail" to ((record + ByteArray(1024)).copyOf(10240) + ByteArray(20000) { 42 }),
                "pax" to (record + pax + ByteArray(1024)).copyOf(10240),
                "later-short" to (record + org.crosswire.common.util.TarReaderTest.tarRecord("mods.d/later.conf", byteArrayOf(2)).copyOf(513)))
            val mismatches = mutableListOf<String>()
            for ((name, tar) in cases) for (variant in listOf("valid", "crc", "truncated")) {
                val encoded = gzip(tar)
                val bytes = when (variant) {
                    "crc" -> encoded.copyOf().apply { this[size - 8] = (this[size - 8].toInt() xor 1).toByte() }
                    "truncated" -> encoded.copyOf(encoded.size - 8)
                    else -> encoded
                }
                val input = java.io.File(root, "input.gz").apply { writeBytes(bytes) }
                val old = java.io.File(root, "old").apply { deleteRecursively(); mkdirs() }
                val expected = runCatching { org.crosswire.jsword.book.install.LegacyArchiveReference.unpack(old.toURI(), input.toURI()) }
                val output = Buffer()
                val writer = org.crosswire.common.util.StoredZipWriter.Writer(output)
                val actual = runCatching {
                    try {
                        org.crosswire.common.util.TarReader(LegacyGzipSource(Buffer().write(bytes), ::JdkInflater)).use { reader ->
                            while (true) {
                                val entry = reader.nextInstaller() ?: break
                                if (!entry.isDirectory && entry.name.endsWith(".conf") && entry.size != 0L) {
                                    writer.beginEntry(entry.name)
                                    val body = try { reader.readInstallerEntry() } catch (error: Throwable) {
                                        try { writer.abortEntry() } catch (abort: Throwable) { error.addSuppressed(abort) }
                                        throw error
                                    }
                                    writer.completeEntry(body)
                                }
                            }
                            reader.consumeLegacyBlockPadding()
                        }
                    } finally { writer.finish() }
                }
                val portableBytes = output.readByteArray()
                val current = java.io.File(root, "current.zip").apply { writeBytes(portableBytes) }
                fun entries(file: java.io.File): Pair<List<Pair<String, List<Byte>>>, Throwable?> {
                    val visible = mutableListOf<Pair<String, List<Byte>>>()
                    val failure = runCatching { org.apache.commons.compress.archivers.zip.ZipArchiveInputStream(file.inputStream()).use { zip ->
                        while (true) {
                            val entry = zip.nextZipEntry ?: break
                            val body = java.io.ByteArrayOutputStream()
                            try {
                                val chunk = ByteArray(4096)
                                while (true) { val count = zip.read(chunk); if (count == -1) break; body.write(chunk, 0, count) }
                            } finally { visible.add(entry.name to body.toByteArray().toList()) }
                        }
                    } }.exceptionOrNull()
                    return visible to failure
                }
                assertEquals(expected.exceptionOrNull()?.javaClass, actual.exceptionOrNull()?.javaClass, "$name/$variant")
                val frozenFile = java.io.File(old, "mods.d.zip")
                val oldCache = entries(frozenFile); val newCache = entries(current)
                println("CACHE DIAGNOSTIC $name/$variant frozen=${oldCache.first}/${oldCache.second} portable=${newCache.first}/${newCache.second}")
                // The frozen File writer is the oracle. Comparing the portable writer with
                // SwordArchive.tarGzConfsToZipSpelling compares the new abort path with itself.
                runCatching {
                    assertEquals(oldCache.first, newCache.first, "$name/$variant cache bodies")
                    assertEquals(oldCache.second?.javaClass, newCache.second?.javaClass, "$name/$variant cache read failure")
                    if (actual.isFailure) assertContentEquals(sharedHeader(orphanHeader(frozenFile.readBytes())), sharedHeader(orphanHeader(portableBytes)), "$name/$variant orphan header")
                }.exceptionOrNull()?.let { mismatches.add(it.message ?: "$name/$variant mismatch") }
            }
            assertTrue(mismatches.isEmpty(), mismatches.joinToString("\n"))
        } finally { root.deleteRecursively() }
    }

    @Test fun constructorFailureClosesInputAndRetainsPrimaryError() {
        val closeError = java.io.IOException("close")
        var closed = 0
        val raw = object : Source {
            override fun timeout() = Timeout.NONE
            override fun read(sink: Buffer, byteCount: Long) = -1L
            override fun close() { closed++; throw closeError }
        }
        val error = assertFailsWith<java.io.IOException> { LegacyGzipSource(raw, ::JdkInflater) }
        assertEquals("Input is not in the .gz format", error.message)
        assertEquals(1, closed)
        assertTrue(error.suppressed.contains(closeError))
    }

    /** The File writer stamps only the DOS time. Every other byte is the orphan-header contract. */
    /** The failed entry's name only. Its header bytes differ by design: the seekable close leaves
     * version 10, method 8 and a ZIP64 extra, which a sequential writer cannot rewrite in place.
     * StoredZipFailureContractTest pins those bytes against the live File writer. */
    private fun orphanHeader(bytes: ByteArray): ByteArray {
        var position = 0
        var last = 0
        while (position + 30 <= bytes.size && bytes[position] == 0x50.toByte() && bytes[position + 1] == 0x4b.toByte() && bytes[position + 2] == 3.toByte()) {
            last = position
            val next = position + 30 + u16(bytes, position + 26) + u16(bytes, position + 28) + u16(bytes, position + 18)
            if (next <= position || next > bytes.size) break
            position = next
        }
        val end = (last + 30 + u16(bytes, last + 26) + u16(bytes, last + 28)).coerceAtMost(bytes.size)
        val header = bytes.copyOfRange(last, end)
        return header
    }

    private fun sharedHeader(header: ByteArray): ByteArray {
        if (header.size < 30) return header
        val end = minOf(30 + u16(header, 26), header.size)
        return header.copyOfRange(30, end)
    }

    private fun u16(bytes: ByteArray, offset: Int) = bytes[offset].toInt() and 0xff or (bytes[offset + 1].toInt() and 0xff shl 8)
}
