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
package org.crosswire.jsword.book.filter.osis;

import java.lang.reflect.Proxy;
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
 * Characterization of {@link OSISFilter}: pins the behaviour of the Java class
 * before it is converted to Kotlin. Expected values were derived by reading the
 * Java (and XMLUtil.cleanAllEntities / recloseTags / cleanAllTags), not by observing it.
 * The fragment is wrapped in a root element, parsed with SAX, and on failure retried
 * after cleanAllEntities, then recloseTags, then (cleanTags) cleanAllTags; if that fails
 * too the result is an empty p whose content (nothing) is returned.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class OSISFilterCharacterizationTest {
    private Book book;

    @Before
    public void setUp() throws Exception {
        book = Books.installed().getBook("KJV");
        Assume.assumeNotNull(book);
    }

    private List<Content> filter(Book b, String input) throws Exception {
        return new OSISFilter().toOSIS(b, b.getKey("Gen 1:1"), input);
    }

    private String osis(Book b, String input) throws Exception {
        XMLOutputter out = new XMLOutputter(Format.getRawFormat());
        return out.outputString(filter(b, input));
    }

    private String osis(String input) throws Exception {
        return osis(book, input);
    }

    @Test
    public void testWellFormedFragmentsPassThrough() throws Exception {
        String w = "<w lemma=\"strong:H7225\">In the beginning</w>";
        Assert.assertEquals(w, osis(w));
        String note = "<note type=\"study\">A note</note>";
        Assert.assertEquals(note, osis(note));
        String hi = "<hi type=\"italic\">a <hi type=\"bold\">b</hi></hi>";
        Assert.assertEquals(hi, osis(hi));
        String title = "<title type=\"main\" canonical=\"true\">T</title>";
        Assert.assertEquals(title, osis(title));
        // The wrapping root is not part of the result: a list of the children.
        Assert.assertEquals(2, filter(book, "<a/>x").size());
    }

    @Test
    public void testPlainTextAndEmpty() throws Exception {
        Assert.assertEquals("just text", osis("just text"));
        Assert.assertEquals(0, filter(book, "").size());
    }

    @Test
    public void testPredefinedEntitiesAreDecodedAndReEncoded() throws Exception {
        Assert.assertEquals("a &amp; b", osis("a &amp; b"));
        Assert.assertEquals("1 &lt; 2", osis("1 &lt; 2"));
    }

    @Test
    public void testNonAscii() throws Exception {
        String text = "\u00E9\u20AC \u0391\u03B3\u03AC\u03C0\u03B7";
        Assert.assertEquals("<w>" + text + "</w>", osis("<w>" + text + "</w>"));
    }

    @Test
    public void testStrayAmpersandIsRepairedByCleanAllEntities() throws Exception {
        // "a & b" is not well formed; cleanAllEntities turns the bare & into &amp; and the retry succeeds.
        Assert.assertEquals("a &amp; b", osis("a & b"));
    }

    @Test
    public void testUndefinedEntityIsReplaced() throws Exception {
        // &foo; is unknown: handleEntity replaces it by a space.
        Assert.assertEquals("a   b", osis("a &foo; b"));
        // &eacute; is a known HTML entity that XML lacks: replaced by its character.
        Assert.assertEquals("caf\u00E9", osis("caf&eacute;"));
    }

    @Test
    public void testUnclosedTagIsRepairedByRecloseTags() throws Exception {
        Assert.assertEquals("<w>unclosed</w>", osis("<w>unclosed"));
        // Closing tags at the end that do not match are stripped, then the open tag is re-closed.
        Assert.assertEquals("<w>a</w>", osis("<w>a</x>"));
    }

    @Test
    public void testEntityRepairThenRecloseBothApply() throws Exception {
        Assert.assertEquals("<w>a &amp; b</w>", osis("<w>a & b"));
    }

    @Test
    public void testUnrepairableFragmentFallsBackToCleanAllTags() throws Exception {
        // recloseTags gives up on a close tag with no open tag; cleanAllTags then replaces
        // "</w>" by a single space and the result parses.
        Assert.assertEquals(" text", osis("</w>text"));
        // An attribute without closing quote.
        // (recloseTags first appends </w>; then cleanAllTags replaces both tags by a space each.)
        Assert.assertEquals(" tail ", osis("<w lemma=\"x>tail"));
    }

    @Test
    public void testUnparsableEvenWithoutTagsYieldsEmptyResult() throws Exception {
        // A control character is illegal in XML, no repair helps: an empty p is created and its (no) content returned.
        List<Content> result = filter(book, "a\u0001b");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testMapMBookPrefixesMissingOpeningTags() throws Exception {
        Book mapM = (Book) Proxy.newProxyInstance(Book.class.getClassLoader(), new Class<?>[] {Book.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getInitials")) {
                        return "MapM";
                    }
                    if (method.getName().equals("getKey")) {
                        return book.getKey((String) args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        // The first loop iteration (cell) adds <cell>, row adds <row>, table adds <table> in that order,
        // so the outermost is the table.
        Assert.assertEquals("<cell>x</cell>", osis(mapM, "x</cell>"));
        Assert.assertEquals("<table><row>a</row>b</table>", osis(mapM, "a</row>b</table>"));
        // A start tag before the end tag is left alone.
        Assert.assertEquals("<cell>x</cell>", osis(mapM, "<cell>x</cell>"));
        // Other books do not get the prefix: same input goes down the recloseTags path.
        // (recloseTags strips the trailing close tag, leaving "x", which parses.)
        Assert.assertEquals("x", osis("x</cell>"));
    }

    @Test
    public void testCloneIsIndependentCopy() throws Exception {
        OSISFilter filter = new OSISFilter();
        OSISFilter clone = filter.clone();
        Assert.assertNotSame(filter, clone);
        Assert.assertEquals(OSISFilter.class, clone.getClass());
    }
}
