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

    @Test fun augmentPathChangeRegistersSwordDriverInEmptyRegistry() {
        val registry = Books.installed()
        val fields = listOf("books", "initials", "names", "driverSet").map { name ->
            Books::class.java.getDeclaredField(name).apply { isAccessible = true }
        }
        val augmentField = SwordBookPath::class.java.getDeclaredField("augmentPath").apply {
            isAccessible = true
        }
        synchronized(registry) {
            val originalRegistry = fields.map { it.get(registry) }
            val originalAugment = augmentField.get(null)
            try {
                // Swap, not clear: retain the original registry objects for exact restoration.
                fields[0].set(registry, java.util.TreeSet<org.crosswire.jsword.book.Book>())
                fields[1].set(registry, HashMap<String, org.crosswire.jsword.book.Book>())
                fields[2].set(registry, HashMap<String, org.crosswire.jsword.book.Book>())
                fields[3].set(registry, HashSet<org.crosswire.jsword.book.BookDriver>())
                assertTrue(registry.drivers.isEmpty())

                SwordBookPath.setAugmentPath(SwordBookPath.getAugmentPath())

                assertEquals(listOf(SwordBookDriver.instance()), registry.drivers.toList())
            } finally {
                // Restore directly: cleanup must not depend on the registration hook under test.
                augmentField.set(null, originalAugment)
                fields.zip(originalRegistry).forEach { (field, original) -> field.set(registry, original) }
            }
        }
    }

    private fun metadata(driver: String, extra: String = "") = SwordBookMetaData(
        ("[X]\nDataPath=./modules/x/\nModDrv=$driver\nLang=en\nEncoding=UTF-8\n" + extra).toByteArray(), "x"
    )
}
