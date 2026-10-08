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

import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.Locator

/**
 * Class to convert a SAX stream into a simple String.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class SerializingContentHandler
/**
 * Default ctor that conditionally inserts newlines; without the argument it
 * does not insert newlines.
 * 
 * @param newlines whether newlines are desired
 */
@JvmOverloads
constructor(newlines: Boolean = false) : ContentHandler {
    private var newlines: Boolean = newlines
    private var buffer: StringBuilder = StringBuilder()

    override fun toString(): String {
        return buffer.toString()
    }

    override fun setDocumentLocator(locator: Locator?) {
    }

    override fun startDocument() {
        // buffer.append("<?xml version=\"1.0\"?>");

        if (newlines) {
            buffer.append('\n')
        }
    }

    override fun endDocument() {
    }

    override fun startPrefixMapping(prefix: String?, uri: String?) {
    }

    override fun endPrefixMapping(prefix: String?) {
    }

    override fun startElement(uri: String?, localname: String?, qname: String?, attrs: Attributes) {
        buffer.append('<')
        if (qname != null) {
            buffer.append(qname)
        } else {
            buffer.append(localname)
        }

        for (i in 0 until attrs.length) {
            buffer.append(' ')
            buffer.append(XMLUtil.getAttributeName(attrs, i))
            buffer.append("=\"")
            buffer.append(attrs.getValue(i))
            buffer.append('\"')
        }

        buffer.append('>')

        if (newlines) {
            buffer.append('\n')
        }
    }

    override fun endElement(uri: String?, localname: String?, qname: String?) {
        buffer.append("</")
        if (qname != null) {
            buffer.append(qname)
        } else {
            buffer.append(localname)
        }

        buffer.append('>')

        if (newlines) {
            buffer.append('\n')
        }
    }

    override fun characters(chars: CharArray, start: Int, length: Int) {
        val s = String(chars, start, length)
        buffer.append(s)
    }

    override fun ignorableWhitespace(chars: CharArray, start: Int, length: Int) {
        val s = String(chars, start, length)
        buffer.append(s)
    }

    override fun processingInstruction(target: String?, data: String?) {
        buffer.append("<!")
        buffer.append(target)
        buffer.append(' ')
        buffer.append(data)
        buffer.append("!>")

        if (newlines) {
            buffer.append('\n')
        }
    }

    override fun skippedEntity(name: String?) {
    }
}
