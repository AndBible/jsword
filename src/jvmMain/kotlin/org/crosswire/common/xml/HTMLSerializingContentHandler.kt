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

import java.io.Writer

/**
 * This class provides for the formatted and syntax highlighted serialization of
 * a SAX stream to a <code>Writer</code>.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class HTMLSerializingContentHandler
/**
 * A formatting serializer that adds whitespace to the document according to
 * the specified <code>FormatType</code>. As the document is serialized it
 * is written to the provided <code>Writer</code>. Without a writer it uses a
 * StringWriter and the toString method will return its content; without a
 * format it does not add whitespace to the document.
 * 
 * @param theFormat
 *            the formatting to use
 * @param theWriter
 *            the writer to use
 */
@JvmOverloads
constructor(theFormat: FormatType = FormatType.AS_IS, theWriter: Writer? = null) : PrettySerializingContentHandler(theFormat, theWriter) {
    override fun decorateTagName(tagName: String?): String? {
        val buf = StringBuilder(50)
        buf.append("<font class='tag'>")
        buf.append(super.decorateTagName(tagName))
        buf.append("</font>")
        return buf.toString()
    }

    override fun decorateAttributeName(attrName: String?): String? {
        val buf = StringBuilder(50)
        buf.append("<font class='attr'>")
        buf.append(super.decorateAttributeName(attrName))
        buf.append("</font>")
        return buf.toString()
    }

    override fun decorateAttributeValue(attrValue: String?): String? {
        val buf = StringBuilder(50)
        buf.append("<font class='value'>")
        buf.append(super.decorateAttributeValue(attrValue))
        buf.append("</font>")
        return buf.toString()
    }

    override fun decorateCharacters(characters: String?): String? {
        val buf = StringBuilder(50)
        buf.append("<font class='text'>")
        buf.append(XMLUtil.escape(super.decorateCharacters(characters))!!.replace("\n", "<br>"))
        buf.append("</font>")
        return buf.toString()
    }

    override fun decorateIndent(indentLevel: Int): String {
        val buf = StringBuilder(100)
        buf.append("<font class='indent'>")
        buf.append(super.decorateIndent(indentLevel).replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"))
        buf.append("</font>")
        return buf.toString()
    }

    override fun startDocument() {
        // Note: we should be using SPAN here but Sun's Java does not support
        // styling it.
        // Also, it introduces whitespace between the span and the text.
        write("<html><head><style type='text/css'>\nFONT.tag    { font-family:courier new, monospaced; color:#666699; font-weight:bold; }\nFONT.attr   { font-family:courier new, monospaced; color:#669966; font-weight:bold; }\nFONT.value  { font-family:courier new, monospaced; color:#669966; font-style:italic; }\nFONT.indent { }\nFONT.text   { font-family:courier new, monospaced; background:#FFFF99; }\n</style></head><body>\n")
    }

    override fun endDocument() {
        write("</body></head>")
    }

    override fun getEmptyTagEnd(): String {
        return XMLUtil.escape(super.getEmptyTagEnd())!!
    }

    override fun getEndTagStart(): String {
        return XMLUtil.escape(super.getEndTagStart())!!
    }

    override fun getPIEnd(): String {
        return XMLUtil.escape(super.getPIEnd())!!
    }

    override fun getPIStart(): String {
        return XMLUtil.escape(super.getPIStart())!!
    }

    override fun getTagEnd(): String {
        return XMLUtil.escape(super.getTagEnd())!!
    }

    override fun getTagStart(): String {
        return XMLUtil.escape(super.getTagStart())!!
    }

    override fun getNewline(): String {
        return "<br>"
    }
}
