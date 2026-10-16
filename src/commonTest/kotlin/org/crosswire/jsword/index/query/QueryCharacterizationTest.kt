package org.crosswire.jsword.index.query

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.index.Index
import org.crosswire.jsword.index.search.SearchModifier
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.passage.PassageTally
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Characterization of the query tree, the default query builder and the default query decorator (port of the JVM
 * test of the Lucene classes; runs on iOS too). Expected values derive from reading the builder's RANGE_PATTERN,
 * BLUR_PATTERN and assembly order, and the decorator, not from observation.
 */
class QueryCharacterizationTest {
    private val v11n = Versifications.instance().getVersification("KJV")

    /** Writes the tree as Name(query), Name(left,right) or Name(left,right|factor); a NullQuery is NullQuery(). */
    private fun dump(q: Query?): String {
        val name = q!!::class.simpleName
        return when (q) {
            is BlurQuery -> "$name(${dump(q.leftQuery)},${dump(q.rightQuery)}|${q.factor})"
            is AbstractBinaryQuery -> "$name(${dump(q.leftQuery)},${dump(q.rightQuery)})"
            is AbstractQuery -> "$name(${q.query})"
            else -> "$name()"
        }
    }

    private fun parse(s: String?) = dump(QueryBuilderFactory.getQueryBuilder().parse(s))

    // ---- builder: no range, no blur -> a single BaseQuery of the whole text ----

    @Test fun plainTextIsOneBaseQuery() {
        assertEquals("BaseQuery(love)", parse("love"))
        assertEquals("BaseQuery(love joy)", parse("love joy"))
        // + and - are query syntax, the builder only looks for [..] and ~
        assertEquals("BaseQuery(+love -hate)", parse("+love -hate"))
        assertEquals("BaseQuery(\"in the beginning\")", parse("\"in the beginning\""))
        assertEquals("BaseQuery(love & joy)", parse("love & joy"))
        assertEquals("BaseQuery(love | joy)", parse("love | joy"))
        assertEquals("BaseQuery(strong:G3056)", parse("strong:G3056"))
    }

    @Test fun nonAsciiTextPassesThroughUnchanged() {
        assertEquals("BaseQuery(café λόγος)", parse("café λόγος"))
    }

    @Test fun emptyAndNullGiveTheNullQuery() {
        assertEquals("NullQuery()", parse(""))
        assertEquals("NullQuery()", parse(null))
    }

    @Test fun whitespaceOnlyIsNotEmpty() {
        assertEquals("BaseQuery(   )", parse("   "))
    }

    // ---- builder: blur "\s~(\d*)?\s" ----

    @Test fun blurWithFactor() {
        assertEquals("BlurQuery(BaseQuery(love),BaseQuery(joy)|2)", parse("love ~2 joy"))
    }

    @Test fun blurWithoutDigitsDefaultsToOne() {
        assertEquals("BlurQuery(BaseQuery(love),BaseQuery(joy)|1)", parse("love ~ joy"))
    }

    @Test fun blurNeedsWhitespaceOnBothSides() {
        assertEquals("BaseQuery(love~2 joy)", parse("love~2 joy"))
        assertEquals("BaseQuery(love ~2)", parse("love ~2"))
    }

    @Test fun onlyTheFirstBlurIsUsed() {
        assertEquals("BlurQuery(BaseQuery(a),BaseQuery(b ~ c)|1)", parse("a ~ b ~ c"))
    }

    @Test fun blurFactorThatOverflowsAnIntThrows() {
        assertFailsWith<NumberFormatException> { parse("a ~99999999999 b") }
    }

    // ---- builder: range "\s*([-+]?)\[([^\[\]]+)\]\s*" ----

    @Test fun rangeWithoutModifierIsAnded() {
        // the matched "[Gen-Exo] " (with trailing space) is replaced by one space
        assertEquals("AndQuery(RangeQuery(Gen-Exo),BaseQuery( light))", parse("[Gen-Exo] light"))
    }

    @Test fun rangeWithPlusIsAndedAndOnlyTheFirstRangeIsParsed() {
        assertEquals("AndQuery(RangeQuery(Mat),BaseQuery( -[Mat.5] peace))", parse("+[Mat] -[Mat.5] peace"))
    }

    @Test fun rangeWithMinusIsAndNotAfterTheRestrictedQuery() {
        assertEquals("AndNotQuery(BaseQuery( peace),RangeQuery(Mat))", parse("-[Mat] peace"))
        assertEquals("AndNotQuery(BaseQuery(peace ),RangeQuery(Mat))", parse("peace -[Mat]"))
    }

    @Test fun surroundingWhitespaceOfARangeIsConsumed() {
        assertEquals("AndQuery(RangeQuery(Gen),BaseQuery( love))", parse("  [Gen]  love"))
    }

    @Test fun rangeAloneLeavesASingleSpaceAsTheText() {
        assertEquals("AndQuery(RangeQuery(Gen),BaseQuery( ))", parse("[Gen]"))
    }

    @Test fun rangeAndBlurCombine() {
        assertEquals("AndQuery(RangeQuery(Gen),BlurQuery(BaseQuery( love),BaseQuery(joy)|1))", parse("[Gen] love ~ joy"))
    }

    @Test fun rangeTextIsReplacedEverywhereItOccurs() {
        assertEquals("AndQuery(RangeQuery(A),BaseQuery( x  y))", parse("[A] x [A] y"))
    }

    @Test fun nestedBracketsMatchTheInnerPair() {
        assertEquals("AndQuery(RangeQuery(B),BaseQuery([A ]))", parse("[A[B]]"))
    }

    // ---- query classes ----

    @Test fun accessorsReturnConstructorArguments() {
        val l = BaseQuery("l")
        val r = RangeQuery("Gen")
        val b = BlurQuery(l, r, 7)
        assertSame(l, b.leftQuery)
        assertSame(r, b.rightQuery)
        assertEquals(7, b.factor)
        assertEquals("l", l.query)
        assertEquals("Gen", r.query)
        assertTrue(AndQuery(l, r) is BinaryQuery)
        assertTrue(OrQuery(l, r) is BinaryQuery)
        assertTrue(AndNotQuery(l, r) is BinaryQuery)
        assertFalse((NullQuery() as Query) is BinaryQuery)
    }

    @Test fun baseQueryAcceptsNullText() {
        assertNull(BaseQuery(null).query)
        assertEquals("BaseQuery(null)", dump(BaseQuery(null)))
    }

    // ---- find() against a stub index ----

    /** Index whose find(String) maps the query text to a passage reference. */
    private inner class StubIndex : Index {
        val refs = HashMap<String, String>()
        val ranked = HashMap<String, Int>()
        val asked = ArrayList<String?>()
        private var modifier: SearchModifier? = null

        override fun find(query: String?): Key {
            asked.add(query)
            if (query == null) return PassageKeyFactory.instance().createEmptyKeyList(v11n)
            val ref = refs[query]
            if (ranked.containsKey(query)) {
                val t = PassageTally(v11n)
                t.add(if (ref == null) PassageKeyFactory.instance().createEmptyKeyList(v11n) else keyOf(ref), ranked[query]!!)
                return t
            }
            return if (ref == null) PassageKeyFactory.instance().createEmptyKeyList(v11n) else keyOf(ref)
        }

        override fun getKey(name: String?): Key = PassageKeyFactory.instance().getKey(v11n, name)
        override var searchModifier: SearchModifier?
            get() = modifier
            set(value) { modifier = value }
        override fun close() {}

        private fun keyOf(ref: String): Key = try {
            PassageKeyFactory.instance().getKey(v11n, ref)
        } catch (e: NoSuchKeyException) {
            throw IllegalStateException(e)
        }
    }

    private fun index() = StubIndex().apply {
        refs["a"] = "Gen 1:1-5"
        refs["b"] = "Gen 1:3-8"
        refs["c"] = "Gen 1:3"
    }

    @Test fun baseQueryPassesItsTextToTheIndex() {
        val i = index()
        assertEquals(5, BaseQuery("a").find(i).getCardinality())
        assertEquals("[a]", i.asked.toString())
    }

    @Test fun nullQueryAsksTheIndexForNull() {
        val i = index()
        assertTrue(NullQuery().find(i).isEmpty())
        assertEquals(1, i.asked.size)
        assertNull(i.asked[0])
    }

    @Test fun andIntersects() {
        assertEquals("Gen.1.3-Gen.1.5", AndQuery(BaseQuery("a"), BaseQuery("b")).find(index()).getOsisRef())
    }

    @Test fun andStopsAtAnEmptyLeftWithoutAskingTheRight() {
        val i = index()
        assertTrue(AndQuery(BaseQuery("none"), BaseQuery("a")).find(i).isEmpty())
        assertEquals("[none]", i.asked.toString())
    }

    @Test fun andPrefersARankedRightResult() {
        val i = index()
        i.ranked["b"] = 3
        val k = AndQuery(BaseQuery("a"), BaseQuery("b")).find(i)
        assertTrue(k is PassageTally)
        assertEquals("Gen.1.3-Gen.1.5", k.getOsisRef())
    }

    @Test fun orUnionsAndAsksBothSides() {
        val i = index()
        assertEquals("Gen.1.1-Gen.1.5", OrQuery(BaseQuery("a"), BaseQuery("c")).find(i).getOsisRef())
        // unlike And, Or evaluates the right side even if the left is empty
        i.asked.clear()
        val r = OrQuery(BaseQuery("none"), BaseQuery("c")).find(i)
        assertEquals("[none, c]", i.asked.toString())
        assertEquals("Gen.1.3", r.getOsisRef())
    }

    @Test fun andNotSubtracts() {
        val k = AndNotQuery(BaseQuery("a"), BaseQuery("c")).find(index())
        assertEquals(4, k.getCardinality())
        assertEquals("Gen.1.1-Gen.1.2 Gen.1.4-Gen.1.5", k.getOsisRef())
    }

    @Test fun blurWidensTheRightByTheFactorBeforeIntersecting() {
        // c = Gen 1:3, blurred by 1 = Gen 1:2-4; a = Gen 1:1-5
        assertEquals("Gen.1.2-Gen.1.4", BlurQuery(BaseQuery("a"), BaseQuery("c"), 1).find(index()).getOsisRef())
    }

    @Test fun rangeQueryResolvesThroughIndexGetKey() {
        assertEquals("Gen.1.1-Gen.1.2", RangeQuery("Gen 1:1-2").find(index()).getOsisRef())
    }

    @Test fun rangeQueryWrapsAnUnparsableRangeInABookException() {
        val e = assertFailsWith<BookException> { RangeQuery("zzz 99").find(index()) }
        assertTrue(e.cause is NoSuchKeyException)
        assertTrue(e.message!!.contains("zzz 99"), e.message)
    }

    // ---- decorator ----

    private val decorator = QueryDecoratorFactory.getSearchSyntax()

    @Test fun decoratorsOnWords() {
        val w = "in the beginning"
        assertEquals("\"in the beginning\"", decorator.decoratePhrase(w))
        assertEquals("+in +the +beginning", decorator.decorateAllWords(w))
        assertEquals("in the beginning", decorator.decorateAnyWords(w))
        assertEquals("-in -the -beginning", decorator.decorateNotWords(w))
        assertEquals("in* the* beginning*", decorator.decorateStartWords(w))
        assertEquals("in~ the~ beginning~", decorator.decorateSpellWords(w))
        assertEquals("+[in the beginning]", decorator.decorateRange(w))
    }

    @Test fun decoratorsOnTwoWords() {
        assertEquals("\"a b\"", decorator.decoratePhrase("a b"))
        assertEquals("+a +b", decorator.decorateAllWords("a b"))
        assertEquals("a b", decorator.decorateAnyWords("a b"))
        assertEquals("-a -b", decorator.decorateNotWords("a b"))
        assertEquals("a* b*", decorator.decorateStartWords("a b"))
        assertEquals("a~ b~", decorator.decorateSpellWords("a b"))
        assertEquals("+[a b]", decorator.decorateRange("a b"))
    }

    @Test fun decoratorsOnTheEmptyString() {
        // "".split(" ") is [""], so the word decorators still add their one marker
        assertEquals("\"\"", decorator.decoratePhrase(""))
        assertEquals("+", decorator.decorateAllWords(""))
        assertEquals("", decorator.decorateAnyWords(""))
        assertEquals("-", decorator.decorateNotWords(""))
        assertEquals("*", decorator.decorateStartWords(""))
        assertEquals("~", decorator.decorateSpellWords(""))
        assertEquals("+[]", decorator.decorateRange(""))
    }

    @Test fun decoratorsSplitOnEverySingleSpace() {
        // "a  b".split(" ") is ["a", "", "b"]: the empty middle word is decorated too
        assertEquals("+a + +b", decorator.decorateAllWords("a  b"))
        assertEquals("a* * b*", decorator.decorateStartWords("a  b"))
        // trailing empty strings are dropped, as Java's split does
        assertEquals("+a", decorator.decorateAllWords("a "))
    }
}
