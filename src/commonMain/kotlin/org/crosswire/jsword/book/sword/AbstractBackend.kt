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

import org.crosswire.common.crypt.Sapphire
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.KeyType
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.book.sword.state.OpenFileState
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.RestrictionType
import org.crosswire.jsword.passage.Verse
import fi.sykero.kdom.Content
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import okio.IOException
import org.crosswire.common.util.KCharset

/**
 * A generic way to read data from disk for later formatting.
 *
 * @param <T> The type of the OpenFileState that this class extends.
 * @author Joe Walker
 * @author DM Smith
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
abstract class AbstractBackend<T : OpenFileState> : BackendCloneBase, StatefulFileBackedBackend<T>, Backend<T> {
    /**
     * Default constructor for the sake of serialization.
     */
    /* protected */
    constructor() : super()

    /**
     * Construct a minimal backend
     *
     * @param sbmd
     */
    constructor(sbmd: SwordBookMetaData?) : super() {
        bmd = sbmd
    }

    /** Copies the exact base fields for known Native backend copies. */
    protected fun copyBackendFieldsTo(copy: AbstractBackend<*>) {
        copy.bmd = bmd
        copy.ignoreReadErrors = ignoreReadErrors
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#getBookMetaData()
     */
    // Null only on an instance rebuilt by serialization through the default constructor (nothing in jsword or :app does that).
    override val bookMetaData: BookMetaData
        get() = bmd!!

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#decipher(byte[])
     */
    override fun decipher(data: ByteArray) {
        val cipherKeyString = bookMetaData.getProperty(SwordBookMetaData.KEY_CIPHER_KEY)
        if (cipherKeyString != null) {
            // Android cipher keys use UTF-8.
            val cipherEngine = Sapphire(KCharset.encode(cipherKeyString, "UTF-8"))
            for (i in data.indices) {
                data[i] = cipherEngine.cipher(data[i])
            }
            // destroy any evidence!
            cipherEngine.burn()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#encipher(byte[])
     */
    override fun encipher(data: ByteArray) {
        // Enciphering and deciphering are the same!
        decipher(data)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#readIndex()
     */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun readIndex(): Key? {
        // TODO(dms): Eliminate readIndex by deriving GenBookBackend from
        // AbstractKeyBackend
        return null
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#contains(org.crosswire.jsword.passage.Key)
     */
    abstract override fun contains(key: Key?): Boolean

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun getRawText(key: Key?): String {
        var state: T? = null
        try {
            state = initState()
            return readRawContent(state, key)
        } catch (e: IOException) {
            throw BookException("Unable to obtain raw content from backend for key='" + key + '\'', e)
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun setAliasKey(alias: Key?, source: Key?) {
        var state: T? = null
        try {
            state = initState()
            setAliasKey(state, alias, source)
        } catch (e: IOException) {
            throw BookException(JSOtherMsg.lookupText("Unable to save {0}.", alias!!.getOsisID()))
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#size(org.crosswire.jsword.passage.Key)
     */
    override fun getRawTextLength(key: Key?): Int {
        try {
            val raw: String? = getRawText(key)
            return if (raw == null) 0 else raw.length
        } catch (e: BookException) {
            return 0
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#getGlobalKeyList()
     */
    @Throws(BookException::class)
    override fun getGlobalKeyList(): Key {
        //by default, this is not implemented
        throw UnsupportedOperationException("Fast global key list unsupported in this backend")
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#readToOsis(org.crosswire.jsword.passage.Key, org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor)
     */
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun readToOsis(key: Key?, processor: RawTextToXmlProcessor?): MutableList<Content> {

        val content: MutableList<Content> = ArrayList()

        var openFileState: T? = null

        try {
            openFileState = initState()
            when (this.bmd!!.keyType!!) {
                KeyType.LIST -> readNormalOsis(key, processor, content, openFileState)
                KeyType.TREE -> readNormalOsisSingleKey(key, processor, content, openFileState)
                KeyType.VERSE -> readPassageOsis(key, processor, content, openFileState)
                else -> throw BookException("Book has unsupported type of key")
            }

            return content
        } finally {
            OpenFileStateManager.instance().release(openFileState)
        }
    }

    @Throws(BookException::class)
    private fun readNormalOsis(key: Key?, processor: RawTextToXmlProcessor?, content: MutableList<Content>, openFileState: T) {
        // simply lookup the key and process the relevant information
        val iterator: Iterator<Key> = key!!.iterator()

        while (iterator.hasNext()) {
            val next = iterator.next()
            val rawText: String
            try {
                rawText = readRawContent(openFileState, next)
                processor!!.postVerse(next, content, rawText)
            } catch (e: IOException) {
                // failed to process key 'next'
                throwFailedKeyException(key, next, e)
            }
        }
    }

    /**
     * Avoid using iterator for GenBook TreeKeys which would cause a GenBook nodes children to be appended to their parent 
     * e.g. the top level page would include the whole book and result in OOM error 
     */
    @Throws(BookException::class)
    private fun readNormalOsisSingleKey(key: Key?, processor: RawTextToXmlProcessor?, content: MutableList<Content>, openFileState: T) {
        val rawText: String
        try {
            rawText = readRawContent(openFileState, key)
            processor!!.postVerse(key, content, rawText)
        } catch (e: IOException) {
            // failed to process key
            throwFailedKeyException(key, key, e)
        }
    }

    /**
     * Reads a passage as OSIS
     *
     * @param key           the given key
     * @param processor     a processor for which to do things with
     * @param content       a list of content to be appended to (i.e. the OSIS data)
     * @param openFileState the open file state, from which we read things
     * @throws BookException a book exception if we failed to read the book
     */
    @Throws(BookException::class)
    private fun readPassageOsis(key: Key?, processor: RawTextToXmlProcessor?, content: MutableList<Content>, openFileState: T): Verse? {
        var currentVerse: Verse? = null
        val ref = KeyUtil.getPassage(key)
        val rit = ref!!.rangeIterator(RestrictionType.CHAPTER)
        while (rit.hasNext()) {
            val range = rit.next()
            processor!!.preRange(range, content)

            // FIXME(CJB): can this now be optimized since we can calculate
            // the buffer size of what to read?
            // now iterate through all verses in range
            for (verseInRange in range) {
                currentVerse = KeyUtil.getVerse(verseInRange)
                try {
                    val rawText = readRawContent(openFileState, currentVerse)
                    processor.postVerse(verseInRange, content, rawText)
                } catch (e: IOException) {
                    if (!ignoreReadErrors) {
                        throw BookException("Error in uncompression", e)
                    } else {
                        //some versifications have more verses than modules contain - so can't throw
                        //an error here...
                        LOGGER.error(e.message, e)
                    }
                }
            }
        }

        return currentVerse
    }

    private var ignoreReadErrors = true

    open fun setIgnoreReadErrors(ignoreReadErrors: Boolean) {
        this.ignoreReadErrors = ignoreReadErrors
    }

    /**
     * If non-null, currentKey is used to throw the exception, other, masterKey
     * is used instead, which will be more general.
     *
     * @param masterKey
     * the key containing currentKey
     * @param currentKey
     * the currentKey
     * @param e
     * the exception that occured
     * @throws BookException
     * always thrown, a [BookException]
     */
    @Throws(BookException::class)
    private fun throwFailedKeyException(masterKey: Key?, currentKey: Key?, e: IOException) {
        // TRANSLATOR: Common error condition: The file could not be read.
        // There can be many reasons.
        // {0} is a placeholder for the key.
        if (currentKey == null) {
            throw BookException(JSMsg.gettext("Error reading {0}", masterKey!!.getName()), e)
        }
        throw BookException(JSMsg.gettext("Error reading {0}", currentKey.getName()), e)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#create()
     */
    @Throws(IOException::class, BookException::class)
    override fun create() {
        val dataPath = storageUriLocation(SwordUtil.getExpandedDataPath(bookMetaData))
        if (!storageExists(dataPath) && !storageMkdirs(dataPath)) {
            throw IOException("Unable to create module data path!")
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#isSupported()
     */
    override val isSupported: Boolean
        get() = true

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.Backend#isWritable()
     */
    override val isWritable: Boolean
        get() = false

    private var bmd: SwordBookMetaData? = null

    private companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AbstractBackend::class)
    }
}
