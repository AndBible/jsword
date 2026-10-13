package org.crosswire.common.util

/** Latin-1 Properties text codec used by Native; JVM actuals retain java.util.Properties. */
internal object LegacyPropertyText {
    fun entries(bytes: ByteArray): List<Pair<String, String>> {
        val text = buildString { bytes.forEach { append((it.toInt() and 255).toChar()) } }
        val result = LegacyPropertyOrder()
        val logical = StringBuilder()
        var continuing = false
        var position = 0
        while (position < text.length) {
            val start = position
            while (position < text.length && text[position] != '\r' && text[position] != '\n') position++
            val line = text.substring(start, position)
            val delimiter = text.getOrNull(position)
            if (delimiter != null) position++
            val crlf = delimiter == '\r' && text.getOrNull(position) == '\n'
            if (crlf) position++
            val part = line.trimStart(' ', '\t', '\u000c')
            if (logical.isEmpty() && (part.isEmpty() || part[0] == '#' || part[0] == '!')) {
                continuing = false
                continue
            }
            logical.append(part)
            val slashes = logical.reversed().takeWhile { it == '\\' }.length
            if (slashes % 2 == 1) {
                logical.setLength(logical.length - 1)
                // LineReader consumes a CR-following LF before checking zero-length EOF.
                continuing = !(crlf && position == text.length && logical.isEmpty())
                continue
            }
            readLine(logical.toString(), result)
            logical.clear()
            continuing = false
        }
        if (logical.isNotEmpty() || continuing) readLine(logical.toString(), result)
        return result.entries()
    }

    private fun readLine(line: String, result: LegacyPropertyOrder) {
        var end = 0
        var escaped = false
        while (end < line.length) {
            val c = line[end]
            if (!escaped && (c == '=' || c == ':' || c == ' ' || c == '\t' || c == '\u000c')) break
            escaped = if (c == '\\') !escaped else false
            end++
        }
        var start = end
        while (start < line.length && line[start] in " \t\u000c") start++
        if (start < line.length && line[start] in "=:") start++
        while (start < line.length && line[start] in " \t\u000c") start++
        result.put(decode(line.substring(0, end)), decode(line.substring(start)))
    }

    private fun decode(text: String): String = buildString {
        var i = 0
        while (i < text.length) {
            val c = text[i++]
            if (c != '\\') { append(c); continue }
            if (i == text.length) break
            when (val escaped = text[i++]) {
                't' -> append('\t')
                'r' -> append('\r')
                'n' -> append('\n')
                'f' -> append('\u000c')
                'u' -> {
                    if (i + 4 > text.length) throw IllegalArgumentException("Malformed \\uxxxx encoding.")
                    var code = 0
                    repeat(4) {
                        val digit = text[i++].digitToIntOrNull(16)
                            ?: throw IllegalArgumentException("Malformed \\uxxxx encoding.")
                        code = code * 16 + digit
                    }
                    append(code.toChar())
                }
                else -> append(escaped)
            }
        }
    }

    fun bytes(map: Map<String, String>, comments: String?, timestamp: String): ByteArray {
        val text = buildString {
            if (comments != null) {
                append('#')
                var i = 0
                while (i < comments.length) {
                    val c = comments[i++]
                    if (c == '\r' || c == '\n') {
                        if (c == '\r' && comments.getOrNull(i) == '\n') i++
                        append('\n')
                        if (comments.getOrNull(i) != '#' && comments.getOrNull(i) != '!') append('#')
                    } else if (c.code > 255) append(unicode(c)) else append(c)
                }
                append('\n')
            }
            append('#'); append(timestamp); append('\n')
            for ((key, value) in LegacyPropertyOrder.storeEntries(map)) {
                append(encode(key, true)); append('='); append(encode(value, false)); append('\n')
            }
        }
        return ByteArray(text.length) { text[it].code.toByte() }
    }

    private fun unicode(c: Char) = "\\u" + c.code.toString(16).uppercase().padStart(4, '0')
    private fun encode(text: String, key: Boolean): String = buildString {
        text.forEachIndexed { index, c ->
            when (c) {
                ' ' -> { if (key || index == 0) append('\\'); append(' ') }
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\u000c' -> append("\\f")
                '=', ':', '#', '!' -> { append('\\'); append(c) }
                else -> if (c.code < 32 || c.code > 126) append(unicode(c)) else append(c)
            }
        }
    }
}
