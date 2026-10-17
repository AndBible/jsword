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

import okio.Buffer
import okio.EOFException
import okio.IOException

/** A new platform raw-deflate (no zlib header) inflater. */
internal expect fun newRawInflater(): RawInflater

/**
 * Inflates one raw-deflate stream from [input], pulling more bytes with [refill] (false at end of data).
 * When the stream ends, the compressed bytes the inflater did not consume are put back at the front of
 * [input], so whatever follows the stream (a gzip trailer, the next member) can be read from it.
 */
internal class RawInflateStream(
    private val inflater: RawInflater,
    private val input: Buffer,
    private val refill: () -> Boolean
) {
    private var fed = ByteArray(0)
    private var ended = false

    /** Uncompressed bytes produced so far. */
    val totalWritten: Long get() = inflater.totalWritten

    /**
     * Inflates into [out]; -1 once the stream has ended. Throws [IOException] if it is corrupt or truncated.
     * [finished] is checked before [RawInflater.needsInput]: a stream that ends exactly where the fed bytes
     * end (nothing follows it) is complete, not truncated.
     */
    fun read(out: ByteArray, offset: Int, count: Int): Int {
        while (true) {
            if (inflater.finished) {
                if (!ended) {
                    ended = true
                    val unread = inflater.remaining
                    val rest = input.readByteArray()
                    input.write(fed, fed.size - unread, unread)
                    input.write(rest)
                }
                return -1
            }
            if (inflater.needsInput) {
                if (input.size == 0L && !refill()) {
                    // Nothing more to feed; the inflater may still hold output it had no room for.
                    val n = inflater.inflate(out, offset, count)
                    if (n > 0) return n
                    if (!inflater.finished) throw EOFException("truncated deflate data")
                    continue
                }
                fed = input.readByteArray(minOf(input.size, 8192L))
                inflater.setInput(fed, 0, fed.size)
            }
            val n = inflater.inflate(out, offset, count)
            if (n > 0) return n
            if (!inflater.finished && !inflater.needsInput) throw IOException("corrupt deflate data")
        }
    }
}
