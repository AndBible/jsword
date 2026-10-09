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
package org.crosswire.jsword.book

import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

import org.crosswire.common.util.CollectionUtil

/**
 * A basic implementation of BookList. The methods in this abstract class are
 * duplicates of those in Books, so bugs fixed in one should be fixed in the
 * other too.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
abstract class AbstractBookList
/**
 * Build a default BookList
 */
() : BookList {
    /**
     * The list of listeners
     */
    private var listeners: MutableList<BooksListener?> = CopyOnWriteArrayList()

    init {
        listeners = CopyOnWriteArrayList()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookList#getBooks(org.crosswire.jsword.book.BookFilter)
     */
    override fun getBooks(filter: BookFilter?): MutableList<Book> {
        val temp = CollectionUtil.createList(BookFilterIterator(getBooks(), filter))
        return Collections.unmodifiableList(temp)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookList#addBooksListener(org.crosswire.jsword.book.BooksListener)
     */
    override fun addBooksListener(li: BooksListener?) {
        listeners.add(li)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookList#removeBooksListener(org.crosswire.jsword.book.BooksListener)
     */
    override fun removeBooksListener(li: BooksListener?) {
        listeners.remove(li)
    }

    /**
     * Kick of an event sequence
     *
     * @param source
     *            The event source
     * @param book
     *            The changed Book
     * @param added
     *            Is it added?
     */
    protected open fun fireBooksChanged(source: Any?, book: Book?, added: Boolean) {
        val ev = BooksEvent(source, book, added)
        for (listener in listeners) {
            if (added) {
                listener!!.bookAdded(ev)
            } else {
                listener!!.bookRemoved(ev)
            }
        }
    }
}
