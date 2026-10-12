package org.crosswire.jsword.book.sword

import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.Books
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FacadeHooksTest {
    @Test fun bookTypeCreatesTheSameFacadeClass() {
        // All thirteen built-ins checked against getBook at d2759f58.
        val expected = mapOf(
            "RawText" to "SwordBook", "zText" to "SwordBook", "zText4" to "SwordBook",
            "RawCom" to "SwordBook", "RawCom4" to "SwordBook", "zCom" to "SwordBook", "zCom4" to "SwordBook",
            "HREFCom" to "SwordBook", "RawFiles" to "SwordBook",
            "RawLD" to "SwordDictionary", "RawLD4" to "SwordDictionary", "zLD" to "SwordDictionary",
            "RawGenBook" to "SwordGenBook",
        )
        for ((driver, cls) in expected) {
            val md = metadata(driver)
            assertEquals(cls, md.bookType!!.createBook(md)::class.simpleName, driver)
        }
    }

    @Test fun everyDictionaryDriverCreatesDailyDevotionFacade() {
        for (driver in listOf("RawLD", "RawLD4", "zLD")) {
            val md = metadata(driver, "Category=Daily Devotional\n")
            assertEquals(BookCategory.DAILY_DEVOTIONS, md.bookCategory)
            assertEquals("SwordDailyDevotion", md.bookType!!.createBook(md)::class.simpleName, driver)
        }
    }

    @Test fun swordDriverIsRegistered() {
        assertTrue(Books.installed().drivers.any { it is SwordBookDriver })
    }

    private fun metadata(driver: String, extra: String = "") = SwordBookMetaData(
        ("[X]\nDataPath=./modules/x/\nModDrv=$driver\nLang=en\nEncoding=UTF-8\n" + extra).toByteArray(), "x"
    )
}
