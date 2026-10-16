package org.crosswire.jsword.index.fts.query

/**
 * Expansion of wildcard, prefix and fuzzy terms against an index vocabulary, as Lucene 3.6.2 expands them against
 * the term dictionary of the `content` (or `strong`) field.
 *
 * The vocabulary holds the terms exactly as indexed (analyzer output: stems for Snowball languages), and the
 * query term is neither analyzed nor stemmed, only lowercased by the parser. That is Lucene's behaviour
 * (`QueryParser` with `lowercaseExpandedTerms`), reproduced on purpose: a wildcard such as `believ*` matches stems,
 * `believing*` matches nothing in a stemmed index.
 */
object VocabularyExpansion {
    /** `BooleanQuery.getMaxClauseCount()`, the size of `TopTermsScoringBooleanQueryRewrite` for a `FuzzyQuery`. */
    const val FUZZY_MAX_TERMS = 1024

    /**
     * The terms of [vocabulary] matching [pattern]: Lucene `WildcardQuery` / `WildcardTermEnum.wildcardEquals`
     * (`*` any run of UTF-16 units, `?` exactly one unit, no escapes, a leading wildcard allowed). There is no cap
     * on the number of terms (`CONSTANT_SCORE_AUTO_REWRITE_DEFAULT`). Order follows [vocabulary].
     */
    fun wildcard(pattern: String, vocabulary: Sequence<String>): List<String> {
        // WildcardTermEnum only tests terms starting with the pattern's literal prefix (up to the first wildcard).
        val firstWild = pattern.indexOfFirst { it == '*' || it == '?' }
        val pre = if (firstWild < 0) "" else pattern.substring(0, firstWild)
        val preLen = pre.length
        return vocabulary.filter { it.startsWith(pre) && wildcardEquals(pattern, preLen, it, preLen) }.toList()
    }

    /** The terms of [vocabulary] starting with [prefix] (Lucene `PrefixQuery`: literal, no wildcard characters). */
    fun prefix(prefix: String, vocabulary: Sequence<String>): List<String> =
        vocabulary.filter { it.startsWith(prefix) }.toList()

    /**
     * Lucene 3.6.2 `FuzzyQuery(term, minSimilarity, prefixLength = 0, maxExpansions = maxTerms)` expansion: the
     * terms accepted by `FuzzyTermEnum` (similarity above [minSimilarity]), restricted to the [maxTerms] best by
     * `(similarity - minSimilarity) / (1 - minSimilarity)` (`TopTermsScoringBooleanQueryRewrite`), ties resolved
     * in favour of the term that sorts first (UTF-16 order, `Term.compareTo`). The result is in that term order.
     *
     * `FuzzyQuery` only enumerates when `term.length > 1 / (1 - minSimilarity)`; a shorter term can only match
     * itself, which is also reproduced here (`x~0.99`).
     */
    fun fuzzy(term: String, minSimilarity: Float, vocabulary: Sequence<String>, maxTerms: Int = FUZZY_MAX_TERMS): List<String> {
        require(minSimilarity >= 0f && minSimilarity < 1f) { "minSimilarity must be in [0, 1)" }
        val longEnough = term.length > 1.0f / (1.0f - minSimilarity)
        val sorted = vocabulary.distinct().sortedWith(Utf16Order)
        if (!longEnough) return if (sorted.contains(term)) listOf(term) else emptyList()
        val scale = 1.0f / (1.0f - minSimilarity)
        val matcher = FuzzyMatcher(term, minSimilarity)
        class Hit(val term: String, val boost: Float)
        val hits = ArrayList<Hit>()
        for (t in sorted) {
            val sim = matcher.similarity(t)
            if (sim > minSimilarity) hits += Hit(t, (sim - minSimilarity) * scale)
        }
        // Stable sort: equal boosts keep term order, so the earlier term survives the cut.
        return hits.sortedByDescending { it.boost }.take(maxTerms).map { it.term }.sortedWith(Utf16Order)
    }

    private object Utf16Order : Comparator<String> {
        override fun compare(a: String, b: String): Int = a.compareTo(b)
    }

    /** Port of `FuzzyTermEnum.similarity` with prefix length 0 (including its early exits). */
    private class FuzzyMatcher(private val text: String, private val minimumSimilarity: Float) {
        private var p = IntArray(text.length + 1)
        private var d = IntArray(text.length + 1)

        fun similarity(target: String): Float {
            val m = target.length
            val n = text.length
            if (n == 0) return 0.0f
            if (m == 0) return 0.0f
            val maxDistance = ((1 - minimumSimilarity) * minOf(n, m).toFloat()).toInt()
            if (maxDistance < kotlin.math.abs(m - n)) return 0.0f
            for (i in 0..n) p[i] = i
            for (j in 1..m) {
                var best = m
                val tj = target[j - 1]
                d[0] = j
                for (i in 1..n) {
                    d[i] = if (tj != text[i - 1]) minOf(minOf(d[i - 1], p[i]), p[i - 1]) + 1
                    else minOf(minOf(d[i - 1] + 1, p[i] + 1), p[i - 1])
                    best = minOf(best, d[i])
                }
                if (j > maxDistance && best > maxDistance) return 0.0f
                val tmp = p; p = d; d = tmp
            }
            return 1.0f - p[n].toFloat() / minOf(n, m).toFloat()
        }
    }

    /** `WildcardTermEnum.wildcardEquals`, ported statement by statement (including its `ca??` quirk). */
    private fun wildcardEquals(pattern: String, patternIdx: Int, string: String, stringIdx: Int): Boolean {
        var p = patternIdx
        var s = stringIdx
        while (true) {
            val sEnd = s >= string.length
            val pEnd = p >= pattern.length
            if (sEnd) {
                var justWildcardsLeft = true
                var pos = p
                while (pos < pattern.length && justWildcardsLeft) {
                    val c = pattern[pos]
                    if (c != '?' && c != '*') justWildcardsLeft = false
                    else {
                        if (c == '?') return false // "cat" does not match "ca??"
                        pos++
                    }
                }
                if (justWildcardsLeft) return true
            }
            if (sEnd || pEnd) break
            if (pattern[p] == '?') { p++; s++; continue }
            if (pattern[p] == '*') {
                while (p < pattern.length && pattern[p] == '*') p++
                for (i in string.length downTo s) if (wildcardEquals(pattern, p, string, i)) return true
                break
            }
            if (pattern[p] != string[s]) break
            p++; s++
        }
        return false
    }
}
