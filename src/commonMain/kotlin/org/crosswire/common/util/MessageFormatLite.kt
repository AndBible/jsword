package org.crosswire.common.util

import kotlin.math.nextUp

/**
 * The subset of `java.text.MessageFormat` JSword's messages use (K2 F1): literal text with `'` quoting,
 * `{n}`, `{n,number}`, `{n,number,integer}`, `{n,number,<DecimalFormat pattern>}` and `{n,choice,<ChoiceFormat pattern>}`.
 * `date`, `time`, `currency` and `percent` throw [UnsupportedOperationException].
 *
 * The pattern is parsed completely before formatting (as `MessageFormat.applyPattern` does), so malformed
 * patterns fail with the same exception class whatever the arguments are. Differentially tested against the JDK.
 */
object MessageFormatLite {
    fun format(pattern: String, locale: KLocale, args: Array<out Any?>): String {
        val elements = parse(pattern, locale)
        val out = StringBuilder()
        for (e in elements) {
            when (e) {
                is Literal -> out.append(e.text)
                is Argument -> out.append(formatArgument(e, locale, args))
            }
        }
        return out.toString()
    }

    private sealed class Element
    private class Literal(val text: String) : Element()
    private class Argument(val index: Int, val kind: Kind, val limits: DoubleArray = DoubleArray(0), val texts: List<String> = emptyList(),
                           val style: String = "") : Element()
    private enum class Kind { PLAIN, NUMBER, INTEGER, PATTERN, CHOICE }

    private const val RAW = 0
    private const val INDEX = 1
    private const val TYPE = 2
    private const val MODIFIER = 3

    /** Port of `MessageFormat.applyPattern`. */
    private fun parse(pattern: String, locale: KLocale): List<Element> {
        val elements = ArrayList<Element>()
        val segments = arrayOf(StringBuilder(), StringBuilder(), StringBuilder(), StringBuilder())
        var part = RAW
        var braceStack = 0
        var inQuote = false
        var i = 0
        while (i < pattern.length) {
            val ch = pattern[i]
            if (part == RAW) {
                if (ch == '\'') {
                    if (i + 1 < pattern.length && pattern[i + 1] == '\'') { segments[RAW].append(ch); i++ } else inQuote = !inQuote
                } else if (ch == '{' && !inQuote) {
                    part = INDEX
                    if (segments[RAW].isNotEmpty()) { elements.add(Literal(segments[RAW].toString())); segments[RAW].setLength(0) }
                } else segments[RAW].append(ch)
            } else if (inQuote) {
                segments[part].append(ch)
                if (ch == '\'') inQuote = false
            } else when (ch) {
                ',' -> if (part < MODIFIER) { part++; segments[part].setLength(0) } else segments[part].append(ch)
                '{' -> { braceStack++; segments[part].append(ch) }
                '}' -> if (braceStack == 0) {
                    part = RAW
                    elements.add(makeArgument(segments[INDEX].toString(), segments[TYPE].toString(), segments[MODIFIER].toString(), locale))
                    segments[INDEX].setLength(0); segments[TYPE].setLength(0); segments[MODIFIER].setLength(0)
                } else { braceStack--; segments[part].append(ch) }
                ' ' -> if (part != TYPE || segments[part].isNotEmpty()) segments[part].append(ch)
                '\'' -> { inQuote = true; segments[part].append(ch) }
                else -> segments[part].append(ch)
            }
            i++
        }
        if (braceStack == 0 && part != RAW) throw IllegalArgumentException("Unmatched braces in the pattern.")
        if (segments[RAW].isNotEmpty()) elements.add(Literal(segments[RAW].toString()))
        return elements
    }

    private fun makeArgument(indexText: String, typeText: String, styleText: String, locale: KLocale): Argument {
        val index = indexText.toIntOrNull() ?: throw IllegalArgumentException("can't parse argument number: $indexText")
        require(index >= 0) { "negative argument number: $indexText" }
        return when (keyword(typeText, listOf("", "number", "date", "time", "choice"))) {
            0 -> Argument(index, Kind.PLAIN)
            1 -> when (keyword(styleText, listOf("", "currency", "percent", "integer"))) {
                0 -> Argument(index, Kind.NUMBER)
                1, 2 -> throw UnsupportedOperationException("number,$styleText")
                3 -> Argument(index, Kind.INTEGER)
                else -> {
                    NumberFormats.pattern(0, styleText, locale) // fail now, like `new DecimalFormat(style)`
                    Argument(index, Kind.PATTERN, style = styleText)
                }
            }
            2, 3 -> throw UnsupportedOperationException(typeText.trim())
            4 -> parseChoice(index, styleText)
            else -> throw IllegalArgumentException("unknown format type: $typeText")
        }
    }

    private fun keyword(s: String, list: List<String>): Int {
        val exact = list.indexOf(s)
        if (exact >= 0) return exact
        return list.indexOf(s.trim().lowercase())
    }

    /** Port of `ChoiceFormat.applyPattern` (infinity limits are not supported; none of our resources use them). */
    private fun parseChoice(index: Int, pattern: String): Argument {
        val limits = ArrayList<Double>()
        val texts = ArrayList<String>()
        val number = StringBuilder()
        val text = StringBuilder()
        var part = 0
        var oldStart = Double.NEGATIVE_INFINITY
        var inQuote = false
        var i = 0
        while (i < pattern.length) {
            val ch = pattern[i]
            if (ch == '\'') {
                if (i + 1 < pattern.length && pattern[i + 1] == ch) { (if (part == 0) number else text).append(ch); i++ } else inQuote = !inQuote
            } else if (inQuote) (if (part == 0) number else text).append(ch)
            else if (ch == '<' || ch == '#' || ch == '≤') {
                require(number.isNotEmpty()) { "Each interval must contain a number before a format" }
                var start = number.toString().trim().toDoubleOrNull()
                    ?: throw IllegalArgumentException("Each interval must contain a number before a format")
                if (ch == '<') start = start.nextUp()
                require(start > oldStart) { "Incorrect order of intervals, must be in ascending order" }
                oldStart = start
                number.setLength(0)
                limits.add(start)
                part = 1
            } else if (ch == '|') {
                if (part == 1) { texts.add(text.toString()); text.setLength(0) }
                part = 0
            } else (if (part == 0) number else text).append(ch)
            i++
        }
        if (part == 1) texts.add(text.toString())
        return Argument(index, Kind.CHOICE, limits.toDoubleArray(), texts)
    }

    private fun formatArgument(a: Argument, locale: KLocale, args: Array<out Any?>): String {
        if (a.index >= args.size) return "{${a.index}}"
        val arg = args[a.index] ?: return "null"
        return when (a.kind) {
            Kind.PLAIN -> when (arg) {
                is Number -> NumberFormats.general(arg, locale)
                else -> arg.toString()
            }
            Kind.NUMBER -> NumberFormats.general(asNumber(arg), locale)
            Kind.INTEGER -> NumberFormats.integer(asNumber(arg), locale)
            Kind.PATTERN -> NumberFormats.pattern(asNumber(arg), a.style, locale)
            Kind.CHOICE -> {
                val text = choose(a, asNumber(arg).toDouble())
                if (text.contains('{')) format(text, locale, args) else text
            }
        }
    }

    private fun asNumber(arg: Any): Number = arg as? Number ?: throw IllegalArgumentException("Cannot format given Object as a Number")

    /** `ChoiceFormat.format(double)`: stops at the first limit the value does not reach; NaN picks the first. */
    private fun choose(a: Argument, value: Double): String {
        var i = 0
        while (i < a.limits.size) {
            if (!(value >= a.limits[i])) break
            i++
        }
        i--
        if (i < 0) i = 0
        return a.texts[i] // empty choice: IndexOutOfBoundsException (java.text: ArrayIndexOutOfBoundsException)
    }
}
