package org.crosswire.jsword.index.analysis

/**
 * Lucene 3.6.2 `LowerCaseTokenizer` (a `CharTokenizer` with `isTokenChar = isLetter` and per-unit lowercasing).
 *
 * Reproduced on purpose:
 * - Tokens are maximal runs of letters as [AnalyzerTables.isLetter] defines them, working on UTF-16 units. A
 *   supplementary character is two surrogate units, neither a letter, so it separates tokens.
 * - A run is cut every [MAX_TOKEN_LENGTH] units, as `CharTokenizer` does at `MAX_WORD_LEN`, so the rest of
 *   the run becomes the next token.
 * - Each unit is lowercased with [AnalyzerTables.lowerCase] (`Character.toLowerCase(char)`), never with a
 *   locale-sensitive or multi-character mapping.
 */
internal object LowerCaseLetterTokenizer {
    /** Lucene's `CharTokenizer.MAX_WORD_LEN`, in UTF-16 units. */
    const val MAX_TOKEN_LENGTH = 255

    /** Tokenizes [text] without any folding; positions are consecutive (every increment is 1). */
    fun tokenize(text: String): List<AnalyzedToken> {
        val out = ArrayList<AnalyzedToken>()
        val current = StringBuilder()
        fun flush() {
            if (current.isEmpty()) return
            out.add(AnalyzedToken(current.toString(), out.size))
            current.clear()
        }
        for (c in text) {
            if (AnalyzerTables.isLetter(c)) {
                current.append(AnalyzerTables.lowerCase(c))
                if (current.length == MAX_TOKEN_LENGTH) flush()
            } else {
                flush()
            }
        }
        flush()
        return out
    }
}
