package org.crosswire.jsword.book.sword

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.junit.Assert.*
import org.junit.Test

class SwordBookReplacementTest {
    @Test fun replacementDiscardsPooledAndLateReturnedStatesEvenWhenDescriptionChanges() {
        val root = Files.createTempDirectory("sword-replacement").toFile()
        val manager = OpenFileStateManager.instance()
        val initials = "ReplacementDict"
        fun register(description: String): org.crosswire.jsword.book.Book {
            val conf = File(root, "mods.d/replacement.conf").apply {
                parentFile.mkdirs()
                writeText("""
                    [$initials]
                    DataPath=./modules/lexdict/rawld/replacement/test
                    ModDrv=RawLD
                    Encoding=UTF-8
                    Description=$description
                """.trimIndent())
            }
            SwordBookDriver.registerNewBook(SwordBookMetaDataJvm.create(conf, root.toURI()).apply {
                setDriver(SwordBookDriver.instance())
            })
            return Books.installed().getBook(initials)!!
        }
        fun data(body: String) {
            val dir = File(root, "modules/lexdict/rawld/replacement").apply { mkdirs() }
            val bytes = "strong\n$body".toByteArray()
            // Rename, as archive publication does: an open handle still sees the previous inode.
            File(dir, "next.dat").apply { writeBytes(bytes) }.renameTo(File(dir, "test.dat"))
            File(dir, "next.idx").apply {
                writeBytes(ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(0).putShort(bytes.size.toShort()).array())
            }.renameTo(File(dir, "test.idx"))
        }
        var outstanding: org.crosswire.jsword.book.sword.state.RawLDBackendState? = null
        try {
            data("old text")
            val old = register("Original dictionary")
            assertTrue(old.getRawText(old.getKey("strong")).contains("old text"))
            val borrowed = manager.getRawLDBackendState(old.bookMetaData).also { outstanding = it }
            data("new text")
            val replacement = register("Original dictionary")
            assertTrue("idle states must be evicted", replacement.getRawText(replacement.getKey("strong")).contains("new text"))
            assertFalse("in-flight reads must finish before their handles close", borrowed.isClosedForTest())
            manager.release(borrowed)
            outstanding = null
            assertTrue("late returns must close their old handles", borrowed.isClosedForTest())
            repeat(3) {
                assertTrue("a late return must not resurrect old data", replacement.getRawText(replacement.getKey("strong")).contains("new text"))
            }
            data("third version")
            val renamed = register("Renamed dictionary")
            assertTrue(renamed.getRawText(renamed.getKey("strong")).contains("third version"))
            assertEquals(1, Books.installed().getBooks().count { it.initials == initials })
            assertNull(Books.installed().getBook("Original dictionary"))
            Books.installed().removeBook(renamed)
            data("after uninstall")
            val reinstalled = register("Renamed dictionary")
            assertTrue("uninstall must retire handles too", reinstalled.getRawText(reinstalled.getKey("strong")).contains("after uninstall"))
        } finally {
            outstanding?.releaseResources()
            manager.evict(initials)
            Books.installed().getBooks().filter { it.initials == initials }.forEach { Books.installed().removeBook(it) }
            root.deleteRecursively()
        }
    }
}
