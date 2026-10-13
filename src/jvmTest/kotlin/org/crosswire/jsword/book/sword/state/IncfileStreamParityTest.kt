package org.crosswire.jsword.book.sword.state

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintStream
import java.nio.file.Files
import org.crosswire.common.util.LoggerFactory
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.SwordUtil
import org.junit.Assert.*
import org.junit.Test

class IncfileStreamParityTest {
    @Test fun zeroThroughFourBytesRetainOldStreamResultAndDiagnostics() {
        val root = Files.createTempDirectory("incfile-parity").toFile()
        val directory = File(root, "probe").apply { mkdirs() }
        listOf("ot", "nt", "ot.vss", "nt.vss").forEach { File(directory, it).writeBytes(byteArrayOf()) }
        val md = SwordBookMetaData("[IncProbe]\nDataPath=./probe\nModDrv=RawFiles\nLang=en\n".toByteArray(), "incprobe.conf")
            .apply { library = root.toURI() }
        val inc = File(directory, "incfile")
        try {
            for (size in 0..4) {
                inc.writeBytes(byteArrayOf(7, 0, 0, 0).copyOf(size))
                val oldLog = capture {
                    var value = -1
                    try {
                        FileInputStream(inc).use { input ->
                            val buffer = ByteArray(4)
                            if (input.read(buffer) != 4) {
                                logger.error("Read data is not of appropriate size of 4 bytes!")
                                throw IOException("Incfile is not 4 bytes long")
                            }
                            value = SwordUtil.decodeLittleEndian32(buffer, 0)
                        }
                    } catch (ex: IOException) { logger.error("IO Error: {}", ex.message, ex) }
                    value
                }
                val state = RawFileBackendState(md)
                try {
                    val current = capture { state.getIncfileValue() }
                    assertEquals("size=$size", oldLog.first, current.first)
                    assertEquals("size=$size diagnostics", events(oldLog.second), events(current.second))
                    assertFalse("capture must see old short-file error", size < 4 && events(oldLog.second).isEmpty())
                } finally { state.releaseResources() }
            }
        } finally { root.deleteRecursively() }
    }

    private fun events(log: String): List<String> = log.lineSequence()
        .filter { it.contains("ERROR") }
        .map { it.substringAfter("ERROR ") }.toList()

    private fun capture(block: () -> Int): Pair<Int, String> {
        // slf4j-simple 1.7 retains its output stream at initialization, not System.err per call.
        val target = Class.forName("org.slf4j.impl.SimpleLogger").getDeclaredField("TARGET_STREAM")
            .apply { isAccessible = true }
        val previous = target.get(null)
        val bytes = ByteArrayOutputStream()
        val stream = PrintStream(bytes)
        try {
            target.set(null, stream)
            return block() to bytes.toString("UTF-8")
        } finally { target.set(null, previous); stream.close() }
    }

    private val logger = LoggerFactory.getLogger(RawFileBackendState::class)
}
