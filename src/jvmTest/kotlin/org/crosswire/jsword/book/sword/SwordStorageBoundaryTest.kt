package org.crosswire.jsword.book.sword

import java.io.File
import java.nio.file.Files
import okio.Path
import okio.Path.Companion.toPath
import org.crosswire.common.util.Filter
import org.crosswire.common.util.KCharset
import org.crosswire.common.util.IniSection
import org.junit.Assert.*
import org.junit.Test

class SwordStorageBoundaryTest {
    private fun temporary(block: (File) -> Unit) {
        val root = Files.createTempDirectory("sword space ä").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }

    @Test fun fileFactoryPreservesConfigLocationAndEncodingRetryOrder() = temporary { root ->
        val config = File(root, "demo.conf")
        val text = "[Demo]\nDescription=Café\nModDrv=RawText\nEncoding=UTF-8\nAndBibleProbe=Café\n"
        config.writeBytes(text.toByteArray(Charsets.UTF_8))
        val metadata = SwordBookMetaDataJvm.create(config, null)
        assertEquals(config, SwordBookMetaDataJvm.getConfigFile(metadata))
        assertEquals("demo.conf", metadata.bookConf)
        assertEquals("Café", metadata.name)
        // Filtering Encoding out forces the legacy Latin-1 retry even for UTF-8 bytes.
        metadata.reload(Filter { it != SwordBookMetaData.KEY_ENCODING })
        assertEquals("CafÃ©", metadata.name)
        config.writeBytes(text.replace("UTF-8", "uTf-8").toByteArray(Charsets.UTF_8))
        metadata.reload(null)
        assertEquals("Café", metadata.name)
        config.writeBytes(KCharset.encode(text.replace("UTF-8", "Latin-1"), "WINDOWS-1252"))
        metadata.reload(null)
        assertEquals("Café", metadata.name)
        val memory = SwordBookMetaData(text.toByteArray(Charsets.UTF_8), "mods.d/demo.conf")
        assertNull(SwordBookMetaDataJvm.getConfigFile(memory))
        assertEquals("demo.conf", memory.bookConf)
    }

    @Test fun childJoiningMatchesFileIncludingNullRelativeAndAbsoluteChildren() {
        for (parent in listOf<File?>(null, File("relative space ä"), File("/tmp/parent"))) {
            for (child in listOf("child.conf", "/absolute.conf", "../other.conf", "")) {
                assertEquals(File(parent, child).path.toPath(), metadataChildPath(parent?.path?.toPath(), child))
            }
        }
        assertThrows(NullPointerException::class.java) { metadataChildPath(null, null) }
    }

    @Test fun fileSettersCopyAugmentsIgnoreEmptyDownloadAndKeepPriority() = temporary { root ->
        val oldDownload = SwordBookPathJvm.getDownloadDir()
        val oldAugment = SwordBookPathJvm.getAugmentPath()
        val override = SwordBookPath::class.java.getDeclaredField("overrideDownloadDir").apply { isAccessible = true }
        try {
            val download = File(root, "download")
            val other = File(root, "relative space ä")
            SwordBookPathJvm.setDownloadDir(download)
            SwordBookPathJvm.setDownloadDir(File(""))
            assertEquals(download, SwordBookPathJvm.getDownloadDir())
            val input = arrayOf(download, other, other)
            SwordBookPathJvm.setAugmentPath(input)
            input[1] = root
            val copy = SwordBookPathJvm.getAugmentPath()
            assertArrayEquals(arrayOf(download, other, other), copy)
            copy[0] = root
            assertEquals(download, SwordBookPathJvm.getAugmentPath()[0])
            SwordBookPathJvm.setAugmentPath(null)
            assertArrayEquals(arrayOf(download, other, other), SwordBookPathJvm.getAugmentPath())
            assertEquals(listOf(download, other), SwordBookPathJvm.getSwordPath().take(2))
        } finally {
            override.set(null, oldDownload?.path?.toPath())
            SwordBookPathJvm.setAugmentPath(oldAugment)
        }
    }

    @Test fun listingPreservesNullAndActualFilenameFilter() = temporary { root ->
        for (name in listOf("a.conf", "b.CONF", "globals.test.conf", "other", "ä.conf")) File(root, name).writeText("")
        File(root, "directory.conf").mkdir()
        assertEquals(setOf("a.conf", "ä.conf", "directory.conf"), SwordBookPathJvm.getBookList(root)!!.toSet())
        assertNull(SwordBookPathJvm.getBookList(File(root, "missing")))
        assertNull(SwordBookPathJvm.getBookList(File(root, "a.conf")))
    }

    @Test fun migrationMovesBothTreesAndRestoresDataWhenConfMoveFails() = temporary { root ->
        val migrate = SwordBookPath.Companion::class.java.getDeclaredMethod("migrateBookDir", Path::class.java, Path::class.java).apply { isAccessible = true }
        val old = File(root, "old").apply { mkdirs() }
        val target = File(root, "target")
        File(old, "modules").mkdir()
        File(old, "modules/data").writeText("data")
        File(old, "mods.d").mkdir()
        File(old, "mods.d/demo.conf").writeText("conf")
        migrate.invoke(SwordBookPath.Companion, old.path.toPath(), target.path.toPath())
        assertEquals("data", File(target, "modules/data").readText())
        assertEquals("conf", File(target, "mods.d/demo.conf").readText())
        assertFalse(File(old, "modules").exists())
        val second = File(root, "second").apply { mkdirs() }
        File(second, "modules").mkdir()
        File(second, "modules/second").writeText("restore")
        File(second, "mods.d").mkdir()
        File(second, "mods.d/new.conf").writeText("new")
        // Existing non-empty conf target makes the conf rename fail, after data has moved.
        File(target, "modules").deleteRecursively()
        migrate.invoke(SwordBookPath.Companion, second.path.toPath(), target.path.toPath())
        assertEquals("restore", File(second, "modules/second").readText())
        assertTrue(File(second, "mods.d/new.conf").exists())
        assertFalse(File(target, "modules").exists())
    }

    @Test fun normalizeWriterHolderRetainsHistoryAndContinuationOrder() {
        val section = IniSection("Demo")
        section.add("History_1.0", "first")
        section.add("Description", "line1\nline2")
        val out = java.io.StringWriter()
        SwordBookMetaDataJvm.normalize(out, section, arrayOf("Description", "History"))
        assertEquals("[Demo]\nDescription=line1 \\\nline2\nHistory_1.0=first\n", out.toString().replace("\r\n", "\n"))
        assertTrue(section.containsKey("History_1.0"))
    }
}
