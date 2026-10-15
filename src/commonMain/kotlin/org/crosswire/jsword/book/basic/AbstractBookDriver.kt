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
package org.crosswire.jsword.book.basic

import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookException

/**
 * The AbstractBookDriver class implements some BibleDriver methods, making a
 * simple read-only BibleDriver.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
abstract class AbstractBookDriver : BookDriver {
    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.book.BookDriver#isWritable()
     */
    override val isWritable: Boolean
        get() = false

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.crosswire.jsword.book.BookDriver#create(org.crosswire.jsword.book
     * .Book, org.crosswire.jsword.book.events.WorkListener)
     */
    @Throws(BookException::class)
    override fun create(source: Book?): Book {
        throw BookException(JSOtherMsg.lookupText("This Book is read-only."))
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.crosswire.jsword.book.BookDriver#isDeletable(org.crosswire.jsword
     * .book.Book)
     */
    override fun isDeletable(dead: Book): Boolean {
        return false
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.crosswire.jsword.book.BookDriver#delete(org.crosswire.jsword
     * .book.Book)
     */
    @Throws(BookException::class)
    override fun delete(dead: Book) {
        throw BookException(JSOtherMsg.lookupText("This Book is read-only."))
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.book.BookProvider#getFirstBook()
     */
    override val firstBook: Book?
        get() {
            val books: Array<Book>? = getBooks()
            return if (books == null || books.size == 0) null else books[0]
        }
}
