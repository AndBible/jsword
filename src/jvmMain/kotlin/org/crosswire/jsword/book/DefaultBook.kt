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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Defines a single default book.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class DefaultBook(bookList: BookList, bookFilter: BookFilter) {
    /**
     * The list of candidate books.
     */
    private val books: BookList = bookList

    /**
     * The filter against books that returns candidates.
     */
    private val filter: BookFilter = bookFilter

    /**
     * The default book
     */
    private var book: Book? = null

    /**
     * Set the default Book. It must satisfy the filter.
     * 
     * @param newBook
     *            The version to use as default.
     */
    open fun setDefault(newBook: Book?) {
        if (filter.test(newBook!!)) {
            book = newBook
        }
    }

    /**
     * Set the default Book conditionally. It has to satisfy the filter and the
     * book must not currently be set.
     * 
     * @param newBook
     *            The version to use as default.
     */
    open fun setDefaultConditionally(newBook: Book?) {
        if (book == null) {
            setDefault(newBook)
        }
    }

    /**
     * Unset the current default book and attempt to appoint another.
     */
    protected open fun unsetDefault() {
        book = null

        checkReplacement()
    }

    /**
     * Calls [unsetDefault] for [Defaults], which is in the same package
     * but, unlike with Java's protected, can no longer reach a protected member.
     */
    internal fun unsetDefaultForPackage() {
        unsetDefault()
    }

    /**
     * Calls [unsetDefaultConditionally] for [Defaults], see [unsetDefaultForPackage].
     */
    internal fun unsetDefaultConditionallyForPackage(oldBook: Book?) {
        unsetDefaultConditionally(oldBook)
    }

    /**
     * Unset the current default book, if it matches the argument and attempt to
     * appoint another.
     * 
     * @param oldBook the book to unset if it is the default
     */
    protected open fun unsetDefaultConditionally(oldBook: Book?) {
        if (book === oldBook) {
            unsetDefault()
        }
    }

    /**
     * Get the current default book or null if there is none.
     * 
     * @return the current default version
     */
    open val default: Book?
        get() = book

    /**
     * This method is identical to <code>getDefault().getName()</code> and is
     * only used by Config which works best with strings under reflection.
     * 
     * @return the default book name
     */
    open val defaultName: String?
        get() {
            if (book == null) {
                return null
            }

            return book!!.name
        }

    /**
     * Trawl through all the known Books satisfying the filter looking for the
     * one matching the given name.
     * <p>
     * This method is for use with config scripts and other things that
     * <b>need</b> to work with Strings. The preferred method is to use Book
     * objects.
     * <p>
     * This method is picky in that it only matches when the driver and the
     * version are the same. The user (probably) only cares about the version
     * though, and so might be disappointed when we fail to match AV (FooDriver)
     * against AV (BarDriver).
     * 
     * @param name
     *            The version to use as default.
     */
    open fun setDefaultByName(name: String?) {
        if (name == null || name.length == 0) {
            LOGGER.warn("Attempt to set empty book as default. Ignoring")
            return
        }

        for (aBook in books.getBooks(filter)) {
            if (aBook.match(name)) {
                setDefault(aBook)
                return
            }
        }

        LOGGER.warn("Book not found. Ignoring: {}", name)
    }

    /**
     * Go through all of the current books checking to see if we need to replace
     * the current defaults with one of these.
     */
    protected open fun checkReplacement() {
        val bookList = books.getBooks(filter)

        val it = bookList.iterator()
        if (it.hasNext()) {
            book = it.next()
        }
    }

    private companion object {
        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(DefaultBook::class.java)
    }
}
