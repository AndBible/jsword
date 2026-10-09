package org.crosswire.common.compress

/** zlib (RFC 1950) inflate/deflate over byte arrays. The JVM actual is `java.util.zip`, iOS uses `platform.zlib`. */
expect object Zlib {
    /**
     * Inflates a zlib stream like `InflaterInputStream(.., Inflater(), expectedSize)`: data after the stream end is
     * ignored; a truncated stream throws okio.IOException ("Unexpected end of ZLIB input stream");
     * expectedSize <= 0 throws IllegalArgumentException.
     *
     * Known platform difference: a stream that needs a preset dictionary (Z_NEED_DICT) makes iOS throw
     * okio.IOException, while the JDK's `Inflater` stops and the stream wrapper returns the partial output.
     * Such streams do not occur in SWORD modules.
     */
    fun inflate(input: ByteArray, expectedSize: Int): ByteArray

    /** Deflates like `DeflaterOutputStream(.., Deflater())` (default level, zlib wrapper). */
    fun deflate(input: ByteArray): ByteArray
}
