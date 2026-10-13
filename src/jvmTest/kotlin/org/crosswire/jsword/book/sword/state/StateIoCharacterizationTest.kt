package org.crosswire.jsword.book.sword.state

import java.io.File
import java.nio.file.Files
import org.crosswire.jsword.book.sword.SwordBookMetaDataJvm
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.versification.Testament
import org.junit.Assert.*
import org.junit.Test

class StateIoCharacterizationTest {
    private fun metadata(root: File) = SwordBookMetaDataJvm.create(
        File(System.getProperty("ksword.fixturesDir"), "space dir/ä/mods.d/K4RawText.conf"),
        File(System.getProperty("ksword.fixturesDir"), "space dir/ä").toURI()
    ).apply { library = root.toURI(); putProperty("DataPath", "./literal\\module") }

    @Test fun rawStateUsesLiteralFilesAndOwnsHandles() {
        val root = Files.createTempDirectory("state-io").toFile()
        val data = File(root, "literal\\module").apply { mkdirs() }
        try {
            File(data, "ot").writeBytes(byteArrayOf(7, 8))
            File(data, "ot.vss").writeBytes(byteArrayOf(1))
            val state = RawBackendState(metadata(root))
            val src = state.getTextRaf(Testament.OLD)!!
            assertArrayEquals(byteArrayOf(7, 8), SwordUtil.readRAF(src, 0, 4))
            assertNull(state.getTextRaf(Testament.NEW))
            state.releaseResources()
            assertNull(state.getTextRaf(Testament.OLD))
            assertThrows(java.io.IOException::class.java) { SwordUtil.readRAF(src, 0, 0) }
            state.releaseResources()
        } finally { root.deleteRecursively() }
    }

    @Test fun missingAndEmptyStatesPreservePartialConstruction() {
        val root = Files.createTempDirectory("state-empty").toFile()
        try {
            val md = metadata(root)
            assertThrows(BookException::class.java) { RawBackendState(md) }
            val rawLd = RawLDBackendState(md)
            val gen = GenBookBackendState(md)
            assertNull(rawLd.getIdxRaf())
            assertNull(rawLd.getDatRaf())
            assertNull(gen.getBdtRaf())
            rawLd.releaseResources(); gen.releaseResources()
            File(root, "literal\\module.idx").writeBytes(byteArrayOf())
            File(root, "literal\\module.dat").writeBytes(byteArrayOf())
            val empty = RawLDBackendState(md)
            try { assertArrayEquals(byteArrayOf(), SwordUtil.readRAF(empty.getDatRaf(), 0, 1)) }
            finally { empty.releaseResources() }
        } finally { root.deleteRecursively() }
    }
}
