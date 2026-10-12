package org.crosswire.common.util

class UnsupportedCharsetException(val charsetName: String) : okio.IOException("Unsupported charset: $charsetName")

/** The three module encodings, with JDK 17 replacement semantics on every platform. */
object KCharset {
    fun canonicalName(name: String): String? = when (name.uppercase()) {
        "UTF-8", "UTF8" -> "UTF-8"
        "ISO-8859-1", "ISO8859_1", "LATIN1", "8859_1" -> "ISO-8859-1"
        "WINDOWS-1252", "CP1252" -> "windows-1252"
        else -> null
    }

    private fun requireCharset(name: String?): String {
        if (name == null) throw NullPointerException()
        return canonicalName(name) ?: throw UnsupportedCharsetException(name)
    }

    fun decode(bytes: ByteArray, offset: Int, length: Int, charsetName: String?): String {
        val charset = requireCharset(charsetName)
        if (offset < 0 || length < 0 || offset > bytes.size - length) throw IndexOutOfBoundsException()
        val end = offset + length
        if (charset == "UTF-8") return decodeUtf8(bytes, offset, end)
        return buildString(length) {
            for (i in offset until end) {
                val b = bytes[i].toInt() and 0xff
                append(if (charset == "windows-1252" && b in 0x80..0x9f) cp1252[b - 0x80] else b.toChar())
            }
        }
    }

    private fun continuation(b: Int) = b in 0x80..0xbf

    /** Consume exactly the malformed length used by sun.nio.cs.UTF_8, including ED surrogates. */
    private fun decodeUtf8(bytes: ByteArray, offset: Int, end: Int): String = buildString(end - offset) {
        var i = offset
        fun byte(at: Int) = bytes[at].toInt() and 0xff
        while (i < end) {
            val b1 = byte(i)
            if (b1 < 0x80) { append(b1.toChar()); i++; continue }
            val width = when (b1) {
                in 0xc2..0xdf -> 2
                in 0xe0..0xef -> 3
                in 0xf0..0xf4 -> 4
                else -> 1
            }
            if (width == 1) { append('\ufffd'); i++; continue }
            if (i + 1 == end) { append('\ufffd'); i++; continue }
            val b2 = byte(i + 1)
            if (!continuation(b2) || (b1 == 0xe0 && b2 < 0xa0) ||
                (b1 == 0xf0 && b2 < 0x90) || (b1 == 0xf4 && b2 > 0x8f)) {
                append('\ufffd'); i++; continue
            }
            var consumed = 2
            while (consumed < width && i + consumed < end && continuation(byte(i + consumed))) consumed++
            if (consumed != width) { append('\ufffd'); i += consumed; continue }
            val code = when (width) {
                2 -> ((b1 and 0x1f) shl 6) or (b2 and 0x3f)
                3 -> ((b1 and 0x0f) shl 12) or ((b2 and 0x3f) shl 6) or (byte(i + 2) and 0x3f)
                else -> ((b1 and 7) shl 18) or ((b2 and 0x3f) shl 12) or
                    ((byte(i + 2) and 0x3f) shl 6) or (byte(i + 3) and 0x3f)
            }
            // Unlike some Unicode decoders, Java replaces a complete encoded surrogate once.
            if (code in 0xd800..0xdfff) append('\ufffd')
            else if (code < 0x10000) append(code.toChar())
            else {
                append((0xd800 + ((code - 0x10000) shr 10)).toChar())
                append((0xdc00 + ((code - 0x10000) and 0x3ff)).toChar())
            }
            i += width
        }
    }

    fun encode(s: String, charsetName: String?): ByteArray {
        val charset = requireCharset(charsetName)
        val out = okio.Buffer()
        var i = 0
        while (i < s.length) {
            var code = s[i++].code
            if (code in 0xd800..0xdbff && i < s.length && s[i].code in 0xdc00..0xdfff) {
                code = 0x10000 + ((code - 0xd800) shl 10) + (s[i++].code - 0xdc00)
            } else if (code in 0xd800..0xdfff) code = 0x3f
            if (charset != "UTF-8") {
                val value = if (charset == "ISO-8859-1") {
                    if (code <= 0xff) code else 0x3f
                } else {
                    when {
                        code < 0x80 || code in 0xa0..0xff -> code
                        code > 0xffff || code == 0xfffd -> 0x3f
                        else -> cp1252.indexOf(code.toChar()).let { if (it < 0) 0x3f else it + 0x80 }
                    }
                }
                out.writeByte(value)
            } else when {
                code < 0x80 -> out.writeByte(code)
                code < 0x800 -> {
                    out.writeByte(0xc0 or (code shr 6)); out.writeByte(0x80 or (code and 0x3f))
                }
                code < 0x10000 -> {
                    out.writeByte(0xe0 or (code shr 12)); out.writeByte(0x80 or ((code shr 6) and 0x3f))
                    out.writeByte(0x80 or (code and 0x3f))
                }
                else -> {
                    out.writeByte(0xf0 or (code shr 18)); out.writeByte(0x80 or ((code shr 12) and 0x3f))
                    out.writeByte(0x80 or ((code shr 6) and 0x3f)); out.writeByte(0x80 or (code and 0x3f))
                }
            }
        }
        return out.readByteArray()
    }

    private const val cp1252 = "\u20ac\ufffd\u201a\u0192\u201e\u2026\u2020\u2021\u02c6\u2030\u0160\u2039\u0152\ufffd\u017d\ufffd" +
        "\ufffd\u2018\u2019\u201c\u201d\u2022\u2013\u2014\u02dc\u2122\u0161\u203a\u0153\ufffd\u017e\u0178"
}
