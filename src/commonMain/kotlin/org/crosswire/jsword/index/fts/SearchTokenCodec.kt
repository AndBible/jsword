package org.crosswire.jsword.index.fts

/**
 * Codec `utf8-hex-v1`: a search token becomes `x` followed by the lowercase hex of its UTF-8 bytes, so every
 * stored term is plain ASCII letters/digits and survives FTS5's `ascii` tokenizer (which folds only ASCII case)
 * unchanged, whatever the script. The empty term (Arabic/Persian tokens made only of marks normalise to "")
 * encodes as just `x`. Two reserved strings never collide with an encoded token: the gap token `g` (see
 * [FtsIndexStore]) because it does not start with `x`.
 */
object SearchTokenCodec {
    // Lone surrogates: encodeToByteArray() turns an unpaired surrogate into '?' (0x3F), so such a token encodes as
    // if it contained '?'. Not reversible, but index and query sides call the same encode, so they stay consistent.
    const val ID = "utf8-hex-v1"

    private const val HEX = "0123456789abcdef"

    fun encode(token: String): String {
        val bytes = token.encodeToByteArray()
        val sb = StringBuilder(1 + bytes.size * 2).append('x')
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4]).append(HEX[v and 0xF])
        }
        return sb.toString()
    }

    /** The token for [encoded], or null when it is not a valid `utf8-hex-v1` string (no `x`, odd length, non lowercase-hex, bad UTF-8). */
    fun decode(encoded: String): String? {
        if (encoded.isEmpty() || encoded[0] != 'x' || (encoded.length - 1) % 2 != 0) return null
        val bytes = ByteArray((encoded.length - 1) / 2)
        for (i in bytes.indices) {
            val hi = nibble(encoded[1 + 2 * i])
            val lo = nibble(encoded[2 + 2 * i])
            if (hi < 0 || lo < 0) return null
            bytes[i] = ((hi shl 4) or lo).toByte()
        }
        val text = bytes.decodeToString()
        // decodeToString replaces malformed input; a strict round trip rejects it.
        return if (text.encodeToByteArray().contentEquals(bytes)) text else null
    }

    private fun nibble(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        else -> -1
    }
}
