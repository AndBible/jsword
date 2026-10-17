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
 * © CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.compress

import java.util.zip.DataFormatException
import java.util.zip.Inflater

/** [RawInflater] over a JDK raw (nowrap) [Inflater]. */
internal class JdkRawInflater : RawInflater {
    private val inflater = Inflater(true)
    override val remaining get() = inflater.remaining
    override val finished get() = inflater.finished()
    override val needsInput get() = inflater.needsInput()
    override val totalWritten get() = inflater.bytesWritten
    override fun setInput(bytes: ByteArray, offset: Int, count: Int) = inflater.setInput(bytes, offset, count)
    override fun inflate(bytes: ByteArray, offset: Int, count: Int): Int = try {
        inflater.inflate(bytes, offset, count)
    } catch (error: DataFormatException) { throw RawDeflateException(error.message) }
    override fun close() = inflater.end()
}

internal actual fun newRawInflater(): RawInflater = JdkRawInflater()
