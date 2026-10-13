package org.crosswire.jsword.book.sword.state

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.jsword.versification.Testament
import org.junit.Assert.*
import org.junit.Test

/** Characterizes actual legacy File IO before the random-access boundary changes. */
class FileStateIoCharacterizationTest {
    private fun temporary(block: (File) -> Unit) {
        val root = Files.createTempDirectory("state-io").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }

    private fun metadata(root: File, driver: String = "RawText"): SwordBookMetaData =
        SwordBookMetaData("[Probe]\nDataPath=./modules/probe\nModDrv=$driver\nLang=en\n".toByteArray(), "probe.conf").apply {
            library = root.toURI()
        }

    @Test fun rawStateReadsLiteralBackslashDirectoryWithoutOpeningProjectedDecoy() = temporary { root ->
        val literal = File(root, "literal\\directory")
        val projected = File(root, "literal/directory")
        for ((base, bytes) in listOf(literal to byteArrayOf(7, 8, 9), projected to byteArrayOf(1, 2, 3))) {
            val directory = File(base, "modules/probe")
            assertTrue(directory.mkdirs())
            File(directory, "ot").writeBytes(bytes)
            File(directory, "ot.vss").writeBytes(byteArrayOf(0))
        }
        val state = RawBackendState(metadata(literal))
        try {
            assertArrayEquals(byteArrayOf(7, 8, 9), SwordUtil.readRAF(state.getTextRaf(Testament.OLD), 0, 3))
            assertEquals(File(literal, "./modules/probe/ot"), RawBackendStateJvm.getTextFile(state, Testament.OLD))
        } finally { state.releaseResources() }
    }

    @Test fun rawStateKeepsEmptyIndexAndShortensReadsAtEof() = temporary { root ->
        val directory = File(root, "modules/probe").apply { assertTrue(mkdirs()) }
        File(directory, "ot").writeBytes(byteArrayOf(10, 20, 30))
        File(directory, "ot.vss").writeBytes(byteArrayOf())
        val state = RawBackendState(metadata(root))
        try {
            assertNotNull(state.getIdxRaf(Testament.OLD))
            assertArrayEquals(byteArrayOf(), SwordUtil.readRAF(state.getIdxRaf(Testament.OLD), 0, 6))
            assertArrayEquals(byteArrayOf(30), SwordUtil.readRAF(state.getTextRaf(Testament.OLD), 2, 6))
            assertArrayEquals(byteArrayOf(), SwordUtil.readRAF(state.getTextRaf(Testament.OLD), 3, 6))
            assertArrayEquals(byteArrayOf(), SwordUtil.readRAF(state.getTextRaf(Testament.OLD), 0, -1))
            // Legacy readRAF seeks before checking size, including zero-sized reads.
            assertThrows(IOException::class.java) { SwordUtil.readRAF(state.getTextRaf(Testament.OLD), -1, 0) }
        } finally { state.releaseResources() }
    }

    @Test fun rawStateWithoutEitherTextFileThrows() = temporary { root ->
        assertThrows(BookException::class.java) { RawBackendState(metadata(root)) }
    }

    @Test fun rawLdMissingIndexReturnsHalfMadeStateButMissingDataThrows() = temporary { root ->
        val directory = File(root, "modules").apply { assertTrue(mkdirs()) }
        val half = RawLDBackendState(metadata(root, "RawLD"))
        try {
            assertNull(half.getIdxRaf())
            assertNull(half.getDatRaf())
        } finally { half.releaseResources() }
        File(directory, "probe.idx").writeBytes(byteArrayOf())
        assertThrows(BookException::class.java) { RawLDBackendState(metadata(root, "RawLD")) }
        File(directory, "probe.dat").writeBytes(byteArrayOf())
        val empty = RawLDBackendState(metadata(root, "RawLD"))
        try {
            assertEquals(0, empty.getIdxRaf()!!.size())
            assertEquals(0, empty.getDatRaf()!!.size())
        } finally { empty.releaseResources() }
    }
}
