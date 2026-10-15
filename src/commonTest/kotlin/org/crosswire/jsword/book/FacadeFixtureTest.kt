package org.crosswire.jsword.book

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import okio.FileSystem
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.IndexManagerFactory
import org.crosswire.jsword.index.SearchUnsupportedException
import org.crosswire.ksword.fixtures.fixtureRoot
import org.crosswire.ksword.fixtures.isJvm

/**
 * Executes the common book facade (Books, SwordBookDriver, SwordBook, SwordDictionary, SwordGenBook,
 * BookData) over K4's generated modules (K5b spec section 5.3). Runs on the JVM and on the iOS simulator.
 */
@OptIn(ExperimentalEncodingApi::class)
class FacadeFixtureTest {
    private var savedAugmentPath: Array<okio.Path> = emptyArray()

    /** Module rows of the frozen manifest: initials mapped to their (key name, raw text) pairs. */
    private fun manifest(): Map<String, List<Pair<String, String>>> =
        FileSystem.SYSTEM.read(fixtureRoot() / "manifest.tsv") { readUtf8() }
            .lineSequence().filter { it.isNotBlank() }.map {
                val columns = it.split('\t')
                columns[0] to (columns[1] to Base64.decode(columns[2]).decodeToString())
            }.toList().groupBy({ it.first }, { it.second })

    /** K4RawText4 has no registered SWORD book type (see FixtureModulesTest), so the facade never lists it. */
    private fun expectedInitials(): Set<String> = manifest().keys - "K4RawText4"

    /** setAugmentPath re-registers the SWORD driver, so the fixture books are listed once it returns. */
    @BeforeTest fun install() {
        savedAugmentPath = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
    }

    /** Re-registering with the previous augment path drops the fixture books from the shared Books instance. */
    @AfterTest fun restore() {
        SwordBookPath.setAugmentPath(savedAugmentPath)
    }

    @Test fun booksListsEveryRegisteredFixtureModule() {
        val expected = expectedInitials()
        assertEquals(14, expected.size, "fixture module count without K4RawText4")
        val initials = Books.installed().getBooks().map { it.initials }.toSet()
        assertTrue(initials.containsAll(expected), "missing: ${expected - initials}")
        for (name in expected) assertNotNull(Books.installed().getBook(name), name)
    }

    /**
     * Spec section 5.3: every fixture module reads its first manifest row through the facade
     * (Books, SwordBook, SwordDictionary, SwordGenBook, SwordDailyDevotion) and yields OSIS content.
     * The manifest values are the decoded raw texts FixtureModulesTest compares with the backends, including
     * the cipher module (its conf carries the key, so the facade returns the deciphered text), the Latin-1
     * and combining-character modules (compared as decoded strings) and the compressed modules.
     */
    @Test fun everyFixtureModuleReadsItsFirstManifestRowThroughTheFacade() {
        for (initials in expectedInitials()) {
            val (name, text) = manifest().getValue(initials).first()
            val book = assertNotNull(Books.installed().getBook(initials), initials)
            val key = book.getKey(name)
            assertEquals(text, book.getRawText(key), "$initials $name raw text")
            val osis = BookData(book, key).getOsisFragment(false)
            assertTrue(osis.contentSize > 0, "$initials $name OSIS fragment has content")
        }
    }

    @Test fun verseModuleReadsKnownTextAndOsis() {
        val expectedText = manifest().getValue("K4RawText").single { it.first == "Genesis 1:1" }.second
        assertEquals("In the beginning God created the heaven and the earth.", expectedText)
        val book = assertNotNull(Books.installed().getBook("K4RawText"))
        val key = book.getKey("Gen.1.1")
        assertEquals(expectedText, book.getRawText(key))
        val osis = BookData(book, key).getOsisFragment(false)
        assertTrue(osis.children.isNotEmpty() || osis.contentSize > 0, "OSIS fragment has content")
    }

    @Test fun genBookAndDictionaryKeysResolve() {
        val gen = assertNotNull(Books.installed().getBook("K4GenBook"))
        assertTrue(gen.getGlobalKeyList().getCardinality() > 0)
        val dict = assertNotNull(Books.installed().getBook("K4RawLD"))
        assertTrue(dict.getGlobalKeyList().getCardinality() > 0)
    }

    /**
     * iOS has no search before K6: Book.find must refuse with SearchUnsupportedException (never an NPE from a
     * missing registry default). On the JVM, find would run Lucene, so it is not called there.
     */
    @Test fun findOnIosIsRefusedWithSearchUnsupportedException() {
        if (isJvm) return
        val book = assertNotNull(Books.installed().getBook("K4RawText"))
        assertFailsWith<SearchUnsupportedException> { book.find("god") }
    }

    /**
     * Calls IndexManager.deleteIndex on the default IndexManager for a fixture book that has no index.
     * iOS: the default is UnsupportedIndexManager, so the call is a no-op and must not throw. JVM: Lucene's
     * deleteIndex refuses a book without an index directory, so only the precondition (the book is not indexed)
     * is checked there and deleteIndex is not called. No fixture file is deleted either way.
     */
    @Test fun deleteIndexOnDefaultIndexManagerIsHarmless() {
        val book = assertNotNull(Books.installed().getBook("K4RawText"))
        val indexManager = IndexManagerFactory.getIndexManager()
        assertFalse(indexManager.isIndexed(book), "fixture books have no index")
        if (!isJvm) indexManager.deleteIndex(book)
    }
}
