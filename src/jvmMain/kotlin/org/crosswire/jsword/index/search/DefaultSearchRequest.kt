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
 * A default implementation of a SearchRequest.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class DefaultSearchRequest
/**
 * Create a DefaultSearchRequest for the provided request and the provided
 * modifiers.
 * 
 * @param theRequest
 *            what is being searched
 * @param theModifier
 *            how the search is to be modified
 */
(theRequest: String?, theModifier: SearchModifier?) : SearchRequest {
    /**
     * Create a DefaultSearchRequest for the provided request.
     * 
     * @param theRequest
     *            what is being searched
     */
    constructor(theRequest: String?) : this(theRequest, null)

    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.index.search.SearchRequest#isRanked()
     */
    /**
     * How the search is to be modified
     */
    private val modifier: SearchModifier? = theModifier

    override val searchModifier: SearchModifier?
        get() = modifier

    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.index.search.SearchRequest#getRequest()
     */
    /**
     * The actual search request
     */
    override val request: String? = theRequest
}
