package org.crosswire.jsword.index.fts

import fi.sykero.kdom.Element
import okio.FileSystem
import okio.Path
import org.crosswire.common.util.Language
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.analysis.BookAnalyzers
import org.crosswire.jsword.index.search.SearchType
import org.crosswire.ksword.fixtures.fixtureRoot
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** #3041: Finnish terms and FinRK-style cross references through the real index builder and query analyzer. */
class FinnishSearchTest {
    private val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "fts-finnish-${Random.nextLong().toString(16)}"
    private var savedAugmentPath: Array<Path> = emptyArray()
    private lateinit var manager: FtsIndexManager
    private lateinit var book: Book

    @BeforeTest fun install() {
        savedAugmentPath = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
        val base = assertNotNull(Books.installed().getBook("K4RawText"))
        val keys = base.createEmptyKeyList().apply {
            listOf("1Thess.2.14", "Rom.14.8", "John.1.1", "1Sam.1.26").forEach { addAll(base.getKey(it)) }
        }
        book = object : Book by base {
            override val language = Language("fi")
            override var indexStatus = IndexStatus.UNDONE
            override fun getGlobalKeyList() = keys
        }
        manager = FtsIndexManager(root / "search", root / "lucene") { _, key ->
            Element("div").apply {
                addContent(Element("verse").apply {
                    addContent(when (key.getOsisRef()) {
                        "1Thess.2.14" -> "Omat maanmiehenne. "
                        "Rom.14.8" -> "Elämme Herralle. "
                        "1Sam.1.26" -> "Elät, herrani. "
                        else -> "Alussa. "
                    })
                    addContent(Element("note").apply {
                        setAttribute("type", "crossReference")
                        addContent(Element("reference").apply {
                            setAttribute("osisRef", "Acts.17.5")
                            addContent("Acts.17.5 maamiehenne Elämme Herralle")
                        })
                    })
                })
            }
        }
        manager.scheduleIndexCreation(book)
    }

    @AfterTest fun restore() {
        if (::manager.isInitialized) manager.closeAllIndexes()
        SwordBookPath.setAugmentPath(savedAugmentPath)
        FileSystem.SYSTEM.deleteRecursively(root, mustExist = false)
    }

    private fun refs(query: String) = manager.getIndex(book).find(query).map { it.getOsisRef() }

    @Test fun correctSpellingFindsTheReportedVerse() {
        assertEquals(listOf("1Thess.2.14"), refs("maanmiehenne"))
        assertEquals(listOf("1Thess.2.14"), refs("MAANMIEHENNE"))
    }

    @Test fun missingLetterIsNotAnExactMatchAndNotesDoNotLeakIntoContent() {
        assertEquals(emptyList(), refs("maamiehenne"))
        assertEquals(emptyList(), refs("Acts"))
    }

    @Test fun finnishInflectionsShareStemsButTheMisspellingDoesNot() {
        val analyzer = BookAnalyzers.forBook(book)
        assertEquals(listOf("elä", "elä", "elä", "her", "her", "her"),
            analyzer.analyze("elämme elät elää Herralle herrani Herra").map { it.text })
        assertEquals(listOf("maamieh", "maanmieh"),
            analyzer.analyze("maamiehenne maanmiehenne").map { it.text })
    }

    @Test fun allWordsAndPhraseDoNotMatchWordsFoundOnlyInCrossReferences() {
        for (type in listOf(SearchType.ALL_WORDS, SearchType.PHRASE)) {
            // Finnish Snowball matches inflections, not literal surface spellings (also in phrase mode).
            assertEquals(listOf("1Sam.1.26", "Rom.14.8"), refs(type.decorate("Elämme Herralle")), type.name)
        }
    }
}
