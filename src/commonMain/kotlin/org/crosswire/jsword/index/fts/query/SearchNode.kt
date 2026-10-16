package org.crosswire.jsword.index.fts.query

/**
 * The parsed form of a Lucene 3.6.2 classic `QueryParser` query (default field `content`, default operator OR).
 *
 * The tree keeps the query text as written (unescaped, and lowercased for the expanded forms): a [Term] or [Phrase]
 * is analyzed later, by the book's analyzer, in [FtsQueryEvaluator]. See [LuceneSyntaxParser] for the grammar.
 */
internal sealed interface SearchNode {
    /** One unquoted word. The analyzer may turn it into zero, one or several tokens (several: a phrase). */
    data class Term(val text: String) : SearchNode

    /** A quoted phrase [text] with [slop] (`"a b"~3`, default 0). Analyzed as a whole, so token positions are kept. */
    data class Phrase(val text: String, val slop: Int) : SearchNode

    /** A term with `*` / `?` wildcards (escapes already removed); lowercased, not analyzed. */
    data class Wildcard(val pattern: String) : SearchNode

    /** `term*`: Lucene `PrefixQuery`, the [prefix] is literal (lowercased, unescaped, not analyzed). */
    data class Prefix(val prefix: String) : SearchNode

    /** `term~` / `term~0.8`; lowercased, not analyzed. [minSimilarity] is 0.5 when not given. */
    data class Fuzzy(val term: String, val minSimilarity: Float) : SearchNode

    /** A `strong:` term. [raw] is the number as written (or the lowercased pattern when [wildcard] is true). */
    data class StrongTerm(val raw: String, val wildcard: Boolean) : SearchNode

    /** A Lucene `BooleanQuery`. An empty list is Lucene's empty BooleanQuery: it matches nothing. */
    data class Bool(val clauses: List<Clause>) : SearchNode
}

/** One clause of a [SearchNode.Bool]. */
internal data class Clause(val node: SearchNode, val occur: Occur)

/** Lucene `BooleanClause.Occur`. */
internal enum class Occur { MUST, SHOULD, MUST_NOT }

/**
 * The form Lucene's `Query.toString()` gives for the tree (field `content` shown as a prefix; this tree's unanalyzed
 * term text, so it equals Lucene's output when analysis leaves the text unchanged). For debugging and tests.
 */
internal fun SearchNode.toLuceneString(): String = when (this) {
    is SearchNode.Term -> "content:$text"
    is SearchNode.Phrase -> "content:\"$text\"" + if (slop != 0) "~$slop" else ""
    is SearchNode.Wildcard -> "content:$pattern"
    is SearchNode.Prefix -> "content:$prefix*"
    is SearchNode.Fuzzy -> "content:$term~$minSimilarity"
    is SearchNode.StrongTerm -> "strong:$raw"
    is SearchNode.Bool -> clauses.joinToString(" ") { c ->
        val prefix = when (c.occur) { Occur.MUST -> "+"; Occur.MUST_NOT -> "-"; Occur.SHOULD -> "" }
        val inner = c.node.toLuceneString()
        prefix + if (c.node is SearchNode.Bool) "($inner)" else inner
    }
}
