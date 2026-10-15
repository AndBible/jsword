package org.crosswire.ksword.k5b

import org.crosswire.common.icu.DateFormatter
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.DevotionDateFormat
import org.crosswire.jsword.book.sword.SwordBookDriver
import org.crosswire.jsword.book.sword.SwordBookMetaDataJvm
import org.crosswire.jsword.book.sword.SwordDailyDevotion
import org.crosswire.jsword.book.sword.SwordDictionary
import org.crosswire.jsword.book.sword.SwordGenBook
import org.crosswire.jsword.index.IndexManagerFactory
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.PreferredKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Characterization of the Sword book classes and the Sword driver before and after their common-ready
 * conversion (K5b Task 9). Uses the K4 fixture modules.
 */
class SwordFacadeContractTest {
    private fun tree(key: Key): List<String> = (0 until key.getChildCount()).flatMap { i ->
        val child = key.get(i)!!
        listOf(child.getName()) + if (child.canHaveChildren()) tree(child).map { "  $it" } else emptyList()
    }

    @Test fun genBookKeyTreeOfK4GenBook() {
        val book = K5bFixtureBook.create("K4GenBook")
        assertTrue(book is SwordGenBook)
        val root = book.getGlobalKeyList()
        // The key tree: names and nesting as the backend reads them.
        assertEquals(listOf("Part 1", "  Chapter 1", "  Chapter 2", "Part 2"), tree(root))
        assertEquals("In the beginning God created the heaven and the earth.",
            book.getRawText(root.get(0)!!.get(0)).trim())
        // Lookup falls back to case-insensitive, prefix and substring matches on the OSIS reference.
        assertEquals(root.get(1), book.getKey("Part 2"))
        assertEquals(root.get(1), book.getKey("part 2"))
    }

    @Test fun dictionaryKeysAndRawTextOfK4RawLD() {
        val book = K5bFixtureBook.create("K4RawLD")
        assertTrue(book is SwordDictionary)
        assertEquals(listOf("G0025", "G0026", "G3056"), book.getGlobalKeyList().map { it.getName() })
        for ((k, v) in listOf("G0025" to "God so loved the world.", "G0026" to "God is love.", "G3056" to "In the beginning was the Word.")) {
            assertEquals(k, "$k\n$v", book.getRawText(book.getKey(k)).trim())
        }
    }

    @Test fun dailyDevotionPreferredKeyIsTodaysFormattedDate() {
        val book = K5bFixtureBook.create("K4Devotion")
        assertTrue(book is SwordDailyDevotion)
        val before = GregorianCalendar()
        val preferred = (book as PreferredKey).getPreferred().getName()
        val after = GregorianCalendar()
        val candidates = setOf(before, after).map {
            DevotionDateFormat.format(it.get(Calendar.MONTH) + 1, it.get(Calendar.DATE))
        }
        assertTrue("$preferred in $candidates", preferred in candidates)
    }

    /**
     * Every day of a leap year goes through DevotionDateFormat and back. DevotionDateFormat formats in the current
     * year, so only 29 February can fail, and only in a non-leap current year (it rolls over to 1 March). That is
     * why SwordDailyDevotion keeps a platform key function instead of formatting (month, day) through
     * DevotionDateFormat: the strings are not equal for every day of a leap year.
     */
    @Test fun leapYearDayLoopAgainstDevotionDateFormat() {
        val mismatches = mutableListOf<String>()
        var days = 0
        val cal = GregorianCalendar(2024, Calendar.JANUARY, 1)
        while (cal.get(Calendar.YEAR) == 2024) {
            days++
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DATE)
            if (DevotionDateFormat.parseMonthDay(DevotionDateFormat.format(month, day)) != (month to day)) mismatches += "$month.$day"
            cal.add(Calendar.DATE, 1)
        }
        assertEquals(366, days)
        val currentYearIsLeap = GregorianCalendar().isLeapYear(GregorianCalendar().get(Calendar.YEAR))
        assertEquals(if (currentYearIsLeap) emptyList<String>() else listOf("2.29"), mismatches)
    }

    @Test fun deleteRemovesConfAndModuleDirectory() {
        val fixtures = File(checkNotNull(System.getProperty("ksword.fixturesDir")), "space dir/ä")
        val tmp = kotlin.io.path.createTempDirectory("k5b-delete").toFile().canonicalFile
        try {
            File(fixtures, "mods.d/K4RawText.conf").copyTo(File(tmp, "mods.d/K4RawText.conf"))
            File(fixtures, "modules/K4RawText").copyRecursively(File(tmp, "modules/K4RawText"))
            val conf = File(tmp, "mods.d/K4RawText.conf")
            val md = SwordBookMetaDataJvm.create(conf, tmp.toURI())
            md.setDriver(SwordBookDriver.instance())
            val book: Book = md.bookType!!.createBook(md)
            // Different initials than the installed fixtures, so it cannot collide with other tests.
            Books.installed().addBook(book)
            val driver = SwordBookDriver.instance()
            assertTrue(driver.isDeletable(book))
            driver.delete(book)
            assertFalse(conf.exists())
            assertFalse(File(tmp, "modules/K4RawText").exists())
            assertFalse(IndexManagerFactory.getIndexManager().isIndexed(book))
            assertEquals(null, Books.installed().getBook("K4RawText")?.takeIf { it === book })
            assertFalse(driver.isDeletable(book))
        } finally {
            Books.installed().getBooks().firstOrNull { it.bookMetaData.library?.path?.startsWith(tmp.path) == true }
                ?.let { Books.installed().removeBook(it) }
            tmp.deleteRecursively()
        }
    }

    @Test fun deleteOfBookWithoutConfFileThrows() {
        val fixtures = File(checkNotNull(System.getProperty("ksword.fixturesDir")), "space dir/ä")
        val tmp = kotlin.io.path.createTempDirectory("k5b-delete-missing").toFile().canonicalFile
        try {
            File(fixtures, "mods.d/K4RawText.conf").copyTo(File(tmp, "mods.d/K4RawText.conf"))
            File(fixtures, "modules/K4RawText").copyRecursively(File(tmp, "modules/K4RawText"))
            val md = SwordBookMetaDataJvm.create(File(tmp, "mods.d/K4RawText.conf"), tmp.toURI())
            md.setDriver(SwordBookDriver.instance())
            val book = md.bookType!!.createBook(md)
            File(tmp, "mods.d/K4RawText.conf").delete()
            val e = runCatching { SwordBookDriver.instance().delete(book) }.exceptionOrNull()
            assertTrue("$e", e is org.crosswire.jsword.book.BookException)
            assertTrue(e!!.message!!.startsWith("Unable to delete: "))
            assertTrue(File(tmp, "modules/K4RawText").exists())
        } finally {
            tmp.deleteRecursively()
        }
    }
}
