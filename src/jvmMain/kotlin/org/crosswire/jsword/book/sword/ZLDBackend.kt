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

import org.crosswire.common.compress.CompressorType
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.crosswire.jsword.book.sword.state.RawLDBackendState
import org.crosswire.jsword.book.sword.state.ZLDBackendState
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.ObjectInputStream

/**
 * An extension of RawLDBackend to read Z format files.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class ZLDBackend
/**
 * Simple ctor
 * @param sbmd 
 */
(sbmd: SwordBookMetaData?) : RawLDBackend<ZLDBackendState>(sbmd, 4) {

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.RawLDBackend#initState()
     */
    @Throws(BookException::class)
    override fun initState(): ZLDBackendState {
        return OpenFileStateManager.instance().getZLDBackendState(bookMetaData)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.RawLDBackend#getEntry(org.crosswire.jsword.book.sword.state.RawLDBackendState, org.crosswire.jsword.book.sword.DataEntry)
     */
    override fun getEntry(fileState: RawLDBackendState?, entry: DataEntry): DataEntry {
        val state: ZLDBackendState
        if (fileState is ZLDBackendState) {
            state = fileState
        } else {
            //something went terribly wrong
            log.error("Backend State was not of type ZLDBackendState. Ignoring this entry and exiting.")
            return DataEntry(entry.getName(), ByteArray(0), entry.getCharset())
        }

        val blockIndex = entry.getBlockIndex()
        val blockNum: Long = blockIndex.getOffset().toLong()
        val blockEntry = blockIndex.getSize()

        // Can we get the data from the cache
        var uncompressed: ByteArray? = null
        if (blockNum == state.lastBlockNum) {
            uncompressed = state.lastUncompressed
        } else {
            var temp: ByteArray
            try {
                temp = SwordUtil.readRAF(state.getZdxRaf(), blockNum * ZDX_ENTRY_SIZE, ZDX_ENTRY_SIZE)
                if (temp.size == 0) {
                    return DataEntry(entry.getName(), ByteArray(0), entry.getCharset())
                }

                val blockStart = SwordUtil.decodeLittleEndian32(temp, 0)
                val blockSize = SwordUtil.decodeLittleEndian32(temp, 4)

                temp = SwordUtil.readRAF(state.getZdtRaf(), blockStart.toLong(), blockSize)

                decipher(temp)

                val compressType = bookMetaData.getProperty(SwordBookMetaData.KEY_COMPRESS_TYPE)
                uncompressed = CompressorType.fromString(compressType)!!.getCompressor(temp).uncompress().toByteArray()

                // cache the uncompressed data for next time
                state.lastBlockNum = blockNum
                state.lastUncompressed = uncompressed
            } catch (e: IOException) {
                return DataEntry(entry.getName(), ByteArray(0), entry.getCharset())
            }
        }

        // get the "entry" from this block.
        val entryCount = SwordUtil.decodeLittleEndian32(uncompressed!!, 0)
        if (blockEntry >= entryCount) {
            return DataEntry(entry.getName(), ByteArray(0), entry.getCharset())
        }

        val entryOffset = BLOCK_ENTRY_COUNT + (BLOCK_ENTRY_SIZE * blockEntry)
        val entryStart = SwordUtil.decodeLittleEndian32(uncompressed, entryOffset)
        var entrySize = SwordUtil.decodeLittleEndian32(uncompressed, entryOffset + 4)
        // Note: the actual entry is '\0' terminated
        val nullTerminator = SwordUtil.findByte(uncompressed, entryStart, 0x00.toByte())
        if (nullTerminator - entryStart + 1 == entrySize) {
            entrySize -= 1
        }
        val entryBytes = ByteArray(entrySize)
        System.arraycopy(uncompressed, entryStart, entryBytes, 0, entrySize)
        val finalEntry = DataEntry(entry.getName(), entryBytes, bookMetaData.bookCharset)

        return finalEntry
    }

    /**
     * Serialization support.
     * 
     * @param is
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`is`: ObjectInputStream) {
        `is`.defaultReadObject()
    }

    /** 
     * Experimental code.
     */
    override fun dumpIdxRaf() {
        var state: RawLDBackendState? = null
        var end: Long = -1
        try {
            state = initState()
            end = getCardinality().toLong()
            val buf = StringBuilder()
            System.out.println("index\toffset\tsize\tkey\tvalue")
            var i: Long = 0
            while (i < end) {
                val index = getIndex(state, i)
                val offset = index.getOffset()
                val size = index.getSize()
                buf.setLength(0)
                buf.append(i)
                buf.append('\t')
                buf.append(offset)
                buf.append('\t')
                buf.append(size)
                if (size > 0) {
                    // Now read the data file for this key using the offset and size
                    val data = SwordUtil.readRAF(state.getDatRaf(), offset.toLong(), size)
                    val blockEntry = DataEntry(i.toString(), data, bookMetaData.bookCharset)
                    val block = blockEntry.getBlockIndex()
                    val dataEntry = getEntry(state, blockEntry)
                    val key = blockEntry.getKey()
                    buf.append('\t')
                    buf.append(key)
                    buf.append('\t')
                    buf.append(block.getOffset())
                    buf.append('\t')
                    buf.append(block.getSize())
                    val raw: String
                    buf.append('\t')
                    if (dataEntry.isLinkEntry()) {
                        raw = dataEntry.getLinkTarget()
                        buf.append("Linked to: ").append(raw.replace('\n', ' '))
                    } else {
                        raw = getRawText(dataEntry)
                        if (raw.length > 43) {
                            buf.append(raw.substring(0, 40).replace('\n', ' '))
                            buf.append("...")
                        } else {
                            buf.append(raw)
                        }
                    }
                }
                System.out.println(buf.toString())
                ++i
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: BookException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    private companion object {
        private const val ZDX_ENTRY_SIZE = 8
        private const val BLOCK_ENTRY_COUNT = 4
        private const val BLOCK_ENTRY_SIZE = 8

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(ZLDBackend::class.java)
        private const val serialVersionUID = 3536098410391064446L
    }
}
