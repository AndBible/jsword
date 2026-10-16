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
package org.crosswire.jsword.index.search

import kotlin.jvm.JvmStatic
import org.crosswire.common.util.PluginRegistry
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.index.IndexManagerFactory
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

import okio.IOException

/**
 * Factory method for creating a new Searcher.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class SearcherFactory
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Create a new Searcher.
         * 
         * @param book the book
         * @return the searcher
         * @throws BookException when the search index cannot be opened or the searcher cannot be created
         */
        @JvmStatic
        @Throws(BookException::class)
        fun createSearcher(book: Book?): Searcher {
            try {
                val imanager = IndexManagerFactory.getIndexManager()
                val index = imanager.getIndex(book)

                val parser = PluginRegistry.default(Searcher::class)?.create() ?: error("No default Searcher is registered")
                parser.init(index)

                return parser
            } catch (e: IOException) {
                log.error("createSearcher failed", e)
                throw BookException(JSOtherMsg.lookupText("Failed to initialize the search index"), e)
            } catch (e: BookException) {
                log.error("createSearcher failed", e)
                throw BookException(JSOtherMsg.lookupText("Failed to initialize the search index"), e)
            }
        }

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(SearcherFactory::class)
    }
}
