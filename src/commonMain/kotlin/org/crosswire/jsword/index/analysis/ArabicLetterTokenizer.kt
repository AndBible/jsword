package org.crosswire.jsword.index.analysis

/**
 * Lucene 3.6.2 `ArabicLetterTokenizer` (a `LetterTokenizer` as the baseline builds it, `Version.LUCENE_29`): a token
 * char is a letter or a non-spacing mark (`Character.NON_SPACING_MARK`), so harakat stay inside words until
 * [ArabicNormalizer] removes them. Everything else separates tokens, ZWNJ included.
 *
 * Works on UTF-16 units like [LowerCaseLetterTokenizer], with the same 255-unit token cut. Nothing is lowercased here:
 * the baseline applies `LowerCaseFilter` after this tokenizer. Positions are consecutive.
 */
internal object ArabicLetterTokenizer {
    /** Lucene's `CharTokenizer.MAX_WORD_LEN`, in UTF-16 units. */
    private const val MAX_TOKEN_LENGTH = 255

    /** Java `Character.NON_SPACING_MARK` bit of [AnalyzerTables.characterType]. */
    private const val NON_SPACING_MARK = 0x01

    fun tokenize(text: String): List<AnalyzedToken> {
        val out = ArrayList<AnalyzedToken>()
        val current = StringBuilder()
        fun flush() {
            if (current.isEmpty()) return
            out.add(AnalyzedToken(current.toString(), out.size))
            current.clear()
        }
        for (c in text) {
            if (isTokenChar(c)) {
                current.append(c)
                if (current.length == MAX_TOKEN_LENGTH) flush()
            } else {
                flush()
            }
        }
        flush()
        return out
    }

    private fun isTokenChar(c: Char): Boolean =
        AnalyzerTables.isLetter(c) || (AnalyzerTables.characterType(c) and NON_SPACING_MARK) != 0
}
