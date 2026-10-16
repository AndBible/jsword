package org.crosswire.jsword.index.analysis

/**
 * Baseline `ArabicLuceneAnalyzer` with stemming on and stop words off (the baseline default): [ArabicLetterTokenizer],
 * then Lucene's `LowerCaseFilter`, [ArabicNormalizer] and [ArabicStemmer]. Tokens that the normalizer empties are kept
 * as empty terms, as Lucene keeps them. Positions are those of the tokenizer (consecutive).
 */
internal class ArabicAnalyzer : TextAnalyzer {
    override fun analyze(text: String): List<AnalyzedToken> =
        ArabicLetterTokenizer.tokenize(text).map { AnalyzedToken(arabicTerm(it.text, stem = true), it.position) }
}

/**
 * The Arabic filter chain applied to one token: `LowerCaseFilter`, `ArabicNormalizationFilter`, and optionally
 * `ArabicStemFilter`. Lowercasing is per UTF-16 unit, as in `LowerCaseFilter` for these units.
 */
internal fun arabicTerm(token: String, stem: Boolean): String {
    val buffer = CharArray(token.length) { AnalyzerTables.lowerCase(token[it]) }
    var len = ArabicNormalizer.normalize(buffer, buffer.size)
    if (stem) len = ArabicStemmer.stem(buffer, len)
    return buffer.concatToString(0, len)
}
