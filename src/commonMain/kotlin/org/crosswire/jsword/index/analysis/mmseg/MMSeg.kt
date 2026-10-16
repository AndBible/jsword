package org.crosswire.jsword.index.analysis.mmseg

import org.crosswire.jsword.index.analysis.AnalyzerTables

/**
 * mmseg4j 1.8.6 `MMSeg`: the character-class tokenizer around [ComplexSeg]. English, Russian and Greek letter runs,
 * digit runs (with a following unit character split off), Roman/other-number runs and CJK runs each become tokens.
 * Full-width Latin letters and digits are narrowed to ASCII and everything is lowercased with Java's tables.
 * Characters that start no run (punctuation, spaces, unknown letters) are skipped. Works on UTF-16 units like the Java.
 */
internal class MMSeg(private val dictionary: Dictionary) {
    private val complex = ComplexSeg(dictionary)

    /** The tokens of [text] in order; only local buffers are used. */
    fun tokens(text: String): List<String> {
        val out = ArrayList<String>()
        val source = text
        var index = 0
        while (index < source.length) {
            var unit = lower(source[index])
            index++
            if (has(unit, CASED)) {
                unit = ascii(unit)
                val kind = nation(unit)
                if (kind == UNKNOWN) continue
                val buffer = StringBuilder().append(unit)
                while (index < source.length) {
                    val next = if (kind == ENGLISH) ascii(lower(source[index])) else lower(source[index])
                    val ok = when (kind) {
                        ENGLISH -> isAsciiLetter(next) || has(next, DECIMAL)
                        RUSSIAN -> isRussian(next)
                        else -> isGreek(next)
                    }
                    if (!ok) break
                    buffer.append(next)
                    index++
                }
                out.add(buffer.toString())
            } else if (has(unit, OTHER_LETTER)) {
                val sentence = StringBuilder().append(unit)
                while (index < source.length) {
                    val next = lower(source[index])
                    if (!has(next, OTHER_LETTER)) break
                    sentence.append(next)
                    index++
                }
                out.addAll(complex.segment(sentence.toString().toCharArray()))
            } else if (has(unit, DECIMAL)) {
                val buffer = StringBuilder().append(ascii(unit))
                while (index < source.length) {
                    val next = ascii(lower(source[index]))
                    if (!has(next, DECIMAL)) break
                    buffer.append(next)
                    index++
                }
                if (index < source.length) {
                    val next = lower(source[index])
                    if (dictionary.isUnit(next)) {
                        out.add(buffer.toString())
                        out.add(next.toString())
                        index++
                        continue
                    }
                }
                while (index < source.length) {
                    val next = ascii(lower(source[index]))
                    if (!(isAsciiLetter(next) || has(next, DECIMAL))) break
                    buffer.append(next)
                    index++
                }
                out.add(buffer.toString())
            } else if (has(unit, LETTER_NUMBER)) {
                out.add(unit.toString())
            } else if (has(unit, OTHER_NUMBER)) {
                val buffer = StringBuilder().append(unit)
                while (index < source.length) {
                    val next = lower(source[index])
                    if (!has(next, OTHER_NUMBER)) break
                    buffer.append(next)
                    index++
                }
                out.add(buffer.toString())
            }
        }
        return out
    }

    private fun lower(c: Char) = AnalyzerTables.lowerCase(c)

    private fun has(c: Char, bit: Int) = AnalyzerTables.characterType(c) and bit != 0

    /** mmseg4j `CharUtils.toAscii`: full-width digits and Latin letters to ASCII. */
    private fun ascii(c: Char): Char = when (c) {
        in '０'..'９', in 'Ａ'..'Ｚ', in 'ａ'..'ｚ' -> c - 0xFEE0
        else -> c
    }

    private fun nation(c: Char) = when {
        isAsciiLetter(c) -> ENGLISH
        isRussian(c) -> RUSSIAN
        isGreek(c) -> GREEK
        else -> UNKNOWN
    }

    private fun isAsciiLetter(c: Char) = c in 'A'..'Z' || c in 'a'..'z'

    private fun isRussian(c: Char) = c in 'А'..'я' || c == 'Ё' || c == 'ё'

    private fun isGreek(c: Char) = c in 'Α'..'Ω' || c in 'α'..'ω'

    private companion object {
        // Bits of AnalyzerTables.characterType.
        const val DECIMAL = 0x02
        const val LETTER_NUMBER = 0x04
        const val OTHER_NUMBER = 0x08
        const val OTHER_LETTER = 0x10
        const val CASED = 0x20
        const val ENGLISH = 0
        const val RUSSIAN = 1
        const val GREEK = 2
        const val UNKNOWN = 3
    }
}
