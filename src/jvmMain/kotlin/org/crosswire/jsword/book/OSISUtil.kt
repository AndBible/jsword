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
package org.crosswire.jsword.book

import org.crosswire.common.diff.Difference
import org.crosswire.common.diff.EditType
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.NoSuchVerseException
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseFactory
import org.crosswire.jsword.versification.Versification
import fi.sykero.kdom.Content
import fi.sykero.kdom.Element
import fi.sykero.kdom.Text
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

import java.util.regex.Pattern

/**
 * Some simple utilities to help working with OSIS classes.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class OSISUtil
/**
 * Prevent instantiation
 */
private constructor() {
    /**
     * A generic way of creating empty Elements of various types
     */
    open class OSISFactory {
        /**
        * @return an abbr element
        */
        open fun createAbbr(): Element {
            return Element(OSIS_ELEMENT_ABBR)
        }

        /**
         * @return a seg element
         */
        open fun createSeg(): Element {
            return Element(OSIS_ELEMENT_SEG)
        }

        /**
         * @return an osisText element
         */
        open fun createOsisText(): Element {
            return Element(OSIS_ELEMENT_OSISTEXT)
        }

        /**
         * @return a header element
         */
        open fun createHeader(): Element {
            return Element(OSIS_ELEMENT_HEADER)
        }

        /**
         * @return a work element
         */
        open fun createWork(): Element {
            return Element(OSIS_ELEMENT_WORK)
        }

        /**
         * @return an osis element
         */
        open fun createOsis(): Element {
            return Element(OSIS_ELEMENT_OSIS)
        }

        /**
         * @return a div element
         */
        open fun createDiv(): Element {
            return Element(OSIS_ELEMENT_DIV)
        }

        /**
         * @return a cell element
         */
        open fun createCell(): Element {
            return Element(OSIS_ELEMENT_CELL)
        }

        /**
         * @return a header cell element (akin to HTML's TH)
         */
        open fun createHeaderCell(): Element {
            val ele = Element(OSIS_ELEMENT_CELL)
            ele.setAttribute(ATTRIBUTE_TABLE_ROLE, TABLE_ROLE_LABEL)
            ele.setAttribute(ATTRIBUTE_CELL_ALIGN, CELL_ALIGN_CENTER)
            return ele
        }

        /**
         * @return a verse element
         */
        open fun createVerse(): Element {
            return Element(OSIS_ELEMENT_VERSE)
        }

        /**
         * @return a w element
         */
        open fun createW(): Element {
            return Element(OSIS_ELEMENT_W)
        }

        /**
         * @return a figure element
         */
        open fun createFigure(): Element {
            return Element(OSIS_ELEMENT_FIGURE)
        }

        /**
         * @return a foreign element
         */
        open fun createForeign(): Element {
            return Element(OSIS_ELEMENT_FOREIGN)
        }

        /**
         * @return an item element
         */
        open fun createItem(): Element {
            return Element(OSIS_ELEMENT_ITEM)
        }

        /**
         * @return a p element
         */
        open fun createP(): Element {
            return Element(OSIS_ELEMENT_P)
        }

        /**
         * @return a list element
         */
        open fun createList(): Element {
            return Element(OSIS_ELEMENT_LIST)
        }

        /**
         * @return a q element
         */
        open fun createQ(): Element {
            return Element(OSIS_ELEMENT_Q)
        }

        /**
         * @return a name element
         */
        open fun createName(): Element {
            return Element(OSIS_ELEMENT_NAME)
        }

        /**
         * @return a note element
         */
        open fun createNote(): Element {
            return Element(OSIS_ELEMENT_NOTE)
        }

        /**
         * @return a reference element
         */
        open fun createReference(): Element {
            return Element(OSIS_ELEMENT_REFERENCE)
        }

        /**
         * @return a row element
         */
        open fun createRow(): Element {
            return Element(OSIS_ELEMENT_ROW)
        }

        /**
         * @return a speaker element
         */
        open fun createSpeaker(): Element {
            return Element(OSIS_ELEMENT_SPEAKER)
        }

        /**
         * @return a speech element
         */
        open fun createSpeech(): Element {
            return Element(OSIS_ELEMENT_SPEECH)
        }

        /**
         * @return a table element
         */
        open fun createTable(): Element {
            return Element(OSIS_ELEMENT_TABLE)
        }

       /**
        * @return a title element
        */
       open fun createTitle(): Element {
           return Element(OSIS_ELEMENT_TITLE)
       }

        /**
         * Create a title marked as generated.
         * 
         * @return a generated title element
         */
        open fun createGeneratedTitle(): Element {
            val title = Element(OSIS_ELEMENT_TITLE)
            title.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.GENERATED_CONTENT)
            return title
        }

        /**
         * Line Group
         * 
         * @return a lg element
         */
        open fun createLG(): Element {
            return Element(OSIS_ELEMENT_LG)
        }

        /**
         * Line
         * 
         * @return a l element
         */
        open fun createL(): Element {
            return Element(OSIS_ELEMENT_L)
        }

        /**
         * Line Break
         * 
         * @return a lb element
         */
        open fun createLB(): Element {
            return Element(OSIS_ELEMENT_LB)
        }

        /**
         * Highlight
         * 
         * @return a hi element
         */
        open fun createHI(): Element {
            return Element(OSIS_ELEMENT_HI)
        }

        /**
         * Text
         * 
         * @param text the text for this element
         * @return a text element
         */
        open fun createText(text: String?): Text {
            return Text(text)
        }
    }

    companion object {
        private const val SPACE_SEPARATOR = ' '
        private const val MORPH_INFO_SEPARATOR = '@'

        /**
         * The following are values for the type attribute on the hi element.
         */
        /**
         * Constant for acrostic highlighting
         */
        const val HI_ACROSTIC = "acrostic"

        /**
         * Constant for rendering bold text
         */
        const val HI_BOLD = "bold"

        /**
         * Constant for rendering emphatic text
         */
        const val HI_EMPHASIS = "emphasis"

        /**
         * Constant for rendering illuminated text.
         */
        const val HI_ILLUMINATED = "illuminated"

        /**
         * Constant for rendering italic text.
         */
        const val HI_ITALIC = "italic"

        /**
         * Constant for rendering strike-through text
         */
        const val HI_LINETHROUGH = "line-through"

        /**
         * Constant for rendering normal text.
         */
        const val HI_NORMAL = "normal"

        /**
         * Constant for rendering small caps
         */
        const val HI_SMALL_CAPS = "small-caps"

        /**
         * Constant for rendering subscripts
         */
        const val HI_SUB = "sub"

        /**
         * Constant for rendering superscripts
         */
        const val HI_SUPER = "super"

        /**
         * Constant for rendering underlined text
         */
        const val HI_UNDERLINE = "underline"

        /**
         * Constant for rendering upper case text
         */
        const val HI_X_CAPS = "x-caps"

        /**
         * Constant for rendering big text
         */
        const val HI_X_BIG = "x-big"

        /**
         * Constant for rendering small text
         */
        const val HI_X_SMALL = "x-small"

        /**
         * Constant for rendering tt text
         */
        const val HI_X_TT = "x-tt"

        /**
         * Constant to help narrow down what we use seg for. In this case the
         * justify right tag
         */
        const val SEG_JUSTIFYRIGHT = "text-align: right;"

        /**
         * Constant to help narrow down what we use seg for. In this case the
         * justify right tag
         */
        const val SEG_JUSTIFYLEFT = "text-align: left;"

        /**
         * Constant to help narrow down what we use seg for. In this case the thml
         * center tag
         */
        const val SEG_CENTER = "text-align: center;"

        /**
         * Constant to help narrow down what we use div for. In this case the thml
         * pre tag
         */
        const val DIV_PRE = "x-pre"

        /**
         * Constant to help narrow down what we use seg for. In this case the color
         * tag
         */
        const val SEG_COLORPREFIX = "color: "

        /**
         * Constant to help narrow down what we use seg for. In this case the
         * font-size tag
         */
        const val SEG_SIZEPREFIX = "font-size: "

        /**
         * Constant for x- types
         */
        const val TYPE_X_PREFIX = "x-"

        /**
         * Constant for the study note type
         */
        const val NOTETYPE_STUDY = "x-StudyNote"

        /**
         * Constant for the cross reference note type
         */
        const val NOTETYPE_REFERENCE = "crossReference"

        /**
         * Constant for the variant type segment
         */
        const val VARIANT_TYPE = "x-variant"
        const val VARIANT_CLASS = "x-"

        /**
         * Constant for JSword generated content. Used for type or subType.
         */
        const val GENERATED_CONTENT = "x-gen"

        /**
         * Constant for the pos (part of speech) type.
         */
        const val POS_TYPE = "x-pos"

        /**
         * Constant for the def (dictionary definition) type
         */
        const val DEF_TYPE = "x-def"

        /**
         * Constant for a Strong's numbering lemma
         */
        const val LEMMA_STRONGS = "strong:"
        const val LEMMA_MISC = "lemma:"
        const val MORPH_ROBINSONS = "robinson:"

        /**
         * Constant for Strong's numbering morphology
         */
        const val MORPH_STRONGS = "x-StrongsMorph:T"

        /**
         * Constant to help narrow down what we use "q" for. In this case:
         * blockquote
         */
        const val Q_BLOCK = "blockquote"

        /**
         * Constant to help narrow down what we use "q" for. In this case: citation
         */
        const val Q_CITATION = "citation"

        /**
         * Constant to help narrow down what we use "q" for. In this case: embedded
         */
        const val Q_EMBEDDED = "embedded"

        /**
         * Constant to help narrow down what we use "list" for.
         */
        const val LIST_ORDERED = "x-ordered"
        const val LIST_UNORDERED = "x-unordered"

        /**
         * Table roles (on table, row and cell elements) can be "data", the default,
         * or label.
         */
        const val TABLE_ROLE_LABEL = "label"

        /**
         * Possible cell alignments
         */
        const val CELL_ALIGN_LEFT = "left"
        const val CELL_ALIGN_RIGHT = "right"
        const val CELL_ALIGN_CENTER = "center"
        const val CELL_ALIGN_JUSTIFY = "justify"
        const val CELL_ALIGN_START = "start"
        const val CELL_ALIGN_END = "end"

        const val OSIS_ELEMENT_ABBR = "abbr"
        const val OSIS_ELEMENT_TITLE = "title"
        const val OSIS_ELEMENT_TABLE = "table"
        const val OSIS_ELEMENT_SPEECH = "speech"
        const val OSIS_ELEMENT_SPEAKER = "speaker"
        const val OSIS_ELEMENT_ROW = "row"
        const val OSIS_ELEMENT_REFERENCE = "reference"
        const val OSIS_ELEMENT_NOTE = "note"
        const val OSIS_ELEMENT_NAME = "name"
        const val OSIS_ELEMENT_Q = "q"
        const val OSIS_ELEMENT_LIST = "list"
        const val OSIS_ELEMENT_P = "p"
        const val OSIS_ELEMENT_ITEM = "item"
        const val OSIS_ELEMENT_FIGURE = "figure"
        const val OSIS_ELEMENT_FOREIGN = "foreign"
        const val OSIS_ELEMENT_W = "w"
        const val OSIS_ELEMENT_CHAPTER = "chapter"
        const val OSIS_ELEMENT_VERSE = "verse"
        const val OSIS_ELEMENT_CELL = "cell"
        const val OSIS_ELEMENT_DIV = "div"
        const val OSIS_ELEMENT_OSIS = "osis"
        const val OSIS_ELEMENT_WORK = "work"
        const val OSIS_ELEMENT_HEADER = "header"
        const val OSIS_ELEMENT_OSISTEXT = "osisText"
        const val OSIS_ELEMENT_SEG = "seg"
        const val OSIS_ELEMENT_LG = "lg"
        const val OSIS_ELEMENT_L = "l"
        const val OSIS_ELEMENT_LB = "lb"
        const val OSIS_ELEMENT_HI = "hi"

        const val ATTRIBUTE_TEXT_OSISIDWORK = "osisIDWork"
        const val ATTRIBUTE_WORK_OSISWORK = "osisWork"
        const val OSIS_ATTR_OSISID = "osisID"
        const val OSIS_ATTR_SID = "sID"
        const val OSIS_ATTR_EID = "eID"
        const val ATTRIBUTE_W_LEMMA = "lemma"
        const val ATTRIBUTE_FIGURE_SRC = "src"
        const val ATTRIBUTE_TABLE_BORDER = "border"
        const val ATTRIBUTE_TABLE_ROLE = "role"
        const val ATTRIBUTE_CELL_ALIGN = "align"
        const val ATTRIBUTE_CELL_ROWS = "rows"
        const val ATTRIBUTE_CELL_COLS = "cols"
        const val OSIS_ATTR_TYPE = "type"
        const val OSIS_ATTR_CANONICAL = "canonical"
        const val OSIS_ATTR_SUBTYPE = "subType"
        const val OSIS_ATTR_REF = "osisRef"
        const val OSIS_ATTR_LEVEL = "level"
        const val ATTRIBUTE_SPEAKER_WHO = "who"
        const val ATTRIBUTE_Q_WHO = "who"
        const val ATTRIBUTE_W_MORPH = "morph"
        const val ATTRIBUTE_OSISTEXT_OSISIDWORK = "osisIDWork"
        // OSIS defines the lang attribute as the one from the xml namespace
        // Typical usage element.setAttribute(OSISUtil.OSIS_ATTR_LANG, lang,
        // Namespace.XML_NAMESPACE);
        const val OSIS_ATTR_LANG = "lang"
        const val ATTRIBUTE_DIV_BOOK = "book"

        /**
         * Prefix for OSIS IDs that refer to Bibles
         */
        private const val OSISID_PREFIX_BIBLE = "Bible."

        private val EXTRA_BIBLICAL_ELEMENTS: Set<String> = HashSet(listOf(
            OSIS_ELEMENT_NOTE, OSIS_ELEMENT_TITLE, OSIS_ELEMENT_REFERENCE
        ))

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(OSISUtil::class)

        private val factory: OSISFactory = OSISFactory()

        /**
         * An accessor for the OSISFactory that creates OSIS objects
         * 
         * @return the singleton OSISFactory
         */
        @JvmStatic
        fun factory(): OSISFactory {
            return factory
        }

        /**
         * Dig past the osis and osisText element, if present, to get the meaningful
         * content of the document.
         * 
         * @param root the element from which to get a fragment
         * @return a fragment
         */
        @JvmStatic
        fun getFragment(root: Element?): MutableList<Content> {
            if (root != null) {
                var content: Element? = root
                if (OSISUtil.OSIS_ELEMENT_OSIS == root.name) {
                    content = root.getChild(OSISUtil.OSIS_ELEMENT_OSISTEXT)
                }

                if (OSISUtil.OSIS_ELEMENT_OSISTEXT == root.name) {
                    content = root.getChild(OSISUtil.OSIS_ELEMENT_DIV)
                }

                // At this point we are at something interesting, possibly null.
                // If this was a semantically valid OSIS document then it is a div.
                // As long as this node has one child dig deeper.
                if (content != null && content.contentSize == 1) {
                    val firstChild = content.getContent(0)
                    if (firstChild is Element && OSISUtil.OSIS_ELEMENT_DIV == firstChild.name) {
                        content = firstChild
                    }
                }

                if (content != null) {
                    return content.content
                }
            }
            return ArrayList<Content>()
        }

        /**
         * Get the canonical text from an osis document consisting of a single
         * fragment. The document is assumed to be valid OSIS2.0 XML. While xml
         * valid is rigidly defined as meaning that an xml parser can validate the
         * document, it does not mean that the document is valid OSIS. This is a
         * semantic problem that is not validated. This method assumes that the root
         * element is also semantically valid.
         * 
         * <p>
         * This means that the top level element's tagname is osis. This can contain
         * either a osisText or an osisCorpus. If it is an osisCorpus, then it
         * contains an osisText. However, as a simplification, since JSword
         * constructs the whole doc for the fragment, osisCorpus can be ignored.
         * <p>
         * The osisText element contains a div element that is either a container or
         * a milestone. Again, JSword is providing the div element and it will be
         * provided as a container. It is this div that "contains" the actual
         * fragment.
         * </p>
         * <p>
         * A verse element may either be a container or a milestone. Sword OSIS
         * books differ in whether they provide the verse element. Most do not. The
         * few that do are using the container model, but it has been proposed that
         * milestones are the best practice.
         * </p>
         * 
         * <p>
         * The fragment may contain elements that are not a part of the original
         * text. These are things such as notes.
         * </p>
         * 
         * <p>
         * Milestones require special handling. Beginning milestones elements have
         * an sID attribute, while ending milestones have an eID with the same value
         * as the opening. So everything between the start and the corresponding end
         * is the content of the element. Also, for a given element, say div, they
         * have to be properly nested as if they were container elements.
         * </p>
         * 
         * @param root
         *            the whole osis document.
         * @return The canonical text without markup
         */
        @JvmStatic
        fun getCanonicalText(root: Element?): String {
            // if someone passes a root element which has text in, we need to check whether it's worth processing.
            // For example. where you have a non-canonical title being passed in, we deal with this here.
            if (!isCanonical(root)) {
                //no point in continuing...
                return ""
            }

            val buffer = StringBuilder()

            // Dig past osis, osisText, if present, to get to the real content.
            val frag = OSISUtil.getFragment(root)

            val dit = frag.iterator()
            var sID: String? = null
            var data: Content? = null
            var ele: Element? = null
            while (dit.hasNext()) {
                data = dit.next()
                if (data is Element) {
                    ele = data
                    if (!isCanonical(ele)) {
                        continue
                    }

                    if (ele.name == OSISUtil.OSIS_ELEMENT_VERSE) {
                        sID = ele.getAttributeValue(OSISUtil.OSIS_ATTR_SID)
                    }

                    if (sID != null) {
                        getCanonicalContent(ele, sID, dit, buffer)
                    } else {
                        getCanonicalContent(ele, null, ele.content.iterator(), buffer)
                    }
                } else if (data is Text) {
                    // make sure that adjacent text elements are separated by
                    // whitespace
                    // TODO(dms): verify that the xml parser does not split words
                    // containing entities.
                    val lastIndex = buffer.length - 1
                    val text = data.text
                    // Ignore empty text nodes and do not add 
                    if (text.length != 0) {
                        //do not add spaces when within a OSIS seg
                        if (lastIndex >= 0 && !Character.isWhitespace(buffer[lastIndex]) && !Character.isWhitespace(text[0])) {
                            buffer.append(' ')
                        }
                        buffer.append(text)
                    }
                }
            }

            // Java's String.trim() removes every char <= ' '
            return buffer.toString().trim { it <= ' ' }
        }

        /**
         * A simplified plain text version of the data in this Element with all the
         * markup stripped out.
         * 
         * @param root
         *            the whole osis document.
         * @return The Bible text without markup
         */
        @JvmStatic
        fun getPlainText(root: Element?): String {
            // Dig past osis, osisText, if present, to get to the real content.
            return getTextContent(OSISUtil.getFragment(root))
        }

        /**
         * A space separate string containing Strong's numbers.
         * 
         * @param root
         *            the whole osis document.
         * @return The Strong's numbers in the text
         */
        @JvmStatic
        fun getStrongsNumbers(root: Element): String {
            return getLexicalInformation(root, false)
        }

        /**
         * A '@' separated list of morphologies and strong numbers
         * 
         * @param root the osis element in question
         * @return the string
         */
        @JvmStatic
        fun getMorphologiesWithStrong(root: Element): String {
            return getLexicalInformation(root, true)
        }

        /**
         * concatenates strong and morphology information together
         * 
         * @param root the osis element in question
         * @param includeMorphology whether to include morphology
         * @return root of the element
         */
        @JvmStatic
        fun getLexicalInformation(root: Element, includeMorphology: Boolean): String {
            val buffer = StringBuilder()

            for (content in getDeepContent(root, OSISUtil.OSIS_ELEMENT_W)) {
                val ele = content as Element
                val attr = ele.getAttributeValue(OSISUtil.ATTRIBUTE_W_LEMMA)
                if (attr != null) {
                    val matcher = strongsNumberPattern.matcher(attr)
                    while (matcher.find()) {
                        var strongsNum = matcher.group(1)
                        if (buffer.length > 0) {
                            buffer.append(' ')
                        }

                        if (includeMorphology) {
                            //if including morphology, we want 1 big field, separated with '@'
                            strongsNum = strongsNum.replace(SPACE_SEPARATOR, MORPH_INFO_SEPARATOR)
                        }
                        buffer.append(strongsNum)

                        if (includeMorphology) {
                            //also include morphology if available
                            val morph = ele.getAttributeValue(OSISUtil.ATTRIBUTE_W_MORPH)
                            if (morph != null && morph.length != 0) {
                                buffer.append(MORPH_INFO_SEPARATOR)
                                buffer.append(morph.replace(SPACE_SEPARATOR, MORPH_INFO_SEPARATOR))
                            }
                        }
                    }
                }
            }

            // Java's String.trim() removes every char <= ' '
            return buffer.toString().trim { it <= ' ' }
        }

        /**
         * A space separate string containing osisID from the reference element.
         * We pass book and key because the xref may not be valid and it needs to be reported.
         *
         * @param book the book to which the references refer
         * @param key the verse containing the cross references
         * @param v11n the versification
         * @param root the osis element in question
         * @return The references in the text
         */
        @JvmStatic
        fun getReferences(book: Book?, key: Key?, v11n: Versification?, root: Element): String {
            val keyf = PassageKeyFactory.instance()
            val collector = keyf.createEmptyKeyList(v11n)

            for (content in getDeepContent(root, OSISUtil.OSIS_ELEMENT_REFERENCE)) {
                val ele = content as Element
                val attr = ele.getAttributeValue(OSISUtil.OSIS_ATTR_REF)
                if (attr != null) {
                    try {
                        collector.addAll(keyf.getKey(v11n, attr))
                    } catch (e: NoSuchKeyException) {
                        DataPolice.report(book, key, "Unable to parse: " + attr + " - No such reference:" + e.message)
                    }
                }
            }

            return collector.getOsisID()
        }

        /**
         * The text of non-reference notes.
         * 
         * @param root the whole OSIS document
         * @return The references in the text
         */
        @JvmStatic
        fun getNotes(root: Element): String {
            val buffer = StringBuilder()

            for (content in getDeepContent(root, OSISUtil.OSIS_ELEMENT_NOTE)) {
                val ele = content as Element
                val attr = ele.getAttributeValue(OSISUtil.OSIS_ATTR_TYPE)
                if (attr == null || attr != NOTETYPE_REFERENCE) {
                    if (buffer.length > 0) {
                        buffer.append(' ')
                    }
                    buffer.append(OSISUtil.getTextContent(ele.content))
                }
            }

            return buffer.toString()
        }

        /**
         * The text of non-reference notes.
         * 
         * @param root the whole OSIS document
         * @return The references in the text
         */
        @JvmStatic
        fun getHeadings(root: Element): String {
            val buffer = StringBuilder()

            for (content in getDeepContent(root, OSISUtil.OSIS_ELEMENT_TITLE)) {
                val ele = content as Element

                if (buffer.length > 0) {
                    buffer.append(' ')
                }
                buffer.append(OSISUtil.getTextContent(ele.content))
            }

            return buffer.toString()
        }

        private fun getCanonicalContent(parent: Element, sID: String?, iter: MutableIterator<Content>, buffer: StringBuilder) {
            if (!isCanonical(parent)) {
                return
            }

            var data: Content? = null
            var ele: Element? = null
            var eleName: String? = null
            var eID: String? = null
            while (iter.hasNext()) {
                data = iter.next()
                if (data is Element) {
                    ele = data
                    // If the milestoned element is done then quit.
                    // This should be a eID=, that matches sID, from the same
                    // element.
                    eleName = ele.name
                    eID = ele.getAttributeValue(OSISUtil.OSIS_ATTR_SID)
                    if (eID != null && eID == sID && eleName == parent.name) {
                        break
                    }
                    OSISUtil.getCanonicalContent(ele, sID, ele.content.iterator(), buffer)
                } else if (data is Text) {
                    // make sure that adjacent text elements are separated by
                    // whitespace
                    // Empty elements also produce whitespace.
                    // TODO(dms): verify that the xml parser does not split words
                    // containing entities.
                    val lastIndex = buffer.length - 1
                    val text = data.text
                    if (lastIndex >= 0 && !Character.isWhitespace(buffer[lastIndex]) && (text.length == 0 || !Character.isWhitespace(text[0])) && OSIS_ELEMENT_SEG != parent.name) {
                        buffer.append(' ')
                    }
                    buffer.append(text)
                }
            }
        }

        private fun isCanonical(content: Content?): Boolean {
            var result = true
            if (content is Element) {
                val element = content

                // Ignore extra-biblical text
                if (EXTRA_BIBLICAL_ELEMENTS.contains(element.name)) {
                    val canonical = element.getAttributeValue(OSISUtil.OSIS_ATTR_CANONICAL)
                    result = java.lang.Boolean.parseBoolean(canonical)
                }
            }

            return result
        }

        private fun getTextContent(fragment: List<Content>): String {
            val buffer = StringBuilder()

            for (next in fragment) {
                recurseElement(next, buffer)
            }

            return buffer.toString()
        }

        /**
         * Find all the instances of elements of type <code>find</code> under the
         * element <code>div</code>.
         * 
         * @param div the element to trawl
         * @param name the element name to search
         * @return the collection of matching content
         */
        @JvmStatic
        fun getDeepContent(div: Element, name: String?): MutableCollection<Content> {
            val reply: MutableList<Content> = ArrayList()
            recurseDeepContent(div, name, reply)
            return reply
        }

        /**
         * Walk up the tree from the W to find out what verse we are in.
         * 
         * @param v11n the versification
         * @param ele
         *            The start point for our verse hunt.
         * @return The verse we are in
         * @throws BookException 
         */
        @JvmStatic
        @Throws(BookException::class)
        fun getVerse(v11n: Versification?, ele: Element): Verse? {
            if (ele.name == OSIS_ELEMENT_VERSE) {
                // If the element is an OSIS Verse then this is fairly easy
                val osisid = ele.getAttributeValue(OSIS_ATTR_OSISID)

                try {
                    return VerseFactory.fromString(v11n, osisid)
                } catch (ex: NoSuchVerseException) {
                    throw BookException(JSOtherMsg.lookupText("OsisID not valid: {0}", osisid), ex)
                }
            }

            // So we just walk up the tree trying to find a verse
            val parent = ele.parent
            if (parent is Element) {
                return getVerse(v11n, parent)
            }

            throw BookException(JSOtherMsg.lookupText("Verse element could not be found"))
        }

        /**
         * Helper method to create the boilerplate headers in an OSIS document from
         * the current metadata object
         * 
         * @param bmd the book's meta data
         * @return the root of an OSIS document
         */
        @JvmStatic
        fun createOsisFramework(bmd: BookMetaData): Element {
            val osis = factory().createOsis()
            val osisid = bmd.initials

            val work = factory().createWork()
            work.setAttribute(ATTRIBUTE_WORK_OSISWORK, osisid)

            val header = factory().createHeader()
            header.addContent(work)

            val text = factory().createOsisText()
            text.setAttribute(ATTRIBUTE_TEXT_OSISIDWORK, OSISID_PREFIX_BIBLE + osisid)
            text.addContent(header)

            osis.addContent(text)

            return osis
        }

        /**
         * Convert a Difference list into a pretty HTML report.
         * 
         * @param diffs
         *            List of Difference objects
         * @return HTML representation
         */
        @JvmStatic
        fun diffToOsis(diffs: MutableList<Difference>): MutableList<Content> {
            val div = factory().createDiv()

            for (x in 0 until diffs.size) {
                val diff = diffs[x]
                val editType = diff.editType // Mode (delete, equal,
                                              // insert)
                val text = factory.createText(diff.text) // Text of change.

                if (EditType.DELETE == editType) {
                    val hi = factory().createHI()
                    hi.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_LINETHROUGH)
                    hi.addContent(text)
                    div.addContent(hi)
                } else if (EditType.INSERT == editType) {
                    val hi = factory().createHI()
                    hi.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_UNDERLINE)
                    hi.addContent(text)
                    div.addContent(hi)
                } else {
                    div.addContent(text)
                }
            }
            return div.cloneContent()
        }

        @JvmStatic
        fun rtfToOsis(rtf: String): MutableList<Content> {
            val div = factory().createDiv()
            val stack = java.util.Stack<Content>()
            stack.push(div)

            val strlen = rtf.length

            val text = StringBuilder(strlen)

            var i = 0
            while (i < strlen) {
                val curChar = rtf[i]
                if (curChar != '\\') {
                    text.append(curChar)
                    i++
                    continue
                }

                // The following are ordered from most to least common
                // and when one is a prefix of another, it follows.

                // Used to end all open attributes. Only \qc in our implementation.
                if (rtf.startsWith("\\pard", i)) {
                    val currentElement = stack.pop() as Element
                    currentElement.addContent(text.toString())
                    text.delete(0, text.length)
                    stack.clear()
                    stack.push(div)
                    i += if (i + 5 < strlen && rtf[i + 5] == ' ') 5 else 4
                    i++
                    continue
                }

                // Simulate a paragraph break.
                if (rtf.startsWith("\\par", i)) {
                    val currentElement = stack.peek() as Element
                    currentElement.addContent(text.toString())
                    text.delete(0, text.length)
                    currentElement.addContent(OSISUtil.factory.createLB())
                    i += if (i + 4 < strlen && rtf[i + 4] == ' ') 4 else 3
                    i++
                    continue
                }

                // OSIS does not have the notion of centered text.
                // So we define our own
                if (rtf.startsWith("\\qc", i)) {
                    val centerDiv = OSISUtil.factory.createDiv()
                    centerDiv.setAttribute(OSIS_ATTR_TYPE, "x-center")
                    val currentElement = stack.peek() as Element
                    currentElement.addContent(text.toString())
                    text.delete(0, text.length)
                    currentElement.addContent(centerDiv)
                    stack.push(centerDiv)
                    // skip following space, if any
                    i += if (i + 3 < strlen && rtf[i + 3] == ' ') 3 else 2
                    i++
                    continue
                }

                // convert Unicode representations to Unicode
                if (rtf.startsWith("\\u", i)) {
                    val buf = StringBuilder()
                    i += 2
                    while (i < strlen) {
                        val curDigit = rtf[i]
                        if (curDigit != '-' && !Character.isDigit(curDigit)) {
                            break
                        }
                        buf.append(curDigit)
                        i++
                    }
                    // At this point:
                    // buf contains the numeric representation of the number, 16-bit
                    // signed
                    // charAt(i) is the substitution character if Unicode is not
                    // supported
                    var value = Integer.parseInt(buf.toString())
                    if (value < 0) {
                        value += 65536
                    }
                    text.append(value.toChar())
                    // don't advance since i is on the substitute character.
                    // (the Java for loop's i++ is applied by the continue)
                    i++
                    continue
                }

                // close italic and bold
                if (rtf.startsWith("\\i0", i) || rtf.startsWith("\\b0", i)) {
                    val currentElement = stack.pop() as Element
                    currentElement.addContent(text.toString())
                    text.delete(0, text.length)
                    i += if (i + 3 < strlen && rtf[i + 3] == ' ') 3 else 2
                    i++
                    continue
                }

                // Skip escaped whitespace
                if (rtf.startsWith(" ", i) || rtf.startsWith("\n", i)) {
                    i += 1
                    i++
                    continue
                }

                // start italic
                if (rtf.startsWith("\\i", i)) {
                    val hiElement = OSISUtil.factory.createHI()
                    hiElement.setAttribute(OSIS_ATTR_TYPE, HI_ITALIC)
                    val currentElement = stack.peek() as Element
                    currentElement.addContent(text.toString())
                    text.delete(0, text.length)
                    currentElement.addContent(hiElement)
                    stack.push(hiElement)
                    i += if (i + 2 < strlen && rtf[i + 2] == ' ') 2 else 1
                    i++
                    continue
                }

                // start bold
                if (rtf.startsWith("\\b", i)) {
                    val hiElement = OSISUtil.factory.createHI()
                    hiElement.setAttribute(OSIS_ATTR_TYPE, HI_BOLD)
                    val currentElement = stack.peek() as Element
                    currentElement.addContent(text.toString())
                    text.delete(0, text.length)
                    currentElement.addContent(hiElement)
                    stack.push(hiElement)
                    i += if (i + 2 < strlen && rtf[i + 2] == ' ') 2 else 1
                    i++
                    continue
                }

                i++
            }

            // If there is any text that has not been consumed
            if (text.length > 0) {
                div.addContent(text.toString())
            }
            // div.addContent(text.toString());
            // // If the fragment is already in a document, then use that.
            // Document doc = div.getDocument();
            // if (doc == null)
            // {
            // doc = new Document(div);
            // }
            // SAXEventProvider ep = new JDOMSAXEventProvider(doc);
            // ContentHandler osis = new
            // PrettySerializingContentHandler(FormatType.CLASSIC_INDENT);
            // try
            // {
            // ep.provideSAXEvents(osis);
            // }
            // catch (SAXException e)
            // {
            // e.printStackTrace();
            // }
            // System.err.println(osis.toString());
            return div.cloneContent()
        }

        /**
         * Find all the instances of elements of type <code>find</code> under the
         * element <code>div</code>. For internal use only.
         * 
         * @param start the node under which searches occur
         * @param name element name to search
         * @param reply the list to modify with matching content
         */
        private fun recurseDeepContent(start: Element, name: String?, reply: MutableList<Content>) {
            if (start.name == name) {
                reply.add(start)
            }

//        Content data = null;
            var ele: Element? = null
            for (data in start.content) {
                if (data is Element) {
                    ele = data
                    recurseDeepContent(ele, name, reply)
                }
            }
        }

        /**
         * If we have a String just add it to the buffer, but if we have an Element
         * then try to dig the strings out of it.
         * 
         * @param sub a sub element or text node
         * @param buffer the buffer to build on match
         */
        private fun recurseElement(sub: Any, buffer: StringBuilder) {
            if (sub is Text) {
                buffer.append(sub.text)
            } else if (sub is Element) {
                recurseChildren(sub, buffer)
            } else {
                log.error("unknown type: {}", sub.javaClass.name)
            }
        }

        /**
         * Helper to extract the Strings from a nest of JDOM elements
         * 
         * @param ele
         *            The JDOM Element to dig into
         * @param buffer
         *            The place we accumulate strings.
         */
        private fun recurseChildren(ele: Element, buffer: StringBuilder) {
            // ele is a JDOM Element that might have a getContent() method
            for (sub in ele.content) {
                recurseElement(sub, buffer)
            }
        }

        private const val strongsNumber = "strong:([GgHh][0-9]+!?[A-Za-z]*)"
        private val strongsNumberPattern: Pattern = Pattern.compile(strongsNumber)
    }
}
