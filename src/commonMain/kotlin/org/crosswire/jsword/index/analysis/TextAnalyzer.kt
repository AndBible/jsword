package org.crosswire.jsword.index.analysis

/** Turns visible text into index terms. Mirrors a Lucene `Analyzer` as used by the K6 search index. */
fun interface TextAnalyzer {
    /** Analyzes [text]; the result is in source order. */
    fun analyze(text: String): List<AnalyzedToken>
}
