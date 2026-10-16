package org.crosswire.jsword.index

import okio.FileSystem
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.fts.FtsIndexManager
import org.crosswire.jsword.index.query.QueryBuilderFactory
import org.crosswire.jsword.index.query.QueryDecoratorFactory
import org.crosswire.jsword.index.search.SearchType
import org.crosswire.jsword.index.search.SearcherFactory
import org.crosswire.ksword.fixtures.fixtureRoot
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The default search plugins are the FTS5 ones on every platform (K6 task 15). */
class SearchRegistryTest {
    @Test fun factoryReturnsTheFtsIndexManager() {
        assertIs<FtsIndexManager>(IndexManagerFactory.getIndexManager())
    }

    @Test fun searchTypesDecorateWithTheJSwordQuerySyntax() {
        assertEquals("\"a b\"", SearchType.PHRASE.decorate("a b"))
        assertEquals("+a +b", SearchType.ALL_WORDS.decorate("a b"))
        assertEquals("a b", SearchType.ANY_WORDS.decorate("a b"))
        assertEquals("-a -b", SearchType.NOT_WORDS.decorate("a b"))
        assertEquals("a* b*", SearchType.START_WORDS.decorate("a b"))
        assertEquals("a~ b~", SearchType.SPELL_WORDS.decorate("a b"))
        assertEquals("+[Gen]", SearchType.RANGE.decorate("Gen"))
        assertEquals("+a +b", QueryDecoratorFactory.getSearchSyntax().decorateAllWords("a b"))
        assertNotNull(QueryBuilderFactory.getQueryBuilder().parse("god"))
    }

    @Test fun anIndexedFixtureBookIsFoundThroughBookFind() {
        val fs = FileSystem.SYSTEM
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "search-registry-${Random.nextLong().toString(16)}"
        fs.createDirectories(root)
        val saved = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
        val manager = FtsIndexManager(root / "search", root / "lucene") { b, k -> org.crosswire.jsword.book.BookData(b, k).getOsisFragment(false) }
        try {
            val book = assertNotNull(Books.installed().getBook("K4RawText"))
            IndexManagerFactory.withIndexManagerForTest(manager) {
                manager.scheduleIndexCreation(book)
                assertTrue(manager.isIndexed(book))
                assertNotNull(SearcherFactory.createSearcher(book))
                val found = assertNotNull(book.find("beginning"))
                assertEquals(listOf("Gen.1.1", "John.1.1", "John.1.2"), found.map { it.getOsisRef() })
                assertEquals(listOf("Gen.1.1", "John.1.1", "John.1.2"), assertNotNull(book.find(SearchType.PHRASE.decorate("beginning"))).map { it.getOsisRef() })
            }
        } finally {
            Books.installed().getBook("K4RawText")?.resetSearcher()
            manager.closeAllIndexes()
            SwordBookPath.setAugmentPath(saved)
            fs.deleteRecursively(root, mustExist = false)
        }
    }
}
