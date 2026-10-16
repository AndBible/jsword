package org.crosswire.jsword.index.analysis

/**
 * Baseline `HebrewLuceneAnalyzer` under `Version.LUCENE_29`: [ClassicTokenizer], then JSword's `HebrewPointingFilter`
 * (`unPoint(word, false)`). No lowercasing and no stop words.
 */
internal class HebrewAnalyzer : TextAnalyzer {
    private val tokenizer = ClassicTokenizer()

    override fun analyze(text: String): List<AnalyzedToken> {
        var position = -1
        return tokenizer.tokenize(text).map {
            position += it.positionIncrement
            AnalyzedToken(unPoint(it.text), position)
        }
    }

    /** JSword `HebrewPointingFilter.unPoint(word, unpointVowels = false)`: drops cantillation and niqqud, maps presentation forms. */
    internal fun unPoint(word: String): String {
        val sb = StringBuilder(word)
        var i = 0
        while (i < sb.length) {
            val c = sb[i].code
            if (c < ETNAHTA || c > ALEPH_LAMED) {
                i++
            } else if (c < SHEVA) {
                sb.deleteAt(i)
            } else if (c >= HEBREW_COMBINED_RANGE_START && c < ALEPH_LAMED) {
                sb[i] = (c - DAGESH_GAP).toChar()
                i++
            } else {
                i++
            }
        }
        return sb.toString()
    }

    private companion object {
        const val SHEVA = 0x05B0
        const val ETNAHTA = 0x0591
        const val DAGESH_GAP = 0xFB44 - 0x05E3
        const val ALEPH_LAMED = 0xFB4F
        const val HEBREW_COMBINED_RANGE_START = 0xFB1D
    }
}
