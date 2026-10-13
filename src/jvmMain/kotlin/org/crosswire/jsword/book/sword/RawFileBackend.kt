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
 * © CrossWire Bible Society, 2009 - 2016
 *
 */
package org.crosswire.jsword.book.sword

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.crosswire.jsword.book.sword.state.RawBackendState
import org.crosswire.jsword.book.sword.state.RawFileBackendState
import org.crosswire.jsword.book.sword.state.RawBackendStateJvm
import org.crosswire.jsword.book.sword.state.RawFileBackendStateJvm
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.versification.Testament
import org.crosswire.jsword.versification.system.Versifications
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import org.crosswire.common.util.RandomAccessSource

/**
 * A Raw File format that allows for each verse to have it's own storage. The
 * basic structure of the index is as follows:
 * <ul>
 * <li><strong>incfile</strong> -- Is initialized with 1 and is incremented once
 * for each non-linked verse that is actually stored in the Book.</li>
 * <li><strong>idx</strong> -- There is one index file for each testament having
 * verses, named nt and ot. These index files contain offsets into the
 * corresponding data file. The idx files are indexed by the ordinal value of
 * the verse within the Testament for the Book's versification.</li>
 * <li><strong>dat</strong> -- There is a data file for each testament having
 * verses, named nt.vss and ot.vss. These data files do not contain the verses
 * but rather the file names that contain the verse text.</li>
 * <li><strong>verse</strong> -- For each stored verse there is a file
 * containing the verse text. The filename is a zero padded number corresponding
 * to the current increment from incfile, when it was created. It is this 7
 * character name that is stored in a dat file.</li>
 * </ul>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author mbergmann
 * @author DM Smith
 */
open class RawFileBackend(sbmd: SwordBookMetaData?, datasize: Int) : RawBackend<RawFileBackendState>(sbmd, datasize) {

    @Throws(BookException::class)
    override fun initState(): RawFileBackendState {
        return OpenFileStateManager.instance().getRawFileBackendState(bookMetaData)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.RawBackend#getEntry(java.lang.String, org.crosswire.jsword.versification.Testament, long)
     */
    @Throws(IOException::class)
    override fun getEntry(state: RawBackendState, name: String?, testament: Testament?, index: Long): String {
        val idxRaf: RandomAccessSource?
        val txtRaf: RandomAccessSource?
        idxRaf = state.getIdxRaf(testament)
        txtRaf = state.getTextRaf(testament)

        val dataIndex = getIndex(idxRaf, index)
        val size = dataIndex.getSize()
        if (size == 0) {
            return ""
        }

        if (size < 0) {
            log.error("In {}: Verse {} has a bad index size of {}.", bookMetaData.initials, name, size.toString())
            return ""
        }

        try {
            val dataFile = getDataTextFile(txtRaf, dataIndex)
            val textBytes = readTextDataFile(dataFile)
            decipher(textBytes)
            return SwordUtil.decode(name, textBytes, bookMetaData.bookCharset)
        } catch (e: BookException) {
            throw IOException(e.message)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.RawBackend#setRawText(org.crosswire.jsword.passage.Key, java.lang.String)
     * 
     */
    // An overload, not an override: RawBackend's is setRawText(RawBackendState, ...). As in Java.
    @Throws(BookException::class, IOException::class)
    open fun setRawText(state: RawFileBackendState, key: Key?, text: String?) {

        val v11nName = bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION)
        val v11n = Versifications.instance().getVersification(v11nName)!!
        val verse = KeyUtil.getVerse(key)
        var index = verse.ordinal
        val testament = v11n.getTestament(index)
        index = v11n.getTestamentOrdinal(index)

        val idxRaf = state.getIdxRaf(testament)
        val txtRaf = state.getTextRaf(testament)
        val txtFile = RawBackendStateJvm.getTextFile(state, testament)

        val dataIndex = getIndex(idxRaf, index.toLong())
        val dataFile: File
        if (dataIndex.getSize() == 0) {
            dataFile = createDataTextFile(state.getIncfileValue())
            updateIndexFile(idxRaf, index.toLong(), txtRaf!!.size())
            updateDataFile(state.getIncfileValue().toLong(), txtFile)
            checkAndIncrementIncfile(state, state.getIncfileValue())
        } else {
            dataFile = getDataTextFile(txtRaf, dataIndex)
        }

        val textData = text!!.toByteArray(Charsets.UTF_8)
        encipher(textData)
        writeTextDataFile(dataFile, textData)
    }

    // An overload, not an override (see setRawText above).
    @Throws(IOException::class)
    open fun setAliasKey(state: RawFileBackendState, alias: Key?, source: Key?) {
        val v11nName = bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION)
        val v11n = Versifications.instance().getVersification(v11nName)!!
        val aliasVerse = KeyUtil.getVerse(alias)
        val sourceVerse = KeyUtil.getVerse(source)
        var aliasIndex = aliasVerse.ordinal
        val testament = v11n.getTestament(aliasIndex)
        aliasIndex = v11n.getTestamentOrdinal(aliasIndex)
        val idxRaf = state.getIdxRaf(testament)

        var sourceOIndex = sourceVerse.ordinal
        sourceOIndex = v11n.getTestamentOrdinal(sourceOIndex)
        val dataIndex = getIndex(idxRaf, sourceOIndex.toLong())

        // Only the index is updated to point to the same place as what is
        // linked.
        updateIndexFile(idxRaf, aliasIndex.toLong(), dataIndex.getOffset().toLong())
    }

    @Throws(BookException::class, IOException::class)
    private fun createDataTextFile(index: Int): File {
        var dataPath = SwordUtil.getExpandedDataPath(bookMetaData).path
        dataPath += File.separator + String.format("%07d", index)
        val dataFile = File(dataPath)
        if (!dataFile.exists() && !dataFile.createNewFile()) {
            throw IOException("Could not create data file.")
        }
        return dataFile
    }

    /**
     * Gets the Filename for the File having the verse text.
     * 
     * @param txtRaf
     * The random access file containing the file names for the verse
     * storage.
     * @param dataIndex
     * The index of where to get the data
     * @return the file having the verse text.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun getTextFilename(txtRaf: RandomAccessSource?, dataIndex: DataIndex): String {
        // data size to be read from the data file (ot or nt) should be 9 bytes
        // this will be the filename of the actual text file "\r\n"
        val data = SwordUtil.readRAF(txtRaf, dataIndex.getOffset().toLong(), dataIndex.getSize())
        decipher(data)
        if (data.size == 7) {
            // default charset on purpose: Java used new String(byte[], int, int)
            return java.lang.String(data, 0, 7) as String
        }
        log.error("Read data is not of appropriate size of 9 bytes!")
        throw IOException("Datalength is not 9 bytes!")
    }

    /**
     * Gets the File having the verse text.
     * 
     * @param txtRaf
     * The random access file containing the file names for the verse
     * storage.
     * @param dataIndex
     * The index of where to get the data
     * @return the file having the verse text.
     * @throws IOException
     * @throws BookException
     */
    @Throws(IOException::class, BookException::class)
    private fun getDataTextFile(txtRaf: RandomAccessSource?, dataIndex: DataIndex): File {
        val dataFilename = getTextFilename(txtRaf, dataIndex)
        val dataPath = SwordUtil.getExpandedDataPath(bookMetaData).path + File.separator + dataFilename
        return File(dataPath)
    }

    @Throws(IOException::class)
    protected open fun updateIndexFile(idxRaf: RandomAccessSource?, index: Long, dataFileStartPosition: Long) {
        val indexFileWriteOffset = index * entrysize
        val dataFileLengthValue = 7 // filename is 7 bytes + 2 bytes for "\r\n"
        val startPositionData = littleEndian32BitByteArrayFromInt(dataFileStartPosition.toInt())
        val lengthValueData = littleEndian16BitByteArrayFromShort(dataFileLengthValue.toShort())
        val indexFileWriteData = ByteArray(6)

        indexFileWriteData[0] = startPositionData[0]
        indexFileWriteData[1] = startPositionData[1]
        indexFileWriteData[2] = startPositionData[2]
        indexFileWriteData[3] = startPositionData[3]
        indexFileWriteData[4] = lengthValueData[0]
        indexFileWriteData[5] = lengthValueData[1]

        SwordUtil.writeRAF(idxRaf, indexFileWriteOffset, indexFileWriteData)
    }

    @Throws(IOException::class)
    protected open fun updateDataFile(ordinal: Long, txtFile: File) {
        val fileName = String.format("%07d\r\n", ordinal)
        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(txtFile, true))
            // charset NAME on purpose (UnsupportedEncodingException, as in Java)
            bos.write((fileName as java.lang.String).getBytes(bookMetaData.bookCharset))
        } finally {
            if (bos != null) {
                bos.close()
            }
        }
    }

    @Throws(IOException::class)
    private fun checkAndIncrementIncfile(state: RawFileBackendState, index: Int) {
        if (index >= state.getIncfileValue()) {
            val incValue = index + 1

            // FIXME(CJB) this portion of code is unsafe for concurrency
            // operations, but without knowing the
            // reason for the inc file, I would guess it unlikely to be a
            // problem
            // this essentially destroys consistency of the state
            state.setIncfileValue(incValue)
            writeIncfile(state, incValue)
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.book.sword.RawBackend#create()
     */
    // FIXME(CJB) : DM - API change to pass in state, or is this a one-off for
    // which we want to release resources afterwards (opted for the second
    // option)
    @Throws(IOException::class, BookException::class)
    override fun create() {
        super.create()

        createDataFiles()
        createIndexFiles()

        var state: RawFileBackendState? = null
        try {
            state = initState()

            createIncfile(state)

            prepopulateIndexFiles(state)
            prepopulateIncfile(state)
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    @Throws(IOException::class, BookException::class)
    private fun createDataFiles() {
        val path = SwordUtil.getExpandedDataPath(bookMetaData).path

        val otTextFile = File(path + File.separator + SwordConstants.FILE_OT)
        if (!otTextFile.exists() && !otTextFile.createNewFile()) {
            throw IOException("Could not create ot text file.")
        }

        val ntTextFile = File(path + File.separator + SwordConstants.FILE_NT)
        if (!ntTextFile.exists() && !ntTextFile.createNewFile()) {
            throw IOException("Could not create nt text file.")
        }
    }

    @Throws(IOException::class, BookException::class)
    private fun createIndexFiles() {
        val path = SwordUtil.getExpandedDataPath(bookMetaData).path
        val otIndexFile = File(path + File.separator + SwordConstants.FILE_OT + SwordConstants.EXTENSION_VSS)
        if (!otIndexFile.exists() && !otIndexFile.createNewFile()) {
            throw IOException("Could not create ot index file.")
        }

        val ntIndexFile = File(path + File.separator + SwordConstants.FILE_NT + SwordConstants.EXTENSION_VSS)
        if (!ntIndexFile.exists() && !ntIndexFile.createNewFile()) {
            throw IOException("Could not create nt index file.")
        }
    }

    @Throws(IOException::class)
    private fun prepopulateIndexFiles(state: RawFileBackendState) {

        val v11nName = bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION)
        val v11n = Versifications.instance().getVersification(v11nName)!!
        val otCount = v11n.getCount(Testament.OLD)
        val ntCount = v11n.getCount(Testament.NEW) + 1
        val otIdxBos = BufferedOutputStream(FileOutputStream(RawBackendStateJvm.getIdxFile(state, Testament.OLD), false))
        try {
            for (i in 0 until otCount) {
                writeInitialIndex(otIdxBos)
            }
        } finally {
            otIdxBos.close()
        }

        val ntIdxBos = BufferedOutputStream(FileOutputStream(RawBackendStateJvm.getIdxFile(state, Testament.NEW), false))
        try {
            for (i in 0 until ntCount) {
                writeInitialIndex(ntIdxBos)
            }
        } finally {
            ntIdxBos.close()
        }
    }

    @Throws(IOException::class, BookException::class)
    private fun createIncfile(state: RawFileBackendState) {
        val tempIncfile = File(SwordUtil.getExpandedDataPath(bookMetaData).path + File.separator + RawFileBackendState.INCFILE)
        if (!tempIncfile.exists() && !tempIncfile.createNewFile()) {
            throw IOException("Could not create incfile file.")
        }
        RawFileBackendStateJvm.setIncfile(state, tempIncfile)
    }

    @Throws(IOException::class)
    private fun prepopulateIncfile(state: RawFileBackendState) {
        writeIncfile(state, 1)
    }

    @Throws(IOException::class)
    private fun writeIncfile(state: RawFileBackendState, value: Int) {
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(RawFileBackendStateJvm.getIncfile(state), false)
            fos.write(littleEndian32BitByteArrayFromInt(value))
        } catch (e: FileNotFoundException) {
            log.error("Error on writing to incfile, file should exist already!", e)
        } finally {
            if (fos != null) {
                fos.close()
            }
        }
    }

    @Throws(IOException::class)
    private fun writeInitialIndex(outStream: BufferedOutputStream) {
        outStream.write(littleEndian32BitByteArrayFromInt(0)) // offset
        outStream.write(littleEndian16BitByteArrayFromShort(0.toShort())) // length
    }

    @Throws(IOException::class)
    private fun readTextDataFile(dataFile: File): ByteArray {
        var inStream: BufferedInputStream? = null
        try {
            val len = dataFile.length().toInt()
            val textData = ByteArray(len)
            inStream = BufferedInputStream(FileInputStream(dataFile))
            if (inStream.read(textData) != len) {
                log.error("Read data is not of appropriate size of {} bytes!", len.toString())
                throw IOException("data is not $len bytes long")
            }
            return textData
        } catch (ex: FileNotFoundException) {
            log.error("Could not read text data file, file not found: {}", dataFile.name, ex)
            throw ex
        } finally {
            if (inStream != null) {
                inStream.close()
            }
        }
    }

    @Throws(IOException::class)
    private fun writeTextDataFile(dataFile: File, textData: ByteArray) {
        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(dataFile, false))
            bos.write(textData)
        } finally {
            if (bos != null) {
                bos.close()
            }
        }
    }

    private fun littleEndian32BitByteArrayFromInt(`val`: Int): ByteArray {
        val buffer = ByteArray(4)
        SwordUtil.encodeLittleEndian32(`val`, buffer, 0)
        return buffer
    }

    private fun littleEndian16BitByteArrayFromShort(`val`: Short): ByteArray {
        val buffer = ByteArray(2)
        SwordUtil.encodeLittleEndian16(`val`.toInt(), buffer, 0)
        return buffer
    }

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(RawFileBackend::class)
    }
}
