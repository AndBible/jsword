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
package org.crosswire.jsword.book.filter.gbf;

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
 * Characterization of {@link GBFFilter}, {@link GBFTagBuilders} and {@link GBFTags}:
 * pins the behaviour of the Java classes before they are converted to Kotlin.
 * Expected values were derived by reading the Java, not by observing it. Note that
 * a text tag escapes its text with XMLUtil.escape and the serializer escapes it
 * again, so an ampersand comes out as {@code &amp;amp;}: that is the (odd) behaviour.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class GBFFilterCharacterizationTest {
    private Book book;

    @Before
    public void setUp() throws Exception {
        book = Books.installed().getBook("KJV");
        Assume.assumeNotNull(book);
    }

    private List<Content> filter(String input) throws Exception {
        return new GBFFilter().toOSIS(book, book.getKey("Gen 1:1"), input);
    }

    private String osis(String input) throws Exception {
        XMLOutputter out = new XMLOutputter(Format.getRawFormat());
        return out.outputString(filter(input));
    }

    @Test
    public void testPlainTextAndTrim() throws Exception {
        Assert.assertEquals("In the beginning", osis("In the beginning"));
        // The input is trimmed (chars <= ' ') before it is tokenized.
        Assert.assertEquals("x y", osis(" \t\n x y \r\n"));
    }

    @Test
    public void testEmptyInputYieldsOneEmptyText() throws Exception {
        // parseTags always ends with a text tag for what remains, here "".
        List<Content> result = filter("");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("", osis(""));
    }

    @Test
    public void testHighlightFamily() throws Exception {
        Assert.assertEquals("<hi type=\"italic\">it</hi>", osis("<FI>it<Fi>"));
        Assert.assertEquals("<hi type=\"bold\">b</hi>", osis("<FB>b<Fb>"));
        Assert.assertEquals("<hi type=\"underline\">u</hi>", osis("<FU>u<Fu>"));
        Assert.assertEquals("<q who=\"Jesus\">red</q>", osis("<FR>red<Fr>"));
        Assert.assertEquals("<q>ot</q>", osis("<FO>ot<Fo>"));
        Assert.assertEquals("<lg>poem</lg>", osis("<PP>poem<Pp>"));
        Assert.assertEquals("<seg type=\"text-align: right;\">r</seg>", osis("<JR>r"));
    }

    @Test
    public void testTitleFamily() throws Exception {
        Assert.assertEquals("<title>Head</title>", osis("<TS>Head<Ts>"));
        Assert.assertEquals("<title>Title</title>", osis("<TH>Title<Th>"));
        Assert.assertEquals("<title type=\"main\">Book</title>", osis("<TT>Book<Tt>"));
        Assert.assertEquals("<title type=\"psalm\" subType=\"x-preverse\" canonical=\"true\">Ps</title>",
                osis("<TB>Ps<Tb>"));
    }

    @Test
    public void testNestedTagsAndText() throws Exception {
        Assert.assertEquals("a <hi type=\"bold\">b<hi type=\"italic\">c</hi>d</hi>e",
                osis("a <FB>b<FI>c<Fi>d<Fb>e"));
    }

    @Test
    public void testParagraphAndLineBreak() throws Exception {
        Assert.assertEquals("a<p />b", osis("a<CM>b"));
        Assert.assertEquals("a<lb />b", osis("a<CL>b"));
    }

    @Test
    public void testFootnote() throws Exception {
        Assert.assertEquals("a<note type=\"x-StudyNote\">a note</note>b", osis("a<RF>a note<Rf>b"));
        // An empty footnote is removed again when it is closed.
        Assert.assertEquals("xy", osis("x<RF><Rf>y"));
        // RB opens a note that is never closed by a matching end tag.
        Assert.assertEquals("<note type=\"x-StudyNote\">x</note>", osis("<RB>x"));
    }

    @Test
    public void testStrongsWord() throws Exception {
        Assert.assertEquals("<w lemma=\"strong:H7225\">beginning</w>", osis("beginning<WH7225>"));
        // A second number is appended to the lemma of the preceding element.
        Assert.assertEquals("<w lemma=\"strong:G3056 strong:G1234\">word</w>", osis("word<WG3056><WG1234>"));
        // Text after a Strong's tag goes to the div, not into the w.
        Assert.assertEquals("<w lemma=\"strong:G1\">a</w>b", osis("a<WG1>b"));
        // Without preceding content an empty w is appended to the root.
        Assert.assertEquals("<w lemma=\"strong:G3056\" />", osis("<WG3056>"));
    }

    @Test
    public void testStrongsMorph() throws Exception {
        Assert.assertEquals("<w morph=\"x-StrongsMorph:TN-NSM\">word</w>", osis("word<WTN-NSM>"));
        Assert.assertEquals("<w lemma=\"strong:G1\" morph=\"x-StrongsMorph:TNSM\">word</w>", osis("word<WG1><WTNSM>"));
        // Without content there is nothing to attach it to.
        Assert.assertEquals("", osis("<WTNSM>"));
    }

    @Test
    public void testVerseBoundaryStrongsTagsAreIgnored() throws Exception {
        // WT?[GH] ?[0-9]+[-:][0-9abc-]+ marks a verse boundary in another versification.
        Assert.assertEquals("ab", osis("a<WG1:2>b"));
        Assert.assertEquals("ab", osis("a<WH7225-1>b"));
    }

    @Test
    public void testIgnoredAndUnknownTags() throws Exception {
        Assert.assertEquals("ab", osis("a<JC>b"));
        Assert.assertEquals("ab", osis("a<BI>b"));
        // Unknown 2-letter tag, and unknown tag with data: not in BUILDERS.
        Assert.assertEquals("ab", osis("a<ZZ>b"));
        Assert.assertEquals("ab", osis("a<Zebra>b"));
    }

    @Test
    public void testCrossReference() throws Exception {
        Assert.assertEquals("<reference osisRef=\"Gen.1.1\">see</reference>", osis("<RXGen.1.1>see<Rx>"));
        // An unparsable reference is reported and the reference has no osisRef.
        Assert.assertEquals("<reference>see</reference>", osis("<RXzzzz qq>see<Rx>"));
    }

    @Test
    public void testEscapeTags() throws Exception {
        // CG/CT create text tags "&gt;" / "&lt;" which are escaped twice.
        Assert.assertEquals("a&amp;amp;gt;b", osis("a<CG>b"));
        Assert.assertEquals("a&amp;amp;lt;b", osis("a<CT>b"));
    }

    @Test
    public void testEntitiesAreEscapedTwice() throws Exception {
        Assert.assertEquals("Tom &amp;amp;amp; Jerry", osis("Tom &amp; Jerry"));
        Assert.assertEquals("&amp;amp;lt;", osis("&lt;"));
        Assert.assertEquals("say &amp;quot;hi&amp;quot;", osis("say \"hi\""));
    }

    @Test
    public void testNonGbfAngleBrackets() throws Exception {
        // A < not followed by an upper case letter is plain text up to the next >.
        Assert.assertEquals("&amp;lt;b&amp;gt;x", osis("<b>x"));
        // GerLut1545 uses << ... >> for quotes.
        Assert.assertEquals("&amp;lt;&amp;lt;hello&amp;gt;&amp;gt;", osis("<<hello>>"));
    }

    @Test
    public void testStrayGreaterThan() throws Exception {
        Assert.assertEquals("a&amp;gt;", osis("a>"));
        Assert.assertEquals("a&amp;gt;b", osis("a>b"));
        Assert.assertEquals("a&amp;gt;b<hi type=\"italic\">c</hi>", osis("a>b<FI>c"));
    }

    @Test
    public void testUnterminatedTag() throws Exception {
        // < without a following > : text up to and including the < , then the rest.
        Assert.assertEquals("abc&amp;lt;FI", osis("abc<FI"));
        Assert.assertEquals("a&amp;lt;b", osis("a<b"));
    }

    @Test
    public void testUnterminatedOpenTagIsClosedImplicitly() throws Exception {
        Assert.assertEquals("<hi type=\"italic\">abc</hi>", osis("<FI>abc"));
    }

    @Test
    public void testLessThanAtEndThrows() throws Exception {
        // Bug kept as is: "ltpos < remains.length() + 1" lets charAt(ltpos + 1) run past the end.
        try {
            osis("a<");
            Assert.fail("expected StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException expected) {
            // expected
        }
    }

    @Test
    public void testEndTagWithoutStartLosesFollowingText() throws Exception {
        // The surplus end tag pops the root div; later text goes to a detached stack entry.
        Assert.assertEquals("a", osis("a<Fi>b"));
        // A second surplus end tag finds the stack empty and is ignored.
        Assert.assertEquals("a", osis("a<Fi><Fi>b"));
    }

    @Test
    public void testStackAccessOnEmptyStackThrows() throws Exception {
        // Footnote end on an empty root: pops the div, then looks at the (empty) stack top.
        try {
            osis("<Rf>");
            Assert.fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
        // Strong's tag after the root was popped.
        try {
            osis("a<Fi><WG1>");
            Assert.fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    @Test
    public void testNonAscii() throws Exception {
        String text = "é€ Αγάπη";
        Assert.assertEquals(text, osis(text));
        Assert.assertEquals("<hi type=\"italic\">" + text + "</hi>", osis("<FI>" + text + "<Fi>"));
    }

    @Test
    public void testSeparatorsAreSplitFromWordsBeforeATag() throws Exception {
        // "?! x" before a tag: separators "?! " and the word "x" become two text nodes;
        // then hi, then the empty trailing text.
        List<Content> result = filter("?! x<FI>y<Fi>");
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("?! ", ((org.jdom2.Text) result.get(0)).getText());
        Assert.assertEquals("x", ((org.jdom2.Text) result.get(1)).getText());
        Assert.assertEquals("hi", ((org.jdom2.Element) result.get(2)).getName());
        Assert.assertEquals("?! x<hi type=\"italic\">y</hi>", osis("?! x<FI>y<Fi>"));
    }

    @Test
    public void testCloneIsIndependentCopy() throws Exception {
        GBFFilter filter = new GBFFilter();
        GBFFilter clone = filter.clone();
        Assert.assertNotSame(filter, clone);
        Assert.assertEquals(GBFFilter.class, clone.getClass());
    }
}
