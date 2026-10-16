package org.crosswire.jsword.index.analysis

import org.crosswire.jsword.index.analysis.snowball.snowballStemmer

/**
 * Baseline `ConfigurableSnowballAnalyzer` with stemming on and stop words off (the baseline default): Lucene's
 * `LowerCaseTokenizer` followed by `SnowballFilter` for the stemmer [stemmerName] (see [snowballStemmer]).
 * No ASCII folding happens, unlike [SimpleAnalyzer]. Positions are those of the tokenizer (consecutive).
 */
class SnowballAnalyzer(stemmerName: String) : TextAnalyzer {
    private val stemmer = requireNotNull(snowballStemmer(stemmerName)) { "No Snowball stemmer named $stemmerName" }

    override fun analyze(text: String): List<AnalyzedToken> {
        // One shared stemmer: analysis is not reentrant across threads (documented on SnowballStemmer).
        return LowerCaseLetterTokenizer.tokenize(text).map { AnalyzedToken(stemmer.stem(it.text), it.position) }
    }
}
