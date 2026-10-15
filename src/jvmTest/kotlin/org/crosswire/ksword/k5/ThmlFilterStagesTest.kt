package org.crosswire.ksword.k5

import java.io.File
import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.filter.thml.THMLFilter
import org.crosswire.jsword.passage.Key
import org.junit.Assert.*
import org.junit.Test

/** Focused immutable subset of the pre-migration oracle, not newly generated expectations. */
class ThmlFilterStagesTest {
    private fun rows(module: String, section: String): List<List<String>> =
        requireNotNull(javaClass.getResourceAsStream("/k5/thml-stages/$module.$section.tsv"))
            .bufferedReader().use { it.readLines() }.map { line -> line.split('\t').map(::unescape) }

    private fun unescape(text: String): String = buildString {
        var index = 0
        while (index < text.length) {
            val c = text[index++]
            if (c != '\\') append(c) else append(when (val escaped = text[index++]) {
                'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; else -> escaped
            })
        }
    }

    @Test fun frozenThmlAcceptanceStagesAndRawOutput() = JswordOracleTest().withExtraModules {
        val book = requireNotNull(Books.installed().getBook("ISV"))
        val sources = rows("ISV", "inputs").filter { it[1] == "source" }.associate { it[0] to it[3] }
        val parse = THMLFilter::class.java.getDeclaredMethod("parse", Book::class.java, Key::class.java,
            String::class.java, String::class.java).apply { isAccessible = true }
        val expected = rows("ISV", "osis")
        assertEquals(21, expected.size)
        for (row in expected) {
            val key = book.getKey(row[0])
            val raw = sources.getValue(row[0])
            assertEquals("Module provenance ${row[0]}", raw, book.getRawText(key))
            val entities = XMLUtil.cleanAllEntities(raw)!!
            val chars = XMLUtil.cleanAllCharacters(entities)
            val stages = listOf(entities, chars, XMLUtil.closeEmptyTags(chars), XMLUtil.cleanAllTags(chars))
            val filter = THMLFilter()
            val accepted = stages.indexOfFirst { parse.invoke(filter, book, key, it, "characterization") != null }
            assertEquals("Acceptance ${row[0]}", row[1].toInt(), accepted)
            val actual = XMLOutputter(Format.getRawFormat()).outputString(BookData(book, key).getOsisFragment(false))
            assertEquals("Raw output ${row[0]}", row[3], actual)
        }
    }

    @Test fun frozenNasbFallbackOutput() = JswordOracleTest().withExtraModules {
        val book = requireNotNull(Books.installed().getBook("NASB"))
        val expected = rows("NASB", "osis")
        assertEquals(4, expected.size)
        for (row in expected) {
            assertEquals("2", row[1])
            val actual = XMLOutputter(Format.getRawFormat()).outputString(BookData(book, book.getKey(row[0])).getOsisFragment(false))
            assertEquals(row[0], row[3], actual)
        }
    }
}
