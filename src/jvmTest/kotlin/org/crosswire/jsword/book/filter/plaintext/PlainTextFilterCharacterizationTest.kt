/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * \u00A9 CrossWire Bible Society, 2026
 *
 */
package org.crosswire.jsword.book.filter.plaintext

import fi.sykero.kdom.Content
import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.junit.Assume
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

/**
 * Characterization of [PlainTextFilter]: pins the behaviour of the Java class
 * before it is converted to Kotlin. Expected values were derived by reading the Java:
 * the input is split at every '\n' (StringUtil.splitAll keeps empty parts, and returns
 * no part for ""), each part but the last is followed by an lb element, and text is
 * added as it is (no trimming, no handling of '\r').
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class PlainTextFilterCharacterizationTest {
    private var book: Book? = null

    @BeforeTest
    fun setUp() {
        book = Books.installed().getBook("KJV")
        Assume.assumeNotNull(book)
    }

    private fun filter(input: String?): List<Content> {
        return PlainTextFilter().toOSIS(book, book!!.getKey("Gen 1:1"), input)
    }

    private fun osis(input: String): String {
        val out = XMLOutputter(Format.getRawFormat())
        return out.outputString(filter(input))
    }

    @Test
    fun testSingleLine() {
        val result = filter("In the beginning")
        assertEquals(1, result.size)
        assertEquals("In the beginning", osis("In the beginning"))
    }

    @Test
    fun testLinesAreSeparatedByLb() {
        assertEquals("line1<lb />line2", osis("line1\nline2"))
        // text, lb, text
        assertEquals(3, filter("line1\nline2").size)
        assertEquals("a<lb /><lb />b", osis("a\n\nb"))
    }

    @Test
    fun testTrailingNewlineGivesLbAndEmptyText() {
        // ["a", ""]: "a", lb, then the last (empty) part is still added.
        val result = filter("a\n")
        assertEquals(3, result.size)
        assertEquals("a<lb />", osis("a\n"))
        // Only a newline: ["", ""].
        assertEquals("<lb />", osis("\n"))
    }

    @Test
    fun testCarriageReturnsAreKeptInTheText() {
        // "\r\n" splits at '\n' only; the '\r' stays and the serializer writes it as a character reference.
        assertEquals("a&#xD;<lb />b&#xD;<lb />c", osis("a\r\nb\r\nc"))
    }

    @Test
    fun testEmptyInputGivesNoContent() {
        assertEquals(0, filter("").size)
        assertEquals(0, filter(null).size)
    }

    @Test
    fun testMarkupCharactersAreTextNotMarkup() {
        assertEquals("a &amp; b &lt; c", osis("a & b < c"))
        assertEquals("&lt;b&gt;x&lt;/b&gt;", osis("<b>x</b>"))
    }

    @Test
    fun testWhitespaceIsNotTrimmed() {
        assertEquals("  a <lb />\tb  ", osis("  a \n\tb  "))
    }

    @Test
    fun testNonAscii() {
        val text = "\u00E9\u20AC \u0391\u03B3\u03AC\u03C0\u03B7"
        assertEquals(text + "<lb />" + text, osis(text + "\n" + text))
    }

    @Test
    fun testCloneIsIndependentCopy() {
        val filter = PlainTextFilter()
        val clone = filter.clone()
        assertNotSame(filter, clone)
        assertEquals(PlainTextFilter::class.java, clone.javaClass)
    }
}
