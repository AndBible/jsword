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
package org.crosswire.jsword.book.filter.plaintext;

import java.util.List;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.Books;
import org.jdom2.Content;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Characterization of {@link PlainTextFilter}: pins the behaviour of the Java class
 * before it is converted to Kotlin. Expected values were derived by reading the Java:
 * the input is split at every '\n' (StringUtil.splitAll keeps empty parts, and returns
 * no part for ""), each part but the last is followed by an lb element, and text is
 * added as it is (no trimming, no handling of '\r').
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class PlainTextFilterCharacterizationTest {
    private Book book;

    @Before
    public void setUp() throws Exception {
        book = Books.installed().getBook("KJV");
        Assume.assumeNotNull(book);
    }

    private List<Content> filter(String input) throws Exception {
        return new PlainTextFilter().toOSIS(book, book.getKey("Gen 1:1"), input);
    }

    private String osis(String input) throws Exception {
        XMLOutputter out = new XMLOutputter(Format.getRawFormat());
        return out.outputString(filter(input));
    }

    @Test
    public void testSingleLine() throws Exception {
        List<Content> result = filter("In the beginning");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("In the beginning", osis("In the beginning"));
    }

    @Test
    public void testLinesAreSeparatedByLb() throws Exception {
        Assert.assertEquals("line1<lb />line2", osis("line1\nline2"));
        // text, lb, text
        Assert.assertEquals(3, filter("line1\nline2").size());
        Assert.assertEquals("a<lb /><lb />b", osis("a\n\nb"));
    }

    @Test
    public void testTrailingNewlineGivesLbAndEmptyText() throws Exception {
        // ["a", ""]: "a", lb, then the last (empty) part is still added.
        List<Content> result = filter("a\n");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("a<lb />", osis("a\n"));
        // Only a newline: ["", ""].
        Assert.assertEquals("<lb />", osis("\n"));
    }

    @Test
    public void testCarriageReturnsAreKeptInTheText() throws Exception {
        // "\r\n" splits at '\n' only; the '\r' stays and the serializer writes it as a character reference.
        Assert.assertEquals("a&#xD;<lb />b&#xD;<lb />c", osis("a\r\nb\r\nc"));
    }

    @Test
    public void testEmptyInputGivesNoContent() throws Exception {
        Assert.assertEquals(0, filter("").size());
        Assert.assertEquals(0, filter(null).size());
    }

    @Test
    public void testMarkupCharactersAreTextNotMarkup() throws Exception {
        Assert.assertEquals("a &amp; b &lt; c", osis("a & b < c"));
        Assert.assertEquals("&lt;b&gt;x&lt;/b&gt;", osis("<b>x</b>"));
    }

    @Test
    public void testWhitespaceIsNotTrimmed() throws Exception {
        Assert.assertEquals("  a <lb />\tb  ", osis("  a \n\tb  "));
    }

    @Test
    public void testNonAscii() throws Exception {
        String text = "\u00E9\u20AC \u0391\u03B3\u03AC\u03C0\u03B7";
        Assert.assertEquals(text + "<lb />" + text, osis(text + "\n" + text));
    }

    @Test
    public void testCloneIsIndependentCopy() throws Exception {
        PlainTextFilter filter = new PlainTextFilter();
        PlainTextFilter clone = filter.clone();
        Assert.assertNotSame(filter, clone);
        Assert.assertEquals(PlainTextFilter.class, clone.getClass());
    }
}
