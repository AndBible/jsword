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
package org.crosswire.jsword.book.filter.thml

import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.DataPolice
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.passage.Key
import fi.sykero.kdom.Element
import org.xml.sax.Attributes

/**
 * THML Tag to process the font element.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class FontTag : AbstractTag() {
    override val tagName: String get() = "font"

    override fun processTag(book: Book?, key: Key?, ele: Element?, attrs: Attributes?): Element? {
        val seg = OSISUtil.factory().createSeg()
        val buf = StringBuilder()
        val color = attrs!!.getValue("color")
        if (color != null) {
            buf.append(OSISUtil.SEG_COLORPREFIX)
            buf.append(color)
            buf.append(';')
        }
        val size = attrs.getValue("size")
        if (size != null) {
            buf.append(OSISUtil.SEG_SIZEPREFIX)
            buf.append(size)
            buf.append(';')
        }
        val type = buf.toString()
        if (type.length > 0) {
            seg.setAttribute(OSISUtil.OSIS_ATTR_TYPE, type)
        } else {
            DataPolice.report(book, key, "Missing color/size attribute.")
            debugLegacySaxAttributes(attrs)
        }
        if (ele != null) {
            ele.addContent(seg)
        }
        return seg
    }
}

// Temporary JAXP diagnostic island, removed with the ThML parser in Task20.
private fun debugLegacySaxAttributes(attrs: org.xml.sax.Attributes) {
    val log = org.crosswire.common.util.LoggerFactory.getLogger(XMLUtil::class)
    for (i in 0 until attrs.length) {
        log.debug("attr[{}]: {}={}", Integer.toString(i), attrs.getQName(i), attrs.getValue(i))
    }
}
