@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.compress

import kotlinx.cinterop.*
import platform.zlib.*

/** Persistent negative-window zlib state. Input pins outlive individual inflate calls. */
internal class NativeRawInflater : RawInflater {
    private val stream = nativeHeap.alloc<z_stream>()
    private var inputPin: Pinned<ByteArray>? = null
    private var ended = false
    private var closed = false

    init {
        stream.zalloc = null; stream.zfree = null; stream.opaque = null
        stream.next_in = null; stream.avail_in = 0u
        stream.next_out = null; stream.avail_out = 0u
        val result = inflateInit2_(stream.ptr, -15, ZLIB_VERSION, sizeOf<z_stream>().toInt())
        if (result != Z_OK) {
            nativeHeap.free(stream)
            throw okio.IOException("inflateInit2 failed: $result")
        }
    }

    override val remaining: Int get() = stream.avail_in.toInt()
    override val finished: Boolean get() = ended
    override val needsInput: Boolean get() = stream.avail_in == 0u
    override val totalWritten: Long get() = stream.total_out.toLong()

    override fun setInput(bytes: ByteArray, offset: Int, count: Int) {
        check(!closed)
        require(count > 0 && offset >= 0 && offset <= bytes.size - count)
        inputPin?.unpin()
        inputPin = bytes.pin().also {
            stream.next_in = it.addressOf(offset).reinterpret()
            stream.avail_in = count.convert()
        }
    }

    override fun inflate(bytes: ByteArray, offset: Int, count: Int): Int {
        check(!closed)
        require(count > 0 && offset >= 0 && offset <= bytes.size - count)
        val result = bytes.usePinned {
            stream.next_out = it.addressOf(offset).reinterpret()
            stream.avail_out = count.convert()
            inflate(stream.ptr, Z_NO_FLUSH)
        }
        stream.next_out = null
        when (result) {
            Z_STREAM_END -> ended = true
            Z_OK, Z_BUF_ERROR -> Unit
            else -> throw RawDeflateException(stream.msg?.toKString() ?: "zlib error $result")
        }
        return count - stream.avail_out.toInt()
    }

    override fun close() {
        if (closed) return
        closed = true
        try { inflateEnd(stream.ptr) } finally {
            inputPin?.unpin(); inputPin = null
            nativeHeap.free(stream)
        }
    }
}

internal actual fun newRawInflater(): RawInflater = NativeRawInflater()
