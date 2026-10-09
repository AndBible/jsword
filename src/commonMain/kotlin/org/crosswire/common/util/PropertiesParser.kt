package org.crosswire.common.util

/** How the bytes of a properties file are decoded; each JSword consumer keeps the encoding it had. */
enum class PropertiesCharset {
    /**
     * `PropertyResourceBundle` (JDK 9+): UTF-8, re-read as ISO-8859-1 when the bytes are not valid UTF-8.
     * Lenient: any malformed UTF-8 makes the whole file be re-decoded as ISO-8859-1, also when a sequence is
     * truncated at the end of the file (where the JDK throws). Meant for fixed embedded resources only.
     */
    UTF8_THEN_LATIN1,
    /** `Properties.load(InputStream)`: ISO-8859-1. */
    LATIN1,
}

/** `java.util.Properties.load` semantics in common code (K2 F1). Later duplicates win. */
object PropertiesParser {
    fun decode(bytes: ByteArray, charset: PropertiesCharset): String = when (charset) {
        PropertiesCharset.LATIN1 -> bytes.latin1String()
        PropertiesCharset.UTF8_THEN_LATIN1 ->
            try { bytes.decodeToString(throwOnInvalidSequence = true) } catch (e: CharacterCodingException) { bytes.latin1String() }
    }

    fun parse(bytes: ByteArray, charset: PropertiesCharset): LinkedHashMap<String, String> = parse(decode(bytes, charset))

    fun parse(text: String): LinkedHashMap<String, String> {
        val result = LinkedHashMap<String, String>()
        for (line in logicalLines(text)) {
            var keyLen = 0
            var valueStart = line.length
            var hasSep = false
            var preceding = false
            while (keyLen < line.length) {
                val c = line[keyLen]
                if ((c == '=' || c == ':') && !preceding) { valueStart = keyLen + 1; hasSep = true; break }
                if (isWhite(c) && !preceding) { valueStart = keyLen + 1; break }
                preceding = c == '\\' && !preceding
                keyLen++
            }
            while (valueStart < line.length) {
                val c = line[valueStart]
                if (!isWhite(c)) {
                    if (!hasSep && (c == '=' || c == ':')) hasSep = true else break
                }
                valueStart++
            }
            result[unescape(line.substring(0, keyLen))] = unescape(line.substring(valueStart))
        }
        return result
    }

    private fun isWhite(c: Char) = c == ' ' || c == '\t' || c == '\u000C'

    /** Natural lines joined at an odd number of trailing backslashes; comments and blank lines dropped. */
    private fun logicalLines(text: String): List<String> {
        val out = ArrayList<String>()
        var i = 0
        val n = text.length
        while (i < n) {
            while (i < n && isWhite(text[i])) i++
            if (i >= n) break
            val first = text[i]
            if (first == '\n' || first == '\r') { i++; continue }
            if (first == '#' || first == '!') {
                while (i < n && text[i] != '\n' && text[i] != '\r') i++
                continue
            }
            val sb = StringBuilder()
            while (i < n) {
                val ch = text[i]
                if (ch == '\n' || ch == '\r') {
                    if (trailingBackslashes(sb) % 2 == 1) {
                        sb.setLength(sb.length - 1)
                        i++
                        if (ch == '\r' && i < n && text[i] == '\n') i++
                        while (i < n && isWhite(text[i])) i++
                        continue
                    }
                    break
                }
                sb.append(ch)
                i++
            }
            if (i >= n && trailingBackslashes(sb) % 2 == 1) sb.setLength(sb.length - 1)
            out.add(sb.toString())
        }
        return out
    }

    private fun trailingBackslashes(sb: StringBuilder): Int {
        var k = sb.length - 1
        var count = 0
        while (k >= 0 && sb[k] == '\\') { count++; k-- }
        return count
    }

    private fun unescape(s: String): String {
        if (s.indexOf('\\') < 0) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            var c = s[i++]
            if (c != '\\') { sb.append(c); continue }
            if (i >= s.length) break
            c = s[i++]
            if (c == 'u') {
                if (i + 4 > s.length) throw IllegalArgumentException("Malformed \\uxxxx encoding.")
                var value = 0
                repeat(4) {
                    val h = s[i++]
                    val d = when (h) {
                        in '0'..'9' -> h - '0'
                        in 'a'..'f' -> h - 'a' + 10
                        in 'A'..'F' -> h - 'A' + 10
                        else -> throw IllegalArgumentException("Malformed \\uxxxx encoding.")
                    }
                    value = (value shl 4) + d
                }
                sb.append(value.toChar())
            } else {
                sb.append(when (c) { 't' -> '\t'; 'r' -> '\r'; 'n' -> '\n'; 'f' -> '\u000C'; else -> c })
            }
        }
        return sb.toString()
    }
}
