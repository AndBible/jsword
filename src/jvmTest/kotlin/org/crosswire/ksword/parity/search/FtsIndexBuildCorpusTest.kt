package org.crosswire.ksword.parity.search

import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.fts.FtsIndexManager
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** K6 task 10 over the real corpus: KJV and MHC build into FTS5 files and a plain term is found. */
class FtsIndexBuildCorpusTest {
    private val dir = Files.createTempDirectory("fts-corpus").toFile()

    @AfterTest fun cleanup() { dir.deleteRecursively() }

    private fun manager() = FtsIndexManager(dir.resolve("search").toOkioPath(), dir.resolve("lucene").toOkioPath(),
        { b, k -> org.crosswire.jsword.book.BookData(b, k).getOsisFragment(false) })

    private fun book(initials: String): Book {
        SwordBookPathJvm.setAugmentPath(arrayOf(K6Corpus.root))
        return requireNotNull(Books.installed().getBook(initials)) { "no such book $initials" }
    }

    @Test fun kjvBuildsAndFindsAPlainTerm() {
        val b = book("KJV")
        val m = manager()
        m.scheduleIndexCreation(b)
        assertEquals(IndexStatus.DONE, b.indexStatus)
        assertTrue(m.isIndexed(b) && !m.needsReindexing(b))
        val hits = m.getIndex(b).find("beginning")
        assertTrue(hits.getCardinality() > 50, "beginning: ${hits.getCardinality()}")
        assertTrue(hits.contains(org.crosswire.jsword.passage.VerseFactory.fromString(
            org.crosswire.jsword.versification.system.Versifications.instance().getVersification("KJV"), "Gen 1:1")), "Gen 1:1")
        m.closeAllIndexes()
    }

    @Test fun mhcBuildsAndFindsAPlainTerm() {
        val b = book("MHC")
        val m = manager()
        m.scheduleIndexCreation(b)
        assertEquals(IndexStatus.DONE, b.indexStatus)
        assertTrue(m.getIndex(b).find("covenant").getCardinality() > 50)
        m.closeAllIndexes()
    }
}
