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
 * A Filter that loads OSIS format data into JDOM.
 */
package org.crosswire.jsword.book.filter.osis

import okio.IOException

import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.DataPolice
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.filter.FilterCloneBase
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.passage.Key
import fi.sykero.kdom.Content
import fi.sykero.kdom.Element
import fi.sykero.kdom.JDOMException
import fi.sykero.kdom.input.KdomBuilder

/**
 * Filter to convert an OSIS XML string to OSIS format.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class OSISFilter : FilterCloneBase(), SourceFilter {

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.filter.Filter#toOSIS(org.crosswire.jsword.book.Book, org.crosswire.jsword.passage.Key, java.lang.String)
     */
    override fun toOSIS(book: Book?, key: Key?, plain: String?): MutableList<Content> {
        var ele: Element? = null
        var ex: Exception? = null
        var clean: String? = plain

        // The following converts simple <div> and </div> to their milestoned versions.
        // Current versions of osis2mod do this already
        // Note: if the div element has attributes, it is not seen.

        // This potentially renders XML invalid. In AndBible we don't care about this. So commenting out.
        // clean = DIV_START.matcher(clean).replaceAll("<div sID=\"xyz\"/>");
        // clean = DIV_END.matcher(clean).replaceAll("<div eID=\"xyz\"/>");
        // clean = CHAPTER_END.matcher(clean).replaceAll("<chapter eID=\"xyz\"/>");
        // clean = SPEECH_START.matcher(clean).replaceAll("<speech sID=\"xyz\"/>");
        // clean = SPEECH_END.matcher(clean).replaceAll("<speech eID=\"xyz\"/>");

        // FIXME(dms): this is a major HACK handling a problem with a badly
        // encoded module.
        /* if (book.getInitials().startsWith("NET") && plain.endsWith("</div>")) {
            clean = clean.substring(0, plain.length() - 6);
            if (clean.matches(".*</div> <chapter eID=\"[A-Za-z0-9.]+\"/>")) {
                clean = clean.substring(0, clean.lastIndexOf("</div> <chapter"));
            }
        } else if (book.getInitials().equals("Kekchi") && plain.endsWith("</div> <lb type=\"x-begin-paragraph\"/>")) {
            clean = clean.substring(0, clean.length() - 37);
        } else if (book.getInitials().equals("VietLCCMN")) {
            int startPos = clean.indexOf("<div>"), endPos = clean.indexOf("</div>");
            if (endPos != -1 && (startPos == -1 || startPos > endPos)) {
                if (clean.startsWith("<l ") || (clean.startsWith("<title ") && clean.contains("</title><l ")))
                    clean = "<lg>"+clean;
                clean = "<div><div><div><div>"+clean;
            }
        } else */
        if (book!!.initials == "MapM") {
            for (tag in listOf("cell", "row", "table")) {
                val startPos = clean!!.indexOf("<" + tag + ">")
                val endPos = clean.indexOf("</" + tag + ">")
                if (endPos != -1 && (startPos == -1 || startPos > endPos)) {
                    clean = "<" + tag + ">" + clean
                }
            }
        }

        try {
            ele = parse(clean)
        } catch (e: JDOMException) {
            ex = e
        } catch (e: IOException) {
            ex = e
        }

        if (ele == null) {
            // There should be no bad entities in OSIS.
            val cleanedEntities = XMLUtil.cleanAllEntities(clean)
            if (cleanedEntities != null && cleanedEntities != clean) {
                clean = cleanedEntities
                try {
                    ele = parse(clean)
                    ex = null
                } catch (e: JDOMException) {
                    ex = e
                } catch (e: IOException) {
                    ex = e
                }
            }
        }

        if (ele == null) {
            val reclosed = XMLUtil.recloseTags(clean!!)
            if (reclosed != null && reclosed != clean) {
                clean = reclosed
                try {
                    ele = parse(clean)
                    ex = null
                } catch (e: JDOMException) {
                    ex = e
                } catch (e: IOException) {
                    ex = e
                }
            }
        }

        if (ex != null) {
            DataPolice.report(book, key, "Parse failed: " + ex.message + "\non: " + clean)
            ele = cleanTags(book, key, clean)
        }

        if (ele == null) {
            ele = OSISUtil.factory().createP()
        }

        return ele.removeContent()
    }

    override fun clone(): OSISFilter = shallowFilterClone() as OSISFilter

    override fun portableFilterClone(): Any {
        check(this::class == OSISFilter::class) { "Host subclass must provide a portable clone" }
        return OSISFilter()
    }

    private fun cleanTags(book: Book?, key: Key?, plain: String?): Element? {
        // So just try to strip out all XML looking things
        val shawn = XMLUtil.cleanAllTags(plain)
        var ex: Exception? = null
        try {
            return parse(shawn)
        } catch (e: JDOMException) {
            ex = e
        } catch (e: IOException) {
            ex = e
        }

        DataPolice.report(book, key, "Parse failed: " + ex.message + "\non: " + shawn)

        return null
    }

    /**
     * If the string is invalid then we might want to have more than one crack
     * at parsing it
     */
    @Throws(JDOMException::class, IOException::class)
    private fun parse(plain: String?): Element {
        // Builders carry no shared parsing state; allocate per call rather than sharing a JVM queue.
        return KdomBuilder().build("<xxx>" + plain + "</xxx>").rootElement
    }

}
