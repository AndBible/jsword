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
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.crosswire.jsword.book.sword.state.ZVerseBackendState
import org.crosswire.jsword.passage.BitwisePassage
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.RocketPassage
import org.crosswire.jsword.versification.Testament
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import java.io.IOException
import java.io.RandomAccessFile

/**
 * A backend to read compressed data verse based files. While the text file
 * contains data compressed with ZIP or LZSS, it cannot be uncompressed using a
 * stand alone zip utility, such as WinZip or gzip. The reason for this is that
 * the data file is a concatenation of blocks of compressed data.
 * 
 * <p>
 * The blocks can either be "b" book (aka testament); "c" chapter or "v" verse
 * The choice is a matter of trade offs. The program needs to uncompress
 * a block into memory. Having it at the book level is very memory expensive.
 * Having it at the verse level is very disk expensive, but takes the least
 * amount of memory. The most common is chapter.
 * </p>
 * 
 * <p>
 * In order to find the data in the text file, we need to find the block. The
 * first index (idx) is used for this. Each verse is indexed to a tuple (block
 * number, verse start, verse size). This data allows us to find the correct
 * block, and to extract the verse from the uncompressed block, but it does not
 * help us uncompress the block.
 * </p>
 * 
 * <p>
 * Once the block is known, then the next index (comp) gives the location of the
 * compressed block, its compressed size and its uncompressed size.
 * </p>
 * 
 * <p>
 * There are 3 files for each testament, 2 (idx and comp) are indexes into the
 * third (text) which contains the data. The key into each index is the verse
 * index within that testament, which is determined by book, chapter and verse
 * of that key.
 * </p>
 * 
 * <p>
 * All unsigned numbers are stored 2-complement, little endian.
 * </p>
 * <p>
 * Then proceed as follows, at all times working on the set of files for the
 * testament in question:
 * </p>
 * 
 * The three files are laid out in the following fashion:
 * <ul>
 * <li>The idx file has one entry per verse in the versification. The number
 * of verses varies by versification and testament. Each entry describes the
 * compressed block in which it is found, the start of the verse in the
 * uncompressed block and the length of the verse.
 * <ul>
 * <li>Block number - 32-bit/4-bytes - the number of the entry in the comp file.</li>
 * <li>Verse start - 32 bit/4-bytes - the start of the verse in the uncompressed block in the dat file.</li>
 * <li>Verse length - 16 or 32 bit/2 or 4-bytes - the length of the verse in the uncompressed block from the dat file.</li>
 * </ul>
 * Algorithm:
 * <ul>
 * <li>Given the ordinal value of the verse, seek to the ordinal * entrysize and read entrysize bytes.
 * <li>Decode the entrysize bytes as Block Number, Verse start and length</li>
 * </ul>
 * </li>
 * <li>The comp file has one entry per block.
 * Each entry describes the location of a compressed block,
 * giving its start and size in the next file.
 * <ul>
 * <li>Block Start - 32-bit/4-byte - the start of the block in the dat file</li>
 * <li>Compressed Block Size - 32-bit/4-byte - the size of the compressed block in the dat file</li>
 * <li>Uncompressed Block Size - 32-bit/4-byte - the size of the block after uncompressing</li>
 * </ul>
 * Algorithm:
 * <ul>
 * <li>Given a block number, seek to block-index * 12 and read 12 bytes</li>
 * <li>Decode the 12 bytes as Block Start, Compressed Block Size and Uncompressed Block Size</li>
 * </ul>
 * </li>
 * <li> The dat file is compressed blocks of verses.
 * <br>
 * Algorithm:
 * <ul>
 * <li>Given the entry from the comp file, seek to the start and read the indicated compressed block size</li>
 * <li>If the book is enciphered it, decipher it.</li>
 * <li>Uncompress the block, using the uncompressed size as an optimization.</li>
 * <li>Using the verse start, seek to that location in the uncompressed block and read the indicated verse size.</li>
 * <li>Convert the bytes to a String using the books indicated charset.</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class ZVerseBackend
/**
 * Simple ctor
 * @param sbmd 
 * @param blockType
 * @param datasize 
 */
(sbmd: SwordBookMetaData?, blockType: BlockType, datasize: Int) : AbstractBackend<ZVerseBackendState>(sbmd) {
    /**
     * Whether the book is blocked by Book, Chapter or Verse.
     */
    private val blockType: BlockType = blockType

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

    /* This method assumes single keys. It is the responsibility of the caller to provide the iteration. 
     * 
     * FIXME: this could be refactored to push the iterations down, but no performance benefit would be gained since we have a manager that keeps the file accesses open
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return getRawTextLength(key) > 0
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#size(org.crosswire.jsword.passage.Key)
     */
    override fun getRawTextLength(key: Key?): Int {
        var rafBook: ZVerseBackendState? = null
        try {
            rafBook = initState()

            val v11nName = bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION)
            val v11n = Versifications.instance().getVersification(v11nName)!!
            val verse = KeyUtil.getVerse(key)

            var index = verse.ordinal
            val testament = v11n.getTestament(index)
            index = v11n.getTestamentOrdinal(index)

            val idxRaf = rafBook.getIdxRaf(testament)

            // If Bible does not contain the desired testament, then false
            if (idxRaf == null) {
                return 0
            }

            // entrysize because the index is entrysize bytes long for each verse
            val temp = SwordUtil.readRAF(idxRaf, 1L * index * entrysize, entrysize)

            // If the Bible does not contain the desired verse, return nothing.
            // Some Bibles have different versification, so the requested verse
            // may not exist.
            if (temp.size == 0) {
                return 0
            }

            // The data is little endian - extract the verseSize
            if (datasize == 2) {
                return SwordUtil.decodeLittleEndian16(temp, 8)
            }
            // datasize == 4:
            return SwordUtil.decodeLittleEndian32(temp, 8)
        } catch (e: IOException) {
            return 0
        } catch (e: BookException) {
            // FIXME(CJB): fail silently as before, but i don't think this is
            // correct behaviour - would cause API changes
            LOGGER.error("Unable to ascertain key validity", e)
            return 0
        } finally {
            OpenFileStateManager.instance().release(rafBook)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#getGlobalKeyList()
     */
    @Throws(BookException::class)
    override fun getGlobalKeyList(): Key {
        var rafBook: ZVerseBackendState? = null
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
                // is in the last datasize bytes. If both bytes are 0, then there is no content.
                // For each entry of entrysize bytes, the length of the verse in bytes
                // is in the last datasize bytes. If all bytes are 0, then there is no content.
                if (datasize == 2) {
                    var ii = 0
                    while (ii < temp.size) {
                        // This can be simplified to temp[ii + 8] == 0 && temp[ii + 8] == 0.
                        // int verseSize = SwordUtil.decodeLittleEndian16(temp, ii + 8);
                        // if (verseSize > 0) {
                        if (temp[ii + 8].toInt() != 0 || temp[ii + 9].toInt() != 0) {
                            val ordinal = ii / entrysize
                            passage.addVersifiedOrdinal(v11n.getOrdinal(currentTestament, ordinal))
                        }
                        ii += entrysize
                    }
                } else { // datasize == 4
                    var ii = 0
                    while (ii < temp.size) {
                        // This can be simplified to temp[ii + 8] == 0 && temp[ii + 8] == 0 && temp[ii + 10] == 0 && temp[ii + 11] == 0.
                        // int verseSize = SwordUtil.decodeLittleEndian32(temp, ii + 8);
                        // if (verseSize > 0) {
                        if (temp[ii + 8].toInt() != 0 || temp[ii + 9].toInt() != 0 || temp[ii + 10].toInt() != 0 || temp[ii + 11].toInt() != 0) {
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

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.StatefulFileBackedBackend#initState()
     */
    @Throws(BookException::class)
    override fun initState(): ZVerseBackendState {
        return OpenFileStateManager.instance().getZVerseBackendState(bookMetaData, blockType)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.StatefulFileBackedBackend#readRawContent(org.crosswire.jsword.book.sword.state.OpenFileState, org.crosswire.jsword.passage.Key)
     */
    @Throws(IOException::class)
    override fun readRawContent(state: ZVerseBackendState, key: Key?): String {
        val rafBook = state

        val bookMetaData = this.bookMetaData
        val charset = bookMetaData.bookCharset
        val compressType = bookMetaData.getProperty(SwordBookMetaData.KEY_COMPRESS_TYPE)

        val v11nName = this.bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION)
        val v11n = Versifications.instance().getVersification(v11nName)!!
        val verse = KeyUtil.getVerse(key)

        var index = verse.ordinal
        val testament = v11n.getTestament(index)
        index = v11n.getTestamentOrdinal(index)
        val idxRaf: RandomAccessFile?
        val compRaf: RandomAccessFile?
        val textRaf: RandomAccessFile?

        idxRaf = rafBook.getIdxRaf(testament)
        compRaf = rafBook.getCompRaf(testament)
        textRaf = rafBook.getTextRaf(testament)

        // If Bible does not contain the desired testament, return nothing.
        if (idxRaf == null) {
            return ""
        }

        //dumpIdxRaf(v11n, 0, compRaf);
        //dumpCompRaf(idxRaf);
        // entrysize because the index is entrysize bytes long for each verse
        var temp = SwordUtil.readRAF(idxRaf, 1L * index * entrysize, entrysize)

        // If the Bible does not contain the desired verse, return nothing.
        // Some Bibles have different versification, so the requested verse
        // may not exist.
        if (temp.size == 0) {
            return ""
        }

        // The data is little endian - extract the blockNum, verseStart
        // and
        // verseSize
        val blockNum: Long = SwordUtil.decodeLittleEndian32(temp, 0).toLong()
        val verseStart = SwordUtil.decodeLittleEndian32(temp, 4)
        val verseSize: Int
        if (datasize == 2) {
            verseSize = SwordUtil.decodeLittleEndian16(temp, 8)
        } else { // datasize == 4:
            verseSize = SwordUtil.decodeLittleEndian32(temp, 8)
        }

        // Can we get the data from the cache
        var uncompressed: ByteArray? = null
        if (blockNum == rafBook.lastBlockNum && testament == rafBook.lastTestament) {
            uncompressed = rafBook.lastUncompressed
        } else {
            // Then seek using this index into the idx file
            temp = SwordUtil.readRAF(compRaf, blockNum * COMP_ENTRY_SIZE, COMP_ENTRY_SIZE)
            if (temp.size == 0) {
                return ""
            }

            val blockStart = SwordUtil.decodeLittleEndian32(temp, 0)
            val blockSize = SwordUtil.decodeLittleEndian32(temp, 4)
            val uncompressedSize = SwordUtil.decodeLittleEndian32(temp, 8)

            // Read from the data file.
            val data = SwordUtil.readRAF(textRaf, blockStart.toLong(), blockSize)

            decipher(data)

            uncompressed = CompressorType.fromString(compressType)!!.getCompressor(data).uncompress(uncompressedSize).toByteArray()

            // cache the uncompressed data for next time
            rafBook.lastBlockNum = blockNum
            rafBook.lastTestament = testament
            rafBook.lastUncompressed = uncompressed
        }

        // and cut out the required section.
        val chopped = ByteArray(verseSize)
        System.arraycopy(uncompressed, verseStart, chopped, 0, verseSize)

        return SwordUtil.decode(key!!.getName(), chopped, charset)

    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    @Throws(IOException::class)
    override fun setAliasKey(state: ZVerseBackendState, alias: Key?, source: Key?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#setRawText(org.crosswire.jsword.passage.Key, java.lang.String)
     */
    @Throws(BookException::class, IOException::class)
    override fun setRawText(state: ZVerseBackendState, key: Key?, text: String?) {
        throw UnsupportedOperationException()
    }

    /** 
     * Experimental code.
     * 
     * @param v11n
     * @param ordinalStart
     * @param raf
     */
    open fun dumpIdxRaf(v11n: Versification, ordinalStart: Int, raf: RandomAccessFile) {
        var end: Long = -1
        try {
            end = raf.length()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        var i = ordinalStart
        val buf = StringBuilder()
        System.out.println("osisID\tblock\tstart\tsize")
        var offset: Long = 0
        while (offset < end) {
            // entrysize because the index is entrysize bytes long for each verse
            var temp: ByteArray? = null
            try {
                temp = SwordUtil.readRAF(raf, offset, entrysize)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            // If the Bible does not contain the desired verse, return nothing.
            // Some Bibles have different versification, so the requested verse
            // may not exist.
            var blockNum: Long = -1
            var verseStart = -1
            var verseSize = -1
            if (temp != null && temp.size > 0) {
                // The data is little endian - extract the blockNum, verseStart and verseSize
                blockNum = SwordUtil.decodeLittleEndian32(temp, 0).toLong()
                verseStart = SwordUtil.decodeLittleEndian32(temp, 4)
                if (datasize == 2) {
                    verseSize = SwordUtil.decodeLittleEndian16(temp, 8)
                } else { // datasize == 4:
                    verseSize = SwordUtil.decodeLittleEndian32(temp, 8)
                }
            }
            buf.setLength(0)
            buf.append(v11n.decodeOrdinal(i++).getOsisID())
            buf.append('\t')
            buf.append(blockNum)
            buf.append('\t')
            buf.append(verseStart)
            buf.append('\t')
            buf.append(verseSize)
            System.out.println(buf.toString())
            offset += entrysize
        }
    }

    /**
     * Experimental code.
     * 
     * @param raf
     */
    open fun dumpCompRaf(raf: RandomAccessFile) {
        var end: Long = -1
        try {
            end = raf.length()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        val blockNum = 0
        val buf = StringBuilder()
        System.out.println("block\tstart\tsize\tuncompressed")
        var offset: Long = 0
        while (offset < end) {
            // 12 because the index is 12 bytes long for each verse
            var temp: ByteArray? = null
            try {
                temp = SwordUtil.readRAF(raf, offset, COMP_ENTRY_SIZE)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            // If the Bible does not contain the desired verse, return nothing.
            // Some Bibles have different versification, so the requested verse
            // may not exist.
            var blockStart = -1
            var blockSize = -1
            var uncompressedSize = -1
            if (temp != null && temp.size > 0) {
                // The data is little endian - extract the blockNum, verseStar and verseSize
                blockStart = SwordUtil.decodeLittleEndian32(temp, 0)
                blockSize = SwordUtil.decodeLittleEndian32(temp, 4)
                uncompressedSize = SwordUtil.decodeLittleEndian32(temp, 8)
            }
            buf.setLength(0)
            buf.append(blockNum)
            buf.append('\t')
            buf.append(blockStart)
            buf.append('\t')
            buf.append(blockSize)
            buf.append('\t')
            buf.append(uncompressedSize)
            System.out.println(buf.toString())
            offset += COMP_ENTRY_SIZE
        }
    }

    companion object {
        /**
         * How many bytes in the offset pointers in the index
         */
        protected const val OFFSETSIZE: Int = 8

        /**
         * How many bytes in the comp index?
         */
        private const val COMP_ENTRY_SIZE = 12

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(ZVerseBackend::class)
    }
}
