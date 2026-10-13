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

import org.crosswire.common.util.Reporter
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.RawLDBackend
import org.crosswire.jsword.book.sword.SwordConstants
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import org.crosswire.jsword.book.sword.*
import okio.IOException
import org.crosswire.common.util.RandomAccessSource
import org.crosswire.common.util.KUri

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
    internal var idxFile: StorageLocation? = null

    /**
     * The index random access file
     */
    private var idxRaf: RandomAccessSource? = null

    /**
     * The data file
     */
    private var datFile: StorageLocation? = null

    /**
     * The data random access file
     */
    private var datRaf: RandomAccessSource? = null

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
        val path: KUri
        try {
            path = SwordUtil.getExpandedDataPath(bookMetaData)
        } catch (e: BookException) {
            Reporter.informUser(this, e)
            throw e
        }

        try {
            val idx = StorageLocation(path.getPath() + SwordConstants.EXTENSION_INDEX)
            idxFile = idx
            val dat = StorageLocation(path.getPath() + SwordConstants.EXTENSION_DATA)
            datFile = dat

            if (!storageCanRead(idx)) {
                // TRANSLATOR: Common error condition: The file could not be
                // read. There can be many reasons.
                // {0} is a placeholder for the file.
                Reporter.informUser(this, BookException(JSMsg.gettext("Error reading {0}", storageAbsolute(idx))))

                // As per previous behaviour, we continue with state half made, having informed the user of the error
                return
            }

            if (!storageCanRead(dat)) {
                // Throwing exception, as if we can't read our data file, then we might as well give up

                // TRANSLATOR: Common error condition: The file could not be
                // read. There can be many reasons.
                // {0} is a placeholder for the file.
                val prob = BookException(JSMsg.gettext("Error reading {0}", storageAbsolute(dat)))
                Reporter.informUser(this, prob)
                throw prob
            }

            // Open the files
            idxRaf = storageOpenRandomAccess(idx, false)
            datRaf = storageOpenRandomAccess(dat, false)
        } catch (ex: IOException) {
            //failed to open the files, so close them now
            closeStateSource(idxRaf)
            closeStateSource(datRaf)

            LOGGER.error("failed to open files", ex)
            idxRaf = null
            datRaf = null
            // TRANSLATOR: Common error condition: The file could not be read.
            // There can be many reasons.
            // {0} is a placeholder for the file.
            throw BookException(JSMsg.gettext("Error reading {0}", storageAbsolute(datFile!!)), ex)
        }
    }

    override fun releaseResources() {
        markResourcesClosed()
        size = -1
        closeStateSource(idxRaf)
        closeStateSource(datRaf)
        idxRaf = null
        datRaf = null
    }

    /**
     * @return the idxFile
     */
    internal fun getIdxLocation(): StorageLocation? {
        return idxFile
    }

    /**
     * @return the idxRaf
     */
    open fun getIdxRaf(): RandomAccessSource? {
        return idxRaf
    }

    /**
     * @return the datRaf
     */
    open fun getDatRaf(): RandomAccessSource? {
        return datRaf
    }

    private companion object {
        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(RawLDBackendState::class)
    }
}
