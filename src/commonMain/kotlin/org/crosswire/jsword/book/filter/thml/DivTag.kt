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

import kotlin.jvm.JvmOverloads

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.filter.FilterOsis as OSISUtil
import org.crosswire.jsword.passage.Key
import fi.sykero.kdom.Element
import fi.sykero.kdom.sax.Attributes

/**
 * THML Tag to process the div element.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class DivTag @JvmOverloads constructor(
    /**
     * The level of the division
     */
    private val level: Int = 0
) : AbstractTag() {
    override val tagName: String get() = if (level == 0) "div" else "div" + level

    override fun processTag(book: Book?, key: Key?, ele: Element?, attrs: Attributes?): Element? {
        // See if there are variant readings e.g. WHNU Mat 1.9
        val typeAttr = attrs!!.getValue("type")
        if ("variant" == typeAttr) {
            val seg = OSISUtil.factory().createSeg()
            seg.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.VARIANT_TYPE)
            val classAttr = attrs.getValue("class")
            if (classAttr != null) {
                seg.setAttribute(OSISUtil.OSIS_ATTR_SUBTYPE, OSISUtil.VARIANT_CLASS + '-' + classAttr)
            }
            if (ele != null) {
                ele.addContent(seg)
            }
            return seg
        }
        val div = OSISUtil.factory().createDiv()
        if (ele != null) {
            ele.addContent(div)
        }
        return div
    }
}
