package org.crosswire.jsword.book.sword

import java.io.IOException
import java.io.RandomAccessFile
import org.crosswire.common.util.LoggerFactory

/** Frozen pre-port helper implementation; never calls replacement SwordUtil IO. */
internal object LegacySwordIo {
    private val log = LoggerFactory.getLogger("org.crosswire.jsword.book.sword.SwordUtil")
        fun readRAF(raf: RandomAccessFile?, offset: Long, theSize: Int): ByteArray {
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
        fun readNextRAF(raf: RandomAccessFile?, theSize: Int): ByteArray {
            val offset = raf!!.filePointer
            var size = theSize
            val rafSize = raf.length()

            // It is common to have an entry that points to nothing.
            // That is the equivalent of an empty string.
            if (size == 0) {
                return ByteArray(0)
            }

            if (size < 0) {
                log.error("Nothing to read at offset = {} returning empty because negative size={}", offset.toString(), size.toString())
                return ByteArray(0)
            }

            if (offset >= rafSize) {
                log.error("Attempt to read beyond end. offset={} size={} but raf.length={}", offset.toString(), size.toString(), rafSize.toString())
                return ByteArray(0)
            }

            if (offset + size > raf.length()) {
                log.error("Need to reduce size to avoid EOFException. offset={} size={} but raf.length={}", offset.toString(), size.toString(), rafSize.toString())
                size = (raf.length() - offset).toInt()
            }

            val read = ByteArray(size)
            raf.readFully(read)

            return read
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
        fun writeRAF(raf: RandomAccessFile?, offset: Long, data: ByteArray?) {
            raf!!.seek(offset)
            writeNextRAF(raf, data)
        }

        // was protected static
        @JvmStatic
        @Throws(IOException::class)
        fun writeNextRAF(raf: RandomAccessFile?, data: ByteArray?) {
            if (data == null) {
                return
            }
            raf!!.write(data)
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
        fun readUntilRAF(raf: RandomAccessFile?, offset: Int, stopByte: Byte): ByteArray {
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
        fun readUntilRAF(raf: RandomAccessFile?, stopByte: Byte): ByteArray {
            // The strategy used here is to read the file twice.
            // Once to determine how much to read and then getting the actual data.
            // It may be more efficient to incrementally build up a byte buffer.
            // Note: that growing a static array by 1 byte at a time is O(n**2)
            // This is negligible when the n is small, but prohibitive otherwise.
            val offset = raf!!.filePointer
            var size = 0

            var nextByte: Int
            do {
                nextByte = raf.read()

                size++
            } while (nextByte != -1 && nextByte != stopByte.toInt())

            // Note: we allow for nextByte == -1 to be included in size
            // so that readRAF will report EOF errors
            return readRAF(raf, offset, size)
        }

}
