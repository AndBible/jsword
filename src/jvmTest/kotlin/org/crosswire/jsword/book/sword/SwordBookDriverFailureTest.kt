package org.crosswire.jsword.book.sword

import okio.Path.Companion.toOkioPath
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.KeyType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class SwordBookDriverFailureTest {
    @Test fun runtimeFailureInOneConfDoesNotAbortOtherModules() {
        val root = Files.createTempDirectory("sword-conf-failure")
        val previousFlag = SwordBookPath.scanDefaultLocations
        val previousDownload = SwordBookPath.getDownloadDir()
        val previousAugment = SwordBookPath.getAugmentPath()
        val field = BookType::class.java.getDeclaredField("supportedBookTypes").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val types = field.get(null) as MutableList<BookType>
        val brokenType = object : BookType("StartupFailureTest", BookCategory.GENERAL_BOOK, KeyType.TREE) {
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> = error("broken module backend")
        }
        try {
            org.crosswire.jsword.book.Books.installed()
            Files.createDirectories(root.resolve("mods.d"))
            Files.createDirectories(root.resolve("modules/broken"))
            Files.createDirectories(root.resolve("modules/valid"))
            Files.writeString(root.resolve("mods.d/broken.conf"), "[Broken]\nModDrv=StartupFailureTest\nDataPath=./modules/broken/\n")
            Files.writeString(root.resolve("mods.d/valid.conf"), "[Valid]\nModDrv=RawText\nDataPath=./modules/valid/\n")
            BookType.addSupportedBookType(brokenType)
            SwordBookPath.scanDefaultLocations = false
            SwordBookPath.setDownloadDir(root.toOkioPath())
            SwordBookPath.setAugmentPath(arrayOf(root.toOkioPath()))
            assertEquals(setOf("Valid"), SwordBookDriver().getBooks().map { it.initials }.toSet())
        } finally {
            types.remove(brokenType)
            SwordBookPath.scanDefaultLocations = previousFlag
            previousDownload?.let { SwordBookPath.setDownloadDir(it) }
            SwordBookPath.setAugmentPath(previousAugment)
            root.toFile().deleteRecursively()
        }
    }
}
