package org.crosswire.jsword.index.analysis

/**
 * Baseline `GreekLuceneAnalyzer` under `Version.LUCENE_29`: [ClassicTokenizer], then Lucene's `GreekLowerCaseFilter`
 * in 2.9 mode. The baseline's stop word filter is off by default (`doStopWords = false`), so none is applied.
 */
internal class GreekAnalyzer : TextAnalyzer {
    private val tokenizer = ClassicTokenizer()

    override fun analyze(text: String): List<AnalyzedToken> {
        var position = -1
        return tokenizer.tokenize(text).map {
            position += it.positionIncrement
            AnalyzedToken(lowerCase(it.text), position)
        }
    }

    /** Lucene 3.6.2 `GreekLowerCaseFilter` (Greek accents and final sigma folded; other units via `Character.toLowerCase`). */
    internal fun lowerCase(token: String): String {
        val sb = StringBuilder(token.length)
        for (c in token) {
            sb.append(
                when (c.code) {
                    0x03C2 -> 'σ'
                    0x0386, 0x03AC -> 'α'
                    0x0388, 0x03AD -> 'ε'
                    0x0389, 0x03AE -> 'η'
                    0x038A, 0x03AA, 0x03AF, 0x03CA, 0x0390 -> 'ι'
                    0x038E, 0x03AB, 0x03CD, 0x03CB, 0x03B0 -> 'υ'
                    0x038C, 0x03CC -> 'ο'
                    0x038F, 0x03CE -> 'ω'
                    0x03A2 -> 'ς'
                    else -> AnalyzerTables.lowerCase(c)
                },
            )
        }
        return sb.toString()
    }
}
