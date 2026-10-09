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
package org.crosswire.jsword.book.sword

import java.io.File
import java.io.IOException
import java.net.URI
import java.util.HashSet

import org.crosswire.common.util.FileUtil
import org.crosswire.common.util.NetUtil
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.basic.AbstractBookDriver
import org.crosswire.jsword.index.IndexManagerFactory
import org.crosswire.jsword.index.IndexStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This represents all of the Sword Books (aka modules).
 *
 * @author Joe Walker
 * @author DM Smith
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.<br></br>
 * The copyright to this program is held by its authors.
 */
open class SwordBookDriver
/**
 * Some basic name initialization
 */
    : AbstractBookDriver() {
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookDriver#getDriverName()
     */
    override fun getDriverName(): String {
        return "Sword"
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookProvider#getBooks()
     */
    override fun getBooks(): Array<Book> {
        val dirs = SwordBookPath.getSwordPath()
        //initial size based on Guava's  newHashMapWithExpectedSize method:
        //http://docs.guava-libraries.googlecode.com/git/javadoc/src-html/com/google/common/collect/Maps.html#line.201
        val valid: MutableSet<Book> = HashSet(dirs.size + dirs.size / 3)
        for (j in dirs.indices) {
            getBooks(valid, dirs[j])
        }
        return valid.toTypedArray()
    }

    private fun getBooks(valid: MutableSet<Book>, bookDir: File) {
        val mods = File(bookDir, SwordConstants.DIR_CONF)
        if (!(mods.isDirectory && mods.canRead())) {
            LOGGER.debug("mods.d directory at {} does not exist or can't be read", mods)
            return
        }

        val bookConfs = SwordBookPath.getBookList(mods)

        // Loop through the entries in this mods.d directory
        val bookDirURI = NetUtil.getURI(bookDir)
        for (i in bookConfs!!.indices) {
            val bookConf = bookConfs[i]
            try {
                var sbmd: SwordBookMetaData? = null

                val configfile = File(mods, bookConf)
                if (configfile.exists()) {
                    // First time here chain is null, indicating that we are at the master BookMetaData
                    sbmd = SwordBookMetaData(configfile, bookDirURI)
                }

                if (sbmd == null) {
                    LOGGER.error("The book's configuration files is not supported.")
                    continue
                }

                // skip any book that is not supported.
                if (!sbmd.isSupported) {
                    LOGGER.error("The book's configuration files is not supported. -> Initials [{}], Driver=[{}], Versification=[{}], Book type=[{}], Book category=[{}]",
                            sbmd.internalName, sbmd.driver, sbmd.getProperty(BookMetaData.KEY_VERSIFICATION), sbmd.bookType, sbmd.bookCategory)
                    continue
                }

                sbmd.setDriver(this)

                // Only take the first "installation" of the Book
                val book = createBook(sbmd)
                if (!valid.contains(book)) {
                    valid.add(book)

                    val imanager = IndexManagerFactory.getIndexManager()
                    if (imanager.isIndexed(book)) {
                        sbmd.indexStatus = IndexStatus.DONE
                    } else {
                        sbmd.indexStatus = IndexStatus.UNDONE
                    }
                }
            } catch (e: IOException) {
                LOGGER.warn("Couldn't create SwordBookMetaData", e)
            } catch (e: BookException) {
                LOGGER.warn("Couldn't create SwordBookMetaData", e)
            }
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookDriver#isDeletable(org.crosswire.jsword.book.Book)
     */
    override fun isDeletable(dead: Book): Boolean {
        val sbmd = dead.bookMetaData as SwordBookMetaData
        val confFile = sbmd.configFile
        // We can only uninstall what we download into our download dir.
        return confFile != null && confFile.exists()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookDriver#delete(org.crosswire.jsword.book.Book)
     */
    @Throws(BookException::class)
    override fun delete(dead: Book) {
        val sbmd = dead.bookMetaData as SwordBookMetaData
        val confFile = sbmd.configFile

        // We can only uninstall what we download into our download dir.
        if (confFile == null || !confFile.exists()) {
            // TRANSLATOR: Common error condition: The file could not be deleted. There can be many reasons.
            // {0} is a placeholder for the file.
            throw BookException(JSMsg.gettext("Unable to delete: {0}", confFile))
        }

        // Delete all conf files
        var failures = FileUtil.delete(confFile)
        if (failures.isEmpty()) {
            val loc = sbmd.location
            if (loc != null) {
                val bookDir = File(loc.path)
                failures = FileUtil.delete(bookDir)
                Books.installed().removeBook(dead)
            }

        }

        // TODO(DM): list all that failed
        if (!failures.isEmpty()) {
            // TRANSLATOR: Common error condition: The file could not be deleted. There can be many reasons.
            // {0} is a placeholder for the file.
            throw BookException(JSMsg.gettext("Unable to delete: {0}", failures.get(0)))
        }
    }

    /**
     * Create a Book appropriate for the BookMetaData
     */
    @Throws(BookException::class)
    private fun createBook(sbmd: SwordBookMetaData): Book {
        val modtype = sbmd.bookType
        @Suppress("SENSELESS_COMPARISON")
        if (modtype == null || modtype.bookCategory == null) {
            // FIXME(DMS): missing parameter
            throw BookException(JSOtherMsg.lookupText("Unsupported type: {0} when reading {1}"))
        }

        return modtype.createBook(sbmd)
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
         * A helper class for the SwordInstaller to tell us that it has copied a new
         * Book into our install directory
         *
         * @param sbmd
         *            The SwordBookMetaData object for the new Book
         * @throws BookException
         */
        @JvmStatic
        @Throws(BookException::class)
        fun registerNewBook(sbmd: SwordBookMetaData) {
            // The sbmd was created from loading a conf from zip or tar.gz.
            // It is not the conf for an installed book.
            // So we have to get the bmd for the conf of the installed book.
            var bmd: SwordBookMetaData? = null
            // Before this was called the library was set so we can know where the conf and module are.
            val bookDirURI: URI = sbmd.library!!
            // Get a File representing the mods.d folder.
            val mods = File(bookDirURI.path, SwordConstants.DIR_CONF)
            // Get the name of the conf from the sbmd. Don't guess even though by convention it is guessable.
            val bookConf = sbmd.bookConf
            // Get the config File.
            val configfile = File(mods, bookConf)
            // Verify that it exists
            if (configfile.exists()) {
                try {
                    // Now we can create the new bmd
                    bmd = SwordBookMetaData(configfile, bookDirURI)
                } catch (e: IOException) {
                    LOGGER.warn("Couldn't create SwordBookMetaData", e)
                }
            }

            if (bmd == null) {
                LOGGER.error("The book's configuration files is not supported.")
                return
            }

            // skip any book that is not supported.
            if (!bmd.isSupported) {
                LOGGER.error("The book's configuration files is not supported. -> Initials [{}], Driver=[{}], Versification=[{}], Book type=[{}], Book category=[{}]",
                        bmd.internalName, bmd.driver, bmd.getProperty(BookMetaData.KEY_VERSIFICATION), bmd.bookType, bmd.bookCategory)
                return
            }

            val d = sbmd.driver as SwordBookDriver?
            bmd.setDriver(d)
            val book = d!!.createBook(bmd)
            Books.installed().addBook(book)
        }

        /**
         * A shared instance of this driver.
         */
        private val INSTANCE: BookDriver = SwordBookDriver()

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(SwordBookDriver::class.java)
    }
}
