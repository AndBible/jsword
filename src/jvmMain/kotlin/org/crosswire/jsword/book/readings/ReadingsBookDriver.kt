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
package org.crosswire.jsword.book.readings

import java.io.IOException
import java.util.ArrayList

import org.crosswire.common.util.IndexFiles
import org.crosswire.common.util.KswordResources
import org.crosswire.common.util.URIFilter
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.basic.AbstractBookDriver

/**
 * A driver for the readings dictionary.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class ReadingsBookDriver : AbstractBookDriver() {
    /**
     * The meta data array
     */
    private var books: Array<Book>? = null

    /**
     * Setup the array of BookMetaDatas
     */
    init {
        val bookList: MutableList<Book> = ArrayList<Book>()
        val installedBooks = getInstalledReadingsSets()
        for (i in installedBooks.indices) {
            bookList.add(ReadingsBook(this, installedBooks[i], BookCategory.DAILY_DEVOTIONS))
        }

        books = bookList.toTypedArray()
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.book.BookDriver#getBooks()
     */
    override fun getBooks(): Array<Book> {
        return books!!.clone()
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.book.BookDriver#getDriverName()
     */
    override fun getDriverName(): String {
        return "Readings"
    }

    /**
     * Get a list of the available readings sets
     *
     * @return all the installed reading sets
     */
    open fun getInstalledReadingsSets(): Array<String> {
        val text = KswordResources.text("readings.txt") ?: return arrayOf<String>()
        val filter = ReadingsFilter()
        return IndexFiles.parse(text) { filter.accept(it) }.toTypedArray()
    }

    /**
     * Get all files mentioned by readings.txt
     */
    // was package-private: Kotlin has no package visibility, internal compiles to public
    internal class ReadingsFilter : URIFilter {
        override fun accept(name: String?): Boolean {
            return true
        }
    }

    companion object {
        /**
         * Get the singleton instance of this driver.
         * 
         * @return this driver instance
         */
        @JvmStatic
        fun instance(): BookDriver {
            return INSTANCE
        }

        /**
         * Resources subdir for readings sets
         */
        const val DIR_READINGS: String = "readings"

        /**
         * A shared instance of this driver.
         */
        private val INSTANCE: BookDriver = ReadingsBookDriver()
    }
}
