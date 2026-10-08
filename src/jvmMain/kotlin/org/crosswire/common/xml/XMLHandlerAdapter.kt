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
/*
 * This is based on XMLProcess.
 * @author Andy Clark, IBM
 * @author Arnaud Le Hors, IBM
 *
 * Copyright 2000-2002,2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.crosswire.common.xml

import java.io.IOException
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.DTDHandler
import org.xml.sax.EntityResolver
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.Locator
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.ext.DeclHandler
import org.xml.sax.ext.LexicalHandler

/**
 * Checks a XML document for problems, reporting line and offset.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class XMLHandlerAdapter : EntityResolver, DTDHandler, ContentHandler, ErrorHandler, DeclHandler, LexicalHandler {
    override fun setDocumentLocator(locator: Locator?) {
    }

    @Throws(SAXException::class)
    override fun startDocument() {
    }

    @Throws(SAXException::class)
    override fun processingInstruction(target: String?, data: String?) {
    }

    @Throws(SAXException::class)
    override fun characters(ch: CharArray?, offset: Int, length: Int) {
    }

    @Throws(SAXException::class)
    override fun ignorableWhitespace(ch: CharArray?, offset: Int, length: Int) {
    }

    @Throws(SAXException::class)
    override fun endDocument() {
    }

    @Throws(SAXException::class)
    override fun startPrefixMapping(prefix: String?, uri: String?) {
    }

    @Throws(SAXException::class)
    override fun startElement(uri: String?, localName: String?, qname: String?, attributes: Attributes?) {
    }

    @Throws(SAXException::class)
    override fun endElement(uri: String?, localName: String?, qname: String?) {
    }

    @Throws(SAXException::class)
    override fun endPrefixMapping(prefix: String?) {
    }

    @Throws(SAXException::class)
    override fun skippedEntity(name: String?) {
    }

    @Throws(SAXException::class)
    override fun notationDecl(name: String?, publicId: String?, systemId: String?) {
    }

    @Throws(SAXException::class)
    override fun unparsedEntityDecl(name: String?, publicId: String?, systemId: String?, notationName: String?) {
    }

    @Throws(SAXException::class)
    override fun startDTD(name: String?, publicId: String?, systemId: String?) {
    }

    @Throws(SAXException::class)
    override fun startEntity(name: String?) {
    }

    @Throws(SAXException::class)
    override fun startCDATA() {
    }

    @Throws(SAXException::class)
    override fun endCDATA() {
    }

    @Throws(SAXException::class)
    override fun comment(ch: CharArray?, offset: Int, length: Int) {
    }

    @Throws(SAXException::class)
    override fun endEntity(name: String?) {
    }

    @Throws(SAXException::class)
    override fun endDTD() {
    }

    @Throws(SAXException::class)
    override fun elementDecl(name: String?, contentModel: String?) {
    }

    @Throws(SAXException::class)
    override fun attributeDecl(elementName: String?, attributeName: String?, type: String?, valueDefault: String?, value: String?) {
    }

    @Throws(SAXException::class)
    override fun internalEntityDecl(name: String?, text: String?) {
    }

    @Throws(SAXException::class)
    override fun externalEntityDecl(name: String?, publicId: String?, systemId: String?) {
    }

    @Throws(SAXException::class, IOException::class)
    override fun resolveEntity(publicId: String?, systemId: String?): InputSource? {
        return null
    }

    @Throws(SAXException::class)
    override fun warning(ex: SAXParseException?) {
        printError("Warning", ex!!)
    }

    @Throws(SAXException::class)
    override fun error(ex: SAXParseException?) {
        printError("Error", ex!!)
    }

    @Throws(SAXException::class)
    override fun fatalError(ex: SAXParseException?) {
        printError("Fatal Error", ex!!)
    }

    /**
     * Prints the error message.
     * 
     * @param type the type of the message
     * @param ex the exception of the error
     */
    private fun printError(type: String, ex: SAXParseException) {
        System.err.print("[")
        System.err.print(type)
        System.err.print("] ")
        var systemId = ex.systemId
        if (systemId != null) {
            val index = systemId.lastIndexOf('/')
            if (index != -1) {
                systemId = systemId.substring(index + 1)
            }
            System.err.print(systemId)
        }
        System.err.print(':')
        System.err.print(ex.lineNumber)
        System.err.print(':')
        System.err.print(ex.columnNumber)
        System.err.print(": ")
        System.err.print(ex.message)
        System.err.println()
        System.err.flush()

    }
}
