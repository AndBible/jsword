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
package org.crosswire.jsword.passage

import java.io.IOException
import java.io.Reader

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications

/**
 * The PassageKeyFactory constructs Passages of the default Passage type.
 * This allows for tuning the application to specific time/space needs
 * and it allows for the development of other types of Passages without
 * needing to change the application.
 * <p>
 * It is strongly recommended to use this factory to create passages
 * if there is no driving need to create them for a specific purpose.
 * </p>
 * <p>
 * Most of the methods take the same arguments:
 * </p>
 * <ul>
 * <li><strong>Versification v11n</strong> - All Passages are created as part
 * of a Versification. Verses and VerseRanges which make up a Passage, require
 * a one.</li>
 * <li><strong>String passageReference</strong> - A string representation for the Passage.
 * The parser is very lenient, but requires the verses to be a member of the
 * Versification.</li>
 * <li><strong>VerseRange basis</strong> - When interpreting a reference, the
 * prior reference forms the context to understand that reference. For example,
 * when seeing 11:1, it needs the context of the BibleBook to know what this
 * chapter and verse belongs. A basis is not needed for OSIS references, as
 * every verse is fully qualified. But for references from ThML and GBF,
 * it is highly needed.</li>
 * </ul>
 * <p>
 * Most of the methods will throw:
 * </p>
 * <ul><li><strong>NoSuchKeyException</strong> - Indicating that something
 * in the string references could not be understood as a verse. The message
 * of the exception will give the precise reason for the failure.</li></ul>
 *
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class PassageKeyFactory
/**
 * This class implements a Singleton pattern. So the ctor is private
 */
private constructor() {
    /**
     * Create an empty list of keys for the v11n
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @return an empty Passage
     */
    fun createEmptyKeyList(v11n: Versification?): Key {
        return defaultType!!.createEmptyPassage(v11n)
    }

    /**
     * Get a Passage containing all the Verses in this Versification.
     * This differs from {@link org.crosswire.jsword.book.Book#getGlobalKeyList} which gets the
     * verses in the Book, which may be a small part of the whole.
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @return the Passage with all the Verses in the Versification
     */
    fun getGlobalKeyList(v11n: Versification?): Key {
        return ReadOnlyPassage(KeyUtil.getPassage(v11n!!.allVerses), true)
    }

    /**
     * Convert the passageReference into a Passage or an empty Passage,
     * if there is an error. Note, this is not recommended as it throws
     * away the error.
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param passageReference
     *            A String containing the text of the Passage
     * @param basis
     *           The basis by which to interpret passageReference
     * @return a new Passage filled with the desired Verses or an empty Passage
     */
    fun getValidKey(v11n: Versification?, passageReference: String?, basis: Key?): Key {
        try {
            return getKey(v11n, passageReference, basis)
        } catch (e: NoSuchKeyException) {
            return createEmptyKeyList(v11n)
        }
    }

    /**
     * Convert the passageReference into a Passage or an empty Passage,
     * if there is an error. Note, this is not recommended as it throws
     * away the error.
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param passageReference
     *            A String containing the text for the Passage
     * @return a new Passage filled with the desired Verses or an empty Passage
     */
    fun getValidKey(v11n: Versification?, passageReference: String?): Key {
        return getValidKey(v11n, passageReference, null)
    }

    /**
     * Convert the passageReference into a Passage. This is the recommended
     * form for understanding references in ThML and GBF.
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param passageReference
     *            A String containing the text for the Passage
     * @param basis
     *           The basis by which to interpret passageReference
     * @return a new Passage filled with the desired Verses
     * @throws NoSuchKeyException
     *             If the passageReference has anything that could not be understood as a Verse
     */
    @Throws(NoSuchKeyException::class)
    fun getKey(v11n: Versification?, passageReference: String?, basis: Key?): Passage {
        // since normalization is relatively expensive
        // don't try it unless it solves a problem.
        try {
            return defaultType!!.createPassage(v11n, passageReference, basis)
        } catch (e: NoSuchKeyException) {
            try {
                return defaultType!!.createPassage(v11n, normalize(passageReference), basis)
            } catch (ex: NoSuchKeyException) {
                // TODO(DM): Parser should allow valid osisRefs!
                return defaultType!!.createPassage(v11n, mungOsisRef(passageReference), basis)
            }
        }
    }

    /**
     * Convert the passageReference into a Passage. This is the recommended
     * form for application constructed references and user input. Both of
     * these should have a fully qualified first reference.
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param passageReference
     *            A String containing the text for the Passage
     * @return a new Passage filled with the desired Verses
     * @throws NoSuchKeyException
     *             If the passageReference has anything that could not be understood as a Verse
     */
    @Throws(NoSuchKeyException::class)
    fun getKey(v11n: Versification?, passageReference: String?): Passage {
        return getKey(v11n, passageReference, null)
    }

    /**
     * Replace spaces with semi-colons, because the parser expects them.
     *
     * @param passageReference
     * @return the munged value
     */
    private fun mungOsisRef(passageReference: String?): String {
        return passageReference!!.replace(' ', ';')
    }

    /**
     * The internals of a Passage require that references are separated with a
     * reference delimiter. However, people and other systems may not be so
     * stringent. So we want to allow for
     * "Ge 1:26  3:22  11:7  20:13  31:7, 53  35:7" (which is from Clarke) This
     * should become "Ge 1:26, 3:22, 11:7, 20:13, 31:7, 53, 35:7" Basically, the
     * rule of thumb is that if two numbers are found separated by whitespace
     * then add a comma between them. One note $, and ff are taken to be
     * numbers. But it is complicated by Book names that are like 1 Cor And by
     * verse references like Gen 1.2 Gen.1.2 Gen 1 2 which are all equivalent.
     * So we use a counter when we see a number, if the counter reaches 2 and
     * then we see a name or a number we emit a reference delimiter.
     *
     * @param passageReference
     * @return the normalized value
     */
    private fun normalize(passageReference: String?): String? {
        if (passageReference == null) {
            return null
        }

        // Note this has a lot in common with AccuracyType.tokenize
        val size = passageReference.length
        val buf = StringBuilder(size * 2)

        var curChar = ' '
        var isNumber = false
        var wasNumberOrMarker = false
        var isEndMarker = false
        var isNumberOrMarker = false
        var i = 0
        while (i < size) {
            curChar = passageReference[i]

            // Determine whether we are starting a number
            isNumber = Character.isDigit(curChar)
            isEndMarker = curChar == '$' || (curChar == 'f' && (if (i + 1 < size) passageReference[i + 1] else ' ') == 'f')
            isNumberOrMarker = isNumber || isEndMarker
            // If the last thing we saw was a number and the next thing we see
            // is another number or a word
            // then we want to put in a ',' or a ' '
            if (wasNumberOrMarker) {
                if (isNumber) {
                    buf.append(AbstractPassage.REF_PREF_DELIM)
                } else if (isEndMarker) {
                    buf.append(VerseRange.RANGE_OSIS_DELIM)
                } else if (Character.isLetter(curChar)) {
                    buf.append(' ')
                }

                // Having handled the condition, we now set it to false
                wasNumberOrMarker = false
            }

            if (isNumberOrMarker) {
                wasNumberOrMarker = true
                buf.append(curChar)
                i++

                // If it started with an 'f' it was also followed by another.
                if (curChar == 'f') {
                    buf.append('f')
                    i++
                } else if (curChar != '$') {
                    // If it wasn't an 'f' or a '$' then it was digits
                    while (i < size) {
                        curChar = passageReference[i]
                        if (!Character.isDigit(curChar)) {
                            break
                        }
                        buf.append(curChar)
                        i++
                    }
                }

                // skip all following whitespace, it will be added back in as
                // needed
                while (i < size && Character.isWhitespace(passageReference[i])) {
                    i++
                }
            } else {
                buf.append(curChar)
                i++
            }
        }

        return buf.toString()
    }

    companion object {
        /**
         * This PassageKeyFactory is accessed through this instance.
         *
         * @return this PassageKeyFactory
         */
        @JvmStatic
        fun instance(): PassageKeyFactory {
            return keyf
        }

        /**
         * Set the default PassageType
         *
         * @param newDefaultType
         *            The new default PassageType.
         */
        @JvmStatic
        fun setDefaultType(newDefaultType: PassageType?) {
            defaultType = newDefaultType
        }

        /**
         * Get the default PassageType.
         *
         * @return The default PassageType.
         */
        @JvmStatic
        fun getDefaultType(): PassageType? {
            return defaultType
        }

        /**
         * Set the default PassageType. Must be the ordinal value of one of:
         * <ul>
         * <li>PassageType.SPEED
         * <li>PassageType.WRITE_SPEED
         * <li>PassageType.SIZE
         * <li>PassageType.MIX
         * <li>PassageType.TALLY
         * </ul>
         *
         * @param newDefaultType
         *            The new default type.
         */
        @JvmStatic
        fun setDefaultPassage(newDefaultType: Int) {
            setDefaultType(PassageType.fromInteger(newDefaultType))
        }

        /**
         * Get the default passage type as the ordinal value of the PassageType.
         *
         * @return default_type The new default type.
         * @see PassageKeyFactory#setDefaultPassage
         */
        @JvmStatic
        fun getDefaultPassage(): Int {
            return PassageType.toInteger(defaultType)
        }

        /**
         * Get a new Passage based on another Passage that synchronizes all access
         * to its members.
         *
         * @param ref
         *            The passage to synchronize
         * @return A new synchronized passage that proxies requests to the original
         */
        @JvmStatic
        fun getSynchronizedPassage(ref: Passage?): Passage {
            return SynchronizedPassage(ref)
        }

        /**
         * Get a new Passage based on another Passage that synchronizes all access
         * to its members.
         *
         * @param ref
         *            The passage to synchronize
         * @param ignore
         *            Do we throw up if someone tries to change us
         * @return A new synchronized passage that proxies requests to the original
         */
        @JvmStatic
        fun getReadOnlyPassage(ref: Passage?, ignore: Boolean): Passage {
            return ReadOnlyPassage(ref, ignore)
        }

        /**
         * Convert us to a binary representation. There are some distinctly
         * endianist happenings here, but that is OK because we are reading the
         * stuff we write here just below.
         *
         * @param ref
         *            The Passage to convert
         * @return a byte array
         */
        // was package-private (PassageUtilTest in the same package calls it)
        @JvmStatic
        fun toBinaryRepresentation(ref: Passage?): ByteArray {
            val v11n = ref!!.versification
            val maxOrdinal = v11n.maximumOrdinal()
            // store these locally we use them so often
            val verses = ref.countVerses()
            val ranges = ref.countRanges(RestrictionType.NONE)

            // the size in bytes of teach storage method
            val bitwiseSize = maxOrdinal / 8
            val rangedSize = (ranges * 4) + 1
            val distinctSize = (verses * 2) + 1

            // if bitwise is equal smallest
            if (bitwiseSize <= rangedSize && bitwiseSize <= distinctSize) {
                val arraySize = binarySize(AbstractPassage.METHOD_COUNT) + (maxOrdinal / 8) + 1
                val buffer = ByteArray(arraySize)
                var index = 0

                index += toBinary(buffer, index, AbstractPassage.BITWISE, AbstractPassage.METHOD_COUNT)

                for (aKey in ref) {
                    val verse = aKey as Verse
                    val ord = verse.ordinal

                    // Which byte should we be altering
                    val idx0 = (ord / 8) + index

                    // Which bit within that byte (0-7)
                    val bit = (ord % 8) - 1

                    buffer[idx0] = (buffer[idx0].toInt() or (1 shl bit)).toByte()
                }

                return buffer
            } else if (distinctSize <= rangedSize) {
                // if distinct is not bigger than ranged
                val arraySize = (binarySize(AbstractPassage.METHOD_COUNT) + binarySize(maxOrdinal)
                        + (verses * binarySize(maxOrdinal)))
                val buffer = ByteArray(arraySize)
                var index = 0

                // write the Passage type and the number of verses. There must be
                // less than 2**16 verses
                index += toBinary(buffer, index, AbstractPassage.DISTINCT, AbstractPassage.METHOD_COUNT)
                index += toBinary(buffer, index, verses, maxOrdinal)

                // write the verse ordinals in a loop
                for (aKey in ref) {
                    val verse = aKey as Verse
                    val ord = verse.ordinal
                    index += toBinary(buffer, index, ord, maxOrdinal)
                }

                return buffer
            } else {
                // otherwise use ranges
                val arraySize = (binarySize(AbstractPassage.METHOD_COUNT) + binarySize(maxOrdinal / 2)
                        + (2 * ranges * binarySize(maxOrdinal)))
                val buffer = ByteArray(arraySize)
                var index = 0

                // write the Passage type and the number of ranges
                index += toBinary(buffer, index, AbstractPassage.RANGED, AbstractPassage.METHOD_COUNT)
                index += toBinary(buffer, index, ranges, maxOrdinal / 2)

                // write the verse ordinals in a loop
                val it = ref.rangeIterator(RestrictionType.NONE)
                while (it.hasNext()) {
                    val range = it.next()
                    index += toBinary(buffer, index, range.start.ordinal, maxOrdinal)
                    index += toBinary(buffer, index, range.getCardinality(), maxOrdinal)
                }

                // chop to size
                return buffer
            }
        }

        /**
         * Write out the object to the given ObjectOutputStream
         *
         * @param buffer
         *            The stream to read our state from
         * @return The converted Passage
         * @throws NoSuchKeyException
         *             If the buffer is invalid
         */
        // was package-private (PassageUtilTest in the same package calls it)
        @JvmStatic
        @Throws(NoSuchKeyException::class)
        fun fromBinaryRepresentation(buffer: ByteArray?): Passage {
            // AV11N(DMS): This is wrong, but toBinaryRepresentation does not write the v11n name
            val rs = Versifications.instance().getVersification("KJV")
            val maxOrdinal = rs!!.maximumOrdinal()
            val ref = keyf.createEmptyKeyList(rs) as Passage

            // Some speedups
            var aref: AbstractPassage? = null
            if (ref is AbstractPassage) {
                aref = ref
                aref.raiseEventSuppresion()
                aref.raiseNormalizeProtection()
            }

            val index = intArrayOf(
                0
            )
            val type = fromBinary(buffer, index, AbstractPassage.METHOD_COUNT)

            when (type) {
                AbstractPassage.BITWISE -> {
                    for (ord in 1..maxOrdinal) {
                        // Which byte should we be viewing
                        val idx0 = (ord / 8) + index[0]

                        // Which bit within that byte (0-7)
                        val bit = (ord % 8) - 1

                        if ((buffer!![idx0].toInt() and (1 shl bit)) != 0) {
                            ref.add(rs.decodeOrdinal(ord))
                        }
                    }
                    // index gets left behind here, but we dont care
                }

                AbstractPassage.DISTINCT -> {
                    val verses = fromBinary(buffer, index, maxOrdinal)
                    for (i in 0 until verses) {
                        val ord = fromBinary(buffer, index, maxOrdinal)
                        ref.add(rs.decodeOrdinal(ord))
                    }
                }

                AbstractPassage.RANGED -> {
                    val ranges = fromBinary(buffer, index, maxOrdinal / 2)
                    for (i in 0 until ranges) {
                        val ord = fromBinary(buffer, index, maxOrdinal)
                        val len = fromBinary(buffer, index, maxOrdinal)
                        ref.add(RestrictionType.NONE.toRange(rs, rs.decodeOrdinal(ord), len))
                    }
                }

                else -> throw NoSuchKeyException(JSOtherMsg.lookupText("Unknown passage type."))
            }

            // Some speedups
            if (aref != null) {
                aref.lowerEventSuppressionAndTest()
                aref.lowerNormalizeProtection()
            }

            return ref
        }

        /**
         * Read a passage from a given stream
         *
         * @param in
         *            The stream to read from
         * @return a newly built Passage
         * @throws IOException
         *             If there was trouble reading the stream
         * @throws NoSuchKeyException
         *             if the data was not a valid passage
         */
        @JvmStatic
        @Throws(IOException::class, NoSuchKeyException::class)
        fun readPassage(`in`: Reader?): Passage {
            // Get any versification. It does not matter. readDescripton will overwrite it.
            val rs = Versifications.instance().getVersification("KJV")
            val ref = keyf.createEmptyKeyList(rs) as Passage
            ref.readDescription(`in`)
            return ref
        }

        /**
         * Write to buffer (starting at index) the given number using a set of bytes
         * as required by the max possible value for the number
         *
         * @param max
         *            The number to write
         * @return The number of bytes needed
         */
        @JvmStatic
        protected fun binarySize(max: Int): Int {
            // 1 byte (2^8)
            if (max < 256) {
                return 1
            }

            // 2 bytes (2^16)
            if (max < 65536) {
                return 2
            }

            // 3 bytes (2^24)
            if (max < 16777216) {
                return 3
            }

            // 4 bytes (2^32)
            return 4
        }

        /**
         * Write to buffer (starting at index) the given number using a set of bytes
         * as required by the max possible value for the number
         *
         * @param buffer
         *            Where to write to
         * @param index
         *            The offset to start at
         * @param number
         *            The number to write
         * @param max
         *            The max size
         * @return The number of bytes written
         */
        @JvmStatic
        protected fun toBinary(buffer: ByteArray?, index: Int, number: Int, max: Int): Int {
            jswordAssert(number >= 0) { "No -ve output $number" }
            jswordAssert(number <= max) { "number $number > max $max" }

            // 1 byte (2^8)
            if (max < 256) {
                buffer!![index] = number.toByte()
                return 1
            }

            // 2 bytes (2^16)
            if (max < 65536) {
                buffer!![index + 0] = (number ushr 8).toByte()
                buffer[index + 1] = (number ushr 0).toByte()
                return 2
            }

            // 3 bytes (2^24)
            if (max < 16777216) {
                buffer!![index + 0] = (number ushr 16).toByte()
                buffer[index + 1] = (number ushr 8).toByte()
                buffer[index + 2] = (number ushr 0).toByte()
                return 3
            }

            // 4 bytes (2^32)
            buffer!![index + 0] = (number ushr 24).toByte()
            buffer[index + 1] = (number ushr 16).toByte()
            buffer[index + 2] = (number ushr 8).toByte()
            buffer[index + 3] = (number ushr 0).toByte()
            return 4
        }

        /**
         * Read and return an int from the buffer (starting at index[0]) using a set
         * of bytes as required by the max possible value for the number, and
         * incrementing index[0] by that number of bytes.
         *
         * @param buffer
         *            The buffer to read from
         * @param index
         *            The offset to start at
         * @param max
         *            The max number of bytes to read
         * @return The converted number
         */
        @JvmStatic
        protected fun fromBinary(buffer: ByteArray?, index: IntArray?, max: Int): Int {
            // Am I naive in thinking that & 0x000000ff turns int -1 into 255?.

            // 1 byte (2^8)
            val b0 = buffer!![index!![0]++].toInt() and 0x000000ff
            if (max < 256) {
                return b0
            }

            // 2 bytes (2^16)
            val b1 = buffer[index[0]++].toInt() and 0x000000ff
            if (max < 65536) {
                return (b0 shl 8) + (b1 shl 0)
            }

            // 3 bytes (2^24)
            val b2 = buffer[index[0]++].toInt() and 0x000000ff
            if (max < 16777216) {
                return (b0 shl 16) + (b1 shl 8) + (b2 shl 0)
            }

            // 4 bytes (2^32)
            val b3 = buffer[index[0]++].toInt() and 0x000000ff
            return (b0 shl 24) + (b1 shl 16) + (b2 shl 8) + (b3 shl 0)
        }

        /**
         * The default type
         */
        private var defaultType: PassageType? = PassageType.SPEED

        /**
         * How we create Passages
         */
        private var keyf: PassageKeyFactory = PassageKeyFactory()
    }
}
