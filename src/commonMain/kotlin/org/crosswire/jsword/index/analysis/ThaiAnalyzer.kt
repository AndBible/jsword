package org.crosswire.jsword.index.analysis

/**
 * Baseline `ThaiLuceneAnalyzer` under `Version.LUCENE_29`: [ClassicTokenizer], then Lucene's `ThaiWordFilter`, which
 * splits every token whose first character is in the Thai Unicode block with [ThaiWordBreaker]. The baseline stop word
 * filter is off by default. Every term is lowercased with Java `Character.toLowerCase` (the oracle yields `abc` for
 * `ABC` and for the Latin tail of a Thai-led token such as `กขABC`). The analyzer holds no state between calls (it is shared between threads).
 *
 * Reproduced on purpose: every piece of a split token advances the position by the increment of the original token
 * (`ThaiWordFilter` restores the captured state, increment included, for every piece), so after an overlong dropped
 * token (increment 2) each piece of the next Thai token gets increment 2.
 *
 * Deviation in order, with the same result: this class breaks first and then lowercases each piece, whereas Lucene
 * lowercases the token first. The corpus parity test is green with this order.
 */
class ThaiAnalyzer : TextAnalyzer {
    private val tokenizer = ClassicTokenizer()

    override fun analyze(text: String): List<AnalyzedToken> {
        var position = -1
        val out = ArrayList<AnalyzedToken>()
        val splitter = ThaiWordBreaker.session()
        for (token in tokenizer.tokenize(text)) {
            val t = token.text
            if (t.isEmpty() || t[0].code !in THAI_BLOCK) {
                position += token.positionIncrement
                out.add(AnalyzedToken(lower(t), position))
                continue
            }
            for (range in splitter.words(t)) {
                position += token.positionIncrement
                out.add(AnalyzedToken(lower(t.substring(range.first, range.last + 1)), position))
            }
        }
        return out
    }

    private fun lower(s: String): String = buildString(s.length) { for (c in s) append(AnalyzerTables.lowerCase(c)) }

    private companion object {
        val THAI_BLOCK = 0x0E00..0x0E7F
    }
}
