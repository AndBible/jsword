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

import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.Locator

/**
 * This class provides for the formatted serialization of a SAX stream to a
 * <code>Writer</code>.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class PrettySerializingContentHandler
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
constructor(theFormat: FormatType = FormatType.AS_IS, theWriter: Writer? = null) : ContentHandler {
    private var formatting: FormatType = theFormat

    private var writer: Writer = theWriter ?: StringWriter()

    override fun toString(): String {
        return writer.toString()
    }

    override fun setDocumentLocator(locator: Locator?) {
    }

    override fun startDocument() {
        // write("<?xml version=\"1.0\"?>");
    }

    override fun endDocument() {
    }

    override fun startPrefixMapping(prefix: String?, uri: String?) {
    }

    override fun endPrefixMapping(prefix: String?) {
    }

    override fun startElement(uri: String?, localname: String?, qname: String?, attrs: Attributes) {
        if (depth > 0) {
            handlePending()
        }

        write(getTagStart())
        write(decorateTagName(localname))

        for (i in 0 until attrs.length) {
            write(' ')
            write(decorateAttributeName(XMLUtil.getAttributeName(attrs, i)))
            write("='")
            write(decorateAttributeValue(XMLUtil.escape(attrs.getValue(i))))
            write('\'')
        }

        pendingEndTag = true
        depth++
    }

    override fun endElement(uri: String?, localname: String?, qname: String?) {
        depth--
        // Java cannot display empty tags <tag/> so most of the following is
        // commented out
        if (pendingEndTag) {
            if (formatting.isAnalytic && depth > 0) {
                emitWhitespace(depth - 1)
            }
            //
            // // Hack alert JTextPane cannot handle <br/>
            //            if (localname.equalsIgnoreCase("br"))
            // {
            write(getTagEnd())
            // }
            // else
            // {
            // write(getEmptyTagEnd());
            // }
        }
        // else
        // {
        if (formatting.isClassic) {
            emitWhitespace(depth)
        }

        write(getEndTagStart())

        write(decorateTagName(localname))

        if (formatting.isAnalytic) {
            emitWhitespace(depth)
        }

        write(getTagEnd())
        // }
        pendingEndTag = false
        lookingForChars = false
    }

    override fun characters(chars: CharArray, start: Int, length: Int) {
        if (!lookingForChars) {
            handlePending()
        }

        val s = String(chars, start, length)
        write(decorateCharacters(s))
        lookingForChars = true
    }

    override fun ignorableWhitespace(chars: CharArray, start: Int, length: Int) {
        characters(chars, start, length)
    }

    override fun processingInstruction(target: String?, data: String?) {
        handlePending()

        write(getPIStart())
        write(target)
        write(' ')
        write(decorateCharacters(data))
        write(getPIEnd())

        if (formatting.isMultiline) {
            write(getNewline())
        }
    }

    override fun skippedEntity(name: String?) {
    }

    protected open fun getTagStart(): String {
        return "<"
    }

    protected open fun getTagEnd(): String {
        return ">"
    }

    protected open fun getEmptyTagEnd(): String {
        return "/>"
    }

    protected open fun getEndTagStart(): String {
        return "</"
    }

    protected open fun getPIStart(): String {
        return "<!"
    }

    protected open fun getPIEnd(): String {
        return "!>"
    }

    protected open fun getNewline(): String {
        return "\n"
    }

    protected open fun decorateTagName(tagName: String?): String? {
        return tagName
    }

    protected open fun decorateAttributeName(attrName: String?): String? {
        return attrName
    }

    protected open fun decorateAttributeValue(attrValue: String?): String? {
        return attrValue
    }

    protected open fun decorateCharacters(characters: String?): String? {
        return characters
    }

    protected open fun decorateIndent(indentLevel: Int): String {
        return String(indentation, 0, indentLevel).intern()
    }

    protected open fun write(obj: String?) {
        try {
            writer.write(obj)
        } catch (e: IOException) {
            e.printStackTrace(System.err)
        }
    }

    protected open fun write(obj: Char) {
        try {
            writer.write(obj.code)
        } catch (e: IOException) {
            e.printStackTrace(System.err)
        }
    }

    private fun handlePending() {
        if (pendingEndTag) {
            pendingEndTag = false

            if (formatting.isAnalytic) {
                emitWhitespace(depth)
            }

            write(getTagEnd())

        }
        if (formatting.isClassic) {
            emitWhitespace(depth)
        }
        lookingForChars = false
    }

    private fun emitWhitespace(indentLevel: Int) {
        write(getNewline())
        if (formatting.isIndented) {
            write(decorateIndent(indentLevel))
        }
    }

    /**
     * The depth is incremented on each startElement and decremented on each
     * endElement. This is used to output the indentation.
     */
    private var depth = 0

    /**
     * It is possible that characters(...) will be called for adjacent pieces of
     * text. Often this is due to entities in the text. This will allow for
     * these to be joined back together.
     */
    private var lookingForChars = false

    /**
     * One of the difficulties in SAX parsing is that it does not retain state.
     * Even for an empty tag, it calls startElement and endElement. This allows
     * for making empty elements to have the empty tag notation: &lt;tag/&gt;.
     */
    private var pendingEndTag = false

    private companion object {
        /**
         * This allows for rapid output of whitespace.
         */
        private val indentation = charArrayOf(
                '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t',
                '\t', '\t', '\t', '\t', '\t', '\t')
    }
}
