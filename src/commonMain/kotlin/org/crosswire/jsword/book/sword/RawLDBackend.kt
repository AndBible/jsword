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

import org.crosswire.common.util.ArrayIndexOutOfBoundsException
import org.crosswire.common.util.KCharset
import org.crosswire.common.util.UnsupportedCharsetException
import org.crosswire.common.util.StringUtil
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.crosswire.jsword.book.sword.state.RawLDBackendState
import org.crosswire.jsword.passage.DefaultLeafKeyList
import org.crosswire.jsword.passage.Key
import okio.IOException
import org.crosswire.common.util.KswordLocale
import org.crosswire.common.util.MessageFormatLite

/**
 * An implementation AbstractKeyBackend to read RAW format files.
 * 
 * @param <T> The type of the RawLDBackendState that this class extends.
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class RawLDBackend<T : RawLDBackendState>
/**
 * Simple ctor
 * 
 * @param sbmd 
 * @param datasize
 * We need to know how many bytes in the size portion of the
 * index
 */
(sbmd: SwordBookMetaData?, datasize: Int) : AbstractKeyBackend<RawLDBackendState>(sbmd) {
    /**
     * The number of bytes in the size count in the index
     */
    private val datasize: Int = datasize

    /**
     * The number of bytes for each entry in the index: either 6 or 8
     */
    private val entrysize: Int = OFFSETSIZE + datasize

    protected constructor(source: RawLDBackend<*>) : this(null, source.datasize) {
        source.copyBackendFieldsTo(this)
    }

    protected override fun portableBackendClone(): Any {
        if (this::class != RawLDBackend::class) {
            throw UnsupportedOperationException("Host subclass must provide a portable clone")
        }
        return RawLDBackend<RawLDBackendState>(this)
    }

    @Throws(IOException::class)
    override fun readRawContent(state: RawLDBackendState, key: Key?): String {
        return doReadRawContent(state, key!!.getName())
    }

    @Throws(BookException::class)
    override fun initState(): RawLDBackendState {
        return OpenFileStateManager.instance().getRawLDBackendState(bookMetaData)
    }

    @Throws(IOException::class)
    private fun doReadRawContent(state: RawLDBackendState, key: String?): String {
        if (key == null || key.length == 0) {
            return ""
        }
        val pos = search(state, key)
        if (pos >= 0) {
            val index = getIndex(state, pos.toLong())
            var entry = getEntry(state, key, index)
            entry = getEntry(state, entry)
            if (entry.isLinkEntry()) {
                return doReadRawContent(state, entry.getLinkTarget())
            }
            return getRawText(entry)
        }

        // TRANSLATOR: Error condition: Indicates that something could not
        // be found in the book. {0} is a placeholder for the unknown key.
        throw IOException(JSMsg.gettext("Key not found {0}", key))
    }

    protected open fun getRawText(entry: DataEntry): String {
        val cipherKeyString = bookMetaData.getProperty(SwordBookMetaData.KEY_CIPHER_KEY)
        var cipherKeyBytes: ByteArray? = null
        if (cipherKeyString != null) {
            try {
                // Unknown names retain the legacy UTF-8 fallback.
                cipherKeyBytes = KCharset.encode(cipherKeyString, bookMetaData.bookCharset!!)
            } catch (e: UnsupportedCharsetException) {
                cipherKeyBytes = KCharset.encode(cipherKeyString, "UTF-8")
            }
        }
        return entry.getRawText(cipherKeyBytes)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        var state: RawLDBackendState? = null
        try {
            state = initState()

            if (state.size == -1) {
                state.size = (state.getIdxRaf()!!.size() / entrysize).toInt()
            }
            return state.size
        } catch (e: BookException) {
            return 0
        } catch (e: IOException) {
            return 0
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key {
        var state: RawLDBackendState? = null
        try {
            state = initState()

            if (index < getCardinality()) {
                val dataIndex = getIndex(state, index.toLong())
                val entry = getEntry(state, bookMetaData.initials, dataIndex)
                val keytitle = internal2external(entry.getKey())
                return DefaultLeafKeyList(keytitle)
            }
        } catch (e: BookException) {
            // This is allowed
            // Fall through to throw an AIOOBE.
        } catch (e: IOException) {
            // This is allowed
            // Fall through to throw an AIOOBE.
        } finally {
            OpenFileStateManager.instance().release(state)
        }
        throw ArrayIndexOutOfBoundsException(index)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        var state: RawLDBackendState? = null
        try {
            state = initState()
            return search(state, that!!.getName())
        } catch (e: IOException) {
            return -getCardinality() - 1
        } catch (e: BookException) {
            return -getCardinality() - 1
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#size(org.crosswire.jsword.passage.Key)
     */
    override fun getRawTextLength(key: Key?): Int {
        var state: RawLDBackendState? = null
        try {
            state = initState()
            val entry = search(state, key!!.getName())
            // Read the offset and size for this key from the index
            val buffer = SwordUtil.readRAF(state.getIdxRaf(), (entry * entrysize).toLong(), entrysize)
            var entrySize = 0
            when (datasize) {
                2 -> entrySize = SwordUtil.decodeLittleEndian16(buffer, 4)
                4 -> entrySize = SwordUtil.decodeLittleEndian32(buffer, 4)
                else -> jswordAssert(false) { datasize }
            }

            return entrySize
        } catch (e: IOException) {
            return 0
        } catch (e: BookException) {
            return 0
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    /**
     * Get the Index (that is offset and size) for an entry.
     * 
     * @param entry
     * @return the index of the entry
     * @throws IOException
     */
    @Throws(IOException::class)
    protected open fun getIndex(state: RawLDBackendState, entry: Long): DataIndex {
        // Read the offset and size for this key from the index
        val buffer = SwordUtil.readRAF(state.getIdxRaf(), entry * entrysize, entrysize)
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
     * 
     * @param state
     * the state object for the storage
     * @param reply
     * the context for this dataIndex, used for debugging
     * @param dataIndex
     * the entry to get
     * @return the text for the entry.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun getEntry(state: RawLDBackendState, reply: String?, dataIndex: DataIndex): DataEntry {
//        DataIndex dataIndex = getIndex(state, index);
        // Now read the data file for this key using the offset and size
        val data = SwordUtil.readRAF(state.getDatRaf(), dataIndex.getOffset().toLong(), dataIndex.getSize())
        return DataEntry(reply, data, bookMetaData.bookCharset)
    }

    /**
     * Get the entry indicated by this entry. If this entry doesn't indicate any other entry
     * then it returns the entry. Note, this is used by compressed dictionaries to get the deeper stuff.
     * 
     * @param state the state where the entry can be found
     * @param entry the entry that might indicate a deeper entry
     * @return the final entry
     */
    protected open fun getEntry(state: RawLDBackendState?, entry: DataEntry): DataEntry {
        return entry
    }

    /**
     * Find a matching entry, returning it's index. Otherwise return &lt; 0, such
     * that (-pos - 1) gives the insertion index.
     * 
     * @param key
     * @return the match
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun search(state: RawLDBackendState, key: String): Int {
        // Note: In some dictionaries, the first element is out of order and
        // represents the title of the work.
        // So, do the bin search from 1 to end and if not found, check the
        // first element as a special case.
        // If that does not match return the position found otherwise.

        // Initialize to one beyond both ends.
        val total = getCardinality()
        var low = 0
        var high = total
        var match = -1
        var dataIndex: DataIndex

        var suppliedKey: String? = null
        while (high - low > 1) {
            // use >>> to keep mid always in range
            var mid = (low + high) ushr 1

            // Get the key for the item at "mid"
            dataIndex = getIndex(state, mid.toLong())
            // Occasionally there's a bogus index entry (size == 0)
            // in the middle of the index. It needs to be skipped.
            while (dataIndex.getSize() == 0) {
                // reset mid toward the longer end
                mid += if (high - mid > mid - low) 1 else -1
                // ensure that we are in bounds.
                if (mid < low || mid > high) {
                    break
                }
                dataIndex = getIndex(state, mid.toLong())
            }
            val entryKey = normalizeForSearch(getEntry(state, key, dataIndex).getKey())
            // Normalize the key based upon the first entry looked at.
            if (suppliedKey == null) {
                suppliedKey = normalizeForSearch(external2internal(key, entryKey))
            }
            val cmp = entryKey.compareTo(suppliedKey)
            if (cmp < 0) {
                low = mid
            } else if (cmp > 0) {
                high = mid
            } else {
                match = mid
                break
            }
        }

        // Do we have an exact match?
        if (match >= 0) {
            return match
        }

        // Many dictionaries have an introductory entry, so check it for a match.
        dataIndex = getIndex(state, 0)
        val entryKey = normalizeForSearch(getEntry(state, key, dataIndex).getKey())
        if (suppliedKey == null) {
            suppliedKey = normalizeForSearch(external2internal(key, entryKey))
        }
        if (entryKey.compareTo(suppliedKey) == 0) {
            return 0
        }

        // It wasn't found so see if it is present in a linear search if case sensitive keys are used.
        if ("true".equals(bookMetaData.getProperty(SwordBookMetaData.KEY_CASE_SENSITIVE_KEYS), ignoreCase = true)) {
            for (i in 0 until total) {
                dataIndex = getIndex(state, i.toLong())
                if (getEntry(state, key, dataIndex).getKey().compareTo(key) == 0) {
                    return i
                }
            }
        }

        return -(high + 1)
    }

    /**
     * Convert the supplied key to something that can be understood by the module.
     * Use firstKey to determine the pattern for Strong's numbers.
     * 
     * @param externalKey The external key to normalize
     * @param pattern The first non-introduction key in the module.
     * @return the internal representation of the key.
     */
    private fun external2internal(externalKey: String, pattern: String): String {
        if (externalKey.length == 0) {
            return externalKey
        }
        val bmd = bookMetaData
        var keytitle = externalKey
        if (BookCategory.DAILY_DEVOTIONS == bmd.bookCategory) {
            // Is it already in internal format? If so, just return it.
            val m = DEVOTION_PATTERN.matchEntire(keytitle)
            if (m != null) {
                return keytitle
            }
            val (month, day) = DevotionDateFormat.parseMonthDay(keytitle)!!
            val objs = arrayOf<Any>(month, day)
            return MessageFormatLite.format(DATE_KEY_FORMAT, KswordLocale.default(), objs)
        }

        if (bmd.hasFeature(FeatureType.GREEK_DEFINITIONS) || bmd.hasFeature(FeatureType.HEBREW_DEFINITIONS)) {
            // Is the string valid?
            var m = STRONGS_PATTERN.matchEntire(keytitle)
            if (m == null) {
                return keytitle
            }
            if ("true".equals(bmd.getProperty(SwordBookMetaData.KEY_STRONGS_PADDING), ignoreCase = true)) {
                // pad to 4 digits
                // NASB has trailing letters!
                var pos = keytitle.length - 1
                val lastLetter = keytitle[pos]
                val hasTrailingLetter = lastLetter.isLetter()
                if (hasTrailingLetter) {
                    keytitle = keytitle.substring(0, pos)
                    // And it might be preceded by a !
                    pos--
                    if (pos > 0 && keytitle[pos] == '!') {
                        keytitle = keytitle.substring(0, pos)
                    }
                }

                // Get the G or the H.
                val type = keytitle[0]

                // Get the number after the G or H
                val strongsNumber = keytitle.substring(1).toInt()
                // If it has both Greek and Hebrew, then the G and H are needed.
                val buf = StringBuilder()
                if (bmd.hasFeature(FeatureType.GREEK_DEFINITIONS) && bmd.hasFeature(FeatureType.HEBREW_DEFINITIONS)) {
                    // The convention is that a Strong's dictionary with both Greek
                    // and Hebrew have G or H prefix
                    buf.append(type)
                    buf.append(backendZeroPad(strongsNumber, 4))

                    // The NAS lexicon has some entries that end in A-Z, but it is
                    // not preceded by a !
                    if (hasTrailingLetter && "naslex".equals(bmd.initials, ignoreCase = true)) {
                        buf.append(lastLetter)
                    }
                    return buf.toString()
                }

                m = STRONGS_PATTERN.matchEntire(pattern)
                if (m != null) {
                    buf.append(type)
                    val numLength = m.groupValues[2].length
                    if (numLength == 4) {
                        buf.append(backendZeroPad(strongsNumber, 4))
                    } else {
                        buf.append(backendZeroPad(strongsNumber, 5))
                    }
                    // The NAS lexicon has some entries that end in A-Z, but it is
                    // not preceded by a !
                    if (hasTrailingLetter && "naslex".equals(bmd.initials, ignoreCase = true)) {
                        buf.append(lastLetter)
                    }
                    return buf.toString()
                }

                // It is just the number
                return backendZeroPad(strongsNumber, 5)
            }
            // else unpad, E.g. G0001 to G1
            // This test is merely an optimization to prevent unnecessary work.
            if (keytitle[1] == '0') {
                val type = keytitle[0]
                // NASB has trailing letters!
                var pos = keytitle.length - 1
                val lastLetter = keytitle[pos]
                val hasTrailingLetter = lastLetter.isLetter()
                if (hasTrailingLetter) {
                    keytitle = keytitle.substring(0, pos)
                    // And it might be preceded by a !
                    pos--
                    if (pos > 0 && keytitle[pos] == '!') {
                        keytitle = keytitle.substring(0, pos)
                    }
                }
                // Get the number after the G or H
                val strongsNumber = keytitle.substring(1).toInt()
                // The convention is that a Strong's dictionary with both Greek
                // and Hebrew have G or H prefix
                val buf = StringBuilder()
                buf.append(type)
                buf.append(strongsNumber)

                // The NAS lexicon has some entries that end in A-Z, but it is
                // not preceded by a !
                if (hasTrailingLetter && "naslex".equals(bmd.initials, ignoreCase = true)) {
                    buf.append(lastLetter)
                }
            }
        }
        return keytitle
    }

    private fun internal2external(internalKey: String): String? {
        val bmd = bookMetaData
        var keytitle: String? = internalKey
        if (BookCategory.DAILY_DEVOTIONS == bmd.bookCategory && internalKey.length >= 3) {
            val spec = StringUtil.splitAll(internalKey, '.')
            keytitle = DevotionDateFormat.format(spec[0].toInt(), spec[1].toInt())
        }
        return keytitle
    }

    private fun normalizeForSearch(internalKey: String): String {
        val bmd = bookMetaData
        val keytitle = internalKey
        val caseSensitive = bmd.getProperty(SwordBookMetaData.KEY_CASE_SENSITIVE_KEYS)
        if (!"true".equals(caseSensitive, ignoreCase = true) && BookCategory.DAILY_DEVOTIONS != bmd.bookCategory) {
            return keytitle.uppercase()
        }

        return keytitle
    }

    /** 
     * Experimental code.
     */
    open fun dumpIdxRaf(output: Appendable) {
        var state: RawLDBackendState? = null
        var end: Long = -1
        try {
            state = initState()
            end = getCardinality().toLong()
            val buf = StringBuilder()
            output.append("index\toffset\tsize\tkey\tvalue\n")
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
                    val entry = DataEntry(i.toString(), data, bookMetaData.bookCharset)
                    val key = entry.getKey()
                    val raw = getRawText(entry)
                    buf.append('\t')
                    buf.append(key)
                    buf.append('\t')
                    if (raw.length > 43) {
                        buf.append(raw.substring(0, 40).replace('\n', ' '))
                        buf.append("...")
                    } else {
                        buf.append(raw.replace('\n', ' '))
                    }
                } else {
                    buf.append("\t\t")
                }
                output.append(buf.toString()).append('\n')
                ++i
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            backendDebugFailure(e)
        } catch (e: BookException) {
            // TODO Auto-generated catch block
            backendDebugFailure(e)
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    /** 
     * Experimental code.
     */
    open fun toIMP(output: Appendable) {
        var state: RawLDBackendState? = null
        var end: Long = -1
        try {
            state = initState()
            end = getCardinality().toLong()
            val buf = StringBuilder()
            var i: Long = 0
            while (i < end) {
                val index = getIndex(state, i)
                val offset = index.getOffset()
                val size = index.getSize()
                buf.setLength(0)
                buf.append("$$$")
                if (size > 0) {
                    // Now read the data file for this key using the offset and size
                    val data = SwordUtil.readRAF(state.getDatRaf(), offset.toLong(), size)
                    val entry = DataEntry(i.toString(), data, bookMetaData.bookCharset)
                    val key = entry.getKey()
                    val raw = getRawText(entry)
                    buf.append(key)
                    buf.append("\n")
                    buf.append(raw)
                }
                output.append(buf.toString()).append('\n')
                ++i
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            backendDebugFailure(e)
        } catch (e: BookException) {
            // TODO Auto-generated catch block
            backendDebugFailure(e)
        } finally {
            OpenFileStateManager.instance().release(state)
        }
    }

    private companion object {
        /**
         * Date formatter
         */
        private const val DATE_KEY_FORMAT = "{0,number,00}.{1,number,00}"

        /**
         * This is the pattern of a Strong's Number. It begins with a G or H. Is
         * followed by a number. It can be followed by a ! and a letter or just a
         * letter.
         */
        private val STRONGS_PATTERN = Regex("^([GH])(\\d+)((!)?([a-z])?)$")

        /**
         * This is the pattern of a date for a DailyDevotion, DD.MM
         */
        private val DEVOTION_PATTERN = Regex("^\\d\\d\\.\\d\\d$")

        /**
         * How many bytes in the offset pointers in the index
         */
        private const val OFFSETSIZE = 4

    }
}
