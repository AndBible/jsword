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

    private fun open(b: Book) = checkNotNull(org.crosswire.jsword.index.fts.FtsIndexStore.open(
        dir.resolve("search/${b.bookMetaData.driverName}/${b.initials}.sqlite").toOkioPath()))

    /** K6-4: the KJV carries Strong's numbers; they are indexed (padded form), in table strongs. */
    @Test fun kjvIndexesStrongsNumbers() {
        val b = book("KJV")
        manager().scheduleIndexCreation(b)
        open(b).use { assertTrue(it.strongs("H0430").size > 1000, "H0430: ${it.strongs("H0430").size}") }
    }

    /**
     * Verse 0 (introductions) is excluded from the text index as Lucene's `content` field did. The Lucene builder put
     * a verse-0 entry's Strong's numbers into the `strong` field regardless; this builder does the same (an entry with no
     * text and no Strong's is not added at all). MHC has verse-0 keys and no Strong's, so none of them has a row.
     */
    @Test fun mhcVerseZeroIsNotIndexed() {
        val b = book("MHC")
        assertTrue(b.getGlobalKeyList().any { it is org.crosswire.jsword.passage.Verse && it.verse == 0 }, "MHC has verse 0 keys")
        manager().scheduleIndexCreation(b)
        open(b).use { s -> assertTrue(s.allRowIds().isNotEmpty() && s.allRowIds().none { s.osisRef(it).endsWith(".0") }) }
    }
}
