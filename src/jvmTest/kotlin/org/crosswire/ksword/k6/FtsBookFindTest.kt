package org.crosswire.ksword.k6

import okio.Path.Companion.toOkioPath
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.IndexManagerFactory
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.fts.FtsIndexManager
import org.crosswire.jsword.index.search.DefaultSearchRequest
import org.crosswire.ksword.fixtures.fixtureRoot
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The FTS manager used the way the app uses it: searches go through `Book.find`, which caches a `Searcher` holding the
 * book's index. A rebuild, [FtsIndexManager.closeAllIndexes] and a delete must leave that cache usable (or fail with a
 * proper error after a delete), never holding a closed index.
 */
class FtsBookFindTest {
    private val dir = Files.createTempDirectory("fts-bookfind").toFile()
    private var saved: Array<okio.Path> = emptyArray()
    private var statusBefore: IndexStatus? = null
    private val book: Book get() = assertNotNull(Books.installed().getBook("K4RawText"))
    private var replacement: String? = null
    private var gate: CountDownLatch? = null
    private val started = CountDownLatch(1)
    private lateinit var manager: FtsIndexManager

    @BeforeTest fun setUp() {
        saved = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
        statusBefore = book.indexStatus
        manager = FtsIndexManager(dir.resolve("search").toOkioPath(), dir.resolve("lucene").toOkioPath()) { b, k ->
            gate?.let { g -> started.countDown(); assertTrue(g.await(20, TimeUnit.SECONDS), "gate not released"); gate = null }
            replacement?.let { w -> fi.sykero.kdom.Element("div").apply { addContent("$w ") } } ?: BookData(b, k).getOsisFragment(false)
        }
    }

    @AfterTest fun tearDown() {
        manager.closeAllIndexes()
        book.resetSearcher()
        statusBefore?.let { book.indexStatus = it }
        SwordBookPath.setAugmentPath(saved)
        dir.deleteRecursively()
    }

    private fun <T> app(block: () -> T): T = IndexManagerFactory.withIndexManagerForTest(manager, block)

    private fun count(q: String) = book.find(DefaultSearchRequest(q))!!.getCardinality()

    @Test fun searchWorksBeforeAndAfterARebuild() = app {
        manager.scheduleIndexCreation(book)
        assertEquals(3, count("beginning"))
        manager.scheduleIndexCreation(book) // same content
        assertEquals(3, count("beginning"))
        assertEquals(IndexStatus.DONE, book.indexStatus)
    }

    @Test fun searchAfterARebuildWithChangedContentSeesTheNewIndex() = app {
        manager.scheduleIndexCreation(book)
        assertEquals(3, count("beginning"))
        replacement = "omega"
        manager.scheduleIndexCreation(book)
        assertEquals(0, count("beginning"))
        assertEquals(6, count("omega"))
    }

    @Test fun searchDuringARebuildIsServedByTheOldIndex() = app {
        manager.scheduleIndexCreation(book)
        assertEquals(3, count("beginning")) // searcher cached
        gate = CountDownLatch(1)
        val release = gate!!
        val builder = thread { manager.scheduleIndexCreation(book) }
        assertTrue(started.await(20, TimeUnit.SECONDS))
        assertEquals(IndexStatus.CREATING, book.indexStatus)
        assertEquals(3, count("beginning"), "old index must keep serving during the build")
        release.countDown()
        builder.join(30_000)
        assertEquals(IndexStatus.DONE, book.indexStatus)
        assertEquals(3, count("beginning"))
    }

    @Test fun aSearcherHeldAcrossARebuildIsForwardedToTheNewIndex() = app {
        manager.scheduleIndexCreation(book)
        val held = manager.getIndex(book)
        manager.scheduleIndexCreation(book)
        assertEquals(3, held.find("beginning").getCardinality(), "an index evicted by the rebuild forwards to the new one")
    }

    @Test fun searchAfterCloseAllIndexesReopens() = app {
        manager.scheduleIndexCreation(book)
        assertEquals(3, count("beginning"))
        manager.closeAllIndexes()
        assertEquals(3, count("beginning"))
    }

    @Test fun searchAfterDeleteIndexFailsWithAProperError() = app {
        manager.scheduleIndexCreation(book)
        assertEquals(3, count("beginning"))
        manager.deleteIndex(book)
        val e = assertFailsWith<BookException> { book.find(DefaultSearchRequest("beginning")) }
        assertTrue(e.message!!.isNotBlank())
        manager.scheduleIndexCreation(book)
        assertEquals(3, count("beginning"), "rebuilt after the delete")
    }
}
