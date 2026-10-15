package org.crosswire.ksword.k5b

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.BookFilter
import org.crosswire.jsword.book.BookFilterIterator
import org.crosswire.jsword.book.BookFilters
import org.crosswire.jsword.book.BookSet
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.BooksEvent
import org.crosswire.jsword.book.BooksListener
import org.crosswire.jsword.book.AbstractBookList
import org.crosswire.jsword.book.DataPolice
import fi.sykero.kdom.output.XMLOutputter
import fi.sykero.kdom.output.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * K5b Task 6 characterization: events, book lists, filters, `BookSet`, `BookFilterIterator`, `BookData`, `DataPolice`.
 * Green on the pre-conversion code and must stay green after the common-ready conversion.
 */
class BookListContractTest {
    private fun initials(books: Collection<Book>) = books.map { it.initials }

    private fun installed() = Books.installed().getBooks()

    @Test fun filterFactoriesSelectExactInitials() {
        val all = Books.installed()
        val actual = linkedMapOf<String, List<String>>(
            "all" to initials(all.getBooks(BookFilters.getAll())),
            "bibles" to initials(all.getBooks(BookFilters.getBibles())),
            "onlyBibles" to initials(all.getBooks(BookFilters.getOnlyBibles())),
            "nonBibles" to initials(all.getBooks(BookFilters.getNonBibles())),
            "dictionaries" to initials(all.getBooks(BookFilters.getDictionaries())),
            "glossaries" to initials(all.getBooks(BookFilters.getGlossaries())),
            "devotionals" to initials(all.getBooks(BookFilters.getDailyDevotionals())),
            "commentaries" to initials(all.getBooks(BookFilters.getCommentaries())),
            "generalBooks" to initials(all.getBooks(BookFilters.getGeneralBooks())),
            "maps" to initials(all.getBooks(BookFilters.getMaps())),
            "other" to initials(all.getBooks(BookFilters.getOther())),
            "greekDefs" to initials(all.getBooks(BookFilters.getGreekDefinitions())),
            "greekParse" to initials(all.getBooks(BookFilters.getGreekParse())),
            "hebrewDefs" to initials(all.getBooks(BookFilters.getHebrewDefinitions())),
            "hebrewParse" to initials(all.getBooks(BookFilters.getHebrewParse())),
            "customKjv" to initials(all.getBooks(BookFilters.getCustom("initials=KJV"))),
            "customLangEn" to initials(all.getBooks(BookFilters.getCustom("Lang=en"))),
            "customNotEn" to initials(all.getBooks(BookFilters.getCustom("Lang!=en"))),
            "customTwo" to initials(all.getBooks(BookFilters.getCustom("Lang=en;initials=KJV"))),
            "both" to initials(all.getBooks(BookFilters.both(BookFilters.getBibles(), BookFilters.getCustom("Lang=fi")))),
            "either" to initials(all.getBooks(BookFilters.either(BookFilters.getDictionaries(), BookFilters.getCustom("initials=KJV")))),
            "byDriver" to initials(all.getBooks(BookFilters.getBooksByDriver(installed().first { it.initials == "KJV" }.driver))),
        )
        val withCommentaries = try {
            BookFilters.isCommentariesWithBibles = true
            initials(all.getBooks(BookFilters.getBibles())) to initials(all.getBooks(BookFilters.getNonBibles()))
        } finally {
            BookFilters.isCommentariesWithBibles = false
        }
        assertEquals(EXPECTED_FILTERS, actual.toString())
        assertEquals(EXPECTED_WITH_COMMENTARIES, withCommentaries.toString())
    }

    @Test fun customFilterRejectsMalformedMatch() {
        for (bad in listOf("Lang", "=en", "Lang=", "a=b=c")) {
            try {
                BookFilters.getCustom(bad)
                fail("expected IllegalArgumentException for $bad")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message!!, expected.message!!.startsWith("Filter format is 'property=value'"))
            }
        }
    }

    @Test fun filterIteratorMatchesNoneOneAndAll() {
        val three = installed().take(3)
        assertEquals(3, three.size)
        for ((filter, expected) in listOf<Pair<BookFilter?, List<Book>>>(
            BookFilter { false } to emptyList(),
            BookFilter { it === three[1] } to listOf(three[1]),
            BookFilter { true } to three,
            null to three,
        )) {
            val it = BookFilterIterator(three, filter)
            val seen = ArrayList<Book>()
            while (it.hasNext()) seen.add(it.next())
            assertEquals(expected, seen)
            assertFalse(it.hasNext())
            try {
                it.next()
                fail("next() past the end must throw")
            } catch (e: NoSuchElementException) {
                // expected
            }
        }
    }

    @Test fun filterIteratorQuirks() {
        val three = installed().take(3)
        val it = BookFilterIterator(three, null)
        assertSame(it, it.iterator())
        assertTrue(it.hasNext())
        assertSame(three[0], it.next())
        assertSame(three[0], it.next()) // next() does not advance; only hasNext() does
        assertTrue(it.hasNext())
        assertSame(three[1], it.next())
        try {
            it.remove()
            fail("remove must be unsupported")
        } catch (e: UnsupportedOperationException) {
            // expected
        }
        try {
            BookFilterIterator(three, null).next()
            fail("next() before hasNext() must throw")
        } catch (e: NoSuchElementException) {
            // expected
        }
    }

    @Test fun bookSetKeepsSortedDistinctOrder() {
        val books = installed()
        val reversed = books.reversed()
        val set = BookSet(reversed + reversed)
        assertEquals(books.sorted().map { it.initials }, initials(set))
        assertFalse(set.add(books[0]))
        val first = set[0]
        set.add(5, first) // index ignored, duplicate dropped
        assertEquals(books.size, set.size)
        val removed = set.removeAt(0)
        assertSame(first, removed)
        assertTrue(set.add(first))
        assertSame(first, set[0])
        val second = set[1]
        assertSame(second, set.set(1, second)) // returns the replaced item, then re-inserts sorted
        assertSame(second, set[1])
        assertEquals(books.sorted().map { it.initials }, initials(set))
    }

    @Test fun bookSetGroupsAndFilter() {
        val books = installed()
        val set = BookSet(books)
        // SwordBookMetaData.propertyKeys is deliberately null, so the groups of real books throw.
        try {
            set.groups
            fail("groups of Sword books must throw NullPointerException")
        } catch (e: NullPointerException) {
            // expected
        }
        val langs = set.getGroup("Lang")
        assertEquals(langs.map { it.toString() }.sorted(), langs.map { it.toString() })
        assertEquals(EXPECTED_GROUPS, "langs=$langs")
        val en = set.filter("Lang", "en")
        assertEquals(initials(books.filter { it.getProperty("Lang") == "en" }.sorted()), initials(en))
        assertEquals(books.size, set.size) // filter works on a copy
        val kjvOnly = set.filter(org.crosswire.common.util.Filter<Book> { it.initials == "KJV" })
        assertEquals(listOf("KJV"), initials(kjvOnly))
        assertTrue(kjvOnly is BookSet)
    }

    @Test fun booksEventAndListenerDispatch() {
        val source = Any()
        val book = installed().first()
        val ev = BooksEvent(source, book, true)
        assertSame(source, ev.getSource())
        assertSame(book, ev.book)
        assertTrue(ev.isAddition)
        assertFalse(BooksEvent(source, book, false).isAddition)
        try {
            BooksEvent(null, book, true)
            fail("null source must be rejected")
        } catch (e: IllegalArgumentException) {
            assertEquals("null source", e.message)
        }
        val list = object : AbstractBookList() {
            override fun getBooks(): MutableList<Book> = installed()
            fun fire(added: Boolean) = fireBooksChanged(this, book, added)
        }
        val log = ArrayList<String>()
        val l = object : BooksListener {
            override fun bookAdded(ev: BooksEvent) { log.add("add " + ev.book!!.initials) }
            override fun bookRemoved(ev: BooksEvent) { log.add("remove " + ev.book!!.initials) }
        }
        list.addBooksListener(l)
        list.fire(true)
        list.fire(false)
        list.removeBooksListener(l)
        list.fire(true)
        assertEquals(listOf("add ${book.initials}", "remove ${book.initials}"), log)
        val filtered = list.getBooks(BookFilters.getCustom("initials=KJV"))
        assertEquals(listOf("KJV"), initials(filtered))
        try {
            filtered.clear()
            fail("filtered list is unmodifiable")
        } catch (e: UnsupportedOperationException) {
            // expected
        }
    }

    @Test fun dataPoliceReportingFlag() {
        assertFalse(DataPolice.isReporting)
        DataPolice.report(null, null, "ignored while reporting is off")
        try {
            DataPolice.isReporting = true
            assertTrue(DataPolice.isReporting)
            DataPolice.report(installed().first(), null, "test message")
            try {
                DataPolice.report(null, null, "needs a book")
                fail("a null book is dereferenced while reporting")
            } catch (e: NullPointerException) {
                // expected
            }
        } finally {
            DataPolice.isReporting = false
        }
        assertFalse(DataPolice.isReporting)
    }

    @Test fun bookDataFragmentMatchesOracleRows() {
        val oracle = File(File(System.getProperty("user.dir")).parentFile, ".local/k5-oracle/jsword/KJV.osis.tsv")
        assumeTrue("K5 oracle absent: $oracle", oracle.isFile)
        val book = requireNotNull(Books.installed().getBook("KJV"))
        val rows = oracle.readLines().filter { it.isNotBlank() }.map { it.split('\t') }.take(8)
        assertTrue(rows.isNotEmpty())
        for (row in rows) {
            val actual = XMLOutputter(Format.getRawFormat()).outputString(BookData(book, book.getKey(row[0])).getOsisFragment(false))
            assertEquals(row[0], row[3], actual)
        }
        val gen11 = XMLOutputter(Format.getRawFormat()).outputString(BookData(book, book.getKey("Gen.1.1")).getOsisFragment(false))
        assertTrue(gen11.contains("beginning"))
        assertTrue(BookData(book, book.getKey("Gen.1.1")).firstBook === book)
    }

    /** Four TreeMap properties the `BookData` verse-content map must keep: collapse, first key, value replace, order. */
    @Test fun treeMapVerseContentSemantics() {
        val v11n = org.crosswire.jsword.versification.system.Versifications.instance().getVersification("KJV")!!
        val a5 = org.crosswire.jsword.passage.Verse(v11n, 5)
        val a5again = org.crosswire.jsword.passage.Verse(v11n, 5)
        val a2 = org.crosswire.jsword.passage.Verse(v11n, 2)
        val a9 = org.crosswire.jsword.passage.Verse(v11n, 9)
        val first = mutableListOf<fi.sykero.kdom.Content>(fi.sykero.kdom.Text("one"))
        val second = mutableListOf<fi.sykero.kdom.Content>(fi.sykero.kdom.Text("two"))
        val map = java.util.TreeMap<org.crosswire.jsword.passage.Verse, MutableList<fi.sykero.kdom.Content>>()
        map[a9] = first
        map[a5] = first
        map[a2] = first
        map[a5again] = second
        assertEquals(3, map.size)
        assertSame(second, map[a5])
        assertSame(a5, map.entries.single { it.key.ordinal == 5 }.key)
        assertEquals(listOf(2, 5, 9), map.entries.map { it.key.ordinal })
        assertNull(map[org.crosswire.jsword.passage.Verse(v11n, 7)])
    }

    /** The same four properties on the replacement helper. */
    @Test fun verseContentMapKeepsTreeMapSemantics() {
        val v11n = org.crosswire.jsword.versification.system.Versifications.instance().getVersification("KJV")!!
        val a5 = org.crosswire.jsword.passage.Verse(v11n, 5)
        val a5again = org.crosswire.jsword.passage.Verse(v11n, 5)
        val first = mutableListOf<fi.sykero.kdom.Content>(fi.sykero.kdom.Text("one"))
        val second = mutableListOf<fi.sykero.kdom.Content>(fi.sykero.kdom.Text("two"))
        val map = org.crosswire.jsword.book.VerseContentMap()
        map[org.crosswire.jsword.passage.Verse(v11n, 9)] = first
        map[a5] = first
        map[org.crosswire.jsword.passage.Verse(v11n, 2)] = first
        map[a5again] = second
        assertEquals(3, map.size)
        assertSame(second, map[a5])
        assertSame(a5, map.entries.single { it.key.ordinal == 5 }.key)
        assertEquals(listOf(2, 5, 9), map.entries.map { it.key.ordinal })
        assertNull(map[org.crosswire.jsword.passage.Verse(v11n, 7)])
    }

    /** The multi-book path builds its table through the verse-content map; its output is pinned by length and hash. */
    @Test fun bookDataTwoBookTable() {
        val kjv = requireNotNull(Books.installed().getBook("KJV"))
        val isv = requireNotNull(Books.installed().getBook("ISV"))
        val key = kjv.getKey("Gen.1.1-Gen.1.3")
        val xml = XMLOutputter(Format.getRawFormat()).outputString(BookData(arrayOf(kjv, isv), key, true).getOsisFragment(false))
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(xml.toByteArray()).joinToString("") { "%02x".format(it) }
        assertEquals(2087, xml.length)
        assertEquals("39c7a97a6db3540035aa1de159aa0ba7a5e76bed394b337f1dd72240cd5a8689", hash)
        assertTrue(xml.contains("<table"))
    }

    private companion object {
        const val EXPECTED_FILTERS =
            "{all=[ESV2011, FinPR, FinRK, FinSTLK2017, ISV, KJV, NASB, StrongsGreek, StrongsHebrew], bibles=[ESV2011, FinPR, FinRK, FinSTLK2017, ISV, KJV, NASB], onlyBibles=[ESV2011, FinPR, FinRK, FinSTLK2017, ISV, KJV, NASB], nonBibles=[StrongsGreek, StrongsHebrew], dictionaries=[StrongsGreek, StrongsHebrew], glossaries=[], devotionals=[], commentaries=[], generalBooks=[], maps=[], other=[], greekDefs=[StrongsGreek], greekParse=[], hebrewDefs=[StrongsHebrew], hebrewParse=[], customKjv=[KJV], customLangEn=[ESV2011, ISV, KJV, NASB, StrongsGreek, StrongsHebrew], customNotEn=[FinPR, FinRK, FinSTLK2017], customTwo=[KJV], both=[FinPR, FinRK, FinSTLK2017], either=[KJV, StrongsGreek, StrongsHebrew], byDriver=[ESV2011, FinPR, FinRK, FinSTLK2017, ISV, KJV, NASB, StrongsGreek, StrongsHebrew]}"
        const val EXPECTED_WITH_COMMENTARIES = "([ESV2011, FinPR, FinRK, FinSTLK2017, ISV, KJV, NASB], [StrongsGreek, StrongsHebrew])"
        const val EXPECTED_GROUPS = "langs=[en, fi]"
    }
}
