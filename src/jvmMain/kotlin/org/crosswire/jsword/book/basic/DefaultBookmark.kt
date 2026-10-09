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
 * © CrossWire Bible Society, 2007 - 2016
 *
 */
package org.crosswire.jsword.book.basic

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Bookmark
import org.crosswire.jsword.index.search.SearchRequest
import java.util.ArrayList
import java.util.Collections

/**
 * A Bookmark remembers a particular view of one or more Books. What is viewed
 * regarding a book set is either a SearchRequest or a key lookup request.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class DefaultBookmark
/**
 * Create an empty default bookmark
 */
() : Bookmark, Cloneable {
    /**
     * The list of books.
     */
    @Transient
    private var bookList: MutableList<Book> = ArrayList()

    /**
     * True while one of the two request setters clears the other one: the setters of
     * searchRequest and lookupRequest each reset the other property, and this stops the
     * reset from bouncing back (Java assigned the other field directly).
     */
    @Transient
    private var clearingOther = false

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Bookmark#addBook(org.crosswire.jsword.book.Book)
     */
    override fun addBook(book: Book?) {
        // Java allowed adding null here; the list is typed non-null for the Bookmark.books property
        @Suppress("UNCHECKED_CAST")
        (bookList as MutableList<Book?>).add(book)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Bookmark#getBooks()
     */
    override val books: MutableList<Book>
        get() = Collections.unmodifiableList(bookList)

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Bookmark#setSearchRequest(org.crosswire.jsword.index.search.SearchRequest)
     */
    override var searchRequest: SearchRequest? = null
        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.Bookmark#getSearchRequest()
         */
        set(request) {
            field = request
            if (!clearingOther) {
                clearingOther = true
                lookupRequest = null
                clearingOther = false
            }
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Bookmark#setLookupRequest(java.lang.String)
     */
    override var lookupRequest: String? = null
        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.Bookmark#getLookupRequest()
         */
        set(request) {
            field = request
            if (!clearingOther) {
                clearingOther = true
                searchRequest = null
                clearingOther = false
            }
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Bookmark#getBookData()
     */
    override val bookData: BookData?
        get() = null

    public override fun clone(): DefaultBookmark {
        var clone: DefaultBookmark? = null
        try {
            clone = super<Cloneable>.clone() as DefaultBookmark
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }
        return clone!!
    }

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID = 6959196267292499574L
    }
}
