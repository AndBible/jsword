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
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.common.xml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class XMLUtilTest {
    @Test
    fun testCleanAllEntities() {
        assertNull(XMLUtil.cleanAllEntities(null))

        assertEquals("", XMLUtil.cleanAllEntities(""))
        assertEquals("aa", XMLUtil.cleanAllEntities("aa"))
        assertEquals("<aa>", XMLUtil.cleanAllEntities("<aa>"))
        assertEquals("<aa>aa", XMLUtil.cleanAllEntities("<aa>aa"))
        assertEquals("<aa>aa;aa", XMLUtil.cleanAllEntities("<aa>aa;aa"))
        assertEquals(";", XMLUtil.cleanAllEntities(";"))

        assertEquals("aa &amp; aa", XMLUtil.cleanAllEntities("aa &amp; aa"))
        assertEquals("aa &amp;amp aa", XMLUtil.cleanAllEntities("aa &amp aa"))
        assertEquals("aa &amp;a-mp aa", XMLUtil.cleanAllEntities("aa &a-mp aa"))
        assertEquals("aa   aa", XMLUtil.cleanAllEntities("aa &am; aa"))
        assertEquals("aa &amp;am aa", XMLUtil.cleanAllEntities("aa &am aa"))
        assertEquals("aa &amp;", XMLUtil.cleanAllEntities("aa &amp;"))
        assertEquals("aa &amp;amp", XMLUtil.cleanAllEntities("aa &amp"))
        assertEquals("aa  ", XMLUtil.cleanAllEntities("aa &am;"))
        assertEquals("aa &amp;am", XMLUtil.cleanAllEntities("aa &am"))
        assertEquals("aa &amp;a", XMLUtil.cleanAllEntities("aa &a"))
        assertEquals("aa &amp;", XMLUtil.cleanAllEntities("aa &"))

        assertEquals("aa \u00A0 aa", XMLUtil.cleanAllEntities("aa &nbsp; aa"))
        assertEquals("aa &amp;nbsp aa", XMLUtil.cleanAllEntities("aa &nbsp aa"))
        assertEquals("aa &amp;nb-sp aa", XMLUtil.cleanAllEntities("aa &nb-sp aa"))
        assertEquals("aa   aa", XMLUtil.cleanAllEntities("aa &nb; aa"))
        assertEquals("aa &amp;nb aa", XMLUtil.cleanAllEntities("aa &nb aa"))

        assertEquals("-&amp;-\u00A0-&lt;-&gt;-&quot;-\u00A3-\u20AC-", XMLUtil.cleanAllEntities("-&amp;-&nbsp;-&lt;-&gt;-&quot;-&pound;-&euro;-"))
    }

    @Test
    fun testRecloseTagsPreservesNotesBeforeBookEndMilestones() {
        val note = """<note type="study"><hi type="italic">sn</hi> Final note.</note>"""
        val chapterEnd = """<chapter eID="Isa.66"/>"""
        assertEquals("Text$note $chapterEnd", XMLUtil.recloseTags("Text$note</div> $chapterEnd</div>"))
        assertEquals("Text$note\n$chapterEnd", XMLUtil.recloseTags("Text$note</div>\n$chapterEnd</div>"))
        // Do not repair an unmatched close in the middle of text or before a new start milestone.
        assertNull(XMLUtil.recloseTags("Text$note</div> more text"))
        assertNull(XMLUtil.recloseTags("""Text$note</div><chapter sID="Isa.67"/>"""))
    }

    @Test
    fun testRecloseTagsBoundaryTailVariants() {
        val note = "<note>n</note>"
        // Two unmatched closes before an end milestone both recover (the final close is stripped first).
        assertEquals(
            "Text$note <chapter eID=\"x\"/>",
            XMLUtil.recloseTags("Text$note</div></div> <chapter eID=\"x\"/></div>"),
        )
        // A malformed close inside an open note is still rejected.
        assertNull(XMLUtil.recloseTags("<note>x</div></note> <chapter eID=\"x\"/>"))
        // An end milestone that is not self-closing is not a boundary tail.
        assertNull(XMLUtil.recloseTags("Text</div> <chapter eID=\"x\">"))
        // Single-quoted eID is accepted.
        assertEquals("Text <chapter eID='x'/>", XMLUtil.recloseTags("Text</div> <chapter eID='x'/>"))
        // Tab and CR count as trailing whitespace.
        assertEquals(
            "Text\t<chapter eID=\"x\"/>\r",
            XMLUtil.recloseTags("Text</div>\t<chapter eID=\"x\"/>\r"),
        )
    }

    @Test
    fun testRecloseTagsMultiLineVerseEndingInUnmatchedClose() {
        // The strip loop's regex does not cross newlines, so the unmatched close is handled by the
        // boundary-tail path; this used to return null.
        assertEquals("line one\nline two", XMLUtil.recloseTags("line one\nline two</div>"))
    }

    @Test
    fun testCleanAllTags() {
        assertNull(XMLUtil.cleanAllTags(null))

        assertEquals("", XMLUtil.cleanAllTags(""))
        assertEquals("aa", XMLUtil.cleanAllTags("aa"))
        assertEquals("aa &amp; aa", XMLUtil.cleanAllTags("aa &amp; aa"))

        assertEquals(" ", XMLUtil.cleanAllTags("<a>"))
        assertEquals(" ", XMLUtil.cleanAllTags("<aa>"))
        assertEquals(" ", XMLUtil.cleanAllTags("</aa>"))
        assertEquals(" ", XMLUtil.cleanAllTags("<aa wibble=\"wobble\">"))
        assertEquals(" keep ", XMLUtil.cleanAllTags("<aa>keep</aa>"))
        assertEquals(" keep ", XMLUtil.cleanAllTags("<aa>keep<aa>"))
        assertEquals(" ke ep", XMLUtil.cleanAllTags("<aa>ke<aa>ep"))
        assertEquals("ke  ep", XMLUtil.cleanAllTags("ke<aa><aa>ep"))
        assertEquals("ke  ep   ke  ep", XMLUtil.cleanAllTags("ke<aa><aa>ep<bb> <cc>ke<aa><aa>ep"))

        assertEquals(" ", XMLUtil.cleanAllTags("<"))
        assertEquals(" ", XMLUtil.cleanAllTags("<a"))
        assertEquals(" ", XMLUtil.cleanAllTags("<aa"))
        assertEquals(" ", XMLUtil.cleanAllTags("<aa;"))
        assertEquals(" ", XMLUtil.cleanAllTags("<\\"))
        assertEquals(" ", XMLUtil.cleanAllTags("<\\a"))
        assertEquals(" ", XMLUtil.cleanAllTags("<\\aa"))
        assertEquals(" ", XMLUtil.cleanAllTags("<\\aa;"))
        assertEquals(" ", XMLUtil.cleanAllTags("< "))
        assertEquals(" ", XMLUtil.cleanAllTags("< a"))
        assertEquals(" ", XMLUtil.cleanAllTags("< aa"))
        assertEquals(" ", XMLUtil.cleanAllTags("< aa;"))
        assertEquals(" ", XMLUtil.cleanAllTags("< aa>"))
        assertEquals("keep ", XMLUtil.cleanAllTags("keep<"))
        assertEquals("keep ", XMLUtil.cleanAllTags("keep<a"))
        assertEquals("keep ", XMLUtil.cleanAllTags("keep<aa"))
        assertEquals("keep ", XMLUtil.cleanAllTags("keep<aa dont=\"want\""))
        assertEquals("keep ", XMLUtil.cleanAllTags("keep<aa dont=\"want\" keep"))
        assertEquals("keep ", XMLUtil.cleanAllTags("keep<aa dont=\"want\" keep>"))
    }
}
