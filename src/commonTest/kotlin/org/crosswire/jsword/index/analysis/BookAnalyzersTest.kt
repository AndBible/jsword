package org.crosswire.jsword.index.analysis

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.ksword.fixtures.fixtureRoot

/**
 * [BookAnalyzers] with literal expectations on every platform. The expected ids are the baseline `AnalyzerFactory.properties`
 * table; the oracle-backed comparison of the token streams, and forBook over non-English corpus books, are in the
 * `searchParityTest` source set.
 */
class BookAnalyzersTest {
    private var savedAugmentPath: Array<okio.Path> = emptyArray()

    /** The K4 fixture modules are installed for the facade tests, and all of them are English (`Lang=en`). */
    @BeforeTest fun install() {
        savedAugmentPath = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
    }

    @AfterTest fun restore() {
        SwordBookPath.setAugmentPath(savedAugmentPath)
    }

    @Test fun everyBaselineKeySelectsItsAnalyzerId() {
        val expected = mapOf(
            "ar" to "ar", "cs" to "cs", "de" to "de", "el" to "el", "grc" to "el", "fa" to "fa", "he" to "he",
            "ja" to "mmseg", "zh" to "mmseg", "th" to "th",
            "da" to "snowball-Danish", "es" to "snowball-Spanish", "fi" to "snowball-Finnish", "fr" to "snowball-French",
            "it" to "snowball-Italian", "nl" to "snowball-Dutch", "no" to "snowball-Norwegian",
            "pt" to "snowball-Portuguese", "ru" to "snowball-Russian", "sv" to "snowball-Swedish",
        )
        assertEquals(20, expected.size)
        for ((code, id) in expected) assertEquals(id, BookAnalyzers.analyzerId(code), "analyzer id for $code")
    }

    @Test fun subtaggedSpecificationsResolveToTheirLanguageCode() {
        assertEquals("mmseg", BookAnalyzers.analyzerId("zh-Hans"))
        assertEquals("mmseg", BookAnalyzers.analyzerId("zh-Hant"))
        assertEquals("snowball-Portuguese", BookAnalyzers.analyzerId("pt-BR"))
        assertSame(BookAnalyzers.forLanguage("zh"), BookAnalyzers.forLanguage("zh-Hans"))
    }

    @Test fun unmappedAndMissingLanguagesUseTheSimpleAnalyzer() {
        for (code in listOf("hbo", "en", "xx", "", null)) {
            assertEquals("simple", BookAnalyzers.analyzerId(code), "analyzer id for $code")
        }
    }

    @Test fun sameInstanceIsReturnedPerAnalyzerId() {
        assertSame(BookAnalyzers.forLanguage("de"), BookAnalyzers.forLanguage("de"))
        assertSame(BookAnalyzers.forLanguage("zh"), BookAnalyzers.forLanguage("ja"))
        assertSame(BookAnalyzers.forLanguage("el"), BookAnalyzers.forLanguage("grc"))
        assertSame(BookAnalyzers.forLanguage(null), BookAnalyzers.forLanguage("hbo"))
    }

    @Test fun forBookOfAnEnglishFixtureBookIsTheSimpleAnalyzer() {
        val book = assertNotNull(Books.installed().getBook("K4ZTextBook"))
        assertEquals("en", book.language.code)
        assertSame(BookAnalyzers.forLanguage("en"), BookAnalyzers.forBook(book))
    }

    @Test fun simpleAnalyzerFoldsAccentsAndLowercases() {
        // Baseline (oracle `analyze en`): surely 1, cafe 1.
        assertEquals(
            listOf(AnalyzedToken("surely", 0), AnalyzedToken("cafe", 1)),
            BookAnalyzers.forLanguage("hbo").analyze("Sûrely Café"),
        )
    }
}
