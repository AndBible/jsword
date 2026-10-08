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
import java.io.StringReader
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.ContentHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader

/**
 * A SAXEventProvider that provides SAX events from a String.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class StringSAXEventProvider
/**
 * Simple ctor
 * 
 * @param xmlstr the xml as a string
 * @throws ParserConfigurationException when there is a parser configuration problem
 * @throws SAXException when there is a SAX problem
 */
@Throws(ParserConfigurationException::class, SAXException::class)
constructor(
    /**
     * The source of XML data
     */
    private var xmlstr: String?
) : SAXEventProvider {
    /**
     * The SAX parser
     */
    private var reader: XMLReader

    init {
        val fact = SAXParserFactory.newInstance()
        val parser = fact.newSAXParser()

        reader = parser.xmlReader
    }

    @Throws(SAXException::class)
    override fun provideSAXEvents(handler: ContentHandler?) {
        try {
            val sr = StringReader(xmlstr)
            val `is` = InputSource(sr)

            reader.contentHandler = handler
            reader.parse(`is`)
        } catch (ex: IOException) {
            throw SAXException(ex)
        }
    }
}
