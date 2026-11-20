package org.crosswire.jsword.index.fts

import okio.FileSystem
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.junit.Assume
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The stored verse text of a real module (KJV, `<w>light</w>:` markup) reads as written, not `light : and`. */
class FtsStoredTextKjvTest {
    @Test fun storedTextHasNoSpacesAroundPunctuation() {
        val fs = FileSystem.SYSTEM
        val book = Books.installed().getBook("KJV")
        Assume.assumeNotNull(book)
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "kjv-stored-${Random.nextLong().toString(16)}"
        fs.createDirectories(root)
        val manager = FtsIndexManager(root / "search", root / "lucene") { bk, k -> BookData(bk, k).getOsisFragment(false) }
        val before = book!!.indexStatus
        try {
            manager.scheduleIndexCreation(book)
            val store = FtsIndexStore.open(manager.finalPathForTest(book), readOnly = true)!!
            val texts = HashMap<String, String>()
            var artifacts = 0
            store.forEachEntryText { e ->
                texts[e.osisRef] = e.text
                if (Regex("\\s[,.;:!?)\\]]|[(\\[]\\s").containsMatchIn(e.text)) artifacts++
                true
            }
            store.close()
            assertEquals("And God said, Let there be light: and there was light.", texts["Gen.1.3"])
            assertEquals(0, artifacts, "verses with a space before closing or after opening punctuation")
            assertTrue(texts.size > 31000)
        } finally {
            manager.closeAllIndexes()
            book.indexStatus = before
            fs.deleteRecursively(root, mustExist = false)
        }
    }
}
