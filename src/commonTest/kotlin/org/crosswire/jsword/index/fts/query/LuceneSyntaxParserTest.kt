package org.crosswire.jsword.index.fts.query

import org.crosswire.jsword.index.analysis.SWIFT_ORACLE_QUERY_CASES
import org.crosswire.jsword.index.analysis.SimpleAnalyzer
import org.crosswire.jsword.index.fts.UnsupportedSearchSyntaxException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LuceneSyntaxParserTest {
    private val analyzer = SimpleAnalyzer()

    private fun lucene(q: String) = LuceneSyntaxParser.parse(q, analyzer).toLuceneString()

    /** Android `SearchControl.cleanSearchString` and the JSword decorator, as the Swift fixture applies them. */
    private fun prepare(input: String, wordMode: String): String {
        val cleaned = input.replace("strong:", "strongCOLON").replace(":", " ").replace("strongCOLON", "strong:")
            .replace("  ", " ").trim(' ', '\t', '\n', '\r')
        return when (wordMode) {
            "allWords" -> "+" + cleaned.split(" ").joinToString(" +")
            "phrase" -> "\"$cleaned\""
            else -> cleaned
        }
    }

    @Test fun swiftOracleCasesGiveLucenesToString() {
        var checked = 0
        for (c in SWIFT_ORACLE_QUERY_CASES) {
            val query = prepare(c.input, c.wordMode)
            if (c.lucene != null) {
                assertEquals(c.lucene, lucene(query), c.name)
                checked++
            } else {
                assertEquals("ParseException", c.luceneError, c.name)
                assertFailsWith<QueryParseException>(c.name) { LuceneSyntaxParser.parse(query, analyzer) }
                checked++
            }
        }
        assertEquals(32, checked)
    }

    @Test fun unsupportedForms() {
        for (q in listOf("heading:x", "x^2", "[a TO b]", "{a TO b}", "faith AND [a TO b]", "(faith)^3", "\"a b\"^2", "*:*", "a:b", "strong:\"h1 h2\"", "strong:h1~")) {
            assertFailsWith<UnsupportedSearchSyntaxException>(q) { LuceneSyntaxParser.parse(q, analyzer) }
        }
        assertEquals("heading:", assertFailsWith<UnsupportedSearchSyntaxException> { LuceneSyntaxParser.parse("heading:x") }.syntax)
    }

    @Test fun aQuotedStrongNumberIsATerm() {
        assertEquals(SearchNode.StrongTerm("h7225", false), LuceneSyntaxParser.parse("strong:\"h7225\""))
        assertEquals(SearchNode.Bool(emptyList()), LuceneSyntaxParser.parse("strong:\"xyz\""))
        assertEquals("strong:h1 strong:h2", lucene("strong:(h1 h2)"), "the field applies to the group")
        assertEquals(SearchNode.Phrase("faith hope", 0), LuceneSyntaxParser.parse("\"faith hope\"~0.9"), "slop is truncated, not rounded")
        assertEquals(SearchNode.Phrase("faith hope", 3), LuceneSyntaxParser.parse("\"faith hope\"~3.5"))
    }

    @Test fun parseErrorsAreTyped() {
        for (q in listOf("", " ", "+", "-", "!", "+ -", "faith AND", "AND faith", "OR", "\"faith", "faith\"", "(faith", "faith)", "()", "(()",
            "--faith", "++faith", "faith +", "faith -", "~faith", "~", "^", ":", "faith\\", "]", "}", "faith AND OR hope", "\"a\" )")) {
            assertFailsWith<QueryParseException>("'$q'") { LuceneSyntaxParser.parse(q, analyzer) }
        }
    }

    /** Lucene's RangeIn/RangeEx lexical states: only `value [TO] value` closed by the right bracket is a range; the rest is a ParseException. */
    @Test fun bracketsThatAreNotACompleteRangeAreParseErrors() {
        for (q in listOf("[Rom] love", "love [Rom]", "[", "{", "[a", "{a", "[a TO", "{a TO b]", "[a TO b}", "faith [a", "[a TO b TO c]", "[TO a]",
            "[a TO TO]", "[a b c]", "[\"a", "[]", "{}", "heading:a [x", "a^2 [x", "[a TO b] [x")) {
            assertFailsWith<QueryParseException>("'$q'") { LuceneSyntaxParser.parse(q, analyzer) }
        }
        for (q in listOf("[a TO b]", "{a TO b}", "[a b]", "[\"a b\" TO c]", "faith [a TO b] hope", "[ a  TO  b ]", "[a TO b]^2", "{a b}")) {
            val e = assertFailsWith<UnsupportedSearchSyntaxException>("'$q'") { LuceneSyntaxParser.parse(q, analyzer) }
            assertEquals(if (q.contains("[")) "[a TO b]" else "{a TO b}", e.syntax, q)
        }
    }

    /** K6-3 syntax followed by a real parse error fails like Lucene (ParseException), not as "unsupported". */
    @Test fun aParseErrorAfterUnsupportedSyntaxWins() {
        for (q in listOf("heading:x )", "a^2 )", "[a TO b] )", "strong:\"h1 h2\" )", "strong:h1~ )", "heading:x (a", "a^2 \"b", "(a^2", "x:y AND", "[a TO b] [c")) {
            assertFailsWith<QueryParseException>("'$q'") { LuceneSyntaxParser.parse(q, analyzer) }
        }
        for (q in listOf("heading:x", "a^2 b", "a^2 b^3", "heading:x AND (a b)", "[a TO b] c")) {
            assertFailsWith<UnsupportedSearchSyntaxException>("'$q'") { LuceneSyntaxParser.parse(q, analyzer) }
        }
    }

    @Test fun bareOperatorsAreDroppedTerms() {
        assertEquals("content:faith", lucene("+ faith"))
        assertEquals("content:faith content:hope", lucene("faith + hope"))
        assertEquals("content:faith content:hope", lucene("faith ! hope"))
        assertEquals("", lucene("+ ")) // BAREOPER alone: every clause dropped -> empty boolean query
    }

    @Test fun clauseRulesOfAddClause() {
        assertEquals("+content:a +content:b content:c", lucene("a AND b OR c"))
        assertEquals("+content:a -content:b", lucene("a AND NOT b"))
        assertEquals("content:a -content:b", lucene("a OR NOT b"))
    }

    @Test fun anAndAfterADroppedClauseStillRequiresTheLastKeptClause() {
        // Lucene addClause marks the previous *kept* clause MUST before it discards a null (analyzed-away) query.
        assertEquals("+content:a +content:b", lucene("a OR \\* AND b"))
        assertEquals("+content:b", lucene("\\* AND b"))
    }

    @Test fun nestedGroupsAndSingleClauseCollapse() {
        assertEquals("content:faith", lucene("((faith))"))
        assertEquals("+content:faith", lucene("+faith"))
        assertEquals("+(content:faith content:hope) +content:love", lucene("(faith OR hope) AND love"))
        assertEquals("content:a (content:b -content:c)", lucene("a (b -c)"))
    }

    @Test fun fieldsEscapesAndTermForms() {
        assertEquals("strong:h7225", lucene("strong:h7225"))
        assertEquals("content:faith", lucene("content:faith"))
        assertEquals(SearchNode.StrongTerm("H72*", true), LuceneSyntaxParser.parse("strong:H72*"))
        assertEquals(SearchNode.StrongTerm("H07225", false), LuceneSyntaxParser.parse("strong:H07225"))
        assertEquals(SearchNode.Term("fa ith"), LuceneSyntaxParser.parse("fa\\ ith"))
        assertEquals(SearchNode.Term("a(b"), LuceneSyntaxParser.parse("a\\(b"))
        assertEquals(SearchNode.Term("aéb"), LuceneSyntaxParser.parse("a\\u00e9b"))
        assertEquals(SearchNode.Term("well-known"), LuceneSyntaxParser.parse("well-known"))
        assertEquals(SearchNode.Term("a+b"), LuceneSyntaxParser.parse("a+b"))
        assertEquals(SearchNode.Term("&&x"), LuceneSyntaxParser.parse("&&x"))
        assertEquals(SearchNode.Term("ANDROID"), LuceneSyntaxParser.parse("ANDROID"))
        assertFailsWith<QueryParseException> { LuceneSyntaxParser.parse("a\\u00zz") }
        assertFailsWith<QueryParseException> { LuceneSyntaxParser.parse("a\\u00") }
    }

    @Test fun wildcardsFuzzyAndProximityNodes() {
        assertEquals(SearchNode.Prefix("faith"), LuceneSyntaxParser.parse("FAITH*"))
        assertEquals(SearchNode.Wildcard("*"), LuceneSyntaxParser.parse("*"))
        assertEquals(SearchNode.Wildcard("**"), LuceneSyntaxParser.parse("**"))
        assertEquals(SearchNode.Wildcard("?"), LuceneSyntaxParser.parse("?"))
        assertEquals(SearchNode.Wildcard("*neld"), LuceneSyntaxParser.parse("*Neld"))
        assertEquals(SearchNode.Wildcard("f*h"), LuceneSyntaxParser.parse("f*h"))
        assertEquals(SearchNode.Wildcard("fa?th"), LuceneSyntaxParser.parse("fa?th"))
        assertEquals(SearchNode.Prefix("ä"), LuceneSyntaxParser.parse("Ä*"))
        assertEquals(SearchNode.Fuzzy("faith", 0.5f), LuceneSyntaxParser.parse("faith~"))
        assertEquals(SearchNode.Fuzzy("faith", 0.8f), LuceneSyntaxParser.parse("Faith~0.8"))
        assertEquals(SearchNode.Phrase("faith hope", 3), LuceneSyntaxParser.parse("\"faith hope\"~3"))
        assertEquals(SearchNode.Phrase("faith hope", 0), LuceneSyntaxParser.parse("\"faith hope\"~"))
        assertEquals(SearchNode.Phrase("\"faith\"", 0), LuceneSyntaxParser.parse("\"\\\"faith\\\"\""))
        assertEquals(SearchNode.Phrase("faith hope", 3), LuceneSyntaxParser.parse("\"faith hope\"~3.9"))
        // Out-of-range similarity: ParseException (above 1) and, in the baseline, a raw IllegalArgumentException (exactly 1).
        assertFailsWith<QueryParseException> { LuceneSyntaxParser.parse("faith~2") }
        assertFailsWith<QueryParseException> { LuceneSyntaxParser.parse("faith~1") }
    }

    @Test fun emptyAnalysisDropsTheClauseOnlyWithAnAnalyzer() {
        assertEquals(SearchNode.Bool(emptyList()), LuceneSyntaxParser.parse("\"  \"", analyzer))
        assertEquals(SearchNode.Bool(emptyList()), LuceneSyntaxParser.parse("\\*", analyzer))
        assertEquals(SearchNode.Phrase("  ", 0), LuceneSyntaxParser.parse("\"  \""))
        assertEquals(SearchNode.Bool(emptyList()), LuceneSyntaxParser.parse("-\\* \"1\"", analyzer)) // "1" has no letters
    }

    /** `QueryParser.Term()` unescapes the whole term first (review of task 11, oracle-checked in the smoke test). */
    @Test fun escapesAreRemovedBeforeWildcardPrefixAndFuzzyHandling() {
        assertEquals(SearchNode.Prefix("b?ess"), LuceneSyntaxParser.parse("B\\?ess*"), "an escaped ? in a prefix is literal")
        assertEquals(SearchNode.Prefix("a*b"), LuceneSyntaxParser.parse("a\\*b*"))
        assertEquals(SearchNode.Wildcard("b?es?"), LuceneSyntaxParser.parse("B\\?es?"), "unescaped, the ? is a wildcard again")
        assertEquals(SearchNode.Wildcard("café?"), LuceneSyntaxParser.parse("Caf\\u00e9?"))
        assertEquals(SearchNode.Wildcard("a*b*x"), LuceneSyntaxParser.parse("a\\*b*x"))
        assertEquals(SearchNode.Fuzzy("b?ess", 0.5f), LuceneSyntaxParser.parse("B\\?ess~"))
        assertEquals(SearchNode.StrongTerm("H7*", true), LuceneSyntaxParser.parse("strong:H7*"))
        assertEquals(SearchNode.Bool(emptyList()), LuceneSyntaxParser.parse("strong:h\\*7*"), "literal * never occurs in a Strong's token")
    }

    @Test fun quotedStrongWithPartCountsAsOneNumber() {
        assertEquals(SearchNode.StrongTerm("G3588!a", false), LuceneSyntaxParser.parse("strong:\"G3588!a\""))
        assertFailsWith<UnsupportedSearchSyntaxException> { LuceneSyntaxParser.parse("strong:\"h7225 g26\"") }
    }

    @Test fun lexerTiesAndLongestMatch() {
        assertIs<SearchNode.Bool>(LuceneSyntaxParser.parse("a AND b"))
        assertEquals(SearchNode.Term("AND:x"), LuceneSyntaxParser.parse("AND\\:x"))
        // "AND*" is longer as a prefix term than the AND token.
        assertEquals(SearchNode.Prefix("and"), LuceneSyntaxParser.parse("AND*"))
        assertEquals(SearchNode.Term("a"), LuceneSyntaxParser.parse("a"))
        assertEquals("content:faith content:hope", lucene("faith　hope")) // ideographic space separates
    }

    @Test fun longQueriesAreLinearAndDeepNestingIsATypedFailure() {
        val flat = (1..1000).joinToString(" ") { "w$it" }
        assertEquals(1000, (LuceneSyntaxParser.parse(flat) as SearchNode.Bool).clauses.size)
        val e = assertFailsWith<QueryParseException> { LuceneSyntaxParser.parse((1..2000).joinToString(" ") { "w$it" }) }
        assertTrue("too many" in e.message!!)
        val ok = "(".repeat(LuceneSyntaxParser.MAX_DEPTH) + "faith" + ")".repeat(LuceneSyntaxParser.MAX_DEPTH)
        assertEquals(SearchNode.Term("faith"), LuceneSyntaxParser.parse(ok))
        val deep = "(".repeat(100_000) + "faith" + ")".repeat(100_000)
        assertFailsWith<QueryParseException> { LuceneSyntaxParser.parse(deep) }
    }
}
