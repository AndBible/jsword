package org.crosswire.jsword.index.fts.query

import org.crosswire.jsword.index.analysis.AnalyzedToken
import org.crosswire.jsword.index.analysis.StrongsTokens
import org.crosswire.jsword.index.analysis.TextAnalyzer
import org.crosswire.jsword.index.fts.FtsIndexStore
import org.crosswire.jsword.index.fts.FtsSchema
import org.crosswire.jsword.index.fts.SearchTokenCodec
import org.crosswire.jsword.index.fts.UnsupportedSearchSyntaxException

/**
 * Evaluates a [SearchNode] tree against one book's [FtsIndexStore] and returns the matching row ids, with the
 * result sets of Lucene 3.6.2 (`LUCENE_29` `QueryParser`, baseline JSword):
 *
 * - [SearchNode.Term]: analyzed by [analyzer] (the book's analyzer). No token: no match (the parser already dropped
 *   such a clause when it was given the analyzer). One token: that token. Several tokens (a term the analyzer
 *   splits, `well-known`, a CJK word): a phrase, because `LUCENE_29` has `autoGeneratePhraseQueries = true`
 *   (checked against the baseline oracle).
 * - [SearchNode.Phrase] with slop 0: the analyzed tokens as one FTS5 phrase. Positions are kept: a gap in the
 *   query's token positions is matched by the index's gap token, exactly like `PhraseQuery` positions. Sloppy
 *   phrases (slop above 0 and at least two tokens) are evaluated as described further down.
 * - [SearchNode.Bool]: `BooleanQuery`: the intersection of the MUST clauses, else the union of the SHOULD clauses
 *   (SHOULD clauses are optional next to a MUST), minus the MUST_NOT clauses; no MUST and no SHOULD (a purely
 *   negative query) matches nothing.
 * - [SearchNode.StrongTerm] with a wildcard (`strong:h7*`) is matched, lowercased as Lucene's `getWildcardQuery` does,
 *   against the uppercase padded tokens of the strongs table, so it intentionally matches nothing whenever the
 *   pattern contains a letter (the baseline quirk, reproduced for parity; verified on KJV by the corpus matrix).
 * - [SearchNode.StrongTerm] without wildcard: `store.strongs` of the padded number (`h7225` is `H07225`).
 *   A term of several whitespace-separated numbers intersects them (the strongs table has no positions, so the
 *   baseline's phrase over the `strong` field is approximated).
 *
 * - [SearchNode.Wildcard], [SearchNode.Prefix], [SearchNode.Fuzzy] and wildcard [SearchNode.StrongTerm]: expanded
 *   against the index vocabulary by [VocabularyExpansion] (Lucene's term-dictionary expansion) and the matches of
 *   the terms are united, [MATCH_BATCH] terms per query.
 *
 * - Sloppy [SearchNode.Phrase] (slop above 0, at least two tokens): candidates from an FTS5 `NEAR` over the single
 *   terms (a superset by construction, see [sloppyCandidates]), then each candidate verified by [SloppyPhraseMatcher]
 *   over the row's tokens (Lucene's `SloppyPhraseScorer`, by stream position).
 *
 * Every FTS5 expression is built defensively: each encoded token is quoted, nothing from the user reaches FTS5
 * syntax.
 */
internal class FtsQueryEvaluator(private val store: FtsIndexStore, private val analyzer: TextAnalyzer) {
    fun evaluate(node: SearchNode): Set<Long> = when (node) {
        is SearchNode.Term -> phraseOrTerm(analyzer.analyze(node.text))
        is SearchNode.Phrase -> {
            val tokens = analyzer.analyze(node.text)
            if (node.slop > 0 && tokens.size > 1) sloppy(tokens, node.slop) else phraseOrTerm(tokens)
        }
        is SearchNode.Bool -> evaluateBool(node)
        is SearchNode.StrongTerm -> {
            if (node.wildcard) return store.strongsAny(VocabularyExpansion.wildcard(node.raw, strongsVocabulary.asSequence()))
            val tokens = StrongsTokens.fromLexical(node.raw)
            if (tokens.isEmpty()) emptySet() else tokens.map { store.strongs(it) }.reduce { a, b -> a intersect b }
        }
        is SearchNode.Wildcard -> wildcard(node.pattern)
        is SearchNode.Prefix -> prefix(node.prefix)
        is SearchNode.Fuzzy -> matchAny(VocabularyExpansion.fuzzy(node.term, node.minSimilarity, vocabulary.asSequence()))
    }

    /** The decoded index terms, read once per evaluator (wildcard and fuzzy terms scan all of them). */
    private val vocabulary: List<String> by lazy { store.vocabulary().toList() }

    /** The distinct Strong's tokens, read once per evaluator (every `strong:` wildcard of a query scans them). */
    private val strongsVocabulary: List<String> by lazy { store.strongsVocabulary().toList() }

    private fun wildcard(pattern: String): Set<Long> {
        // A pattern that is a literal followed by one final `*` is a prefix: let FTS5 scan the term range.
        val star = pattern.indexOf('*')
        if (star > 0 && star == pattern.length - 1 && pattern.none { it == '?' } && wellFormed(pattern)) return prefix(pattern.dropLast(1))
        return matchAny(VocabularyExpansion.wildcard(pattern, vocabulary.asSequence()))
    }

    /**
     * Terms starting with [prefix]. The FTS5 prefix query over the hex encoding gives the same terms as comparing
     * decoded strings (UTF-8 hex prefix = whole-character prefix), unless [prefix] has a lone surrogate, which has
     * no UTF-8 form: that and the empty prefix take the vocabulary path.
     */
    private fun prefix(prefix: String): Set<Long> {
        if (prefix.isNotEmpty() && wellFormed(prefix)) return store.match("\"${SearchTokenCodec.encode(prefix)}\"*")
        return matchAny(VocabularyExpansion.prefix(prefix, vocabulary.asSequence()))
    }

    private fun wellFormed(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isHighSurrogate()) { if (i + 1 >= s.length || !s[i + 1].isLowSurrogate()) return false; i++ }
            else if (c.isLowSurrogate()) return false
            i++
        }
        return true
    }

    /** The union of the single-term matches of [terms], [MATCH_BATCH] terms per FTS5 expression (`OR`-joined). */
    private fun matchAny(terms: List<String>): Set<Long> {
        val out = LinkedHashSet<Long>()
        for (chunk in terms.chunked(MATCH_BATCH)) out += store.match(chunk.joinToString(" OR ") { quote(it) })
        return out
    }

    private fun evaluateBool(node: SearchNode.Bool): Set<Long> {
        var must: Set<Long>? = null
        var should: Set<Long>? = null
        val not = LinkedHashSet<Long>()
        for (clause in node.clauses) {
            val rows = evaluate(clause.node)
            when (clause.occur) {
                Occur.MUST -> must = must?.intersect(rows) ?: rows
                Occur.SHOULD -> should = should?.union(rows) ?: rows
                Occur.MUST_NOT -> not += rows
            }
        }
        val positive = must ?: should ?: return emptySet()
        return if (not.isEmpty()) positive else positive - not
    }

    private fun sloppy(tokens: List<AnalyzedToken>, slop: Int): Set<Long> {
        if (SloppyPhraseMatcher.hasSamePosition(tokens)) throw UnsupportedSearchSyntaxException("\"phrase\"~$slop with tokens at one position")
        return sloppyCandidates(tokens, slop).filterTo(LinkedHashSet()) { SloppyPhraseMatcher.matches(tokens, slop, store.tokensOf(it)) }
    }

    /**
     * Rows that contain every phrase term close enough to match. A sloppy match puts every term `i` within
     * `[m + o_i, m + o_i + slop]` for some `m` (`o_i` the query offset), so all terms lie within a window of
     * `o_last - o_first + slop + 1` positions. FTS5 `NEAR(t1 .. tn, N)` (one quoted token per phrase, any order)
     * accepts a row when its terms fit a window of `N + 2` tokens: measured on SQLite 3.46, with `n` single-token phrases
     * the least `N` is the number of non-matching tokens in the window plus `n - 2` (so a window of `w` tokens needs
     * `N = w - 2`, independent of `n`). Hence `N = o_last - o_first + slop - 1`, never losing a Lucene match (the
     * differential tests compare against brute force). A very long phrase falls back to the all-terms intersection.
     */
    private fun sloppyCandidates(tokens: List<AnalyzedToken>, slop: Int): Set<Long> {
        val terms = tokens.map { quote(it.text) }
        if (terms.size > MAX_NEAR_TERMS) return terms.map { store.match(it) }.reduce { a, b -> a intersect b }
        val span = tokens.maxOf { it.position } - tokens.minOf { it.position }
        val near = span.toLong() + slop - 1
        return store.match(terms.joinToString(" ", "NEAR(", ", $near)"))
    }

    /** One token: the token; several: one FTS5 phrase with a gap token for every skipped position. */
    private fun phraseOrTerm(tokens: List<AnalyzedToken>): Set<Long> {
        if (tokens.isEmpty()) return emptySet()
        if (tokens.size == 1) return store.match(quote(tokens[0].text))
        val parts = ArrayList<String>()
        var next = tokens[0].position
        for (t in tokens) {
            while (next < t.position) { parts += FtsSchema.GAP; next++ }
            parts += SearchTokenCodec.encode(t.text)
            next = t.position + 1
        }
        return store.match(parts.joinToString(" ", "\"", "\""))
    }

    private fun quote(token: String) = "\"${SearchTokenCodec.encode(token)}\""

    private companion object {
        /** Terms per FTS5 expression: far below the expression-depth and length limits of SQLite. */
        const val MATCH_BATCH = 200

        /** Longest sloppy phrase given to FTS5 `NEAR`; longer ones use the plain intersection of their terms. */
        const val MAX_NEAR_TERMS = 50
    }
}
