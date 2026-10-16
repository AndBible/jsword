/*
 * Provenance: hand port of `org.apache.lucene.queryParser.QueryParser` (grammar and actions of `QueryParser.jj`,
 * `QueryParser.discardEscapeChar`) from Apache Lucene 3.6.2 (`lucene-core`).
 * Lucene 3.6.2 (Apache License 2.0, see src/embeddedResources/search/LICENSE.apache-2.0.txt); listed in
 * jsword/NOTICE-third-party.md.
 */

package org.crosswire.jsword.index.fts.query

import org.crosswire.jsword.index.analysis.StrongsTokens
import org.crosswire.jsword.index.analysis.TextAnalyzer
import org.crosswire.jsword.index.fts.SearchFields
import org.crosswire.jsword.index.fts.UnsupportedSearchSyntaxException

/**
 * A port of the Lucene 3.6.2 classic `QueryParser` (JavaCC grammar `QueryParser.jj`) as JSword used it: version
 * `LUCENE_29`, default field `content`, default operator OR, `lowercaseExpandedTerms = true`,
 * `allowLeadingWildcard = true`. Same lexer (longest match, ties by declaration order) and the same clause rules
 * (`addClause`), so the same inputs parse, fail, or silently lose clauses the same way:
 *
 * - `+ faith`, `faith ! hope`: a whitespace-separated `+`, `-` or `!` is the `BAREOPER` token, parsed as a term
 *   whose text is that character; the analyzer drops it, so the clause disappears (no failure).
 * - A term or phrase that analyzes to no token yields no clause at all (Lucene: a `null` query). The `AND` that
 *   follows still makes the previous *remaining* clause required, as in `addClause`. Pass the book's [TextAnalyzer]
 *   as [parse]'s `analyzer` to get this; without one nothing is dropped.
 * - A query with no clauses is the empty boolean query (matches nothing), not an error; an empty string is an error.
 * - More than 1,024 clauses in one boolean query is a failure (`BooleanQuery.TooManyClauses`, which the baseline
 *   parser turns into a `ParseException`).
 *
 * Failure kinds: invalid syntax throws [QueryParseException] (the baseline `ParseException`); constructs outside
 * the K6 query language (K6-3: `field:` other than `content:`/`strong:`, `^` boosts, `[a TO b]` / `{a TO b}`
 * ranges, a fuzzy or phrase `strong:` term) throw [UnsupportedSearchSyntaxException] when the parser reaches them.
 * A syntax error earlier in the query wins over a later unsupported construct.
 *
 * Differences from the baseline, all failures: nesting deeper than [MAX_DEPTH] parentheses is a
 * [QueryParseException] (the baseline's recursive parser works to about 2,000 levels and overflows the stack, an untyped
 * `StackOverflowError`, around 3,000);
 * `term~1` is a [QueryParseException] (the baseline throws a raw `IllegalArgumentException`).
 *
 * Escapes: `QueryParser.Term()` removes them from the whole term before it builds a wildcard or fuzzy query (then
 * lowercases), so `b\?ess*` is a [SearchNode.Prefix] with the literal `b?ess`, and `b\?es?` is the wildcard
 * `b?es?` where the first `?` is a wildcard again (`WildcardQuery` itself has no escape mechanism).
 */
internal object LuceneSyntaxParser {
    /** Deepest parenthesis nesting accepted. Bounds the recursion on every platform (Kotlin/Native threads have small stacks). */
    const val MAX_DEPTH = 200

    /** Lucene `BooleanQuery.getMaxClauseCount()`. */
    const val MAX_CLAUSES = 1024

    private const val DEFAULT_FIELD = "content"
    private const val DEFAULT_FUZZY_SIMILARITY = 0.5f

    /**
     * Parses [query]. [analyzer] (the book's analyzer for the `content` field) is used only to find terms and
     * phrases that analyze to nothing, which Lucene drops (see the class KDoc).
     */
    fun parse(query: String, analyzer: TextAnalyzer? = null): SearchNode {
        val tokens = Lexer.tokenize(query)
        // A lexical error anywhere fails the whole query in Lucene (as a ParseException), also when K6-3 syntax comes
        // first: report it before any `UnsupportedSearchSyntaxException`, so the failure kind equals the baseline's.
        tokens.lastOrNull()?.takeIf { it.kind == Kind.ERROR }?.let { throw QueryParseException(it.image) }
        try {
            return Parser(tokens, analyzer).parseTop() ?: SearchNode.Bool(emptyList())
        } catch (unsupported: UnsupportedSearchSyntaxException) {
            // Lucene accepts the K6-3 form, but a query that is also malformed later fails there with a ParseException:
            // finish the parse leniently so that such a query gets the baseline's failure kind, not "unsupported".
            Parser(tokens, analyzer).also { it.lenient = true }.parseTop()
            throw unsupported
        }
    }

    // ---- lexer ----

    private enum class Kind { AND, OR, NOT, PLUS, MINUS, BAREOPER, LPAREN, RPAREN, COLON, STAR, CARAT, QUOTED, TERM, FUZZY_SLOP, PREFIXTERM, WILDTERM, RANGE, EOF, ERROR }

    private class Tok(val kind: Kind, val image: String)

    /** The `DEFAULT` lexical state of `QueryParser.jj`. A lexical error becomes a final [Kind.ERROR] token (JavaCC raises it lazily too). */
    private object Lexer {
        private const val SPECIAL = " \t\n\r　+-!():^[]\"{}~*?\\"

        private fun isWhitespace(c: Char) = c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '　'

        fun tokenize(s: String): List<Tok> {
            val out = ArrayList<Tok>()
            var i = 0
            while (true) {
                while (i < s.length && isWhitespace(s[i])) i++
                if (i >= s.length) { out += Tok(Kind.EOF, ""); return out }
                var bestLen = 0
                var best = Kind.ERROR
                // Candidates in declaration order; a later one wins only with a strictly longer match.
                fun offer(kind: Kind, len: Int) { if (len > bestLen) { bestLen = len; best = kind } }
                val c = s[i]
                offer(Kind.AND, if (s.startsWith("AND", i)) 3 else if (s.startsWith("&&", i)) 2 else 0)
                offer(Kind.OR, if (s.startsWith("OR", i) || s.startsWith("||", i)) 2 else 0)
                offer(Kind.NOT, if (s.startsWith("NOT", i)) 3 else if (c == '!') 1 else 0)
                offer(Kind.PLUS, if (c == '+') 1 else 0)
                offer(Kind.MINUS, if (c == '-') 1 else 0)
                offer(Kind.BAREOPER, if ((c == '+' || c == '-' || c == '!') && i + 1 < s.length && isWhitespace(s[i + 1])) 2 else 0)
                offer(Kind.LPAREN, if (c == '(') 1 else 0)
                offer(Kind.RPAREN, if (c == ')') 1 else 0)
                offer(Kind.COLON, if (c == ':') 1 else 0)
                offer(Kind.STAR, if (c == '*') 1 else 0)
                offer(Kind.CARAT, if (c == '^') 1 else 0)
                offer(Kind.QUOTED, if (c == '"') quotedLength(s, i) else 0)
                val termLen = termLength(s, i, wild = false)
                offer(Kind.TERM, termLen)
                offer(Kind.FUZZY_SLOP, if (c == '~') slopLength(s, i) else 0)
                offer(Kind.PREFIXTERM, if (c == '*') 1 else if (termLen > 0 && i + termLen < s.length && s[i + termLen] == '*') termLen + 1 else 0)
                offer(Kind.WILDTERM, termLength(s, i, wild = true))
                offer(Kind.RANGE, if (c == '[' || c == '{') rangeLength(s, i) else 0)
                if (bestLen == 0) { out += Tok(Kind.ERROR, "Lexical error at column ${i + 1}: unexpected '${s[i]}'"); return out }
                out += Tok(best, s.substring(i, i + bestLen))
                i += bestLen
            }
        }

        /**
         * The `RangeIn`/`RangeEx` lexical states of `QueryParser.jj`: `[` (or `{`), whitespace-skipped goops or quoted
         * strings and an optional `TO`, closed by `]` (`}`). The parser needs exactly `value [TO] value`; anything else
         * (`[Rom]`, `[a`, `[a TO`, `{a TO b]`) is a lexical or parse error in Lucene. Returns the length of a complete
         * range, 0 when it is malformed (then a [Kind.ERROR] results).
         */
        private fun rangeLength(s: String, start: Int): Int {
            val close = if (s[start] == '[') ']' else '}'
            var j = start + 1
            val items = ArrayList<Boolean>() // true = the TO token, false = a value
            while (true) {
                while (j < s.length && isWhitespace(s[j])) j++
                if (j >= s.length) return 0
                if (s[j] == close) { j++; break }
                if (s[j] == '"') {
                    var k = j + 1
                    while (k < s.length && s[k] != '"') k += if (s[k] == '\\' && k + 1 < s.length && s[k + 1] == '"') 2 else 1
                    if (k >= s.length || k == j + 1) return 0
                    items += false
                    j = k + 1
                } else {
                    var k = j
                    while (k < s.length && s[k] != ' ' && s[k] != close) k++
                    items += (s.substring(j, k) == "TO")
                    j = k
                }
            }
            val values = items.filter { !it }.size
            val valid = values == 2 && (items.size == 2 || (items.size == 3 && items[1]))
            return if (valid) j - start else 0
        }

        /** `_QUOTED_CHAR` sequence between quotes, 0 when unterminated. */
        private fun quotedLength(s: String, start: Int): Int {
            var j = start + 1
            while (j < s.length) {
                when (s[j]) {
                    '"' -> return j - start + 1
                    '\\' -> if (j + 1 < s.length) j += 2 else return 0
                    else -> j++
                }
            }
            return 0
        }

        /** `_TERM_START_CHAR _TERM_CHAR*`; with [wild] also `*` and `?` anywhere (`WILDTERM`). 0 when no match. */
        private fun termLength(s: String, start: Int, wild: Boolean): Int {
            var j = start
            while (j < s.length) {
                val c = s[j]
                if (c == '\\') {
                    if (j + 1 < s.length) j += 2 else break
                } else if (c == '*' || c == '?') {
                    if (wild) j++ else break
                } else if ((c == '+' || c == '-') && j > start) {
                    j++
                } else if (SPECIAL.indexOf(c) < 0) {
                    j++
                } else break
            }
            return j - start
        }

        /** `"~" ( [0-9]+ ( "." [0-9]+ )? )?` */
        private fun slopLength(s: String, start: Int): Int {
            var j = start + 1
            val digitsStart = j
            while (j < s.length && s[j] in '0'..'9') j++
            if (j == digitsStart) return 1
            if (j + 1 < s.length && s[j] == '.' && s[j + 1] in '0'..'9') {
                j++
                while (j < s.length && s[j] in '0'..'9') j++
            }
            return j - start
        }
    }

    // ---- parser ----

    private enum class Conj { NONE, AND, OR }
    private enum class Mod { NONE, REQ, NOT }

    private class MutableClause(val node: SearchNode, var occur: Occur)

    private class Parser(val toks: List<Tok>, val analyzer: TextAnalyzer?) {
        var idx = 0

        /** Second pass only (see [parse]): K6-3 syntax is noted and skipped instead of thrown, to find any parse error after it. */
        var lenient = false

        private fun unsupported(syntax: String) {
            if (!lenient) throw UnsupportedSearchSyntaxException(syntax)
        }

        /** `^` boost (K6-3). Lenient: consumes the `^` and its number, as Lucene's Boost lexical state would. */
        fun skipBoost() {
            if (peek().kind != Kind.CARAT) return
            unsupported("^")
            idx++
            if (peek().kind == Kind.TERM) idx++
        }

        fun peek(k: Int = 0): Tok {
            val t = toks[minOf(idx + k, toks.size - 1)]
            if (t.kind == Kind.ERROR) throw QueryParseException(t.image)
            return t
        }

        fun next(): Tok = peek().also { idx++ }

        fun parseTop(): SearchNode? {
            val q = parseQuery(DEFAULT_FIELD, 0)
            if (peek().kind != Kind.EOF) throw QueryParseException("Cannot parse query: unexpected '${peek().image}'")
            return q
        }

        /** `Query(field)`: a flat clause sequence; null when every clause was dropped. */
        fun parseQuery(field: String, depth: Int): SearchNode? {
            val clauses = ArrayList<MutableClause>()
            var mods = modifiers()
            var q = parseClause(field, depth)
            addClause(clauses, Conj.NONE, mods, q)
            val first = if (mods == Mod.NONE) q else null
            while (peek().kind in CLAUSE_START) {
                val conj = conjunction()
                mods = modifiers()
                q = parseClause(field, depth)
                addClause(clauses, conj, mods, q)
            }
            if (clauses.size == 1 && first != null) return first
            if (clauses.isEmpty()) return null
            if (clauses.size > MAX_CLAUSES) throw QueryParseException("Cannot parse query: too many boolean clauses")
            return SearchNode.Bool(clauses.map { Clause(it.node, it.occur) })
        }

        fun conjunction(): Conj = when (peek().kind) {
            Kind.AND -> { idx++; Conj.AND }
            Kind.OR -> { idx++; Conj.OR }
            else -> Conj.NONE
        }

        fun modifiers(): Mod = when (peek().kind) {
            Kind.PLUS -> { idx++; Mod.REQ }
            Kind.MINUS, Kind.NOT -> { idx++; Mod.NOT }
            else -> Mod.NONE
        }

        /** `QueryParser.addClause` with the default OR operator. A null [q] was filtered away by the analyzer. */
        fun addClause(clauses: MutableList<MutableClause>, conj: Conj, mods: Mod, q: SearchNode?) {
            if (clauses.isNotEmpty() && conj == Conj.AND) {
                val last = clauses.last()
                if (last.occur != Occur.MUST_NOT) last.occur = Occur.MUST
            }
            if (q == null) return
            val prohibited = mods == Mod.NOT
            var required = mods == Mod.REQ
            if (conj == Conj.AND && !prohibited) required = true
            clauses += MutableClause(q, if (prohibited) Occur.MUST_NOT else if (required) Occur.MUST else Occur.SHOULD)
        }

        /** `Clause(field)`: an optional `field:` then a term or a parenthesized query. */
        fun parseClause(defaultField: String, depth: Int): SearchNode? {
            var field = defaultField
            if (peek().kind == Kind.TERM && peek(1).kind == Kind.COLON) {
                field = unescape(peek().image)
                idx += 2
            } else if (peek().kind == Kind.STAR && peek(1).kind == Kind.COLON) {
                field = "*"
                idx += 2
            }
            if (peek().kind == Kind.LPAREN) {
                idx++
                if (depth + 1 > MAX_DEPTH) throw QueryParseException("Cannot parse query: nested more than $MAX_DEPTH parentheses")
                val q = parseQuery(field, depth + 1)
                if (peek().kind != Kind.RPAREN) throw QueryParseException("Cannot parse query: expected ')' but found '${peek().image}'")
                idx++
                skipBoost()
                return q
            }
            return parseTerm(field)
        }

        /** `Term(field)`: builds the query as the grammar's actions do (wildcard, prefix, fuzzy, else analyzed field query). */
        fun parseTerm(field: String): SearchNode? {
            val tok = peek()
            when (tok.kind) {
                Kind.TERM, Kind.STAR, Kind.PREFIXTERM, Kind.WILDTERM, Kind.BAREOPER -> idx++
                Kind.RANGE -> {
                    unsupported(if (tok.image.startsWith("[")) "[a TO b]" else "{a TO b}")
                    idx++
                    skipBoost()
                    return null
                }
                Kind.QUOTED -> return parseQuoted(field)
                else -> throw QueryParseException("Cannot parse query: unexpected '${tok.image}'")
            }
            val slop = if (peek().kind == Kind.FUZZY_SLOP) next() else null
            skipBoost()
            val image = if (tok.kind == Kind.BAREOPER) tok.image.substring(0, 1) else tok.image
            when {
                tok.kind == Kind.STAR || tok.kind == Kind.WILDTERM -> {
                    requireField(field)
                    // QueryParser.Term(): discardEscapeChar over the whole term, then getWildcardQuery lowercases.
                    val pattern = unescape(image).lowercase()
                    return if (field == SearchFields.STRONG) SearchNode.StrongTerm(pattern, true) else SearchNode.Wildcard(pattern)
                }
                tok.kind == Kind.PREFIXTERM -> {
                    requireField(field)
                    // getPrefixQuery gets the unescaped term without the final `*`; PrefixQuery takes it literally,
                    // so an escaped `\*` or `\?` in it is an ordinary character, not a wildcard.
                    val prefix = unescape(image.substring(0, image.length - 1)).lowercase()
                    return if (field == SearchFields.STRONG) {
                        // Strong's tokens never contain `*` or `?`: such a literal prefix matches nothing.
                        if (prefix.any { it == '*' || it == '?' }) SearchNode.Bool(emptyList()) else SearchNode.StrongTerm("$prefix*", true)
                    } else SearchNode.Prefix(prefix)
                }
                slop != null -> {
                    requireField(field)
                    if (field == SearchFields.STRONG) { unsupported("strong:term~"); return null }
                    val similarity = slop.image.substring(1).toFloatOrNull() ?: DEFAULT_FUZZY_SIMILARITY
                    if (similarity < 0f || similarity > 1f) {
                        throw QueryParseException("Minimum similarity for a FuzzyQuery has to be between 0.0f and 1.0f !")
                    }
                    if (similarity >= 1f) throw QueryParseException("minimumSimilarity >= 1")
                    return SearchNode.Fuzzy(unescape(image).lowercase(), similarity)
                }
                else -> {
                    requireField(field)
                    val text = unescape(image)
                    return if (field == SearchFields.STRONG) {
                        if (StrongsTokens.fromLexical(text).isEmpty()) null else SearchNode.StrongTerm(text, false)
                    } else if (isEmpty(text)) null else SearchNode.Term(text)
                }
            }
        }

        fun parseQuoted(field: String): SearchNode? {
            val tok = next()
            val slopTok = if (peek().kind == Kind.FUZZY_SLOP) next() else null
            skipBoost()
            requireField(field)
            val slop = slopTok?.image?.substring(1)?.toFloatOrNull()?.toInt() ?: 0
            val text = unescape(tok.image.substring(1, tok.image.length - 1))
            if (field == SearchFields.STRONG) {
                // One number is a term query in Lucene too. Several would be a phrase over the strong field's token
                // positions, which the strongs table does not keep.
                // Count the numbers as written: `G3588!a` is one number (the filter emits its two tokens for it).
                val numbers = text.split(Regex("\\s+")).filter { it.isNotEmpty() && StrongsTokens.normalize(it) != null }
                if (numbers.size > 1) { unsupported("strong:\"phrase\""); return null }
                return if (numbers.isEmpty()) null else SearchNode.StrongTerm(text, false)
            }
            return if (isEmpty(text)) null else SearchNode.Phrase(text, slop)
        }

        fun requireField(field: String) {
            if (field != DEFAULT_FIELD && field != SearchFields.STRONG) unsupported("$field:")
        }

        fun isEmpty(text: String): Boolean = analyzer != null && analyzer.analyze(text).isEmpty()
    }

    private val CLAUSE_START = setOf(
        Kind.AND, Kind.OR, Kind.NOT, Kind.PLUS, Kind.MINUS, Kind.BAREOPER, Kind.LPAREN, Kind.STAR, Kind.QUOTED,
        Kind.TERM, Kind.FUZZY_SLOP, Kind.PREFIXTERM, Kind.WILDTERM, Kind.RANGE,
    )

    /** `QueryParser.discardEscapeChar`: removes backslashes; `\uXXXX` is the UTF-16 unit. */
    private fun unescape(input: String): String {
        if (input.indexOf('\\') < 0) return input
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i++]
            if (c != '\\') { sb.append(c); continue }
            if (i >= input.length) throw QueryParseException("Term can not end with escape character.")
            val e = input[i++]
            if (e == 'u') {
                if (i + 4 > input.length) throw QueryParseException("Truncated unicode escape sequence.")
                val code = input.substring(i, i + 4).toIntOrNull(16)
                    ?.takeIf { input.substring(i, i + 4).all { h -> h in '0'..'9' || h in 'a'..'f' || h in 'A'..'F' } }
                    ?: throw QueryParseException("Invalid hexadecimal digit in unicode escape.")
                sb.append(code.toChar())
                i += 4
            } else sb.append(e)
        }
        return sb.toString()
    }
}
