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

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.crosswire.jsword.book.sword.state.RawBackendState
import org.crosswire.jsword.passage.BitwisePassage
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.RocketPassage
import org.crosswire.jsword.versification.Testament
import org.crosswire.jsword.versification.system.Versifications
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import java.io.IOException
import org.crosswire.common.util.RandomAccessSource

/**
 * Both Books and Commentaries seem to use the same format so this class
 * abstracts out the similarities.
 * 
 * @param <T> The type of the RawBackendState that this class extends.
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class RawBackend<T : RawBackendState>
/**
 * Simple ctor
 * 
 * @param sbmd 
 * @param datasize 
 */
(sbmd: SwordBookMetaData?, datasize: Int) : AbstractBackend<RawBackendState>(sbmd) {

    /**
     * How many bytes in the size count in the index
     */
    @JvmField
    protected val datasize: Int = datasize

    /**
     * The number of bytes for each entry in the index: either 6 or 8
     */
    @JvmField
    protected val entrysize: Int = OFFSETSIZE + datasize

    init {
        jswordAssert(datasize == 2 || datasize == 4) { datasize }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return getRawTextLength(key) > 0
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#size(org.crosswire.jsword.passage.Key)
     */
    override fun getRawTextLength(key: Key?): Int {
        val v11nName = bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION)
        val v11n = Versifications.instance().getVersification(v11nName)!!
        val verse = KeyUtil.getVerse(key)

        var initState: RawBackendState? = null
        try {
            var index = verse.ordinal
            val testament = v11n.getTestament(index)
            index = v11n.getTestamentOrdinal(index)
            initState = initState()
            val idxRaf = initState.getIdxRaf(testament)

            // If this is a single testament Bible, return nothing.
            if (idxRaf == null) {
                return 0
            }

            val dataIndex = getIndex(idxRaf, index.toLong())

            return dataIndex.getSize()
        } catch (ex: IOException) {
            return 0
        } catch (e: BookException) {
            return 0
        } finally {
            OpenFileStateManager.instance().release(initState)
        }
    }

    @Throws(BookException::class)
    override fun getGlobalKeyList(): Key {
        var rafBook: RawBackendState? = null
        try {
            rafBook = initState()

            val v11nName = bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION)
            val v11n = Versifications.instance().getVersification(v11nName)!!

            val testaments = arrayOf(
                Testament.OLD, Testament.NEW
            )

            val passage: BitwisePassage = RocketPassage(v11n)
            passage.raiseEventSuppresion()
            passage.raiseNormalizeProtection()

            for (currentTestament in testaments) {
                val idxRaf = rafBook.getIdxRaf(currentTestament)

                // If Bible does not contain the desired testament, then false
                if (idxRaf == null) {
                    // no keys in this testament
                    continue
                }

                // The first index in each testament is ignored.
                // We need to include it here in order to read in the entire index.
                val maxCount = v11n.getCount(currentTestament) + 1

                // Read in the whole index, a few hundred Kb at most.
                val temp = SwordUtil.readRAF(idxRaf, 0, entrysize * maxCount)

                // For each entry of entrysize bytes, the length of the verse in bytes
                // is in the last datasize bytes. If all bytes are 0, then there is no content.
                if (datasize == 2) {
                    var ii = 0
                    while (ii < temp.size) {
                        // This can be simplified to temp[ii + 4] == 0 && temp[ii + 5] == 0.
                        // int verseSize = SwordUtil.decodeLittleEndian16(temp, ii + 4);
                        // if (verseSize > 0) {
                        if (temp[ii + 4].toInt() != 0 || temp[ii + 5].toInt() != 0) {
                            val ordinal = ii / entrysize
                            passage.addVersifiedOrdinal(v11n.getOrdinal(currentTestament, ordinal))
                        }
                        ii += entrysize
                    }
                } else { // datasize == 4
                    var ii = 0
                    while (ii < temp.size) {
                        // This can be simplified to temp[ii + 4] == 0 && temp[ii + 5] == 0 && temp[ii + 6] == 0 && temp[ii + 7] == 0.
                        // int verseSize = SwordUtil.decodeLittleEndian32(temp, ii + 4);
                        // if (verseSize > 0) {
                        if (temp[ii + 4].toInt() != 0 || temp[ii + 5].toInt() != 0 || temp[ii + 6].toInt() != 0 || temp[ii + 7].toInt() != 0) {
                            val ordinal = ii / entrysize
                            passage.addVersifiedOrdinal(v11n.getOrdinal(currentTestament, ordinal))
                        }
                        ii += entrysize
                    }
                }
            }

            passage.lowerNormalizeProtection()
            passage.lowerEventSuppressionAndTest()

            return passage
        } catch (e: IOException) {
            throw BookException(JSMsg.gettext("Unable to read key list from book."))
        } finally {
            OpenFileStateManager.instance().release(rafBook)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(BookException::class)
    override fun initState(): T {
        return OpenFileStateManager.instance().getRawBackendState(bookMetaData) as T
    }

    @Throws(IOException::class)
    open fun getRawText(state: RawBackendState, key: Key?): String {
        return readRawContent(state, key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(IOException::class)
    override fun readRawContent(state: RawBackendState, key: Key?): String {
        val v11nName = bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION)
        val v11n = Versifications.instance().getVersification(v11nName)!!
        val verse = KeyUtil.getVerse(key)

        var index = verse.ordinal

        val testament = v11n.getTestament(index)
        index = v11n.getTestamentOrdinal(index)
        var initState: RawBackendState? = null
        try {
            initState = initState()
            return getEntry(state, verse.getName(), testament, index.toLong())
        } catch (e: BookException) {
            return ""
        } finally {
            OpenFileStateManager.instance().release(initState)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#setRawText(org.crosswire.jsword.passage.Key, java.lang.String)
     */
    @Throws(BookException::class, IOException::class)
    override fun setRawText(state: RawBackendState, key: Key?, text: String?) {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#isWritable()
     */
    override val isWritable: Boolean
        get() {
            var rawBackendState: RawBackendState? = null
            try {
                rawBackendState = initState()
                return rawBackendState.isWritable()
            } catch (e: BookException) {
                return false
            } finally {
                OpenFileStateManager.instance().release(rawBackendState)
            }
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    @Throws(IOException::class)
    override fun setAliasKey(state: RawBackendState, alias: Key?, source: Key?) {
        throw UnsupportedOperationException()
    }

    /**
     * Get the Index (that is offset and size) for an entry.
     * 
     * @param entry
     * @return the index for the entry
     * @throws IOException
     */
    @Throws(IOException::class)
    protected open fun getIndex(raf: RandomAccessSource?, entry: Long): DataIndex {
        // Read the offset and size for this key from the index
        val buffer = SwordUtil.readRAF(raf, entry * entrysize, entrysize)
        if (buffer.size == 0) {
            return DataIndex(0, 0)
        }

        val entryOffset = SwordUtil.decodeLittleEndian32(buffer, 0)
        var entrySize = -1
        when (datasize) {
            2 -> entrySize = SwordUtil.decodeLittleEndian16(buffer, 4)
            4 -> entrySize = SwordUtil.decodeLittleEndian32(buffer, 4)
            else -> jswordAssert(false) { datasize }
        }
        return DataIndex(entryOffset, entrySize)
    }

    /**
     * Get the text for an indexed entry in the book.
     * @param state 
     * 
     * @param index
     * the entry to get
     * @param name
     * name of the entry
     * @param testament
     * the testament for the entry
     * @return the text for the entry.
     * @throws IOException
     * on a IO problem
     */
    @Throws(IOException::class)
    protected open fun getEntry(state: RawBackendState, name: String?, testament: Testament?, index: Long): String {
        val idxRaf: RandomAccessSource?
        val txtRaf: RandomAccessSource?
        idxRaf = state.getIdxRaf(testament)
        txtRaf = state.getTextRaf(testament)

        // It may be that this is a single testament Bible
        if (idxRaf == null) {
            return ""
        }

        val dataIndex = getIndex(idxRaf, index)

        val size = dataIndex.getSize()
        if (size == 0) {
            return ""
        }

        if (size < 0) {
            LOGGER.error("In {}: Verse {} has a bad index size of {}", bookMetaData.initials, name, size.toString())
            return ""
        }

        val data = SwordUtil.readRAF(txtRaf, dataIndex.getOffset().toLong(), size)

        decipher(data)

        return SwordUtil.decode(name, data, bookMetaData.bookCharset)
    }

    companion object {
        /**
         * How many bytes in the offset pointers in the index
         */
        protected const val OFFSETSIZE: Int = 4

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(RawBackend::class)
    }
}
