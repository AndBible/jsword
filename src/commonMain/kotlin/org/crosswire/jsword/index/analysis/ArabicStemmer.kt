package org.crosswire.jsword.index.analysis

/**
 * Port of Lucene 3.6.2 `ArabicStemmer` on a char buffer: one prefix at most (the first match in list order), then every
 * suffix in list order, each applied at most once. The length guards are Lucene's: the wa- prefix needs 4 units, the
 * other prefixes need 2 units after themselves, and a suffix needs 2 units before it. Results are not always words
 * (`علي` stems to `عل`), and that is the baseline behaviour.
 */
internal object ArabicStemmer {
    private val prefixes: List<CharArray> = listOf(
        "ال", // al
        "وال", // wal
        "بال", // bal
        "كال", // kal
        "فال", // fal
        "لل", // ll
        "و", // wa
    ).map { it.toCharArray() }

    private val suffixes: List<CharArray> = listOf(
        "ها", // haa
        "ان", // aan
        "ات", // aat
        "ون", // oon
        "ين", // ein
        "يه", // yh
        "ية", // yah marbuta
        "ه", // h
        "ة", // teh marbuta
        "ي", // y
    ).map { it.toCharArray() }

    /** Stems the first [len] units of [s] in place and returns the new length. */
    fun stem(s: CharArray, len: Int): Int {
        var length = stemPrefix(s, len)
        length = stemSuffix(s, length)
        return length
    }

    private fun stemPrefix(s: CharArray, len: Int): Int {
        for (prefix in prefixes) {
            if (startsWithCheckLength(s, len, prefix)) return deleteN(s, 0, len, prefix.size)
        }
        return len
    }

    private fun stemSuffix(s: CharArray, length: Int): Int {
        var len = length
        for (suffix in suffixes) {
            if (endsWithCheckLength(s, len, suffix)) len = deleteN(s, len - suffix.size, len, suffix.size)
        }
        return len
    }

    private fun startsWithCheckLength(s: CharArray, len: Int, prefix: CharArray): Boolean = when {
        prefix.size == 1 && len < 4 -> false // wa- prefix requires at least 3 characters
        len < prefix.size + 2 -> false // other prefixes require only 2
        else -> prefix.indices.all { s[it] == prefix[it] }
    }

    private fun endsWithCheckLength(s: CharArray, len: Int, suffix: CharArray): Boolean = when {
        len < suffix.size + 2 -> false // all suffixes require at least 2 characters after stemming
        else -> suffix.indices.all { s[len - suffix.size + it] == suffix[it] }
    }

    private fun deleteN(s: CharArray, pos: Int, len: Int, nChars: Int): Int {
        var length = len
        repeat(nChars) { length = deleteChar(s, pos, length) }
        return length
    }
}
