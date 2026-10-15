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
package org.crosswire.jsword.book.filter.gbf

import java.util.LinkedList

import org.crosswire.common.util.ClassUtil
import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.DataPolice
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.Passage
import fi.sykero.kdom.Content
import fi.sykero.kdom.Element
import fi.sykero.kdom.Text

/**
 * A holder of all of the GBF Tag Handler classes.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class GBFTags private constructor() {
    /**
     *
     * @param name
     */
    class DefaultEndTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            if (osisStack.isEmpty()) {
                DataPolice.report(book, key, "Ignoring end tag without corresponding start tag: " + name)
                return
            }
            osisStack.removeFirst()
        }
    }

    /**
     *
     * @param name
     */
    class BoldStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createHI()
            ele.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_BOLD)
            updateOsisStack(osisStack, ele)
        }
    }

    /**
     *
     * @param name
     */
    class BookTitleStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createTitle()
            ele.setAttribute(OSISUtil.OSIS_ATTR_TYPE, "main")
            updateOsisStack(osisStack, ele)
        }
    }

    class CrossRefStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createReference()

            val refstr = name.substring(2)
            try {
                val ref = book!!.getKey(refstr) as Passage
                ele.setAttribute(OSISUtil.OSIS_ATTR_REF, ref.getOsisRef())
            } catch (ex: NoSuchKeyException) {
                DataPolice.report(book, key, "unable to parse reference: " + refstr)
            }
            updateOsisStack(osisStack, ele)
        }
    }

    /**
     *
     * @param name
     */
    class EOLTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {

            val p = OSIS_FACTORY.createLB()
            if (osisStack.isEmpty()) {
                osisStack.addFirst(p)
            } else {
                val top = osisStack[0]
                if (top is Element) {
                    val current = top
                    current.addContent(p)
                }
            }
        }
    }

    /**
     *
     * @param name
     */
    class FootnoteStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createNote()
            ele.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.NOTETYPE_STUDY)
            updateOsisStack(osisStack, ele)
        }
    }

    /**
     *
     * @param name
     */
    class FootnoteEndTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            if (osisStack.isEmpty()) {
                DataPolice.report(book, key, "Ignoring end tag without corresponding start tag: " + name)
                return
            }

            val pop: Any = osisStack.removeFirst()
            if (pop !is Element) {
                DataPolice.report(book, key, "expected to pop a Note, but found " + ClassUtil.getShortClassName(pop.javaClass))
                return
            }

            val note = pop
            if (note.contentSize < 1) {
                val top = osisStack[0]
                if (top is Element) {
                    val ele = top
                    ele.removeContent(note)
                }
            }
        }
    }

    /**
     *
     * @param name
     */
    class HeaderStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            updateOsisStack(osisStack, OSIS_FACTORY.createTitle())
        }
    }

    /**
     *
     * @param name
     */
    class IgnoredTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
        }
    }

    /**
     *
     * @param name
     */
    class ItalicStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createHI()
            ele.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_ITALIC)
            updateOsisStack(osisStack, ele)
        }
    }

    /**
     *
     * @param name
     */
    class JustifyRightTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            // LATER(joe): is seg the right thing?
            val ele = OSIS_FACTORY.createSeg()
            ele.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.SEG_JUSTIFYRIGHT)
            updateOsisStack(osisStack, ele)
        }
    }

    /**
     *
     * @param name
     */
    class JustifyLeftTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createSeg()
            ele.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.SEG_JUSTIFYLEFT)
            updateOsisStack(osisStack, ele)
        }
    }

    /**
     *
     * @param name
     */
    class OTQuoteStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            updateOsisStack(osisStack, OSIS_FACTORY.createQ())
        }
    }

    /**
     *
     * @param name
     */
    class ParagraphTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {

            if (osisStack.isEmpty()) {
                val p = OSIS_FACTORY.createLB()
                osisStack.addFirst(p)
            } else {
                val p = OSIS_FACTORY.createP()
                val top = osisStack[0]
                if (top is Element) {
                    val current = top
                    current.addContent(p)
                }
            }
        }
    }

    /**
     *
     * @param name
     */
    class PoetryStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            updateOsisStack(osisStack, OSIS_FACTORY.createLG())
        }
    }

    /**
     *
     * @param name
     */
    class PsalmStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            // In OSIS Psalm titles are canonical
            val title = OSIS_FACTORY.createTitle()
            title.setAttribute(OSISUtil.OSIS_ATTR_TYPE, "psalm")
            title.setAttribute(OSISUtil.OSIS_ATTR_SUBTYPE, "x-preverse")
            title.setAttribute(OSISUtil.OSIS_ATTR_CANONICAL, "true")
            updateOsisStack(osisStack, title)
        }
    }

    /**
     *
     * @param name
     */
    class RedLetterStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createQ()
            ele.setAttribute(OSISUtil.ATTRIBUTE_Q_WHO, "Jesus")
            updateOsisStack(osisStack, ele)
        }
    }

    class StrongsMorphTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val name = name.trim { it <= ' ' }

            val top = osisStack[0]
            if (top is Element) {
                val ele = top
                val size = ele.contentSize
                if (size == 0) {
                    DataPolice.report(book, key, "No content to attach Strong's Morph tag to: <" + name + ">.")
                    return
                }

                val lastIndex = size - 1
                val prevObj = ele.getContent(lastIndex)
                var word: Element? = null

                if (prevObj is Text) {
                    word = OSIS_FACTORY.createW()
                    ele.removeContent(prevObj)
                    word.addContent(prevObj)
                    ele.addContent(word)
                } else if (prevObj is Element) {
                    word = prevObj
                } else {
                    DataPolice.report(book, key, "No words to attach Strong's Morph tag to: <" + name + ">.")
                    return
                }

                val existingMorph = word.getAttributeValue(OSISUtil.ATTRIBUTE_W_MORPH)
                val newMorph = StringBuilder()

                if (existingMorph != null && existingMorph.length > 0) {
                    newMorph.append(existingMorph).append('|')
                }
                newMorph.append(OSISUtil.MORPH_STRONGS).append(name.substring(2))
                word.setAttribute(OSISUtil.ATTRIBUTE_W_MORPH, newMorph.toString())
            }
        }
    }

    /**
     *
     * @param name
     */
    class StrongsWordTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            var empty = false
            val name = name.trim { it <= ' ' }
            var word: Element? = null

            var top = osisStack[0]
            if (top is Element) {
                val ele = top
                val size = ele.contentSize
                if (size > 0) {
                    val lastIndex = size - 1
                    val prevObj = ele.getContent(lastIndex)

                    if (prevObj is Text) {
                        val textItem = prevObj
                        word = OSIS_FACTORY.createW()
                        ele.removeContent(textItem)
                        word.addContent(textItem)
                        ele.addContent(word)
                    } else if (prevObj is Element) {
                        word = prevObj
                    }
                }
            }

            if (word == null) {
                word = OSIS_FACTORY.createW()
                empty = true
            }

            val existingLemma = word.getAttributeValue(OSISUtil.ATTRIBUTE_W_LEMMA)
            val newLemma = StringBuilder()

            // Strong's numbers are separated by spaces w/in the attribute
            if (existingLemma != null && existingLemma.length > 0) {
                newLemma.append(existingLemma).append(' ')
            }

            // Grab the G or H and the number that follows
            newLemma.append(OSISUtil.LEMMA_STRONGS).append(name.substring(1))
            word.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, newLemma.toString())

            if (empty) {
                // The last element of the osisStack is the wrapping div.
                // Empty elements are merely appended to the parent container
                top = osisStack.last()
                if (top is Element) {
                    val ele = top
                    ele.addContent(word)
                }
            }
        }
    }

    /**
     *
     * @param name
     */
    class TextFootnoteTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createNote()
            ele.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.NOTETYPE_STUDY)
            updateOsisStack(osisStack, ele)
        }
    }

    /**
     *
     * @param name
     */
    class TextTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            // Make sure that characters that XML requires to be escaped are.
            val text = XMLUtil.escape(name)
            if (osisStack.isEmpty()) {
                osisStack.addFirst(Text(text))
            } else {
                var top = osisStack[0]
                if (top is Element) {
                    var ele = top
                    // Don't make this text the child of a preceding <w>
                    if (OSISUtil.OSIS_ELEMENT_W == ele.name) {
                        // The last element of the osisStack is the wrapping div.
                        top = osisStack.last()
                        if (top is Element) {
                            ele = top
                            ele.addContent(Text(text))
                        }
                    } else {
                        ele.addContent(text)
                    }
                }
            }
        }
    }

    /**
     *
     * @param name
     */
    class TitleStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            updateOsisStack(osisStack, OSIS_FACTORY.createTitle())
        }
    }

    /**
     *
     * @param name
     */
    class UnderlineStartTag(name: String) : AbstractTag(name) {
        override fun updateOsisStack(book: Book?, key: Key?, osisStack: LinkedList<Content>) {
            val ele = OSIS_FACTORY.createHI()
            ele.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_UNDERLINE)
            updateOsisStack(osisStack, ele)
        }
    }

    private companion object {
        /* private */
        private fun updateOsisStack(osisStack: LinkedList<Content>, content: Content) {
            val top = osisStack[0]
            if (top is Element) {
                val current = top
                current.addContent(content)
                osisStack.addFirst(content)
            }
        }

        /**
         * To create OSIS DOM nodes.
         */
        private val OSIS_FACTORY: OSISUtil.OSISFactory = OSISUtil.factory()
    }
}
