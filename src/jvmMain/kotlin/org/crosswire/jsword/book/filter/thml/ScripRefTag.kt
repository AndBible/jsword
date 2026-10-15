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
import org.crosswire.jsword.book.DataPolice
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.Passage
import org.crosswire.jsword.passage.PassageKeyFactory
import fi.sykero.kdom.Element
import org.xml.sax.Attributes

/**
 * THML Tag to process the scripRef element.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class ScripRefTag : AbstractTag() {
    override val tagName: String get() = "scripRef"

    override fun processTag(book: Book?, key: Key?, ele: Element?, attrs: Attributes?): Element? {
        val refstr = attrs!!.getValue("passage")
        val reference: Element
        if (refstr != null) {
            var ref: Passage? = null
            try {
                ref = PassageKeyFactory.instance().getKey(KeyUtil.getVersification(key), refstr, key)
            } catch (ex: NoSuchKeyException) {
                DataPolice.report(book, key, "Unparsable passage: (" + refstr + ") due to " + ex.message)
            }
            // If we don't have a Passage then use the original string
            val osisname = if (ref != null) ref.getOsisRef() else refstr
            reference = OSISUtil.factory().createReference()
            reference.setAttribute(OSISUtil.OSIS_ATTR_REF, osisname)
        } else {
            // The reference will be filled in by processContent
            reference = OSISUtil.factory().createReference()
        }
        if (ele != null) {
            ele.addContent(reference)
        }
        return reference
    }

    override fun processContent(book: Book?, key: Key?, ele: Element?) {
        val refstr = ele!!.value
        try {
            if (ele.getAttribute(OSISUtil.OSIS_ATTR_REF) == null) {
                val ref = PassageKeyFactory.instance().getKey(KeyUtil.getVersification(key), refstr, key)
                val osisname = ref.getOsisRef()
                ele.setAttribute(OSISUtil.OSIS_ATTR_REF, osisname)
            }
        } catch (ex: NoSuchKeyException) {
            DataPolice.report(book, key, "scripRef has no passage attribute, unable to guess: (" + refstr + ") due to " + ex.message)
        }
    }
}
