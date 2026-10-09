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
 * © CrossWire Bible Society, 2013 - 2016
 *
 */
package org.crosswire.jsword.book.sword.state

import org.crosswire.common.util.FileUtil
import org.crosswire.common.util.IOUtil
import org.crosswire.common.util.Reporter
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.RawLDBackend
import org.crosswire.jsword.book.sword.SwordConstants
import org.crosswire.jsword.book.sword.SwordUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URI

/**
 * State for [RawLDBackend]
 * 
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class RawLDBackendState : AbstractOpenFileState {
    /**
     * The number of entries in the book.
     */
    open var size: Int = -1

    /**
     * The index file
     */
    private var idxFile: File? = null

    /**
     * The index random access file
     */
    private var idxRaf: RandomAccessFile? = null

    /**
     * The data file
     */
    private var datFile: File? = null

    /**
     * The data random access file
     */
    private var datRaf: RandomAccessFile? = null

    /**
     * This is default package access for forcing the use of the
     * OpenFileStateManager to manage the creation. Not doing so may result in
     * new instances of OpenFileState being created for no reason, and as a
     * result, if they are released to the OpenFileStateManager by mistake this
     * would result in leakage
     * 
     * @param bookMetaData the appropriate metadata for the book
     */
    @Throws(BookException::class)
    internal constructor(bookMetaData: BookMetaData) : super(bookMetaData) {
        val path: URI
        try {
            path = SwordUtil.getExpandedDataPath(bookMetaData)
        } catch (e: BookException) {
            Reporter.informUser(this, e)
            throw e
        }

        try {
            val idx = File(path.path + SwordConstants.EXTENSION_INDEX)
            idxFile = idx
            val dat = File(path.path + SwordConstants.EXTENSION_DATA)
            datFile = dat

            if (!idx.canRead()) {
                // TRANSLATOR: Common error condition: The file could not be
                // read. There can be many reasons.
                // {0} is a placeholder for the file.
                Reporter.informUser(this, BookException(JSMsg.gettext("Error reading {0}", idx.absolutePath)))

                // As per previous behaviour, we continue with state half made, having informed the user of the error
                return
            }

            if (!dat.canRead()) {
                // Throwing exception, as if we can't read our data file, then we might as well give up

                // TRANSLATOR: Common error condition: The file could not be
                // read. There can be many reasons.
                // {0} is a placeholder for the file.
                val prob = BookException(JSMsg.gettext("Error reading {0}", dat.absolutePath))
                Reporter.informUser(this, prob)
                throw prob
            }

            // Open the files
            idxRaf = RandomAccessFile(idx, FileUtil.MODE_READ)
            datRaf = RandomAccessFile(dat, FileUtil.MODE_READ)
        } catch (ex: IOException) {
            //failed to open the files, so close them now
            IOUtil.close(idxRaf)
            IOUtil.close(datRaf)

            LOGGER.error("failed to open files", ex)
            idxRaf = null
            datRaf = null
            // TRANSLATOR: Common error condition: The file could not be read.
            // There can be many reasons.
            // {0} is a placeholder for the file.
            throw BookException(JSMsg.gettext("Error reading {0}", datFile!!.absolutePath), ex)
        }
    }

    override fun releaseResources() {
        size = -1
        IOUtil.close(idxRaf)
        IOUtil.close(datRaf)
        idxRaf = null
        datRaf = null
    }

    /**
     * @return the idxFile
     */
    open fun getIdxFile(): File? {
        return idxFile
    }

    /**
     * @return the idxRaf
     */
    open fun getIdxRaf(): RandomAccessFile? {
        return idxRaf
    }

    /**
     * @return the datRaf
     */
    open fun getDatRaf(): RandomAccessFile? {
        return datRaf
    }

    private companion object {
        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(RawLDBackendState::class.java)
    }
}
