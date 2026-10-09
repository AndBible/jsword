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
 * © CrossWire Bible Society, 2008 - 2016
 *
 */
package org.crosswire.jsword.index

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.Books

/**
 * BookIndexer allows one to check the status of an index, build an index or
 * delete an index. This is similar to SWORD's mkfastmod.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class BookIndexer(book: Book?) {
    @JvmField
    protected var book: Book? = book

    @JvmField
    protected var indexManager: IndexManager = IndexManagerFactory.getIndexManager()
    private val isl: IndexStatusListener = StatusListener(this)
    private var done = true // not busy

    open val isIndexed: Boolean
        get() =
            // If we are busy then the index is being created
            // or it is being deleted. So for all practical purposes
            // it is not indexed.
            done && indexManager.isIndexed(book)

    @Throws(BookException::class)
    open fun deleteIndex() {
        if (done) {
            done = false
            book!!.addIndexStatusListener(isl)
            indexManager.deleteIndex(book)
            while (!done) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    // This is allowed
                }
            }
            book!!.removeIndexStatusListener(isl)
        }
    }

    @Throws(BookException::class)
    open fun createIndex() {
        if (done) {
            done = false
            book!!.addIndexStatusListener(isl)
            if (isIndexed) {
                deleteIndex()
            }
            val work = Thread(object : Runnable {
                override fun run() {
                    indexManager.scheduleIndexCreation(book)
                }
            })
            work.start()
            while (!done) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    // This is allowed
                }
            }
            book!!.removeIndexStatusListener(isl)
        }
    }

    protected open fun setDone(state: Boolean) {
        done = state
    }

    /* //todo static function: Demo of how clients can reindex, after a upgrade on a user computer. If reindexAll successful, update Installed.Index.DefaultVersion prop on the client computer

    //note: Need separate method for downloadLatestIndexForAllBooksIfNeeded()
    public static void reindexAllBooksIfNeeded() throws Exception {

        Books myBooks = Books.installed();

        for(Book insBook: myBooks.getBooks()) {
            //reindex if needsReindexing(insBook) true
            if(indexManager.needsReindexing(insBook)) {
                createIndex(insBook);
                //reindex & update Books Installed.Index.Version
                InstalledIndex.instance().storeLatestVersionAsInstalledIndexMetadata(insBook);
            }
            //manage all Installed.Index.Version property values in  metadata file
        }
        //set Installed.Index.DefaultVersion={Latest Version}
        PropertyMap map = new PropertyMap();
        map.put(InstalledIndex.INSTALLED_INDEX_DEFAULT_VERSION , IndexMetadata.instance().getLatestIndexVersionStr());
        InstalledIndex.instance().storeInstalledIndexMetadata(map);


    }
    */

    /**
     * Listen for the end of indexing.
     */
    class StatusListener(private var indexer: BookIndexer) : IndexStatusListener {
        override fun statusChanged(ev: IndexStatusEvent?) {
            val newStatus = ev!!.indexStatus
            if (IndexStatus.DONE == newStatus || IndexStatus.UNDONE == newStatus || IndexStatus.INVALID == newStatus) {
                indexer.setDone(true)
            }
        }
    }

    companion object {
        /**
         * Call with &lt;operation&gt; book. Where operation can be one of:
         * <ul>
         * <li>check - returns "TRUE" or "FALSE" indicating whether the index exists
         * or not</li>
         * <li>create - (re)create the index</li>
         * <li>delete - delete the index if it exists</li>
         * </ul>
         * And book is the initials of a book, e.g. KJV.
         * 
         * @param args
         */
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size != 2) {
                usage()
                return
            }

            System.err.println("BookIndexer " + args[0] + " " + args[1])

            val operation = args[0]
            val b = Books.installed().getBook(args[1])
            if (b == null) {
                System.err.println("Book not found")
                return
            }

            val indexer = BookIndexer(b)
            if ("create".equals(operation, ignoreCase = true)) {
                try {
                    indexer.createIndex()
                } catch (e: BookException) {
                    System.err.println("Unable to re-index book.")
                    e.printStackTrace()
                }
            } else if ("delete".equals(operation, ignoreCase = true)) {
                try {
                    indexer.deleteIndex()
                } catch (e: BookException) {
                    System.err.println("Unable to delete index for book.")
                    e.printStackTrace()
                }
            } else if ("check".equals(operation, ignoreCase = true)) {
                System.err.println(indexer.isIndexed)
            } else {
                usage()
            }
        }

        @JvmStatic
        fun usage() {
            System.err.println("Usage: BookIndexer operation book")
        }
    }
}
