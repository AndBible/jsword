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
package org.crosswire.jsword.book.filter.osis

import java.lang.reflect.Proxy
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
 * Characterization of [OSISFilter]: pins the behaviour of the Java class
 * before it is converted to Kotlin. Expected values were derived by reading the
 * Java (and XMLUtil.cleanAllEntities / recloseTags / cleanAllTags), not by observing it.
 * The fragment is wrapped in a root element, parsed with SAX, and on failure retried
 * after cleanAllEntities, then recloseTags, then (cleanTags) cleanAllTags; if that fails
 * too the result is an empty p whose content (nothing) is returned.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class OSISFilterCharacterizationTest {
    private var book: Book? = null

    @BeforeTest
    fun setUp() {
        book = Books.installed().getBook("KJV")
        Assume.assumeNotNull(book)
    }

    private fun filter(b: Book, input: String): List<Content> {
        return OSISFilter().toOSIS(b, b.getKey("Gen 1:1"), input)
    }

    private fun osis(b: Book, input: String): String {
        val out = XMLOutputter(Format.getRawFormat())
        return out.outputString(filter(b, input))
    }

    private fun osis(input: String): String {
        return osis(book!!, input)
    }

    @Test
    fun testWellFormedFragmentsPassThrough() {
        val w = "<w lemma=\"strong:H7225\">In the beginning</w>"
        assertEquals(w, osis(w))
        val note = "<note type=\"study\">A note</note>"
        assertEquals(note, osis(note))
        val hi = "<hi type=\"italic\">a <hi type=\"bold\">b</hi></hi>"
        assertEquals(hi, osis(hi))
        val title = "<title type=\"main\" canonical=\"true\">T</title>"
        assertEquals(title, osis(title))
        // The wrapping root is not part of the result: a list of the children.
        assertEquals(2, filter(book!!, "<a/>x").size)
    }

    @Test
    fun testPlainTextAndEmpty() {
        assertEquals("just text", osis("just text"))
        assertEquals(0, filter(book!!, "").size)
    }

    @Test
    fun testPredefinedEntitiesAreDecodedAndReEncoded() {
        assertEquals("a &amp; b", osis("a &amp; b"))
        assertEquals("1 &lt; 2", osis("1 &lt; 2"))
    }

    @Test
    fun testNonAscii() {
        val text = "\u00E9\u20AC \u0391\u03B3\u03AC\u03C0\u03B7"
        assertEquals("<w>" + text + "</w>", osis("<w>" + text + "</w>"))
    }

    @Test
    fun testStrayAmpersandIsRepairedByCleanAllEntities() {
        // "a & b" is not well formed; cleanAllEntities turns the bare & into &amp; and the retry succeeds.
        assertEquals("a &amp; b", osis("a & b"))
    }

    @Test
    fun testUndefinedEntityIsReplaced() {
        // &foo; is unknown: handleEntity replaces it by a space.
        assertEquals("a   b", osis("a &foo; b"))
        // &eacute; is a known HTML entity that XML lacks: replaced by its character.
        assertEquals("caf\u00E9", osis("caf&eacute;"))
    }

    @Test
    fun testUnclosedTagIsRepairedByRecloseTags() {
        assertEquals("<w>unclosed</w>", osis("<w>unclosed"))
        // Closing tags at the end that do not match are stripped, then the open tag is re-closed.
        assertEquals("<w>a</w>", osis("<w>a</x>"))
    }

    @Test
    fun testEntityRepairThenRecloseBothApply() {
        assertEquals("<w>a &amp; b</w>", osis("<w>a & b"))
    }

    @Test
    fun testUnrepairableFragmentFallsBackToCleanAllTags() {
        // recloseTags gives up on a close tag with no open tag; cleanAllTags then replaces
        // "</w>" by a single space and the result parses.
        assertEquals(" text", osis("</w>text"))
        // An attribute without closing quote.
        // (recloseTags first appends </w>; then cleanAllTags replaces both tags by a space each.)
        assertEquals(" tail ", osis("<w lemma=\"x>tail"))
    }

    @Test
    fun testUnparsableEvenWithoutTagsYieldsEmptyResult() {
        // A control character is illegal in XML, no repair helps: an empty p is created and its (no) content returned.
        val result = filter(book!!, "a\u0001b")
        assertEquals(0, result.size)
    }

    @Test
    fun testMapMBookPrefixesMissingOpeningTags() {
        val mapM = Proxy.newProxyInstance(Book::class.java.classLoader, arrayOf<Class<*>>(Book::class.java)) { _, method, args ->
            when (method.name) {
                "getInitials" -> "MapM"
                "getKey" -> book!!.getKey(args!![0] as String)
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Book
        // The first loop iteration (cell) adds <cell>, row adds <row>, table adds <table> in that order,
        // so the outermost is the table.
        assertEquals("<cell>x</cell>", osis(mapM, "x</cell>"))
        assertEquals("<table><row>a</row>b</table>", osis(mapM, "a</row>b</table>"))
        // A start tag before the end tag is left alone.
        assertEquals("<cell>x</cell>", osis(mapM, "<cell>x</cell>"))
        // Other books do not get the prefix: same input goes down the recloseTags path.
        // (recloseTags strips the trailing close tag, leaving "x", which parses.)
        assertEquals("x", osis("x</cell>"))
    }

    @Test
    fun testCloneIsIndependentCopy() {
        val filter = OSISFilter()
        val clone = filter.clone()
        assertNotSame(filter, clone)
        assertEquals(OSISFilter::class.java, clone.javaClass)
    }
}
