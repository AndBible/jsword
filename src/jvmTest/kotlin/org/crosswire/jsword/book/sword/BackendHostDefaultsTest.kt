package org.crosswire.jsword.book.sword

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

/** Fresh JVMs are required: both default charset and host newline are cached. */
class BackendHostDefaultsTest {
    private fun fork(mode: String, vararg options: String) {
        val process = ProcessBuilder(listOf(
            File(System.getProperty("java.home"), "bin/java").path
        ) + options + listOf("-cp", System.getProperty("java.class.path"),
            BackendHostDefaultsProbe::class.java.name, mode)).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        assertEquals(output, 0, process.waitFor())
    }

    @Test fun nonUtf8FilenameMatchesOldJavaConstructorAndReadsActualFile() =
        fork("charset", "-Dfile.encoding=ISO-8859-1")

    @Test fun crlfDebugMatchesOldPrintStreamOnFreshHost() =
        fork("newline", "-Dline.separator=\r\n")
}

object BackendHostDefaultsProbe {
    @JvmStatic fun main(args: Array<String>) {
        if (args.single() == "charset") {
            check(Charset.defaultCharset().name() == "ISO-8859-1")
            val bytes = byteArrayOf(0xe9.toByte(), 49, 50, 51, 52, 53, 54)
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            val reference = java.lang.String(bytes, 0, 7).toString()
            val actual = rawFileDecodeName(bytes)
            check(reference == actual) { "decoder differs: reference=${reference.map { it.code }} actual=${actual.map { it.code }}" }
            val dir = Files.createTempDirectory("host-default-name").toFile()
            try {
                File(dir, reference).writeBytes(byteArrayOf(42))
                check(File(dir, actual).readBytes().contentEquals(byteArrayOf(42)))
            } finally { dir.deleteRecursively() }
        } else {
            check(System.lineSeparator() == "\r\n")
            val dir = Files.createTempDirectory("host-default-newline").toFile()
            try {
                val file = File(dir, "comp").apply { writeBytes(ByteArray(12)) }
                val md = SwordBookMetaData("[Debug]\nModDrv=zText\n".toByteArray(), "debug")
                val backend = ZVerseBackend(md, BlockType.BLOCK_CHAPTER, 2)
                val actual = StringBuilder()
                storageOpenRandomAccess(StorageLocation(file.path), false).use { backend.dumpCompRaf(it, actual) }
                val bytes = java.io.ByteArrayOutputStream()
                java.io.PrintStream(bytes, true, "UTF-8").use {
                    it.println("block\tstart\tsize\tuncompressed")
                    it.println("0\t0\t0\t0")
                }
                check(bytes.toString("UTF-8") == actual.toString()) { "debug host separator differs" }
                val index = File(dir, "idx").apply { writeBytes(ByteArray(10)) }
                val v11n = org.crosswire.jsword.versification.system.Versifications.instance().getVersification("KJV")!!
                actual.setLength(0); bytes.reset()
                storageOpenRandomAccess(StorageLocation(index.path), false).use { backend.dumpIdxRaf(v11n, 1, it, actual) }
                java.io.PrintStream(bytes, true, "UTF-8").use {
                    it.println("osisID\tblock\tstart\tsize")
                    it.println("${v11n.decodeOrdinal(1).getOsisID()}\t0\t0\t0")
                }
                check(bytes.toString("UTF-8") == actual.toString()) { "index debug host separator differs" }
            } finally { dir.deleteRecursively() }
        }
    }
}
