package org.crosswire.jsword.index.analysis

/**
 * Baseline `GermanLuceneAnalyzer` with stemming on and stop words off (the baseline default): Lucene's
 * `LowerCaseTokenizer` followed by `GermanStemFilter` (see [GermanStemmer]). Positions are those of the tokenizer.
 *
 * Not thread-safe: the stemmer's buffer is reused.
 */
class GermanAnalyzer : TextAnalyzer {
    private val stemmer = GermanStemmer()

    override fun analyze(text: String): List<AnalyzedToken> =
        LowerCaseLetterTokenizer.tokenize(text).map { AnalyzedToken(stemmer.stem(it.text), it.position) }
}
