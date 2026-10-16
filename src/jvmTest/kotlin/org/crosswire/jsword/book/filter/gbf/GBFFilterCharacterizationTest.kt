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
package org.crosswire.jsword.book.filter.gbf

import fi.sykero.kdom.Content
import fi.sykero.kdom.Element
import fi.sykero.kdom.Text
import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.junit.Assume
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

/**
 * Characterization of [GBFFilter], [GBFTagBuilders] and [GBFTags]:
 * pins the behaviour of the Java classes before they are converted to Kotlin.
 * Expected values were derived by reading the Java, not by observing it. Note that
 * a text tag escapes its text with XMLUtil.escape and the serializer escapes it
 * again, so an ampersand comes out as `&amp;amp;`: that is the (odd) behaviour.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class GBFFilterCharacterizationTest {
    private var book: Book? = null

    @BeforeTest
    fun setUp() {
        book = Books.installed().getBook("KJV")
        Assume.assumeNotNull(book)
    }

    private fun filter(input: String): List<Content> {
        return GBFFilter().toOSIS(book, book!!.getKey("Gen 1:1"), input)
    }

    private fun osis(input: String): String {
        val out = XMLOutputter(Format.getRawFormat())
        return out.outputString(filter(input))
    }

    @Test
    fun testPlainTextAndTrim() {
        assertEquals("In the beginning", osis("In the beginning"))
        // The input is trimmed (chars <= ' ') before it is tokenized.
        assertEquals("x y", osis(" \t\n x y \r\n"))
    }

    @Test
    fun testEmptyInputYieldsOneEmptyText() {
        // parseTags always ends with a text tag for what remains, here "".
        val result = filter("")
        assertEquals(1, result.size)
        assertEquals("", osis(""))
    }

    @Test
    fun testHighlightFamily() {
        assertEquals("<hi type=\"italic\">it</hi>", osis("<FI>it<Fi>"))
        assertEquals("<hi type=\"bold\">b</hi>", osis("<FB>b<Fb>"))
        assertEquals("<hi type=\"underline\">u</hi>", osis("<FU>u<Fu>"))
        assertEquals("<q who=\"Jesus\">red</q>", osis("<FR>red<Fr>"))
        assertEquals("<q>ot</q>", osis("<FO>ot<Fo>"))
        assertEquals("<lg>poem</lg>", osis("<PP>poem<Pp>"))
        assertEquals("<seg type=\"text-align: right;\">r</seg>", osis("<JR>r"))
    }

    @Test
    fun testTitleFamily() {
        assertEquals("<title>Head</title>", osis("<TS>Head<Ts>"))
        assertEquals("<title>Title</title>", osis("<TH>Title<Th>"))
        assertEquals("<title type=\"main\">Book</title>", osis("<TT>Book<Tt>"))
        assertEquals("<title type=\"psalm\" subType=\"x-preverse\" canonical=\"true\">Ps</title>",
            osis("<TB>Ps<Tb>"))
    }

    @Test
    fun testNestedTagsAndText() {
        assertEquals("a <hi type=\"bold\">b<hi type=\"italic\">c</hi>d</hi>e",
            osis("a <FB>b<FI>c<Fi>d<Fb>e"))
    }

    @Test
    fun testParagraphAndLineBreak() {
        assertEquals("a<p />b", osis("a<CM>b"))
        assertEquals("a<lb />b", osis("a<CL>b"))
    }

    @Test
    fun testFootnote() {
        assertEquals("a<note type=\"x-StudyNote\">a note</note>b", osis("a<RF>a note<Rf>b"))
        // An empty footnote is removed again when it is closed.
        assertEquals("xy", osis("x<RF><Rf>y"))
        // RB opens a note that is never closed by a matching end tag.
        assertEquals("<note type=\"x-StudyNote\">x</note>", osis("<RB>x"))
    }

    @Test
    fun testStrongsWord() {
        assertEquals("<w lemma=\"strong:H7225\">beginning</w>", osis("beginning<WH7225>"))
        // A second number is appended to the lemma of the preceding element.
        assertEquals("<w lemma=\"strong:G3056 strong:G1234\">word</w>", osis("word<WG3056><WG1234>"))
        // Text after a Strong's tag goes to the div, not into the w.
        assertEquals("<w lemma=\"strong:G1\">a</w>b", osis("a<WG1>b"))
        // Without preceding content an empty w is appended to the root.
        assertEquals("<w lemma=\"strong:G3056\" />", osis("<WG3056>"))
    }

    @Test
    fun testStrongsMorph() {
        assertEquals("<w morph=\"x-StrongsMorph:TN-NSM\">word</w>", osis("word<WTN-NSM>"))
        assertEquals("<w lemma=\"strong:G1\" morph=\"x-StrongsMorph:TNSM\">word</w>", osis("word<WG1><WTNSM>"))
        // Without content there is nothing to attach it to.
        assertEquals("", osis("<WTNSM>"))
    }

    @Test
    fun testVerseBoundaryStrongsTagsAreIgnored() {
        // WT?[GH] ?[0-9]+[-:][0-9abc-]+ marks a verse boundary in another versification.
        assertEquals("ab", osis("a<WG1:2>b"))
        assertEquals("ab", osis("a<WH7225-1>b"))
    }

    @Test
    fun testIgnoredAndUnknownTags() {
        assertEquals("ab", osis("a<JC>b"))
        assertEquals("ab", osis("a<BI>b"))
        // Unknown 2-letter tag, and unknown tag with data: not in BUILDERS.
        assertEquals("ab", osis("a<ZZ>b"))
        assertEquals("ab", osis("a<Zebra>b"))
    }

    @Test
    fun testCrossReference() {
        assertEquals("<reference osisRef=\"Gen.1.1\">see</reference>", osis("<RXGen.1.1>see<Rx>"))
        // An unparsable reference is reported and the reference has no osisRef.
        assertEquals("<reference>see</reference>", osis("<RXzzzz qq>see<Rx>"))
    }

    @Test
    fun testEscapeTags() {
        // CG/CT create text tags "&gt;" / "&lt;" which are escaped twice.
        assertEquals("a&amp;amp;gt;b", osis("a<CG>b"))
        assertEquals("a&amp;amp;lt;b", osis("a<CT>b"))
    }

    @Test
    fun testEntitiesAreEscapedTwice() {
        assertEquals("Tom &amp;amp;amp; Jerry", osis("Tom &amp; Jerry"))
        assertEquals("&amp;amp;lt;", osis("&lt;"))
        assertEquals("say &amp;quot;hi&amp;quot;", osis("say \"hi\""))
    }

    @Test
    fun testNonGbfAngleBrackets() {
        // A < not followed by an upper case letter is plain text up to the next >.
        assertEquals("&amp;lt;b&amp;gt;x", osis("<b>x"))
        // GerLut1545 uses << ... >> for quotes.
        assertEquals("&amp;lt;&amp;lt;hello&amp;gt;&amp;gt;", osis("<<hello>>"))
    }

    @Test
    fun testStrayGreaterThan() {
        assertEquals("a&amp;gt;", osis("a>"))
        assertEquals("a&amp;gt;b", osis("a>b"))
        assertEquals("a&amp;gt;b<hi type=\"italic\">c</hi>", osis("a>b<FI>c"))
    }

    @Test
    fun testUnterminatedTag() {
        // < without a following > : text up to and including the < , then the rest.
        assertEquals("abc&amp;lt;FI", osis("abc<FI"))
        assertEquals("a&amp;lt;b", osis("a<b"))
    }

    @Test
    fun testUnterminatedOpenTagIsClosedImplicitly() {
        assertEquals("<hi type=\"italic\">abc</hi>", osis("<FI>abc"))
    }

    @Test
    fun testLessThanAtEndThrows() {
        // Bug kept as is: "ltpos < remains.length() + 1" lets charAt(ltpos + 1) run past the end.
        assertFailsWith<StringIndexOutOfBoundsException> {
            osis("a<")
        }
    }

    @Test
    fun testEndTagWithoutStartLosesFollowingText() {
        // The surplus end tag pops the root div; later text goes to a detached stack entry.
        assertEquals("a", osis("a<Fi>b"))
        // A second surplus end tag finds the stack empty and is ignored.
        assertEquals("a", osis("a<Fi><Fi>b"))
    }

    @Test
    fun testStackAccessOnEmptyStackThrows() {
        // Footnote end on an empty root: pops the div, then looks at the (empty) stack top.
        assertFailsWith<IndexOutOfBoundsException> {
            osis("<Rf>")
        }
        // Strong's tag after the root was popped.
        assertFailsWith<IndexOutOfBoundsException> {
            osis("a<Fi><WG1>")
        }
    }

    @Test
    fun testNonAscii() {
        val text = "\u00E9\u20AC \u0391\u03B3\u03AC\u03C0\u03B7"
        assertEquals(text, osis(text))
        assertEquals("<hi type=\"italic\">" + text + "</hi>", osis("<FI>" + text + "<Fi>"))
    }

    @Test
    fun testSeparatorsAreSplitFromWordsBeforeATag() {
        // "?! x" before a tag: separators "?! " and the word "x" become two text nodes;
        // then hi, then the empty trailing text.
        val result = filter("?! x<FI>y<Fi>")
        assertEquals(4, result.size)
        assertEquals("?! ", (result[0] as Text).text)
        assertEquals("x", (result[1] as Text).text)
        assertEquals("hi", (result[2] as Element).name)
        assertEquals("?! x<hi type=\"italic\">y</hi>", osis("?! x<FI>y<Fi>"))
    }

    @Test
    fun testCloneIsIndependentCopy() {
        val filter = GBFFilter()
        val clone = filter.clone()
        assertNotSame(filter, clone)
        assertEquals(GBFFilter::class.java, clone.javaClass)
    }
}
