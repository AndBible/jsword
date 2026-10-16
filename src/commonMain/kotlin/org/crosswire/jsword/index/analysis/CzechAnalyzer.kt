package org.crosswire.jsword.index.analysis

/**
 * Baseline `CzechLuceneAnalyzer` with stop words off: Lucene's `LowerCaseTokenizer` and nothing else. There is no
 * stemmer: the baseline comment says "Stemming not implemented yet", and `Version.LUCENE_29` has no `CzechStemFilter`
 * (added in 3.1). Verified against the oracle. Positions are consecutive.
 */
class CzechAnalyzer : TextAnalyzer {
    override fun analyze(text: String): List<AnalyzedToken> = LowerCaseLetterTokenizer.tokenize(text)
}
