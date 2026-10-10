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
 * © CrossWire Bible Society, 2012 - 2016
 *
 */
package org.crosswire.jsword.versification

import kotlin.jvm.JvmField


/**
 * A BibleBookList is an ordered list of one or more BibleBooks.
 * Typically, a BibleBookList is a member of a Versification.
 *
 * Java had this class package-private; Kotlin has no package visibility and the public
 * `Versification.dump`/`optimize` expose it, so it is public here (was package-private).
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
/* pkg protected */
open class BibleBookList
/**
 * Create an ordered BibleBookList from the input.
 * @param books
 */
internal constructor(books: Array<BibleBook>) : Iterable<BibleBook> {
    /** The ordered books in this list  */
    @JvmField
    protected var books: Array<BibleBook> = books.copyOf()

    /** The bookMap maps from a BibleBook to the position that it has in `books`.  */
    private lateinit var bookMap: IntArray

    init {
        initialize()
    }

    /**
     * Does this Versification contain the BibleBook.
     *
     * @param book
     * @return true if it is present.
     */
    open fun contains(book: BibleBook?): Boolean {
        return book != null && bookMap[book.ordinal] != -1
    }

    /**
     * Where does the BibleBook come in the order of books of the Bible.
     * The first book is 0, the next is 1 and so forth.
     * If the BibleBook is not in this Reference System,
     * then the return value of this routine is -1.
     *
     * @param book
     * @return the ordinal value of the book or -1 if not present
     */
    open fun getOrdinal(book: BibleBook?): Int {
        return bookMap[book!!.ordinal]
    }

    /**
     * Get the number of books in this Versification.
     * @return the number of books
     */
    open val bookCount: Int
        get() = books.size

    /**
     * Get the BibleBook by its position in this Versification.
     * If the position is negative, return the first book.
     * If the position is greater than the last, return the last book.
     *
     * @param ordinal
     * @return the indicated book
     */
    open fun getBook(ordinal: Int): BibleBook {
        var ord = ordinal
        if (ord < 0) {
            ord = 0
        }
        if (ord >= books.size) {
            ord = books.size - 1
        }
        return books[ord]
    }

    /**
     * Get the BibleBooks in this Versification.
     *
     * @return an Iterator over the books
     */
    override fun iterator(): Iterator<BibleBook> {
        return object : MutableIterator<BibleBook> {
            private var nextBook: BibleBook? = books[0]

            override fun hasNext(): Boolean {
                return nextBook != null
            }

            override fun next(): BibleBook {
                if (nextBook == null) {
                    throw NoSuchElementException()
                }

                val current = nextBook!!
                nextBook = getNextBook(nextBook)
                return current
            }

            override fun remove() {
                throw UnsupportedOperationException()
            }
        }
    }

    /**
     * Return the first book in the list.
     *
     * @return the first book in the list
     */
    open val firstBook: BibleBook
        get() = books[0]

    /**
     * Return the first book in the list.
     *
     * @return the first book in the list
     */
    open val lastBook: BibleBook
        get() = books[books.size - 1]

    /**
     * Given a BibleBook, get the previous BibleBook in this Versification. If it is the first book, return null.
     * @param book A BibleBook in the Versification
     * @return the previous BibleBook or null.
     */
    open fun getPreviousBook(book: BibleBook?): BibleBook? {
        val ordinal = book!!.ordinal
        val position = bookMap[ordinal]
        if (position > 0) {
            return books[position - 1]
        }

        return null
    }

    /**
     * Given a BibleBook, get the next BibleBook in this Versification. If it is the last book, return null.
     * @param book A BibleBook in the Versification
     * @return the previous BibleBook or null.
     */
    open fun getNextBook(book: BibleBook?): BibleBook? {
        val ordinal = book!!.ordinal
        val position = bookMap[ordinal]
        if (position != -1 && position + 1 < books.size) {
            return books[position + 1]
        }
        return null
    }

    /**
     * The bookMap contains one slot for every BibleBook, indexed by it's ordinal value.
     * The value of that entry is the position of the book in the BookList.
     * If the BibleBook is not present in books, it's value is -1.
     */
    private fun initialize() {
        bookMap = IntArray(BibleBook.values().size + 1)
        // Initialize all slots to -1
        for (b in BibleBook.values()) {
            bookMap[b.ordinal] = -1
        }

        // Fill in the position of the books into that list
        for (i in books.indices) {
            bookMap[books[i].ordinal] = i
        }
    }

}
