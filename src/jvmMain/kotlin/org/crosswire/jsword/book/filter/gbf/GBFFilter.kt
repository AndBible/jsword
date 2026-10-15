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
 * A Filter that converts from GBF format data to OSIS format data.
 *
 * <p>See <a href="http://ebible.org/bible/gbf.htm">http://ebible.org/bible/gbf.htm</a>
 * for more information.</p>
 */
package org.crosswire.jsword.book.filter.gbf

import java.util.ArrayList
import java.util.LinkedList

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.DataPolice
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.passage.Key
import fi.sykero.kdom.Content

/**
 * Filter to convert GBF data to OSIS format.
 * 
 * The best place to go for more information about the GBF spec is:
 * <a href="http://ebible.org/bible/gbf.htm">http://ebible.org/bible/gbf.htm</a>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class GBFFilter : SourceFilter, Cloneable {
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.filter.Filter#toOSIS(org.crosswire.jsword.book.Book, org.crosswire.jsword.passage.Key, java.lang.String)
     */
    override fun toOSIS(book: Book?, key: Key?, plain: String?): MutableList<Content> {
        val ele = OSISUtil.factory().createDiv()
        val stack = LinkedList<Content>()
        stack.addFirst(ele)

        // String.trim() in Java removes every char <= ' ', which differs from Kotlin's trim()
        val taglist = parseTags(book, key, plain!!.trim { it <= ' ' })
        while (true) {
            if (taglist.isEmpty()) {
                break
            }

            val tag = taglist.removeAt(0)
            tag.updateOsisStack(book, key, stack)
        }

        stack.removeFirst()
        return ele.removeContent()
    }

    override fun clone(): GBFFilter {
        var clone: GBFFilter? = null
        try {
            clone = super<Cloneable>.clone() as GBFFilter
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }
        return clone!!
    }

    /**
     * Turn the string into a list of tags in the order that they appear in the
     * original string.
     */
    private fun parseTags(book: Book?, key: Key?, aRemains: String): MutableList<Tag> {
        var remains = aRemains
        val taglist: MutableList<Tag> = ArrayList()

        // A GBF code is of the form <XY...> or <Xy...>
        // where the first letter is always capitalized and
        // the second letter indicates an open or close tag.
        // Upper letters are open, lower are close.
        // The ... is optional and represents an argument.
        // Sometimes the argument is preceded by a space.
        // In GBF it is legal to have < and > otherwise.
        // In at least one module, GerLut1545, << ... >> is used for quotes.
        while (true) {
            val ltpos = remains.indexOf('<')
            val gtpos = remains.indexOf('>', ltpos + 1)

            // check whether we have unmatched < and >, or no tags at all
            // If so then we don't have a tag in the remaining.
            if (ltpos == -1 || gtpos == -1) {
                // If the first letter after < is an upper case letter
                // then report it as a potential problem
                if (ltpos >= 0
                        && ltpos < remains.length + 1
                        && Character.isUpperCase(remains[ltpos + 1]))
                {
                    DataPolice.report(book, key, "Possible bad GBF tag" + remains)
                }
                if (gtpos != -1 && ltpos >= 0) {
                    DataPolice.report(book, key, "Possible bad GBF tag" + remains)
                }
                val pos = Math.max(ltpos, gtpos) + 1
                // If there were not any <, > or either ended the string
                // then we only have text.
                if (pos == 0 || pos == remains.length) {
                    taglist.add(GBFTagBuilders.getTextTag(remains))
                    break
                }
                taglist.add(GBFTagBuilders.getTextTag(remains.substring(0, pos)))
                remains = remains.substring(pos)
                continue
            }

            // If the character after the < is not an upper case letter
            // then we don't have GBF.
            // So, create a text tag that ends with the found >.
            // Note that in JST, there are spurious html tags and
            // this will treat them as valid GBF text.
            val firstChar = remains[ltpos + 1]
            if (!Character.isUpperCase(firstChar)) {
                taglist.add(GBFTagBuilders.getTextTag(remains.substring(0, gtpos + 1)))
                remains = remains.substring(gtpos + 1)
                continue
            }

            // generate tags
            val start = remains.substring(0, ltpos)
            val strLen = start.length
            if (strLen > 0) {
                var beginIndex = 0
                var inSepStr = SEPARATORS.indexOf(start[0]) >= 0
                // split words from separators...
                // e.g., "a b c? e g." -> "a b c", "? ", "e g."
                // "a b c<tag> e g." -> "a b c", tag, " ", "e g."
                var i = 1
                while (inSepStr && i < strLen) {
                    val currentChar = start[i]
                    if (!(SEPARATORS.indexOf(currentChar) >= 0)) {
                        taglist.add(GBFTagBuilders.getTextTag(start.substring(beginIndex, i)))
                        beginIndex = i
                        inSepStr = false
                    }
                    i++
                }

                if (beginIndex < strLen) {
                    taglist.add(GBFTagBuilders.getTextTag(start.substring(beginIndex)))
                }
            }

            val tag = remains.substring(ltpos + 1, gtpos)
            val length = tag.length
            if (length > 0) {
                val reply = GBFTagBuilders.getTag(book, key, tag)
                if (reply != null) {
                    taglist.add(reply)
                }
            }

            remains = remains.substring(gtpos + 1)
        }

        return taglist
    }

    private companion object {
        private const val SEPARATORS = " ,:;.?!"
    }
}
