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
package org.crosswire.jsword.index

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.index.search.SearchModifier
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException

/**
 * An index into a body of text that knows what words exist and where they are.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
interface Index {
    /**
     * Find the set of references that satisfy the query. Query is anything that
     * the underlying index can handle. If the `query` being searched
     * for is null then an empty Key **MUST** be returned. Users of this
     * index may use this functionality to get empty KeyLists which they then
     * use to aggregate other searches done on this index.
     * 
     * @param query
     *            The text to search for
     * @return The references to the word
     * @throws BookException 
     */
    @Throws(BookException::class)
    fun find(query: String?): Key

    /**
     * An index must be able to create KeyLists for users in a similar way to
     * the Book that it is indexing.
     * 
     * @param name
     *            The string to convert to a Key
     * @return A new Key representing the given string, if possible
     * @throws NoSuchKeyException
     *             If the string can not be turned into a Key
     * @see org.crosswire.jsword.passage.KeyFactory#getKey(String)
     */
    @Throws(NoSuchKeyException::class)
    fun getKey(name: String?): Key

    /**
     * Set any modifier for the current and subsequent search. Using null will
     * clear the search modifier.
     * 
     * The current SearchModifier. If there is none then it is null.
     */
    var searchModifier: SearchModifier?

    /**
     * Closes resources related to the index
     */
    fun close()
}
