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

import org.xml.sax.ContentHandler
import org.xml.sax.DTDHandler
import org.xml.sax.EntityResolver
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader

/**
 * A helper to aid people working with a SAXEventProvider.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class SAXEventProviderXMLReader
/**
 * Constructor SAXEventProviderXMLReader.
 * 
 * @param docIn the document provided as SAX events
 */
(private var docIn: SAXEventProvider?) : XMLReader {
    override fun getFeature(arg0: String?): Boolean {
        return false
    }

    override fun setFeature(arg0: String?, arg1: Boolean) {
    }

    override fun getProperty(arg0: String?): Any? {
        return null
    }

    override fun setProperty(arg0: String?, arg1: Any?) {
    }

    override fun setEntityResolver(entities: EntityResolver?) {
        this.entities = entities
    }

    override fun getEntityResolver(): EntityResolver? {
        return entities
    }

    override fun setDTDHandler(dtds: DTDHandler?) {
        this.dtds = dtds
    }

    override fun getDTDHandler(): DTDHandler? {
        return dtds
    }

    override fun setContentHandler(content: ContentHandler?) {
        this.content = content
    }

    override fun getContentHandler(): ContentHandler? {
        return content
    }

    override fun setErrorHandler(errors: ErrorHandler?) {
        this.errors = errors
    }

    override fun getErrorHandler(): ErrorHandler? {
        return errors
    }

    @Throws(SAXException::class)
    override fun parse(`is`: InputSource?) {
        if (`is` !is SAXEventProviderInputSource) {
            throw SAXException("SAXEventProviderInputSource required")
        }

        docIn!!.provideSAXEvents(getContentHandler())
    }

    @Throws(SAXException::class)
    override fun parse(arg0: String?) {
        throw SAXException("SAXEventProviderInputSource required")
    }

    private var errors: ErrorHandler? = null
    private var content: ContentHandler? = null
    private var dtds: DTDHandler? = null
    private var entities: EntityResolver? = null
}
