package org.crosswire.jsword.index.analysis

/**
 * One token produced by a [TextAnalyzer].
 *
 * @property text the normalized term (lowercased, folded, and so on, as the analyzer defines it).
 * @property position the absolute token position: the sum of the position increments so far, the first token being 0.
 */
data class AnalyzedToken(val text: String, val position: Int)
