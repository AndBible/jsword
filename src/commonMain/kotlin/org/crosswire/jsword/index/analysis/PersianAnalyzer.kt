package org.crosswire.jsword.index.analysis

/**
 * Baseline `PersianLuceneAnalyzer` with stop words off: [ArabicLetterTokenizer], `LowerCaseFilter`,
 * `ArabicNormalizationFilter`, then `PersianNormalizationFilter`. There is no stemmer, even with stemming on, because
 * the baseline Persian analyzer has none. Tokens emptied by the normalizers are kept. Positions are consecutive.
 */
internal class PersianAnalyzer : TextAnalyzer {
    override fun analyze(text: String): List<AnalyzedToken> =
        ArabicLetterTokenizer.tokenize(text).map { AnalyzedToken(persianTerm(it.text), it.position) }

    private fun persianTerm(token: String): String {
        val buffer = CharArray(token.length) { AnalyzerTables.lowerCase(token[it]) }
        var len = ArabicNormalizer.normalize(buffer, buffer.size)
        len = persianNormalize(buffer, len)
        return buffer.concatToString(0, len)
    }

    /** Lucene 3.6.2 `PersianNormalizer`: Farsi yeh and yeh barree to yeh, keheh to kaf, heh yeh and heh goal to heh; drops the hamza above. */
    private fun persianNormalize(s: CharArray, length: Int): Int {
        var len = length
        var i = 0
        while (i < len) {
            when (s[i]) {
                FARSI_YEH, YEH_BARREE -> s[i] = ArabicNormalizer.YEH
                KEHEH -> s[i] = KAF
                HEH_YEH, HEH_GOAL -> s[i] = ArabicNormalizer.HEH
                HAMZA_ABOVE -> {
                    len = deleteChar(s, i, len)
                    i--
                }
            }
            i++
        }
        return len
    }

    private companion object {
        const val FARSI_YEH = 'ی'
        const val YEH_BARREE = 'ے'
        const val KEHEH = 'ک'
        const val KAF = 'ك'
        const val HAMZA_ABOVE = 'ٔ'
        const val HEH_YEH = 'ۀ'
        const val HEH_GOAL = 'ہ'
    }
}
