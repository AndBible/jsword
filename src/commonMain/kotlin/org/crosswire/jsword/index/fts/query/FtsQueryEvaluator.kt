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
 *   phrases (slop above 0 and at least two tokens) are Task 13.
 * - [SearchNode.Bool]: `BooleanQuery`: the intersection of the MUST clauses, else the union of the SHOULD clauses
 *   (SHOULD clauses are optional next to a MUST), minus the MUST_NOT clauses; no MUST and no SHOULD (a purely
 *   negative query) matches nothing.
 * - [SearchNode.StrongTerm] without wildcard: `store.strongs` of the padded number (`h7225` is `H07225`).
 *   A term of several whitespace-separated numbers intersects them (the strongs table has no positions, so the
 *   baseline's phrase over the `strong` field is approximated).
 *
 * Wildcards, fuzzy terms and `strong:` wildcards are Tasks 12/13: until they land, evaluating one throws
 * [UnsupportedSearchSyntaxException] rather than returning a wrong set.
 *
 * Every FTS5 expression is built defensively: each encoded token is quoted, nothing from the user reaches FTS5
 * syntax.
 */
class FtsQueryEvaluator(private val store: FtsIndexStore, private val analyzer: TextAnalyzer) {
    fun evaluate(node: SearchNode): Set<Long> = when (node) {
        is SearchNode.Term -> phraseOrTerm(analyzer.analyze(node.text))
        is SearchNode.Phrase -> {
            val tokens = analyzer.analyze(node.text)
            if (node.slop > 0 && tokens.size > 1) throw UnsupportedSearchSyntaxException("\"phrase\"~${node.slop}")
            phraseOrTerm(tokens)
        }
        is SearchNode.Bool -> evaluateBool(node)
        is SearchNode.StrongTerm -> {
            if (node.wildcard) throw UnsupportedSearchSyntaxException("strong:${node.raw}")
            val tokens = StrongsTokens.fromLexical(node.raw)
            if (tokens.isEmpty()) emptySet() else tokens.map { store.strongs(it) }.reduce { a, b -> a intersect b }
        }
        is SearchNode.Wildcard -> throw UnsupportedSearchSyntaxException(node.pattern)
        is SearchNode.Fuzzy -> throw UnsupportedSearchSyntaxException("${node.term}~")
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
}
