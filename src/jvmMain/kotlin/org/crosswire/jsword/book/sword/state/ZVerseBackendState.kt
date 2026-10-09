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
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.BlockType
import org.crosswire.jsword.book.sword.SwordConstants
import org.crosswire.jsword.book.sword.SwordUtil
import org.crosswire.jsword.versification.Testament
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile

/**
 * Stores the random access files required for processing the passage request.
 * 
 * The caller is required to close to correctly free resources and avoid File
 * pointer leaks.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class ZVerseBackendState
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
internal constructor(bookMetaData: BookMetaData, blockType: BlockType) : AbstractOpenFileState(bookMetaData) {
    /**
     * The compressed random access files
     */
    private var otCompRaf: RandomAccessFile? = null
    private var ntCompRaf: RandomAccessFile? = null

    /**
     * The data random access files
     */
    private var otTextRaf: RandomAccessFile? = null
    private var ntTextRaf: RandomAccessFile? = null

    /**
     * The index random access files
     */
    private var otIdxRaf: RandomAccessFile? = null
    private var ntIdxRaf: RandomAccessFile? = null

    /**
     * @return the lastTestament
     */
    open var lastTestament: Testament? = null

    /**
     * @return the lastBlockNum
     */
    open var lastBlockNum: Long = -1

    /**
     * @return the lastUncompressed
     */
    open var lastUncompressed: ByteArray? = null

    init {
        val path = SwordUtil.getExpandedDataPath(bookMetaData)
        val otAllButLast = NetUtil.lengthenURI(path, File.separator + SwordConstants.FILE_OT + '.' + blockType.indicator + SUFFIX_PART1)!!.path
        val otIdxFile = File(otAllButLast + SUFFIX_INDEX)
        val otTextFile = File(otAllButLast + SUFFIX_TEXT)
        val otCompFile = File(otAllButLast + SUFFIX_COMP)

        val ntAllButLast = NetUtil.lengthenURI(path, File.separator + SwordConstants.FILE_NT + '.' + blockType.indicator + SUFFIX_PART1)!!.path
        val ntIdxFile = File(ntAllButLast + SUFFIX_INDEX)
        val ntTextFile = File(ntAllButLast + SUFFIX_TEXT)
        val ntCompFile = File(ntAllButLast + SUFFIX_COMP)

        // check whether exists to swallow any exception as before
        if (otIdxFile.canRead()) {
            try {
                otCompRaf = RandomAccessFile(otIdxFile, FileUtil.MODE_READ)
                otTextRaf = RandomAccessFile(otTextFile, FileUtil.MODE_READ)
                otIdxRaf = RandomAccessFile(otCompFile, FileUtil.MODE_READ)
            } catch (ex: FileNotFoundException) {
                //failed to open the files, so close them now
                IOUtil.close(otCompRaf)
                IOUtil.close(otTextRaf)
                IOUtil.close(otIdxRaf)

                jswordAssert(false) { ex }
                LOGGER.error("Could not open OT", ex)
            }
        }

        // why do swallow the exception and log. Can Books have one testament
        // without the other.
        if (ntIdxFile.canRead()) {
            try {
                ntCompRaf = RandomAccessFile(ntIdxFile, FileUtil.MODE_READ)
                ntTextRaf = RandomAccessFile(ntTextFile, FileUtil.MODE_READ)
                ntIdxRaf = RandomAccessFile(ntCompFile, FileUtil.MODE_READ)
            } catch (ex: FileNotFoundException) {
                //failed to open the files, so close them now
                IOUtil.close(ntCompRaf)
                IOUtil.close(ntTextRaf)
                IOUtil.close(ntIdxRaf)

                jswordAssert(false) { ex }
                LOGGER.error("Could not open OT", ex)
            }
        }
    }

    override fun releaseResources() {
        IOUtil.close(ntCompRaf)
        IOUtil.close(ntTextRaf)
        IOUtil.close(ntIdxRaf)
        IOUtil.close(otCompRaf)
        IOUtil.close(otTextRaf)
        IOUtil.close(otIdxRaf)
        ntCompRaf = null
        ntTextRaf = null
        ntIdxRaf = null
        otCompRaf = null
        otTextRaf = null
        otIdxRaf = null
    }

    /**
     * Get the compression file for the given testament.
     * 
     * @param testament the testament for the index
     * @return the index for the testament
     */
    open fun getCompRaf(testament: Testament?): RandomAccessFile? {
        return if (testament == Testament.NEW) ntCompRaf else otCompRaf
    }

    /**
     * Get the text file for the given testament.
     * 
     * @param testament the testament for the index
     * @return the index for the testament
     */
    open fun getTextRaf(testament: Testament?): RandomAccessFile? {
        return if (testament == Testament.NEW) ntTextRaf else otTextRaf
    }

    /**
     * Get the index file for the given testament.
     * 
     * @param testament the testament for the index
     * @return the index for the testament
     */
    open fun getIdxRaf(testament: Testament?): RandomAccessFile? {
        return if (testament == Testament.NEW) ntIdxRaf else otIdxRaf
    }

    private companion object {
        private const val SUFFIX_COMP = "v"
        private const val SUFFIX_INDEX = "s"
        private const val SUFFIX_PART1 = "z"
        private const val SUFFIX_TEXT = "z"

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(ZVerseBackendState::class.java)
    }
}
