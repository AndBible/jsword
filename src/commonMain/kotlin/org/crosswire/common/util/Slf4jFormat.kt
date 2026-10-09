package org.crosswire.common.util

/** Port of slf4j 1.7.6 `MessageFormatter.arrayFormat` (MIT licence), so iOS logs read like Android's. */
internal object Slf4jFormat {
    class Result(val message: String?, val throwable: Throwable?)

    /**
     * Formats like the JVM dispatch does: a lone [Throwable] argument selects slf4j's `(String, Throwable)`
     * overload, so the message is kept as-is (no placeholder substitution) and the throwable is attached.
     * Every other argument shape goes through [format].
     */
    fun formatForLog(pattern: String?, args: Array<out Any?>): Result {
        val only = args.singleOrNull()
        return if (only is Throwable) Result(pattern, only) else format(pattern, args)
    }

    fun format(pattern: String?, args: Array<out Any?>): Result {
        val candidate = args.lastOrNull() as? Throwable
        if (pattern == null) return Result(null, candidate)
        var i = 0
        val sb = StringBuilder(pattern.length + 50)
        var l = 0
        while (l < args.size) {
            val j = pattern.indexOf("{}", i)
            if (j == -1) {
                if (i == 0) return Result(pattern, candidate)
                sb.append(pattern, i, pattern.length)
                return Result(sb.toString(), candidate)
            }
            if (j != 0 && pattern[j - 1] == '\\') {
                if (!(j >= 2 && pattern[j - 2] == '\\')) {
                    // The delimiter was escaped: emit "{" and do not consume an argument.
                    l--
                    sb.append(pattern, i, j - 1)
                    sb.append('{')
                    i = j + 1
                } else {
                    // The escape character itself is escaped: consume one backslash and format the argument.
                    sb.append(pattern, i, j - 1)
                    appendParameter(sb, args[l], ArrayList())
                    i = j + 2
                }
            } else {
                sb.append(pattern, i, j)
                appendParameter(sb, args[l], ArrayList())
                i = j + 2
            }
            l++
        }
        sb.append(pattern, i, pattern.length)
        return Result(sb.toString(), if (l < args.size - 1) candidate else null)
    }

    /** [seen] holds the arrays on the current path, compared by identity (slf4j's self-reference guard). */
    private fun appendParameter(sb: StringBuilder, o: Any?, seen: MutableList<Any>) {
        when (o) {
            null -> sb.append("null")
            is BooleanArray -> sb.append(o.joinToString(", ", "[", "]"))
            is ByteArray -> sb.append(o.joinToString(", ", "[", "]"))
            is CharArray -> sb.append(o.joinToString(", ", "[", "]"))
            is ShortArray -> sb.append(o.joinToString(", ", "[", "]"))
            is IntArray -> sb.append(o.joinToString(", ", "[", "]"))
            is LongArray -> sb.append(o.joinToString(", ", "[", "]"))
            is FloatArray -> sb.append(o.joinToString(", ", "[", "]"))
            is DoubleArray -> sb.append(o.joinToString(", ", "[", "]"))
            is Array<*> -> {
                sb.append('[')
                if (seen.none { it === o }) {
                    seen.add(o)
                    o.forEachIndexed { idx, e ->
                        appendParameter(sb, e, seen)
                        if (idx != o.size - 1) sb.append(", ")
                    }
                    seen.removeAt(seen.lastIndex)
                } else {
                    sb.append("...")
                }
                sb.append(']')
            }
            else -> sb.append(try { o.toString() } catch (t: Throwable) { "[FAILED toString()]" })
        }
    }
}
