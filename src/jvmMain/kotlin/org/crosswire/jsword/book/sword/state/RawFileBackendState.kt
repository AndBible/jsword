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

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.common.util.Logger
import org.crosswire.common.util.RandomAccessSource
import org.crosswire.common.util.LoggerFactory
import org.crosswire.jsword.book.sword.*
import okio.FileNotFoundException
import okio.IOException

/**
 * Stores the random access files required for processing the passage request
 * 
 * The caller is required to close to correctly free resources and avoid File
 * pointer leaks
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class RawFileBackendState
/**
 * This is default package access for forcing the use of the
 * OpenFileStateManager to manage the creation. Not doing so may result in
 * new instances of OpenFileState being created for no reason, and as a
 * result, if they are released to the OpenFileStateManager by mistake this
 * would result in leakage
 * 
 * @param bookMetaData
 * the appropriate metadata for the book
 */
@Throws(BookException::class)
internal constructor(bookMetaData: BookMetaData) : RawBackendState(bookMetaData) {
    private var incfile: StorageLocation? = null
    private var incfileValue = 0

    init {
        incfileValue = -1
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#isWritable()
     */
    override fun isWritable(): Boolean {
        val incFile = getIncLocation()

        if (existsAndCanReadAndWrite(otTextFile)
            && existsAndCanReadAndWrite(ntTextFile)
            && existsAndCanReadAndWrite(otIdxFile)
            && existsAndCanReadAndWrite(ntIdxFile)
            && (incFile == null || existsAndCanReadAndWrite(incFile))
        ) {
            return true
        }
        return false
    }

    /**
     * Returns true if the file exists, can be read and can be written to.
     *
     * @param file the file
     * @return true, if successful
     */
    private fun existsAndCanReadAndWrite(file: StorageLocation): Boolean {
        return storageExists(file) && storageCanRead(file) && storageCanWrite(file)
    }

    @Throws(IOException::class)
    private fun readIncfile(): Int {
        var ret = -1

        if (incfile == null) {
            // then attempt to initialise it
            initIncFile()
        }

        if (this.incfile != null) {
            var fis: RandomAccessSource? = null
            try {
                fis = storageOpenRandomAccess(this.incfile!!, false)
                val buffer = fis.readNext(4)
                if (buffer.size != 4) {
                    LOGGER.error("Read data is not of appropriate size of 4 bytes!")
                    throw IOException("Incfile is not 4 bytes long")
                }
                ret = SwordUtil.decodeLittleEndian32(buffer, 0)

                // also store this
                this.incfileValue = ret
            } catch (e: FileNotFoundException) {
                LOGGER.error("Error on writing to incfile, file should exist already!: {}", e.message, e)
            } finally {
                closeStateSource(fis)
            }
        }

        return ret
    }

    private fun initIncFile() {
        try {
            val tempIncfile = StorageLocation(SwordUtil.getExpandedDataPath(getBookMetaData()).getPath() + storageSeparator() + INCFILE)
            if (storageExists(tempIncfile)) {
                this.incfile = tempIncfile
            }
        } catch (e: BookException) {
            LOGGER.error("Error on checking incfile: {}", e.message, e)
            this.incfile = null
        }
    }

    /**
     * @return the incfileValue
     */
    open fun getIncfileValue(): Int {
        if (incfileValue == -1) {
            try {
                readIncfile()
            } catch (e: IOException) {
                LOGGER.error("IO Error: {}", e.message, e)
            }
        }

        return incfileValue
    }

    open fun setIncfileValue(incValue: Int) {
        this.incfileValue = incValue
    }

    /**
     * @return the incfile
     */
    internal fun getIncLocation(): StorageLocation? {
        if (incfile == null) {
            initIncFile()
        }
        return incfile
    }

    /**
     * @param incfile
     * the incfile to set
     */
    internal fun setIncLocation(incfile: StorageLocation?) {
        this.incfile = incfile
    }

    companion object {
        const val INCFILE: String = "incfile"

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(RawFileBackendState::class)
    }
}
