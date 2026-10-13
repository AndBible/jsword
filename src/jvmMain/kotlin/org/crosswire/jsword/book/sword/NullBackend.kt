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
 * © CrossWire Bible Society, 2014 - 2016
 */
package org.crosswire.jsword.book.sword

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.book.sword.state.OpenFileState
import org.crosswire.jsword.passage.DefaultKeyList
import org.crosswire.jsword.passage.Key
import org.crosswire.common.kdom.Content
import okio.IOException

/**
 * A NullBackend is not attached to resources.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class NullBackend : Backend<OpenFileState> {

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#getBookMetaData()
     */
    override val bookMetaData: SwordBookMetaData?
        get() = null

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#decipher(byte[])
     */
    override fun decipher(data: ByteArray) {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#encipher(byte[])
     */
    override fun encipher(data: ByteArray) {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#readIndex()
     */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun readIndex(): Key {
        return DefaultKeyList()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun getRawText(key: Key?): String {
        return ""
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun setAliasKey(alias: Key?, source: Key?) {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#size(org.crosswire.jsword.passage.Key)
     */
    override fun getRawTextLength(key: Key?): Int {
        return 0
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#getGlobalKeyList()
     */
    @Throws(BookException::class)
    override fun getGlobalKeyList(): Key {
        return DefaultKeyList()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#readToOsis(org.crosswire.jsword.passage.Key, org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor)
     */
    @Throws(BookException::class)
    override fun readToOsis(key: Key?, processor: RawTextToXmlProcessor?): MutableList<Content> {
        return ArrayList()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#create()
     */
    @Throws(IOException::class, BookException::class)
    override fun create() {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#isSupported()
     */
    override val isSupported: Boolean
        get() = true

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#isWritable()
     */
    override val isWritable: Boolean
        get() = false

}
