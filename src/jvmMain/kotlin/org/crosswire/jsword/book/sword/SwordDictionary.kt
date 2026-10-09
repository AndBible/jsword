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
package org.crosswire.jsword.book.sword

import java.util.ArrayList

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.basic.AbstractBook
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.passage.DefaultKeyList
import org.crosswire.jsword.passage.DefaultLeafKeyList
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.jdom2.Content

/**
 * A Sword version of Dictionary.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class SwordDictionary
/**
 * Construct an SwordDictionary given the BookMetaData and the AbstractBackend.
 *
 * @param sbmd the metadata that describes the book
 * @param backend the means by which the resource is accessed
 */
(sbmd: SwordBookMetaData, backend: Backend<*>?) : AbstractBook(sbmd, backend) {
    init {
        if (backend !is AbstractKeyBackend<*>) {
            throw IllegalArgumentException("AbstractBackend must be an AbstractKeyBackened")
        }
    }

    /**
     * The filter to use to convert to OSIS.
     */
    private val filter: SourceFilter = sbmd.filter

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getOsisIterator(org.crosswire.jsword.passage.Key, boolean, boolean)
     */
    @Throws(BookException::class)
    override fun getOsisIterator(key: Key?, allowEmpty: Boolean, allowGenTitles: Boolean): MutableIterator<Content> {
        jswordAssert(key != null) { "key must not be null" }

        val content: MutableList<Content> = ArrayList()
        val title = OSISUtil.factory().createGeneratedTitle()
        title.addContent(key!!.getName())
        content.add(title)

        val txt = backend!!.getRawText(key)
        val osisContent = filter.toOSIS(this, key, txt)
        content.addAll(osisContent)

        return content.iterator()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun getRawText(key: Key?): String {
        return backend!!.getRawText(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        val backend = backend
        return backend != null && backend.contains(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    public override fun getOsis(key: Key?, noOpRawTextProcessor: RawTextToXmlProcessor?): MutableList<Content> {
        jswordAssert(key != null) { "key must not be null" }

        return backend!!.readToOsis(key, noOpRawTextProcessor)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isWritable()
     */
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
        throw BookException(JSOtherMsg.lookupText("This Book is read-only."))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getGlobalKeyList()
     */
    override fun getGlobalKeyList(): Key {
        return backend as AbstractKeyBackend<*>
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getValidKey(java.lang.String)
     */
    override fun getValidKey(name: String?): Key {
        try {
            return getKey(name)
        } catch (e: NoSuchKeyException) {
            return createEmptyKeyList()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getKey(java.lang.String)
     */
    @Throws(NoSuchKeyException::class)
    override fun getKey(name: String?): Key {
        val keyBackend = backend as AbstractKeyBackend<*>

        val pos = keyBackend.indexOf(DefaultLeafKeyList(name))
        if (pos < 0) {
            throw NoSuchKeyException(JSMsg.gettext("No entry for '{0}' in {1}.", name, initials))
            //if (keyBackend.getCardinality() > -pos - 1) {
            //    return keyBackend.get(-pos - 1);
            //}
            //return keyBackend.get(keyBackend.getCardinality() - 1);
        }
        return keyBackend.get(pos)!!
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#createEmptyKeyList()
     */
    override fun createEmptyKeyList(): Key {
        return DefaultKeyList()
    }
}
