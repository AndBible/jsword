package org.crosswire.jsword.book.install

import java.io.ByteArrayOutputStream
import java.io.File
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Independent commons-compress observations of completed entries, partial bodies and read failure. */
internal object FailedCacheEvidence {
    data class Observation(val entries: List<Pair<String, List<Byte>>>, val failureClass: Class<out Throwable>?)

    fun observe(file: File): Observation {
        val visible = mutableListOf<Pair<String, List<Byte>>>()
        val failure = runCatching {
            ZipArchiveInputStream(file.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextZipEntry ?: break
                    val body = ByteArrayOutputStream()
                    try {
                        val chunk = ByteArray(4096)
                        while (true) {
                            val count = zip.read(chunk)
                            if (count == -1) break
                            body.write(chunk, 0, count)
                        }
                    } finally {
                        visible.add(entry.name to body.toByteArray().toList())
                    }
                }
            }
        }.exceptionOrNull()
        return Observation(visible, failure?.javaClass)
    }

    fun assertMatches(reference: File, actual: File, label: String) {
        val expected = observe(reference)
        val observed = observe(actual)
        assertEquals(expected.entries, observed.entries, "$label visible cache bodies")
        assertEquals(expected.failureClass, observed.failureClass, "$label cache read failure")
        val expectedHeader = orphanHeader(reference.readBytes())
        val actualHeader = orphanHeader(actual.readBytes())
        assertEquals(expectedHeader == null, actualHeader == null, "$label orphan presence")
        if (expectedHeader != null) {
            // Independently pinned by StoredZipFailureContractTest: only DOS time varies.
            assertContentEquals(expectedHeader, actualHeader, "$label provisional orphan header")
        }
    }

    /** Walk seekable local records using full 32-bit sizes and their ZIP64 replacements.
     * Completed bodies may be DEFLATED or STORED; they are compared through the reader above,
     * not as compressed bytes. A provisional orphan has zero ZIP64 sizes and ends after its header.
     */
    fun orphanHeader(bytes: ByteArray): ByteArray? {
        var position = 0L
        while (position < bytes.size) {
            val start = position.toInt()
            assertTrue(start + 4 <= bytes.size, "Truncated ZIP signature at $start")
            val signature = unsigned(bytes, start, 4)
            if (signature == 0x02014b50L || signature == 0x06054b50L) return null
            assertEquals(0x04034b50L, signature, "Local ZIP signature at $start")
            assertTrue(start.toLong() + 30 <= bytes.size, "Truncated local header at $start")
            val nameSize = unsigned(bytes, start + 26, 2).toInt()
            val extraSize = unsigned(bytes, start + 28, 2).toInt()
            val end = start + 30 + nameSize + extraSize
            assertTrue(end <= bytes.size, "Truncated local name/extra at $start")
            assertEquals(0L, unsigned(bytes, start + 6, 2) and 8, "Seekable cache must not use descriptors")
            var compressedSize = unsigned(bytes, start + 18, 4)
            val size = unsigned(bytes, start + 22, 4)
            if (compressedSize == 0xffffffffL) {
                var extra = start + 30 + nameSize
                var zip64: Int? = null
                while (extra < end) {
                    assertTrue(extra + 4 <= end, "Truncated extra field")
                    val fieldSize = unsigned(bytes, extra + 2, 2).toInt()
                    assertTrue(extra + 4 + fieldSize <= end, "Truncated extra body")
                    if (unsigned(bytes, extra, 2) == 1L) {
                        val offset = extra + 4 + if (size == 0xffffffffL) 8 else 0
                        assertTrue(offset + 8 <= extra + 4 + fieldSize, "Missing ZIP64 compressed size")
                        zip64 = offset
                    }
                    extra += 4 + fieldSize
                }
                compressedSize = unsigned(bytes, checkNotNull(zip64) { "Missing ZIP64 extra" }, 8)
                if (compressedSize == 0L && end == bytes.size) {
                    return bytes.copyOfRange(start, end).apply { repeat(4) { this[10 + it] = 0 } }
                }
            }
            position = end.toLong() + compressedSize
            assertTrue(position <= bytes.size, "Local body exceeds cache at $start")
        }
        return null
    }

    private fun unsigned(bytes: ByteArray, offset: Int, count: Int): Long {
        var value = 0L
        repeat(count) { value = value or ((bytes[offset + it].toLong() and 255) shl (8 * it)) }
        assertTrue(value >= 0, "ZIP size exceeds signed Long")
        return value
    }
}
