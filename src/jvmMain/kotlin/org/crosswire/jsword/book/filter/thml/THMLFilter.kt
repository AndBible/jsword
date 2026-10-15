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
/**
 * A Filter that converts from THML format data to OSIS format data.
 *
 * <p>See <a href="http://www.ccel.org/ThML/ThML1.04.htm">http://www.ccel.org/ThML/ThML1.04.htm</a>
 * for more information.</p>
 */
package org.crosswire.jsword.book.filter.thml

import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

import org.crosswire.common.util.jswordAssert
import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.passage.Key
import fi.sykero.kdom.Content
import fi.sykero.kdom.Element
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException

/**
 * Filter to convert THML to OSIS format.
 *
 * <p>
 * I used the THML ref page: <a
 * href="http://www.ccel.org/ThML/ThML1.04.htm">http
 * ://www.ccel.org/ThML/ThML1.04.htm</a> to work out what the tags meant.
 *
 * LATER(joe): check nesting on these THML elements
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class THMLFilter : SourceFilter, Cloneable {
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.filter.Filter#toOSIS(org.crosswire.jsword.book.Book, org.crosswire.jsword.passage.Key, java.lang.String)
     */
    override fun toOSIS(book: Book?, key: Key?, plain: String?): MutableList<Content> {
        var ele: Element? = cleanParse(book, key, plain)

        if (ele == null) {
            val failure = error
            if (failure is SAXParseException) {
                val spe: SAXParseException = failure
                val colNumber = spe.columnNumber
                val start = Math.max(0, colNumber - 40)
                val stop = Math.min(finalInput!!.length, colNumber + 40)
                val here = stop - start
                log.warn("Could not fix {}({}) by {}: Error here({},{},{}): {}",
                         book!!.initials,
                         key!!.getName(),
                         errorMessage,
                         Integer.toString(colNumber),
                         Integer.toString(finalInput!!.length),
                         Integer.toString(here),
                         finalInput!!.substring(start, stop))
            } else {
                log.warn("Could not fix {}({}) by {}: {}",
                         book!!.initials,
                         key!!.getName(),
                         errorMessage,
                         error!!.message)
            }
            ele = OSISUtil.factory().createP()
        }

        return ele.removeContent()
    }

    override fun clone(): THMLFilter {
        var clone: THMLFilter? = null
        try {
            clone = super<Cloneable>.clone() as THMLFilter
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }
        return clone!!
    }

    private fun cleanParse(book: Book?, key: Key?, plain: String?): Element? {
        // So just try to strip out all XML looking things
        val clean = XMLUtil.cleanAllEntities(plain)
        var ele = parse(book, key, clean, "cleaning entities")

        if (ele == null) {
            ele = cleanText(book, key, clean)
        }

        return ele
    }

    private fun cleanText(book: Book?, key: Key?, plain: String?): Element? {
        // So just try to strip out all XML looking things
        val clean = XMLUtil.cleanAllCharacters(plain!!)
        var ele = parse(book, key, clean, "cleaning text")

        if (ele == null) {
            ele = parse(book, key, XMLUtil.closeEmptyTags(clean), "closing empty tags")
        }

        if (ele == null) {
            ele = cleanTags(book, key, clean)
        }

        return ele
    }

    private fun cleanTags(book: Book?, key: Key?, plain: String?): Element? {
        // So just try to strip out all XML looking things
        val clean = XMLUtil.cleanAllTags(plain)
        return parse(book, key, clean, "cleaning tags")
    }

    private fun parse(book: Book?, key: Key?, plain: String?, failMessage: String): Element? {
        var ex: Exception? = null
        // We need to create a root element to house our document fragment
        // 15 for the tags we add
        val buf = StringBuilder(15 + plain!!.length)
        buf.append('<').append(TAG_ROOT).append('>').append(plain).append("</").append(TAG_ROOT).append('>')
        finalInput = buf.toString()
        try {
            val `in` = StringReader(finalInput)
            val `is` = InputSource(`in`)
            val spf = SAXParserFactory.newInstance()
            val parser = spf.newSAXParser()
            val handler = CustomHandler(book, key)

            parser.parse(`is`, handler)
            return handler.rootElement
        } catch (e: SAXParseException) {
            ex = e
        } catch (e: SAXException) {
            ex = e
        } catch (e: IOException) {
            ex = e
        } catch (e: ParserConfigurationException) {
            ex = e
        } catch (e: IllegalArgumentException) {
            // JDOM has a few exceptions which are all derived from this.
            ex = e
        }

        errorMessage = failMessage
        error = ex
        return null
    }

    private var errorMessage: String? = null
    private var error: Exception? = null
    private var finalInput: String? = null

    private companion object {
        /**
         * The name of the root element that is added around the fragment. This is
         * RootTag.TAG_ROOT, which is protected in Kotlin (and so not visible here).
         */
        private const val TAG_ROOT = "root"

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(THMLFilter::class)
    }
}
