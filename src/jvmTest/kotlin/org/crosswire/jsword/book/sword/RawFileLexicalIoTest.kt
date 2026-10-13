package org.crosswire.jsword.book.sword

import java.io.*
import java.nio.file.Files
import java.util.Locale
import org.crosswire.common.util.LoggerFactory
import org.junit.Assert.*
import org.junit.Test

class RawFileLexicalIoTest {
    @Test fun lexicalAppendReadsAndLocaleFormatMatchLegacyFileStreams() {
        val dir = Files.createTempDirectory("raw-file-lexical").toFile()
        val original = Locale.getDefault(Locale.Category.FORMAT)
        try {
            val file = File(dir, "literal\\file")
            val reference = File(dir, "reference")
            val location = StorageLocation(file.path)
            assertTrue(rawFileCreate(location)); assertFalse(rawFileCreate(location))
            for (locale in listOf(Locale.US, Locale.forLanguageTag("ar"))) {
                Locale.setDefault(Locale.Category.FORMAT, locale)
                for (number in listOf(-1L, 0L, 25L, Long.MAX_VALUE)) {
                    val text = String.format("%07d\r\n", number)
                    BufferedOutputStream(FileOutputStream(reference, true)).use { it.write(text.toByteArray(charset("UTF-8"))) }
                    rawFileAppend(location, number, "UTF-8")
                    assertEquals(String.format("%07d", number), rawFileNumber(number, false))
                }
            }
            assertArrayEquals(reference.readBytes(), file.readBytes())
            val log = LoggerFactory.getLogger(RawFileLexicalIoTest::class)
            assertArrayEquals(file.readBytes(), rawFileRead(location, log))
            rawFileWrite(location, byteArrayOf(1, 2, 3))
            assertArrayEquals(byteArrayOf(1, 2, 3), file.readBytes())
            val invalid = File(dir, "invalid")
            try { rawFileAppend(StorageLocation(invalid.path), 1, "invalid-charset"); fail() }
            catch (e: UnsupportedEncodingException) { assertTrue(invalid.exists()); assertEquals(0L, invalid.length()) }
            val empty = File(dir, "empty").apply { createNewFile() }
            assertArrayEquals(byteArrayOf(), rawFileRead(StorageLocation(empty.path), log))
        } finally { Locale.setDefault(Locale.Category.FORMAT, original); dir.deleteRecursively() }
    }
}
