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
package org.crosswire.jsword.book

import fi.sykero.kdom.Element
import fi.sykero.kdom.output.XMLOutputter
import fi.sykero.kdom.output.output
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class OsisTest {
    @Test
    fun testManual() {
        val seg: Element = OSISUtil.factory().createSeg()
        seg.addContent("In the beginning God created the heaven and the earth.")

        val verse: Element = OSISUtil.factory().createVerse()
        verse.setAttribute(OSISUtil.OSIS_ATTR_OSISID, "Gen.1.1")
        verse.addContent(seg)

        val div: Element = OSISUtil.factory().createDiv()
        div.setAttribute("type", "chapter")
        div.setAttribute("osisID", "Gen.1.1")
        div.addContent(verse)

        val work: Element = OSISUtil.factory().createWork()

        val header: Element = OSISUtil.factory().createHeader()
        header.addContent(work)

        val osistext: Element = OSISUtil.factory().createOsisText()
        osistext.setAttribute(OSISUtil.ATTRIBUTE_OSISTEXT_OSISIDWORK, "Bible.KJV")
        osistext.addContent(header)
        osistext.addContent(div)

        val blank: Element = OSISUtil.factory().createOsis()
        blank.addContent(osistext)

        // create a Marshaller and marshal to System.out
        val out = ByteArrayOutputStream()
        outputter.output(blank, out)
        assertTrue(out.toString().trim { it <= ' ' }.length > 0)
    }

    private val outputter = XMLOutputter()
}
