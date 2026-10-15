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

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.filter.FilterOsis as OSISUtil
import org.crosswire.jsword.passage.Key
import fi.sykero.kdom.Element
import fi.sykero.kdom.sax.Attributes

/**
 * THML Tag to process the quote element.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class QTag : AbstractTag() {
    override val tagName: String get() = "blockquote"

    override fun processTag(book: Book?, key: Key?, ele: Element?, attrs: Attributes?): Element? {
        val q = OSISUtil.factory().createQ()
        if (ele != null) {
            ele.addContent(q)
        }
        return q
    }
}
