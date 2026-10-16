package org.crosswire.jsword.index.fts

/**
 * Codec `utf8-hex-v1`: a search token becomes `x` followed by the lowercase hex of its UTF-8 bytes, so every
 * stored term is plain ASCII letters/digits and survives FTS5's `ascii` tokenizer (which folds only ASCII case)
 * unchanged, whatever the script. The empty term (Arabic/Persian tokens made only of marks normalise to "")
 * encodes as just `x`. Two reserved strings never collide with an encoded token: the gap token `g` (see
 * [FtsIndexStore]) because it does not start with `x`.
 *
 * Unpaired (lone) surrogates cannot be UTF-8 encoded; [encode] replaces each with U+FFFD before encoding, so the
 * result is identical on every platform (the platform encoders differ: JVM gives '?', Kotlin/Native U+FFFD).
 * That is not reversible, but index and query sides both call [encode], so they stay consistent.
 */
internal object SearchTokenCodec {
    const val ID = "utf8-hex-v1"

    private const val HEX = "0123456789abcdef"

    fun encode(token: String): String {
        val bytes = replaceLoneSurrogates(token).encodeToByteArray()
        val sb = StringBuilder(1 + bytes.size * 2).append('x')
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4]).append(HEX[v and 0xF])
        }
        return sb.toString()
    }

    private fun replaceLoneSurrogates(s: String): String {
        var sb: StringBuilder? = null
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val valid = when {
                c.isHighSurrogate() -> i + 1 < s.length && s[i + 1].isLowSurrogate().also { if (it) { sb?.append(c)?.append(s[i + 1]); i++ } }
                c.isLowSurrogate() -> false
                else -> true.also { sb?.append(c) }
            }
            if (!valid) {
                if (sb == null) sb = StringBuilder(s.length).append(s, 0, i)
                sb.append('\uFFFD')
            }
            i++
        }
        return sb?.toString() ?: s
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
