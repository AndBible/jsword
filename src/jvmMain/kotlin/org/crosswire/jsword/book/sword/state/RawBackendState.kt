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
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.Reporter
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.SwordConstants
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.jsword.versification.Testament
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile

/**
 * Stores the random access files required for processing the passage request
 * 
 * The caller is required to close to correctly free resources and avoid File
 * pointer leaks
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class RawBackendState
/**
 * This is default package access for forcing the use of the
 * OpenFileStateManager to manage the creation. Not doing so may result in
 * new instances of OpenFileState being created for no reason, and as a
 * result, if they are released to the OpenFileStateManager by mistake this
 * would result in leakage.
 * 
 * @param bookMetaData
 * the appropriate metadata for the book
 */
@Throws(BookException::class)
internal constructor(bookMetaData: BookMetaData) : AbstractOpenFileState(bookMetaData) {
    @JvmField
    protected var otIdxRaf: RandomAccessFile? = null
    @JvmField
    protected var ntIdxRaf: RandomAccessFile? = null
    @JvmField
    protected var otTextRaf: RandomAccessFile? = null
    @JvmField
    protected var ntTextRaf: RandomAccessFile? = null
    @JvmField
    protected var ntIdxFile: File
    @JvmField
    protected var ntTextFile: File
    @JvmField
    protected var otIdxFile: File
    @JvmField
    protected var otTextFile: File

    init {
        val path = SwordUtil.getExpandedDataPath(bookMetaData)

        val otPath = NetUtil.lengthenURI(path, File.separator + SwordConstants.FILE_OT)!!
        otTextFile = File(otPath.path)
        otIdxFile = File(otPath.path + SwordConstants.EXTENSION_VSS)

        val ntPath = NetUtil.lengthenURI(path, File.separator + SwordConstants.FILE_NT)!!
        ntTextFile = File(ntPath.path)
        ntIdxFile = File(ntPath.path + SwordConstants.EXTENSION_VSS)

        // It is an error to be neither OT nor NT
        // Throwing exception, as if we can't read either the ot or nt file,
        // then we might as well give up
        if (!otTextFile.canRead() && !ntTextFile.canRead()) {
            val prob = BookException(JSOtherMsg.lookupText("Missing data files for old and new testaments in {0}.", path))
            Reporter.informUser(this, prob)
            throw prob
        }

        val fileMode = if (isWritable()) FileUtil.MODE_WRITE else FileUtil.MODE_READ

        if (otIdxFile.canRead()) {
            try {
                otIdxRaf = RandomAccessFile(otIdxFile, fileMode)
                otTextRaf = RandomAccessFile(otTextFile, fileMode)
            } catch (ex: FileNotFoundException) {
                //failed to open the files, so close them now
                IOUtil.close(otIdxRaf)
                IOUtil.close(otTextRaf)

                jswordAssert(false) { ex }

                LOGGER.error("Could not open OT", ex)
                ntIdxRaf = null
                ntTextRaf = null
            }
        }

        if (ntIdxFile.canRead()) {
            try {
                ntIdxRaf = RandomAccessFile(ntIdxFile, fileMode)
                ntTextRaf = RandomAccessFile(ntTextFile, fileMode)
            } catch (ex: FileNotFoundException) {
                //failed to open the files, so close them now
                IOUtil.close(ntIdxRaf)
                IOUtil.close(ntTextRaf)

                jswordAssert(false) { ex }
                LOGGER.error("Could not open NT", ex)
                ntIdxRaf = null
                ntTextRaf = null
            }
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#isWritable()
     */
    open fun isWritable(): Boolean {
        // For the module to be writable either the old testament or the new
        // testament needs to be present
        // (i.e. readable) and both the index and the data files need to be
        // writable
        if (otIdxFile.canRead() && (otIdxFile.canWrite() || !otTextFile.canWrite())) {
            return false
        }
        if (ntIdxFile.canRead() && (ntIdxFile.canWrite() || !ntTextFile.canWrite())) {
            return false
        }
        return otIdxFile.canRead() || ntIdxFile.canRead()
    }

    override fun releaseResources() {
        IOUtil.close(ntIdxRaf)
        IOUtil.close(ntTextRaf)
        IOUtil.close(otIdxRaf)
        IOUtil.close(otTextRaf)
        ntIdxRaf = null
        ntTextRaf = null
        otIdxRaf = null
        otTextRaf = null
    }

    /**
     * Get the index file for the given testament.
     * 
     * @param testament the testament for the file
     * @return the requested file for the testament
     */
    open fun getIdxRaf(testament: Testament?): RandomAccessFile? {
        return if (testament == Testament.NEW) ntIdxRaf else otIdxRaf
    }

    /**
     * Get the text file for the given testament.
     * 
     * @param testament the testament for the file
     * @return the requested file for the testament
     */
    open fun getTextRaf(testament: Testament?): RandomAccessFile? {
        return if (testament == Testament.NEW) ntTextRaf else otTextRaf
    }

    /**
     * @return the otTextRaf
     */
    open fun getOtTextRaf(): RandomAccessFile? {
        return otTextRaf
    }

    /**
     * @return the ntTextRaf
     */
    open fun getNtTextRaf(): RandomAccessFile? {
        return ntTextRaf
    }

    /**
     * Get the text file for the given testament.
     * 
     * @param testament the testament for the file
     * @return the requested file for the testament
     */
    open fun getTextFile(testament: Testament?): File {
        return if (testament == Testament.NEW) ntTextFile else otTextFile
    }

    /**
     * Get the index file for the given testament.
     * 
     * @param testament the testament for the file
     * @return the requested file for the testament
     */
    open fun getIdxFile(testament: Testament?): File {
        return if (testament == Testament.NEW) ntIdxFile else otIdxFile
    }

    private companion object {
        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(RawBackendState::class.java)
    }
}
