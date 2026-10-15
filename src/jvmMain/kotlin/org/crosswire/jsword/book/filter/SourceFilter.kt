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
 * This data package manages the filtering of book data returned by the various books.
 *
 * <p>There are a number of SourceFilters that are designed to marshal text from THML,
 * GBF, OSIS or plain text into a BookData object. The Filters class gives
 * access to these,</p>
 */
package org.crosswire.jsword.book.filter

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.passage.Key
import fi.sykero.kdom.Content

/**
 * A generic interface for things that can convert a String into OSIS data.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
actual interface SourceFilter : Cloneable {
    /**
     * Converter from plain (encoded) text to OSIS data
     * 
     * @param book the book
     * @param key
     *            The key for the text
     * @param plain
     *            The encoded text
     * @return a List of OSIS Elements
     */
    fun toOSIS(book: Book?, key: Key?, plain: String?): MutableList<Content>

    /**
     * This needs to be declared here so that it is visible as a method on a
     * derived Filter.
     * 
     * @return A complete copy of ourselves
     */
    public override fun clone(): SourceFilter
}
