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
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import org.crosswire.jsword.book.sword.*
import okio.IOException
import org.crosswire.common.util.RandomAccessSource
import org.crosswire.common.util.KUri

/**
 * Stores the random access files required for processing the passage request.
 * 
 * The caller is required to close to correctly free resources and avoid File
 * pointer leaks.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class ZLDBackendState : RawLDBackendState {
    /**
     * The compressed index.
     */
    private var zdxFile: StorageLocation? = null

    /**
     * The compressed index random access file.
     */
    private var zdxRaf: RandomAccessSource? = null

    /**
     * The compressed text.
     */
    private var zdtFile: StorageLocation? = null

    /**
     * The compressed text random access file.
     */
    private var zdtRaf: RandomAccessSource? = null

    /**
     * The index of the block that is cached.
     */
    open var lastBlockNum: Long = 0

    /**
     * The cache for a read of a compressed block.
     */
    open var lastUncompressed: ByteArray? = null

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
        zdxFile = null
        zdtFile = null
        zdxRaf = null
        zdtRaf = null
        lastBlockNum = -1
        lastUncompressed = EMPTY_BYTES

        val path: KUri
        try {
            path = SwordUtil.getExpandedDataPath(bookMetaData)
        } catch (e: BookException) {
            Reporter.informUser(this, e)
            return
        }

        try {
            val zdx = StorageLocation(path.getPath() + EXTENSION_Z_INDEX)
            zdxFile = zdx
            val zdt = StorageLocation(path.getPath() + EXTENSION_Z_DATA)
            zdtFile = zdt

            if (!storageCanRead(zdx)) {
                // TRANSLATOR: Common error condition: The file could not be read. There can be many reasons.
                // {0} is a placeholder for the file.
                Reporter.informUser(this, BookException(JSMsg.gettext("Error reading {0}", storageAbsolute(zdt))))
                return
            }

            if (!storageCanRead(zdt)) {
                // TRANSLATOR: Common error condition: The file could not be read. There can be many reasons.
                // {0} is a placeholder for the file.
                Reporter.informUser(this, BookException(JSMsg.gettext("Error reading {0}", storageAbsolute(zdt))))
                return
            }

            // Open the files
            zdxRaf = storageOpenRandomAccess(zdx, false)
            zdtRaf = storageOpenRandomAccess(zdt, false)
        } catch (ex: IOException) {
            //failed to open the files, so close them now
            closeStateSource(zdxRaf)
            closeStateSource(zdtRaf)

            LOGGER.error("failed to open files", ex)
            zdxRaf = null
            zdtRaf = null
            return
        }
    }

    override fun releaseResources() {
        super.releaseResources()
        lastBlockNum = -1
        lastUncompressed = EMPTY_BYTES

        closeStateSource(zdxRaf)
        closeStateSource(zdtRaf)
        zdxRaf = null
        zdtRaf = null
    }

    /**
     * @return the zdxRaf
     */
    open fun getZdxRaf(): RandomAccessSource? {
        return zdxRaf
    }

    /**
     * @return the zdtRaf
     */
    open fun getZdtRaf(): RandomAccessSource? {
        return zdtRaf
    }

    private companion object {
        private val EMPTY_BYTES = ByteArray(0)
        private const val EXTENSION_Z_INDEX = ".zdx"
        private const val EXTENSION_Z_DATA = ".zdt"

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(ZLDBackendState::class)
    }
}
