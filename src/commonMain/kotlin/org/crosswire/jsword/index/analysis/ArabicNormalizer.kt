package org.crosswire.jsword.index.analysis

/**
 * Port of Lucene 3.6.2 `ArabicNormalizer` on a char buffer of [length] units (the function returns the new length).
 * Alef forms fold to bare alef, dotless yeh to yeh, teh marbuta to heh; tatweel, the tanwin, fatha, damma, kasra,
 * shadda and sukun are deleted, which can leave an empty term.
 */
internal object ArabicNormalizer {
    fun normalize(s: CharArray, length: Int): Int {
        var len = length
        var i = 0
        while (i < len) {
            when (s[i]) {
                ALEF_MADDA, ALEF_HAMZA_ABOVE, ALEF_HAMZA_BELOW -> s[i] = ALEF
                DOTLESS_YEH -> s[i] = YEH
                TEH_MARBUTA -> s[i] = HEH
                TATWEEL, KASRATAN, DAMMATAN, FATHATAN, FATHA, DAMMA, KASRA, SHADDA, SUKUN -> {
                    len = deleteChar(s, i, len)
                    i--
                }
            }
            i++
        }
        return len
    }

    const val ALEF = 'ا'
    private const val ALEF_MADDA = 'آ'
    private const val ALEF_HAMZA_ABOVE = 'أ'
    private const val ALEF_HAMZA_BELOW = 'إ'
    const val YEH = 'ي'
    private const val DOTLESS_YEH = 'ى'
    private const val TEH_MARBUTA = 'ة'
    const val HEH = 'ه'
    private const val TATWEEL = 'ـ'
    private const val FATHATAN = 'ً'
    private const val DAMMATAN = 'ٌ'
    private const val KASRATAN = 'ٍ'
    private const val FATHA = 'َ'
    private const val DAMMA = 'ُ'
    private const val KASRA = 'ِ'
    private const val SHADDA = 'ّ'
    private const val SUKUN = 'ْ'
}

/** Lucene 3.6.2 `StemmerUtil.delete`: removes the unit at [pos] of the first [len] units; returns the new length. */
internal fun deleteChar(s: CharArray, pos: Int, len: Int): Int {
    if (pos < len) {
        for (k in pos until len - 1) s[k] = s[k + 1]
    }
    return len - 1
}
