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
package org.crosswire.common.util

import okio.Closeable
import okio.IOException
import kotlin.jvm.JvmStatic

/**
 * .
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class IOUtil
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Get a zip entry by specification, returning a buffer of the contents.
         * If there is an error, return a zero length buffer.
         *
         * @param entrySpec This is of the form /path/to/zip!entryName
         * @return the contents as a buffer
         * @throws IOException
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getZipEntry(entrySpec: String): ByteArray {
            return ioZipEntry(entrySpec)
        }

        /**
         * Closes any {@link Closeable} object
         *
         * @param closeable
         *            The zip file to close
         */
        @JvmStatic
        fun close(closeable: Closeable?) {
            if (null != closeable) {
                try {
                    closeable.close()
                } catch (ex: IOException) {
                    log.error("close", ex)
                }
            }
        }

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(IOUtil::class)
    }
}
