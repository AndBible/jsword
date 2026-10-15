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

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.book.sword.state.OpenFileState
import org.crosswire.jsword.passage.Key
import fi.sykero.kdom.Content
import okio.IOException

/**
 * Uniform representation of all Backends.
 *
 * @param <T> The type of the OpenFileState that this class extends.
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
interface Backend<T : OpenFileState> {

    /**
     * @return Returns the Sword BookMetaData.
     */
    val bookMetaData: BookMetaData?

    /**
     * Decipher the data in place, if it is enciphered and there is a key to
     * unlock it.
     *
     * @param data the data to unlock
     */
    fun decipher(data: ByteArray)

    /**
     * Encipher the data in place, if there is a key to unlock it.
     *
     * @param data
     */
    fun encipher(data: ByteArray)

    /**
     * Initialize a AbstractBackend before use. This method needs to call
     * addKey() a number of times on GenBookBackend
     * 
     * @return the list of all keys for the book
     */
    @Deprecated("no replacement")
    fun readIndex(): Key?

    /**
     * Determine whether this Book contains the key in question
     *
     * @param key The key whose presence is desired.
     * @return true if the Book contains the key
     */
    fun contains(key: Key?): Boolean

    /**
     * Get the text as it is found in the Book for the given key
     * 
     * @param key the key for which the raw text is desired.
     * @return the text from the module
     * @throws BookException 
     */
    @Throws(BookException::class)
    fun getRawText(key: Key?): String

    @Throws(BookException::class)
    fun setAliasKey(alias: Key?, source: Key?)

    /**
     * Determine the size of the raw data for the key in question.
     * This method may not be faster than getting the raw text and getting its size.
     *
     * @param key The key whose raw data length is desired.
     * @return The length of the raw data, 0 if not a valid key.
     */
    fun getRawTextLength(key: Key?): Int

    /**
     * Gets the fast global key list, and if this operation is not supported, throws a [UnsupportedOperationException]
     *
     * @return the fast global key list
     * @throws BookException the book exception if for some reason the book failed to be read properly.
     */
    @Throws(BookException::class)
    fun getGlobalKeyList(): Key

    /**
     * Get the text allotted for the given entry
     *
     * @param key       The key to fetch
     * @param processor processor that executes before/after the content is read from
     * disk or another kind of backend
     * @return String The data for the verse in question
     * @throws BookException If the data can not be read.
     */
    @Throws(BookException::class)
    fun readToOsis(key: Key?, processor: RawTextToXmlProcessor?): MutableList<Content>

    /**
     * Create the directory to hold the Book if it does not exist.
     *
     * @throws IOException
     * @throws BookException
     */
    @Throws(IOException::class, BookException::class)
    fun create()

    /**
     * Returns whether this AbstractBackend is implemented.
     *
     * @return true if this AbstractBackend is implemented.
     */
    val isSupported: Boolean

    /**
     * A Backend is writable if the file system allows the underlying files to
     * be opened for writing and if the backend has implemented writing.
     * Ultimately, all drivers should allow writing. At this time writing is not
     * supported by most backends, so abstract implementations should return false
     * and let specific implementations return true otherwise.
     *
     * @return true if the book is writable
     */
    val isWritable: Boolean
}
