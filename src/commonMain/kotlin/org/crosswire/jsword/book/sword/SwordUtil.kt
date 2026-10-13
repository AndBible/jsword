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

import org.crosswire.common.util.NetUtil
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import okio.IOException
import org.crosswire.common.util.RandomAccessSource
import org.crosswire.common.util.KCharset
import org.crosswire.common.util.UnsupportedCharsetException
import org.crosswire.common.util.KUri

/**
 * Various utilities used by different Sword classes.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class SwordUtil private constructor() {
    // Prevent instantiation

    companion object {
        /**
         * Read a RandomAccessFile
         * 
         * @param raf
         * The file to read
         * @param offset
         * The start of the record to read
         * @param theSize
         * The number of bytes to read
         * @return the read data
         * @throws IOException
         * on error
         */
        // was protected static (package access for the other sword classes); public because Kotlin protected has no package access
        @JvmStatic
        @Throws(IOException::class)
        fun readRAF(raf: RandomAccessSource?, offset: Long, theSize: Int): ByteArray {
            raf!!.seek(offset)
            return readNextRAF(raf, theSize)
        }

        /**
         * Read a RandomAccessFile from the current location in the file.
         * 
         * @param raf
         * The file to read
         * @param theSize
         * The number of bytes to read
         * @return the read data
         * @throws IOException
         * on error
         */
        // was protected static
        @JvmStatic
        @Throws(IOException::class)
        fun readNextRAF(raf: RandomAccessSource?, theSize: Int): ByteArray {
            return raf!!.readNext(theSize)
        }

        /**
         * Writes "data" to a RandomAccessFile at the "offset" position
         * 
         * @param raf
         * RandomAccessFile
         * @param offset
         * offset to write at
         * @param data
         * data to write
         * @throws IOException
         * on error
         */
        // was protected static
        @JvmStatic
        @Throws(IOException::class)
        fun writeRAF(raf: RandomAccessSource?, offset: Long, data: ByteArray?) {
            raf!!.seek(offset)
            writeNextRAF(raf, data)
        }

        // was protected static
        @JvmStatic
        @Throws(IOException::class)
        fun writeNextRAF(raf: RandomAccessSource?, data: ByteArray?) {
            if (data == null) {
                return
            }
            raf!!.writeNext(data)
        }

        /**
         * Read a RandomAccessFile until a particular byte is seen
         * 
         * @param raf
         * The file to read
         * @param offset
         * The start of the record to read
         * @param stopByte
         * The point at which to stop reading
         * @return the read data
         * @throws IOException
         * on error
         */
        // was protected static
        @JvmStatic
        @Throws(IOException::class)
        fun readUntilRAF(raf: RandomAccessSource?, offset: Int, stopByte: Byte): ByteArray {
            raf!!.seek(offset.toLong())
            return readUntilRAF(raf, stopByte)
        }

        /**
         * Read a RandomAccessFile until a particular byte is seen
         * 
         * @param raf
         * The file to read
         * @param stopByte
         * The point at which to stop reading
         * @return the read data
         * @throws IOException
         * on error
         */
        // was protected static
        @JvmStatic
        @Throws(IOException::class)
        fun readUntilRAF(raf: RandomAccessSource?, stopByte: Byte): ByteArray {
            return raf!!.readUntil(stopByte)
        }

        /**
         * Decode little endian data from a byte array. This assumes that the high
         * order bit is not set as this is used solely for an offset in a file in
         * bytes. For a practical limit, 2**31 is way bigger than any document that
         * we can have.
         * 
         * @param data
         * the byte[] from which to read 4 bytes
         * @param offset
         * the offset into the array
         * @return The decoded data
         */
        @JvmStatic
        fun decodeLittleEndian32(data: ByteArray, offset: Int): Int {
            // Convert from a byte to an int, but prevent sign extension.
            // So -16 becomes 240
            val byte1 = data[0 + offset].toInt() and 0xFF
            val byte2 = (data[1 + offset].toInt() and 0xFF) shl 8
            val byte3 = (data[2 + offset].toInt() and 0xFF) shl 16
            val byte4 = (data[3 + offset].toInt() and 0xFF) shl 24

            return byte4 or byte3 or byte2 or byte1
        }

        /**
         * Encode little endian data from a byte array. This assumes that the number
         * fits in a Java integer. That is, the range of an unsigned C integer is
         * greater than a signed Java integer. For a practical limit, 2**31 is way
         * bigger than any document that we can have. If this ever doesn't work, use
         * a long for the number.
         * 
         * @param `val`
         * the number to encode into little endian
         * @param data
         * the byte[] from which to write 4 bytes
         * @param offset
         * the offset into the array
         */
        // was protected static
        @JvmStatic
        fun encodeLittleEndian32(`val`: Int, data: ByteArray, offset: Int) {
            data[0 + offset] = (`val` and 0xFF).toByte()
            data[1 + offset] = ((`val` shr 8) and 0xFF).toByte()
            data[2 + offset] = ((`val` shr 16) and 0xFF).toByte()
            data[3 + offset] = ((`val` shr 24) and 0xFF).toByte()
        }

        /**
         * Decode little endian data from a byte array
         * 
         * @param data
         * the byte[] from which to read 2 bytes
         * @param offset
         * the offset into the array
         * @return The decoded data
         */
        // was protected static
        @JvmStatic
        fun decodeLittleEndian16(data: ByteArray, offset: Int): Int {
            // Convert from a byte to an int, but prevent sign extension.
            // So -16 becomes 240
            val byte1 = data[0 + offset].toInt() and 0xFF
            val byte2 = (data[1 + offset].toInt() and 0xFF) shl 8

            return byte2 or byte1
        }

        /**
         * Encode a 16-bit little endian from an integer. It is assumed that the
         * integer's lower 16 bits are the only that are set.
         * 
         * @param data
         * the byte[] from which to write 2 bytes
         * @param offset
         * the offset into the array
         */
        // was protected static
        @JvmStatic
        fun encodeLittleEndian16(`val`: Int, data: ByteArray, offset: Int) {
            data[0 + offset] = (`val` and 0xFF).toByte()
            data[1 + offset] = ((`val` shr 8) and 0xFF).toByte()
        }

        /**
         * Find a byte of data in an array
         * 
         * @param data
         * The array to search
         * @param sought
         * The data to search for
         * @return The index of the found position or -1 if not found
         */
        // was protected static
        @JvmStatic
        fun findByte(data: ByteArray, sought: Byte): Int {
            return findByte(data, 0, sought)
        }

        /**
         * Find a byte of data in an array
         * 
         * @param data
         * The array to search
         * @param offset
         * The position in the array to begin looking
         * @param sought
         * The data to search for
         * @return The index of the found position or -1 if not found
         */
        // was protected static
        @JvmStatic
        fun findByte(data: ByteArray, offset: Int, sought: Byte): Int {
            for (i in offset until data.size) {
                if (data[i] == sought) {
                    return i
                }
            }

            return -1
        }

        /**
         * Transform a byte array into a string given the encoding. If the encoding
         * is bad then it just does it as a string.
         * Note: this may modify data. Don't use it to examine data.
         * 
         * @param key the key
         * @param data
         * The byte array to be converted
         * @param charset
         * The encoding of the byte array
         * @return a string that is UTF-8 internally
         */
        @JvmStatic
        fun decode(key: String?, data: ByteArray, charset: String?): String {
            return decode(key, data, 0, data.size, charset)
        }

        /**
         * Transform a portion of a byte array into a string given the encoding. If
         * the encoding is bad then it just does it as a string.
         * Note: this may modify data. Don't use it to examine data.
         * 
         * @param key the key
         * @param data
         * The byte array to be converted
         * @param length
         * The number of bytes to use.
         * @param charset
         * The encoding of the byte array
         * @return a string that is UTF-8 internally
         */
        @JvmStatic
        fun decode(key: String?, data: ByteArray, length: Int, charset: String?): String {
            return decode(key, data, 0, length, charset)
        }

        /**
         * Transform a portion of a byte array starting at an offset into a string
         * given the encoding. If the encoding is bad then it just does it as a
         * string. Note: this may modify data. Don't use it to examine data.
         * 
         * @param key the key
         * @param data
         * The byte array to be converted
         * @param offset
         * The starting position in the byte array
         * @param length
         * The number of bytes to use.
         * @param charset
         * The encoding of the byte array
         * @return a string that is UTF-8 internally
         */
        @JvmStatic
        fun decode(key: String?, data: ByteArray, offset: Int, length: Int, charset: String?): String {
            if ("WINDOWS-1252" == charset) {
                clean1252(key, data, offset, length)
            }
            var txt = ""
            try {
                if (offset + length <= data.size) {
                    txt = KCharset.decode(data, offset, length, charset)
                }
            } catch (ex: UnsupportedCharsetException) {
                // The Android platform default used by the legacy fallback is UTF-8.
                log.error("{}: Encoding {} not supported.", key, charset, ex)
                txt = KCharset.decode(data, offset, length, "UTF-8")
            }

            return txt
        }

        /**
         * Remove rogue characters in the source. These are characters that are not
         * valid in cp1252 aka WINDOWS-1252 and in UTF-8 or are non-printing control
         * characters in the range of 0-32.
         */
        private fun clean1252(key: String?, data: ByteArray, offset: Int, length: Int) {
            var end = offset + length
            // make sure it doesn't go off the end
            if (end > data.size) {
                end = data.size
            }
            for (i in offset until end) {
                // between 0-32 only allow whitespace: \t, \n, \r, ' '
                // characters 0x81, 0x8D, 0x8F, 0x90 and 0x9D are undefined in
                // cp1252
                val c = data[i].toInt() and 0xFF
                if ((c >= 0x00 && c < 0x20 && c != 0x09 && c != 0x0A && c != 0x0D) || (c == 0x81 || c == 0x8D || c == 0x8F || c == 0x90 || c == 0x9D)) {
                    data[i] = 0x20
                    log.error("{} has bad character 0x{} at position {} in input.", key, c.toString(16), i.toString())
                }
            }
        }

        /**
         * Returns where the book should be located
         * @param bookMetaData meta information about the book
         * @return the URI locating the resource
         * @throws BookException thrown if an issue is encountered, e.g. missing data files.
         */
        @JvmStatic
        @Throws(BookException::class)
        fun getExpandedDataPath(bookMetaData: BookMetaData): KUri {
            val loc = NetUtil.lengthenURI(bookMetaData.library!!, bookMetaData.getProperty(SwordBookMetaData.KEY_DATA_PATH)!!)

            if (loc == null) {
                // FIXME(DMS): missing parameter
                throw BookException(JSOtherMsg.lookupText("Missing data files for old and new testaments in {0}."))
            }

            return loc
        }

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(SwordUtil::class)
    }
}
