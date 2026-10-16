package org.crosswire.jsword.index.analysis

/**
 * Lucene 3.6.2 `LowerCaseTokenizer` followed by `ASCIIFoldingFilter`: the baseline analyzer for languages
 * without a dedicated one (English, for instance, falls back to this in the baseline `AnalyzerFactory`).
 */
class SimpleAnalyzer : TextAnalyzer {
    override fun analyze(text: String): List<AnalyzedToken> =
        LowerCaseLetterTokenizer.tokenize(text).asciiFolded()
}
