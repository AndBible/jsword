package org.crosswire.jsword.index.analysis

import org.crosswire.jsword.index.analysis.snowball.snowballStemmer

/**
 * Baseline `ConfigurableSnowballAnalyzer` with stemming on and stop words off (the baseline default): Lucene's
 * `LowerCaseTokenizer` followed by `SnowballFilter` for the stemmer [stemmerName] (see [snowballStemmer]).
 * No ASCII folding happens, unlike [SimpleAnalyzer]. Thread-safe, see [analyze]. Positions are those of the tokenizer
 * (consecutive).
 */
internal class SnowballAnalyzer(private val stemmerName: String) : TextAnalyzer {
    init {
        requireNotNull(snowballStemmer(stemmerName)) { "No Snowball stemmer named $stemmerName" }
    }

    /** A new stemmer per call: a [SnowballStemmer] is not thread-safe, and one analyzer may be shared by callers. */
    override fun analyze(text: String): List<AnalyzedToken> {
        val stemmer = requireNotNull(snowballStemmer(stemmerName))
        return LowerCaseLetterTokenizer.tokenize(text).map { AnalyzedToken(stemmer.stem(it.text), it.position) }
    }
}
