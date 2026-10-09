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
 * An implementation of the Book interfaces that reads from SWORD project
 * books (modules).
 */
package org.crosswire.jsword.book.sword

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.basic.AbstractPassageBook
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.passage.VerseKey
import org.jdom2.Attribute
import org.jdom2.Content
import org.jdom2.Element
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * SwordBook is a base class for all verse based Sword type books.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class SwordBook
/**
 * Construct an SwordBook given the BookMetaData and the AbstractBackend.
 *
 * @param sbmd the metadata that describes the book
 * @param backend the means by which the resource is accessed
 */
(sbmd: SwordBookMetaData, backend: Backend<*>?) : AbstractPassageBook(sbmd, backend) {
    /**
     * The filter to use to convert to OSIS.
     */
    private val filter: SourceFilter = sbmd.filter

    /**
     * A cached representation of the global key list.
     */
    private var global: Key? = null

    init {
        if (backend == null) {
            throw IllegalArgumentException("AbstractBackend must not be null.")
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getGlobalKeyList()
     */
    final override fun getGlobalKeyList(): Key {
        if (global == null) {
            try {
                global = backend!!.getGlobalKeyList()
                return global!!
            } catch (ex: UnsupportedOperationException) {
                // fail silently, operation not supported by the backend
                log.debug(ex.message)
            } catch (ex: BookException) {
                // failing silently, as previous behavior was to attempt to
                // return as much as we can using the slower method
                log.debug(ex.message)
            }

            val v11n = super.versification

            global = super.createEmptyKeyList()
            val all = PassageKeyFactory.instance().getGlobalKeyList(v11n)

            for (key in all) {
                if (contains(key)) {
                    global!!.addAll(key)
                }
            }
        }

        return global!!
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBook#getScope()
     */
    override val scope: Key?
        get() {
            val sbmd = bookMetaData as SwordBookMetaData
            //if the book type doesn't have verses, then leave it.
            if (sbmd.getProperty(BookMetaData.KEY_VERSIFICATION) == null) {
                //then we're not looking at a versified book
                return null
            }

            val keyString: Any? = sbmd.getProperty(BookMetaData.KEY_SCOPE)

            if (keyString != null) {
                try {
                    return getKey(keyString as String)
                } catch (ex: NoSuchKeyException) {
                    //the scope defined is not correct
                    log.error("Unable to parse scope from book", ex)
                    return null
                }
            }

            //need to calculate the scope
            //now comes the expensive part
            val bookKeys = getGlobalKeyList()

            //this is practically impossible, but cater for it just in case.
            if (bookKeys !is VerseKey<*>) {
                log.error("Global key list isn't a verse key. A very expensive no-op has just occurred.")
                return null
            }

            return bookKeys
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return backend!!.contains(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun getRawText(key: Key?): String {
        return backend!!.getRawText(key)
    }

    @Throws(BookException::class)
    override fun getOsis(key: Key?, noOpRawTextProcessor: RawTextToXmlProcessor?): MutableList<Content> {
        val result = backend!!.readToOsis(key, noOpRawTextProcessor)
        jswordAssert(result != null) { "readToOsis returned null" }
        return result
    }

    override fun addOSIS(key: Key?, div: Element?, osisContent: MutableList<Content>?) {
        // See if the text is marked up with verses
        // If it is then just add it.
        for (content in osisContent!!) {
            if (content is Element) {
                val ele: Element = content
                if (ele.getName() == OSISUtil.OSIS_ELEMENT_VERSE) {
                    super.addOSIS(key, div, osisContent)
                    return
                }
            }
        }

        // If we get here then the text is not marked up with verse
        // In this case we add the verse markup, if the verse is not 0.
        if (KeyUtil.getVerse(key).verse == 0) {
            super.addOSIS(key, div, osisContent)
        } else {
            // In a SWORD module, the verse element is to be put
            // after the "preverse" material.
            val everse = OSISUtil.factory().createVerse()
            everse.setAttribute(OSISUtil.OSIS_ATTR_OSISID, key!!.getOsisID())
            everse.setAttribute("verseOrdinal", KeyUtil.getVerse(key).ordinal.toString())
            div!!.addContent(everse)
            super.addOSIS(key, everse, osisContent)
        }
    }

    override fun addOSIS(key: Key?, content: MutableList<Content>?, osisContent: MutableList<Content>?) {
        // If there is no content, then there is nothing to do.
        if (osisContent!!.size == 0) {
            return
        }

        // Note: Verse 0 is an introduction and not a verse so it never gets verse markup.
        // However, it should be wrapped by a div as it should be isolated as an intro.
        if (KeyUtil.getVerse(key).verse == 0) {
            val div = OSISUtil.factory().createDiv()
            div.setAttribute(OSISUtil.OSIS_ATTR_OSISID, key!!.getOsisID())
            // Mark it as generated
            div.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.GENERATED_CONTENT)
            // Put the OSIS into the div
            div.addContent(osisContent)
            // Then put the div at the end of the contentList
            content!!.add(div)
            return
        }

        // SWORD modules typically do not have the verse marker. When they don't
        // then the verse element needs to wrap the verse content.
        // However, the verse content may have "pre-verse" content.
        // This is marked up in one of two ways:
        // 1) old way
        //      <title subType="x-preverse">...</title>
        //    There may be more than one title marked as such.
        // 2) current way
        //      <div sID="xxx" type="x-milestone" subType="x-preverse"/>
        //         ...
        //      <div eID="xxx" type="x-milestone" subType="x-preverse"/>
        //      verse content
        //    In this we only need to look for the ending.
        // The critical observation is that the verse marker is to
        // follow the last element marked x-preverse.
        // Also, there are a good number of modules that have a title marked
        // type="psalm" and not canonical="true" which they should be.

        // See if the text is marked up with verse elements
        // If it is then just add it.
        var start = 0
        var found = -1
        var wrapped = false
        var preverse: Element? = null
        for (item in osisContent) {
            if (item is Element) {
                val ele: Element = item
                val name = ele.getName()
                if (OSISUtil.OSIS_ELEMENT_VERSE == name) {
                    wrapped = true
                    ele.setAttribute("verseOrdinal", KeyUtil.getVerse(key).ordinal.toString())
                    continue
                }
                val typeAttr: Attribute? = ele.getAttribute(OSISUtil.OSIS_ATTR_TYPE)
                val subTypeAttr: Attribute? = ele.getAttribute(OSISUtil.OSIS_ATTR_SUBTYPE)
                if (subTypeAttr != null && "x-preverse" == subTypeAttr.value) {
                    if (OSISUtil.OSIS_ELEMENT_DIV == name || OSISUtil.OSIS_ELEMENT_TITLE == name) {
                        preverse = ele
                        found = start
                    }
                } else if (typeAttr != null && "psalm" == typeAttr.value && OSISUtil.OSIS_ELEMENT_TITLE == name) {
                    // Psalm titles should be both canonical and preverse
                    // set the appropriate attributes if not already set.
                    val canonicalAttr: Attribute? = ele.getAttribute(OSISUtil.OSIS_ATTR_CANONICAL)
                    if (canonicalAttr == null) {
                        ele.setAttribute(OSISUtil.OSIS_ATTR_CANONICAL, "true")
                    }
                    if (subTypeAttr == null) {
                        ele.setAttribute(OSISUtil.OSIS_ATTR_SUBTYPE, "x-preverse")
                        preverse = ele
                        found = start
                    }
                }
            }
            start++
        }

        // Check to see whether the text is marked up with verse
        if (wrapped) {
            super.addOSIS(key, content, osisContent)
            return
        }

        // If we get here then the text is not marked up with verse
        // In this case we add the verse markup, if the verse is not 0.
        val everse = OSISUtil.factory().createVerse()
        everse.setAttribute(OSISUtil.OSIS_ATTR_OSISID, key!!.getOsisID())
        everse.setAttribute("verseOrdinal", KeyUtil.getVerse(key).ordinal.toString())
        if (preverse == null) {
            everse.addContent(osisContent)
        } else {
            val sublist = osisContent.subList(found + 1, osisContent.size)
            everse.addContent(sublist)
            // a sub list is actually part of the original list
            // clearing it removes it from the original list
            sublist.clear()
            // Now append shortened list
            super.addOSIS(key, content, osisContent)
        }
        // Then put the verse at the end of the contentList
        content!!.add(everse)
    }

    override fun isWritable(): Boolean {
        return backend!!.isWritable
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#setRawText(org.crosswire.jsword.passage.Key, java.lang.String)
     */
    @Throws(BookException::class)
    override fun setRawText(key: Key?, rawData: String?) {
        throw BookException(JSOtherMsg.lookupText("This Book is read-only."))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun setAliasKey(alias: Key?, source: Key?) {
        backend!!.setAliasKey(alias, source)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractPassageBook#getFilter()
     */
    override fun getFilter(): SourceFilter {
        return filter
    }

    private companion object {
        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(SwordBook::class)
    }
}
