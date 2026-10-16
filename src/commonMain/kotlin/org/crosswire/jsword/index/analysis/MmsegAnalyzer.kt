package org.crosswire.jsword.index.analysis

import org.crosswire.jsword.index.analysis.mmseg.Dictionary
import org.crosswire.jsword.index.analysis.mmseg.MMSeg

/**
 * mmseg4j 1.8.6 `ComplexAnalyzer`, which the baseline `Mmseg4jChineseLuceneAnalyzer` uses for Chinese and Japanese.
 * Every token has position increment 1. Holds no state across calls: the dictionary is immutable and shared.
 */
internal class MmsegAnalyzer(dictionary: Dictionary = MmsegDictionary.default) : TextAnalyzer {
    private val tokenizer = MMSeg(dictionary)

    override fun analyze(text: String): List<AnalyzedToken> =
        tokenizer.tokens(text).mapIndexed { i, t -> AnalyzedToken(t, i) }
}

/** The bundled mmseg4j dictionary, loaded once and lazily. */
internal object MmsegDictionary {
    val default: Dictionary get() = Dictionary.default
}
