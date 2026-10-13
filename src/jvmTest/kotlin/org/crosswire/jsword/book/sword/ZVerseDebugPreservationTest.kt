package org.crosswire.jsword.book.sword

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import org.crosswire.jsword.versification.system.Versifications
import org.junit.Assert.*
import org.junit.Test

class ZVerseDebugPreservationTest {
    @Test fun populatedDebugRowsRetainFrozenPrintStreamLayout() {
        val directory = Files.createTempDirectory("zverse-debug").toFile()
        try {
            val md = SwordBookMetaData("[Debug]\nModDrv=zText\n".toByteArray(), "debug")
            val backend = ZVerseBackend(md, BlockType.BLOCK_CHAPTER, 2)
            val idx = ByteArray(20)
            for (i in 0..1) {
                SwordUtil.encodeLittleEndian32(i + 2, idx, i * 10)
                SwordUtil.encodeLittleEndian32(i + 3, idx, i * 10 + 4)
                SwordUtil.encodeLittleEndian16(i + 4, idx, i * 10 + 8)
            }
            val comp = ByteArray(24)
            for (i in 0..1) for (j in 0..2) SwordUtil.encodeLittleEndian32(i + j + 10, comp, i * 12 + j * 4)
            val idxFile = java.io.File(directory, "idx").apply { writeBytes(idx) }
            val compFile = java.io.File(directory, "comp").apply { writeBytes(comp) }
            val v11n = Versifications.instance().getVersification("KJV")!!
            fun reference(compMode: Boolean): String {
                val old = System.out; val bytes = ByteArrayOutputStream()
                try {
                    System.setOut(PrintStream(bytes, true, "UTF-8"))
                    if (compMode) {
                        System.out.println("block\tstart\tsize\tuncompressed")
                        for (i in 0..1) System.out.println("0\t${i + 10}\t${i + 11}\t${i + 12}")
                    } else {
                        System.out.println("osisID\tblock\tstart\tsize")
                        for (i in 0..1) System.out.println("${v11n.decodeOrdinal(i + 1).getOsisID()}\t${i + 2}\t${i + 3}\t${i + 4}")
                    }
                } finally { System.setOut(old) }
                return bytes.toString("UTF-8")
            }
            val output = StringBuilder()
            storageOpenRandomAccess(StorageLocation(idxFile.path), false).use { backend.dumpIdxRaf(v11n, 1, it, output) }
            assertEquals(reference(false), output.toString())
            output.setLength(0)
            storageOpenRandomAccess(StorageLocation(compFile.path), false).use { backend.dumpCompRaf(it, output) }
            assertEquals(reference(true), output.toString())
        } finally { directory.deleteRecursively() }
    }
}
