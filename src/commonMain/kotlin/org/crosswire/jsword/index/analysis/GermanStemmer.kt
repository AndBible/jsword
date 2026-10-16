package org.crosswire.jsword.index.analysis

/**
 * Port of Lucene 3.6.2 `GermanStemmer` (the stemmer behind the baseline `GermanLuceneAnalyzer`).
 *
 * Reproduced on purpose, including the quirks that change results:
 * - `substCount =+ 2` in [substitute] is Lucene's typo for `+=`, so it assigns 2. It is kept as is.
 * - The `*` (doubled letter), `$`, `§`, `%`, `&`, `#` and `!` markers are the Lucene placeholders; they are resolved
 *   in [resubstitute] exactly as Lucene does, on the buffer as it has been changed by the earlier steps.
 * - The `Character.isLetter` check and the stemming steps work on UTF-16 units, as they do in Lucene.
 *
 * Not thread-safe: one instance is reused by one analyzer.
 */
internal class GermanStemmer {
    private val sb = StringBuilder()
    private var substCount = 0

    /** Stems [term], which the tokenizer has already lowercased. Non-letter terms are returned unchanged. */
    fun stem(term: String): String {
        if (!isStemmable(term)) return term
        sb.clear()
        sb.append(term)
        substitute(sb)
        strip(sb)
        optimize(sb)
        resubstitute(sb)
        removeParticleDenotion(sb)
        return sb.toString()
    }

    private fun isStemmable(term: String): Boolean = term.all { AnalyzerTables.isLetter(it) }

    private fun strip(buffer: StringBuilder) {
        var doMore = true
        while (doMore && buffer.length > 3) {
            val last = buffer[buffer.length - 1]
            if (buffer.length + substCount > 5 && buffer.endsWith("nd")) {
                buffer.deleteRange(buffer.length - 2, buffer.length)
            } else if (buffer.length + substCount > 4 && buffer.endsWith("em")) {
                buffer.deleteRange(buffer.length - 2, buffer.length)
            } else if (buffer.length + substCount > 4 && buffer.endsWith("er")) {
                buffer.deleteRange(buffer.length - 2, buffer.length)
            } else if (last == 'e' || last == 's' || last == 'n' || last == 't') {
                // "t" occurs only as suffix of verbs.
                buffer.deleteAt(buffer.length - 1)
            } else {
                doMore = false
            }
        }
    }

    private fun optimize(buffer: StringBuilder) {
        // Additional step for female plurals of professions and inhabitants.
        if (buffer.length > 5 && buffer.endsWith("erin*")) {
            buffer.deleteAt(buffer.length - 1)
            strip(buffer)
        }
        // Additional step for irregular plural nouns like "Matrizen -> Matrix".
        if (buffer.length > 0 && buffer[buffer.length - 1] == 'z') {
            buffer[buffer.length - 1] = 'x'
        }
    }

    private fun removeParticleDenotion(buffer: StringBuilder) {
        if (buffer.length > 4) {
            for (c in 0 until buffer.length - 3) {
                if (buffer.substring(c, c + 4) == "gege") {
                    buffer.deleteRange(c, c + 2)
                    return
                }
            }
        }
    }

    private fun substitute(buffer: StringBuilder) {
        substCount = 0
        var c = 0
        while (c < buffer.length) {
            // Replace the second char of a pair of the equal characters with an asterisk.
            if (c > 0 && buffer[c] == buffer[c - 1]) {
                buffer[c] = '*'
            } else if (buffer[c] == UMLAUT_A) {
                buffer[c] = 'a'
            } else if (buffer[c] == UMLAUT_O) {
                buffer[c] = 'o'
            } else if (buffer[c] == UMLAUT_U) {
                buffer[c] = 'u'
            } else if (buffer[c] == SHARP_S) {
                // 'ß' becomes "ss", which lengthens the buffer.
                buffer[c] = 's'
                buffer.insert(c + 1, "s")
                substCount++
            }
            // Take care that at least one character is left at the left side of the current one.
            if (c < buffer.length - 1) {
                if (c < buffer.length - 2 && buffer[c] == 's' && buffer[c + 1] == 'c' && buffer[c + 2] == 'h') {
                    buffer[c] = '$'
                    buffer.deleteRange(c + 1, c + 3)
                    substCount = 2 // sic: Lucene writes `substCount =+ 2`
                } else if (buffer[c] == 'c' && buffer[c + 1] == 'h') {
                    buffer[c] = SECTION_SIGN
                    buffer.deleteAt(c + 1)
                    substCount++
                } else if (buffer[c] == 'e' && buffer[c + 1] == 'i') {
                    buffer[c] = '%'
                    buffer.deleteAt(c + 1)
                    substCount++
                } else if (buffer[c] == 'i' && buffer[c + 1] == 'e') {
                    buffer[c] = '&'
                    buffer.deleteAt(c + 1)
                    substCount++
                } else if (buffer[c] == 'i' && buffer[c + 1] == 'g') {
                    buffer[c] = '#'
                    buffer.deleteAt(c + 1)
                    substCount++
                } else if (buffer[c] == 's' && buffer[c + 1] == 't') {
                    buffer[c] = '!'
                    buffer.deleteAt(c + 1)
                    substCount++
                }
            }
            c++
        }
    }

    private fun resubstitute(buffer: StringBuilder) {
        var c = 0
        while (c < buffer.length) {
            when (buffer[c]) {
                '*' -> buffer[c] = buffer[c - 1]
                '$' -> {
                    buffer[c] = 's'
                    buffer.insert(c + 1, "ch")
                }
                SECTION_SIGN -> {
                    buffer[c] = 'c'
                    buffer.insert(c + 1, "h")
                }
                '%' -> {
                    buffer[c] = 'e'
                    buffer.insert(c + 1, "i")
                }
                '&' -> {
                    buffer[c] = 'i'
                    buffer.insert(c + 1, "e")
                }
                '#' -> {
                    buffer[c] = 'i'
                    buffer.insert(c + 1, "g")
                }
                '!' -> {
                    buffer[c] = 's'
                    buffer.insert(c + 1, "t")
                }
            }
            c++
        }
    }

    private companion object {
        const val UMLAUT_A = 'ä'
        const val UMLAUT_O = 'ö'
        const val UMLAUT_U = 'ü'
        const val SHARP_S = 'ß'
        const val SECTION_SIGN = '§'
    }
}

private fun StringBuilder.endsWith(suffix: String): Boolean =
    length >= suffix.length && substring(length - suffix.length) == suffix
