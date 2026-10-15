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

/**
 * An iterator that filters as it goes.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class BookFilterIterator
/**
 * Simple ctor
 *
 * @param books an iterator over a set of books
 * @param filter
 *            The filter to use, if null, will iterate over all values
 */
(books: Iterable<Book>, private var filter: BookFilter?) : Iterable<Book>, MutableIterator<Book> {
    /**
     * The stored next value
     */
    private var next: Book? = null

    private var it: Iterator<Book> = books.iterator()

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): Iterator<Book> {
        return this
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    override fun hasNext(): Boolean {
        next = findNext()
        return next != null
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    override fun next(): Book {
        return next ?: throw NoSuchElementException()
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    override fun remove() {
        throw UnsupportedOperationException()
    }

    /**
     * Find the next (if there is one)
     *
     * @return the next book
     */
    private fun findNext(): Book? {
        while (it.hasNext()) {
            val book = it.next()
            val f = filter
            if (f == null || f.test(book)) {
                return book
            }
        }

        return null
    }
}
