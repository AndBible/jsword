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
 */
package org.crosswire.jsword.index.search

/**
 * The DefaultSearchModifier provides a simple implementation of a
 * SearchModifier.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class DefaultSearchModifier
/**
 * A default SearchModifier that returns all hits and does not rank the
 * results.
 */
() : SearchModifier {
    /**
     * The indicator of whether the request should be ranked.
     */
    private var ranked: Boolean = false

    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.index.search.SearchModifier#isRanked()
     */
    /**
     * Set whether or not the search should be ranked.
     * 
     * @param newRanked
     *            true if the search should be ranked
     */
    // backed by a field named "ranked" to keep the serialized form
    override var isRanked: Boolean
        get() = ranked
        set(newRanked) {
            ranked = newRanked
        }

    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.index.search.SearchModifier#getMaxResults()
     */
    /**
     * The maximum number of results to provide. A value of Int.MAX_VALUE,
     * the default, means get all results.
     * 
     * @param newMaxResults
     *            the maxResults to set
     */
    override var maxResults: Int = Int.MAX_VALUE
}
