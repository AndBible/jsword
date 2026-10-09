package org.crosswire.common.compress

import kotlinx.cinterop.*
import okio.Buffer
import platform.zlib.*

@OptIn(ExperimentalForeignApi::class)
actual object Zlib {
    actual fun inflate(input: ByteArray, expectedSize: Int): ByteArray {
        require(expectedSize > 0) { "Buffer size <= 0" }
        if (input.isEmpty()) throw okio.EOFException("Unexpected end of ZLIB input stream")
        val out = Buffer()
        memScoped {
            val strm = alloc<z_stream>()
            strm.zalloc = null; strm.zfree = null; strm.opaque = null
            strm.next_in = null; strm.avail_in = 0u
            check(inflateInit_(strm.ptr, ZLIB_VERSION, sizeOf<z_stream>().toInt()) == Z_OK) { "inflateInit failed" }
            try {
                val chunk = ByteArray(expectedSize)
                input.usePinned { inPin ->
                    strm.next_in = inPin.addressOf(0).reinterpret()
                    strm.avail_in = input.size.convert()
                    while (true) {
                        val rc = chunk.usePinned { outPin ->
                            strm.next_out = outPin.addressOf(0).reinterpret()
                            strm.avail_out = chunk.size.convert()
                            inflate(strm.ptr, Z_NO_FLUSH)
                        }
                        val produced = chunk.size - strm.avail_out.toInt()
                        if (produced > 0) out.write(chunk, 0, produced)
                        when (rc) {
                            Z_STREAM_END -> return@usePinned
                            Z_OK -> if (strm.avail_in == 0u && produced == 0) throw okio.EOFException("Unexpected end of ZLIB input stream")
                            Z_BUF_ERROR -> if (strm.avail_in == 0u) throw okio.EOFException("Unexpected end of ZLIB input stream")
                            else -> throw okio.IOException(strm.msg?.toKString() ?: "zlib error $rc")
                        }
                    }
                }
            } finally {
                inflateEnd(strm.ptr)
            }
        }
        return out.readByteArray()
    }

    actual fun deflate(input: ByteArray): ByteArray {
        val bound = 64 + input.size + input.size / 1000 + 12
        val dest = ByteArray(bound)
        val written = memScoped {
            val destLen = alloc<ULongVar>().apply { value = bound.convert() }
            val rc = if (input.isEmpty()) {
                compress2(dest.refTo(0).getPointer(this).reinterpret(), destLen.ptr, null, 0u, Z_DEFAULT_COMPRESSION)
            } else input.usePinned { p ->
                compress2(dest.refTo(0).getPointer(this).reinterpret(), destLen.ptr, p.addressOf(0).reinterpret(), input.size.convert(), Z_DEFAULT_COMPRESSION)
            }
            check(rc == Z_OK) { "compress2 failed: $rc" }
            destLen.value.toInt()
        }
        return dest.copyOf(written)
    }
}
