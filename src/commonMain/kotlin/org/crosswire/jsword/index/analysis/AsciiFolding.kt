package org.crosswire.jsword.index.analysis

/**
 * Lucene 3.6.2 `ASCIIFoldingFilter` over a token stream: each token's text is folded with
 * [AnalyzerTables.foldAscii]. Positions are unchanged, because the filter never adds or drops tokens.
 */
internal fun List<AnalyzedToken>.asciiFolded(): List<AnalyzedToken> =
    map { AnalyzedToken(AnalyzerTables.foldAscii(it.text), it.position) }
