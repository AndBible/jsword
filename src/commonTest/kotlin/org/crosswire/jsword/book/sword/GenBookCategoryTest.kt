package org.crosswire.jsword.book.sword

import org.crosswire.jsword.book.BookCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class GenBookCategoryTest {
    private fun metadata(driver: String, category: String?): SwordBookMetaData {
        val conf = """
            [CategoryTest]
            ModDrv=$driver
            Description=Category test
            DataPath=./test
        """.trimIndent() + (category?.let { "\nCategory=$it" } ?: "")
        return SwordBookMetaData(conf.encodeToByteArray(), "categorytest.conf")
    }

    @Test
    fun treeBackedBooksCannotBeVerseBasedDocuments() {
        for (category in listOf("Biblical Texts", "Commentaries")) {
            val metadata = metadata("RawGenBook", category)
            assertEquals(BookCategory.GENERAL_BOOK, metadata.bookCategory, category)
            assertEquals("Generic Books", metadata.getProperty("Category"), category)
        }
    }

    @Test
    fun legitimateGenBookCategoriesArePreserved() {
        for ((category, expected) in listOf(
            null to BookCategory.GENERAL_BOOK,
            "Generic Books" to BookCategory.GENERAL_BOOK,
            "Maps" to BookCategory.MAPS,
            "And Bible" to BookCategory.AND_BIBLE,
        )) {
            assertEquals(expected, metadata("RawGenBook", category).bookCategory, category)
        }
    }

    @Test
    fun passageDriversKeepTheirVerseBasedCategories() {
        assertEquals(BookCategory.BIBLE, metadata("RawText", "Biblical Texts").bookCategory)
        assertEquals(BookCategory.COMMENTARY, metadata("RawCom", "Commentaries").bookCategory)
    }
}
