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
/**
 * Abstraction of an Index.
 */
package org.crosswire.jsword.index

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException

import org.crosswire.common.util.KUri

/**
 * Manages the life-cycle of an Index.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
interface IndexManager {
    /**
     * Detects if index data has been stored and is valid for this Bible.
     * 
     * @param book the book
     * @return true if the book has a usable index
     */
    fun isIndexed(book: Book?): Boolean

    /**
     * Create a new Searcher.
     * 
     * @param book the book
     * @return an index that can be searched.
     * @throws BookException 
     */
    @Throws(BookException::class)
    fun getIndex(book: Book?): Index

    /**
     * Detect or checking whether this book needs reindexing.
     * It is safe methods, you can always call it whether the book
     * is already indexed or not.
     * This check for <br></br>
     * <pre>
     * - isIndexed(Book book)
     * - Is index valid, eg index version changed incompatibly (due to internal structure change or search engine update)
     * -
     * </pre>
     * 
     * @param book the Book
     * @return true if no index present or current index is of incompatible/older version
     */
    fun needsReindexing(book: Book?): Boolean

    /**
     * Read from the given source version to generate ourselves. On completion
     * of this method the index should be usable.
     * 
     * @param book The book that should be indexed
     */
    fun scheduleIndexCreation(book: Book?)

    /**
     * We have downloaded a search index to a zip file. It should be installed
     * from here.
     * 
     * @param book
     *            The book that we downloaded an index for
     * @param tempDest
     *            The URI of a zip file to install
     * @throws BookException 
     */
    @Throws(BookException::class)
    fun installDownloadedIndex(book: Book?, tempDest: KUri?)

    /**
     * Tidy up after yourself and remove all the files that make up any indexes
     * you created.
     * 
     * @param book the book who's index should be deleted.
     * @throws BookException 
     */
    @Throws(BookException::class)
    fun deleteIndex(book: Book?)

    /**
     * Close all indexes associated with this Index Manager
     */
    fun closeAllIndexes()

    /**
     * The current IndexPolicy. Defaults to IndexPolicyAdapter.
     * Setting to null will cause the IndexPolicyAdapter to be used
     * (the policy to use when creating indexes).
     */
    var indexPolicy: IndexPolicy?
}
