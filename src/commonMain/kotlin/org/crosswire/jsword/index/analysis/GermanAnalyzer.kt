package org.crosswire.jsword.index.analysis

/**
 * Baseline `GermanLuceneAnalyzer` with stemming on and stop words off (the baseline default): Lucene's
 * `LowerCaseTokenizer` followed by `GermanStemFilter` (see [GermanStemmer]). Positions are those of the tokenizer.
 *
 * Thread-safe: each call creates its own [GermanStemmer], so one analyzer can be shared by concurrent callers.
 */
internal class GermanAnalyzer : TextAnalyzer {
    override fun analyze(text: String): List<AnalyzedToken> {
        val stemmer = GermanStemmer()
        return LowerCaseLetterTokenizer.tokenize(text).map { AnalyzedToken(stemmer.stem(it.text), it.position) }
    }
}
