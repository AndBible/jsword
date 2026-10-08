package org.crosswire.common.util

/** ISO-8859-1 decoding, as `String(bytes, ISO_8859_1)`: every byte maps to U+0000..U+00FF. */
fun ByteArray.latin1String(): String = buildString(size) {
    for (b in this@latin1String) append((b.toInt() and 0xFF).toChar())
}

/**
 * ISO-8859-1 encoding, as `getBytes(ISO_8859_1)`: chars above U+00FF become '?' (0x3F). A valid surrogate
 * pair is one code point, so it becomes a single '?' (as the JDK encoder); a lone surrogate is also one '?'.
 */
fun String.latin1Bytes(): ByteArray {
    val out = ByteArray(length)
    var n = 0
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c.code <= 0xFF) {
            out[n++] = c.code.toByte()
        } else {
            out[n++] = '?'.code.toByte()
            if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) i++
        }
        i++
    }
    return if (n == out.size) out else out.copyOf(n)
}

/**
 * `String.split(regex)` / `Pattern.split(input)` semantics: `[input]` when nothing matches, trailing
 * empty strings removed. Not for zero-width patterns (Java's leading zero-width rule is not mirrored).
 */
fun CharSequence.splitLikeJava(regex: Regex): List<String> {
    if (!regex.containsMatchIn(this)) return listOf(toString())
    return split(regex).dropLastWhile { it.isEmpty() }
}
