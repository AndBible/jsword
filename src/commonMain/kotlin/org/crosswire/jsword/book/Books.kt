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

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.jvm.JvmStatic

import org.crosswire.common.activate.Activator
import org.crosswire.common.util.PluginRegistry
import org.crosswire.common.util.Reporter
import org.crosswire.common.util.SortedArraySet
import org.crosswire.common.util.currentTimeMillis
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * The Books class (along with Book) is the central point of contact between the
 * rest of the world and this set of packages.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class Books
/**
 * Create a singleton instance of the class. This is private to ensure that
 * only one can be created. This also makes the class final!
 */
private constructor() : AbstractBookList() {
    /**
     * The collection of Books
     */
    private var books: MutableSet<Book> = SortedArraySet(Comparator { a, b -> a.compareTo(b) })

    /**
     * The map of book initials
     */
    private var initials: MutableMap<String, Book> = HashMap()

    /**
     * The map of book names
     */
    private var names: MutableMap<String, Book> = HashMap()

    /**
     * An array of BookDrivers
     */
    private var driverSet: MutableSet<BookDriver> = HashSet()

    /**
     * Guards books, initials, names and driverSet together (formerly the monitor of this instance, rule §19).
     * Reentrant, because registerDriver calls addBook and removeBook.
     */
    private val lock = SynchronizedObject()

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookList#getBooks()
     */
    override fun getBooks(): MutableList<Book> = synchronized(lock) {
        copyToList(books)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookList#getBooks(org.crosswire.jsword.book.BookFilter)
     */
    override fun getBooks(filter: BookFilter?): MutableList<Book> = synchronized(lock) {
        copyToList(BookFilterIterator(books, filter))
    }

    /**
     * Search for the book by initials and name.
     * Looks for exact matches first, then searches case insensitive.
     * In all cases the whole initials or the whole name has to match.
     *
     * @param name The initials or name of the book to find
     * @return the book or null
     */
    fun getBook(name: String?): Book? = synchronized(lock) { getBookLocked(name) }

    private fun getBookLocked(name: String?): Book? {
        if (name == null) {
            return null
        }

        var book = initials.get(name)
        if (book != null) {
            return book
        }

        book = names.get(name)
        if (book != null) {
            return book
        }

        // Check for case-insensitive initial and name matches
        for (b in books) {
            if (name.equals(b.initials, ignoreCase = true) || name.equals(b.name, ignoreCase = true)) {
                return b
            }
        }

        return null
    }

    /**
     * Add a Book to the current list of Books. This method should only be
     * called by BibleDrivers, it is not a method for general consumption.
     *
     * @param book the book to add to this book list
     */
    fun addBook(book: Book?) = synchronized(lock) { addBookLocked(book) }

    private fun addBookLocked(book: Book?) {
        if (book == null) return

        // If we are upgrading module, we need to remove existing data first.
        if (books.contains(book)) {
            initials.remove(book.initials)
            names.remove(book.name)
            books.remove(book)
        }

        if (books.add(book)) {
            initials.put(book.initials, book)
            names.put(book.name, book)
            fireBooksChanged(instance, book, true)
        }
    }

    /**
     * Remove a Book from the current list of Books. This method should only be
     * called by BibleDrivers, it is not a method for general consumption.
     *
     * @param book the book to be removed from this book list
     * @throws BookException when an error occurs when performing this method
     */
    @Throws(BookException::class)
    fun removeBook(book: Book) = synchronized(lock) { removeBookLocked(book) }

    private fun removeBookLocked(book: Book) {
        // log.debug("unregistering book: {}", bmd.getName());

        Activator.deactivate(book)

        val removed = books.remove(book)
        if (removed) {
            initials.remove(book.initials)
            names.remove(book.name)
            fireBooksChanged(instance, book, false)
        } else {
            throw BookException(JSOtherMsg.lookupText("Could not remove unregistered Book: {0}", book.name))
        }
    }

    /**
     * Register the driver, adding its books to the list. Any books that this
     * driver used, but not any more are removed. This can be called repeatedly
     * to re-register the driver.
     *
     * @param driver
     *            The BookDriver to add
     * @throws BookException when an error occurs when performing this method
     */
    @Throws(BookException::class)
    fun registerDriver(driver: BookDriver) = synchronized(lock) { registerDriverLocked(driver) }

    private fun registerDriverLocked(driver: BookDriver) {
        log.debug("begin registering driver: {}", driver::class.simpleName)

        driverSet.add(driver)

        // Go through all the books and add all the new ones.
        // Remove those that are not known to the driver, but used to be.
        val bookArray = driver.getBooks()
        val current: MutableSet<Book> = HashSet()
        for (b in BookFilterIterator(books, BookFilters.getBooksByDriver(driver))) {
            current.add(b)
        }

        for (j in bookArray.indices) {
            val b = bookArray[j]
            if (current.contains(b)) {
                // Since it was already in there, we don't add it.
                // By removing it from current we will be left with
                // what is not now known by the driver.
                current.remove(b)
            } else {
                addBook(bookArray[j])
            }
        }

        // Remove the books from the previous version of the driver
        // that are not in this version.
        for (book in current) {
            removeBook(book)
        }

        log.debug("end registering driver: {}", driver::class.simpleName)
    }

    /**
     * Get an array of all the known drivers
     *
     * @return Found int or the default value
     */
    val drivers: Array<BookDriver>
        get() = synchronized(lock) { driverSet.toTypedArray() }

    private fun copyToList(source: Iterable<Book>): MutableList<Book> {
        val reply: MutableList<Book> = ArrayList()
        for (b in source) {
            reply.add(b)
        }
        return reply
    }

    /**
     * Registers all the drivers known to the program.
     */
    private fun autoRegister() {
        val entries = PluginRegistry.entries(BookDriver::class)

        log.debug("begin auto-registering {} drivers:", entries.size.toString())

        for (entry in entries) {
            try {
                registerDriver(entry.create())
            } catch (e: Exception) {
                // Every failure of a driver's creation, a BookException included, is reported to the user.
                Reporter.informUser(this, e)
            }
        }
    }

    companion object {
        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(Books::class)

        /**
         * The singleton instance.
         * This needs to be declared after all other statics it uses.
         */
        private val instance = Books()

        /**
         * Accessor for the singleton instance
         *
         * @return The singleton instance
         */
        @JvmStatic
        fun installed(): Books {
            return instance
        }

        // And it cannot register books until it is fully constructed
        // When this was the last call in the constructor it resulted
        // in "instance" being null in something it called.
        init {
            log.trace("Auto-registering start @ {}", currentTimeMillis().toString())
            instance.autoRegister()
            log.trace("Auto-registering stop @ {}", currentTimeMillis().toString())
        }
    }
}
