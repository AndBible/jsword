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
 * © CrossWire Bible Society, 2007 - 2016
 *
 */
package org.crosswire.common.compress

import kotlin.jvm.JvmStatic
import org.crosswire.common.util.jswordAssert

/**
 * An Enumeration of the possible Compressions.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
enum class CompressorType {
    ZIP {
        override fun getCompressor(input: ByteArray): Compressor {
            return Zip(input)
        }
    },

    LZSS {
        override fun getCompressor(input: ByteArray): Compressor {
            return org.crosswire.common.compress.LZSS(input)
        }
    },

    BZIP2 {
        override fun getCompressor(input: ByteArray): Compressor {
            return BZip2(input)
        }
    },

    GZIP {
        override fun getCompressor(input: ByteArray): Compressor {
            return Gzip(input)
        }
    },

    XZ {
        override fun getCompressor(input: ByteArray): Compressor {
            return org.crosswire.common.compress.XZ(input)
        }
    };

    /**
     * Get a compressor.
     *
     * @param input the stream to compress or to uncompress.
     * @return the compressor for the stream
     */
    abstract fun getCompressor(input: ByteArray): Compressor

    companion object {
        /**
         * Get a CompressorType from a String
         *
         * @param name the case insensitive representation of the desired CompressorType
         * @return the desired compressor or null if not found.
         */
        @JvmStatic
        fun fromString(name: String?): CompressorType? {
            for (v in values()) {
                if (v.name.equals(name, ignoreCase = true)) {
                    return v
                }
            }

            // cannot get here
            jswordAssert(false) { "CompressorType not found: $name" }
            return null
        }
    }
}
