/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2005 - 2016
 */
package org.crosswire.jsword.index.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.index.Index;
import org.crosswire.jsword.index.search.SearchModifier;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.crosswire.jsword.passage.PassageKeyFactory;
import org.crosswire.jsword.passage.PassageTally;
import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.system.Versifications;
import org.junit.Test;

/**
 * Characterization of the query tree, the query builder (the Lucene
 * implementation, resolved through QueryBuilder.plugin) and the query decorator
 * (QueryDecorator.plugin). Expected values are derived from reading
 * LuceneQueryBuilder (RANGE_PATTERN, BLUR_PATTERN and the assembly order) and
 * LuceneQueryDecorator, not from observation.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class QueryCharacterizationTest {
    private static final Versification V11N = Versifications.instance().getVersification("KJV");

    /**
     * Writes the tree as SimpleClassName(getQuery()), SimpleClassName(left,right)
     * or SimpleClassName(left,right|factor); a NullQuery is NullQuery().
     */
    static String dump(Query q) {
        String name = q.getClass().getSimpleName();
        if (q instanceof BlurQuery) {
            BlurQuery b = (BlurQuery) q;
            return name + "(" + dump(b.getLeftQuery()) + "," + dump(b.getRightQuery()) + "|" + b.getFactor() + ")";
        }
        if (q instanceof AbstractBinaryQuery) {
            AbstractBinaryQuery b = (AbstractBinaryQuery) q;
            return name + "(" + dump(b.getLeftQuery()) + "," + dump(b.getRightQuery()) + ")";
        }
        if (q instanceof AbstractQuery) {
            return name + "(" + ((AbstractQuery) q).getQuery() + ")";
        }
        return name + "()";
    }

    private static String parse(String s) {
        return dump(QueryBuilderFactory.getQueryBuilder().parse(s));
    }

    // ---- builder: no range, no blur -> a single BaseQuery of the whole text ----

    @Test
    public void plainTextIsOneBaseQuery() {
        assertEquals("BaseQuery(love)", parse("love"));
        assertEquals("BaseQuery(love joy)", parse("love joy"));
        // + and - are Lucene syntax, the builder only looks for [..] and ~
        assertEquals("BaseQuery(+love -hate)", parse("+love -hate"));
        assertEquals("BaseQuery(\"in the beginning\")", parse("\"in the beginning\""));
        assertEquals("BaseQuery(love & joy)", parse("love & joy"));
        assertEquals("BaseQuery(love | joy)", parse("love | joy"));
        assertEquals("BaseQuery(strong:G3056)", parse("strong:G3056"));
    }

    @Test
    public void nonAsciiTextPassesThroughUnchanged() {
        assertEquals("BaseQuery(café λόγος)", parse("café λόγος"));
    }

    @Test
    public void emptyAndNullGiveTheNullQuery() {
        // parse returns NULL_QUERY when the text is null or has length 0
        assertEquals("NullQuery()", parse(""));
        assertEquals("NullQuery()", dump(QueryBuilderFactory.getQueryBuilder().parse(null)));
    }

    @Test
    public void whitespaceOnlyIsNotEmpty() {
        // length is 3, no range, no blur: the final else-if (length > 0) wins
        assertEquals("BaseQuery(   )", parse("   "));
    }

    // ---- builder: blur "\s~(\d*)?\s" ----

    @Test
    public void blurWithFactor() {
        // " ~2 " matches at 4..8: left = "love", right = "joy"
        assertEquals("BlurQuery(BaseQuery(love),BaseQuery(joy)|2)", parse("love ~2 joy"));
    }

    @Test
    public void blurWithoutDigitsDefaultsToOne() {
        // group(1) is "" so the factor stays at its initial 1
        assertEquals("BlurQuery(BaseQuery(love),BaseQuery(joy)|1)", parse("love ~ joy"));
    }

    @Test
    public void blurNeedsWhitespaceOnBothSides() {
        assertEquals("BaseQuery(love~2 joy)", parse("love~2 joy"));
        assertEquals("BaseQuery(love ~2)", parse("love ~2"));
    }

    @Test
    public void onlyTheFirstBlurIsUsed() {
        // find() takes the first match; everything after it is the right side verbatim
        assertEquals("BlurQuery(BaseQuery(a),BaseQuery(b ~ c)|1)", parse("a ~ b ~ c"));
    }

    @Test
    public void blurFactorThatOverflowsAnIntThrows() {
        try {
            parse("a ~99999999999 b");
            fail("Integer.parseInt of 11 digits must overflow");
        } catch (NumberFormatException expected) {
            // expected
        }
    }

    // ---- builder: range "\s*([-+]?)\[([^\[\]]+)\]\s*" ----

    @Test
    public void rangeWithoutModifierIsAnded() {
        // the matched "[Gen-Exo] " (with trailing space) is replaced by one space
        assertEquals("AndQuery(RangeQuery(Gen-Exo),BaseQuery( light))", parse("[Gen-Exo] light"));
    }

    @Test
    public void rangeWithPlusIsAndedAndOnlyTheFirstRangeIsParsed() {
        // the first match is "+[Mat] "; the "-[Mat.5]" stays in the remaining text
        assertEquals("AndQuery(RangeQuery(Mat),BaseQuery( -[Mat.5] peace))", parse("+[Mat] -[Mat.5] peace"));
    }

    @Test
    public void rangeWithMinusIsAndNotAfterTheRestrictedQuery() {
        assertEquals("AndNotQuery(BaseQuery( peace),RangeQuery(Mat))", parse("-[Mat] peace"));
        // leading \s* swallows the space before the '-'; the text keeps one replacement space
        assertEquals("AndNotQuery(BaseQuery(peace ),RangeQuery(Mat))", parse("peace -[Mat]"));
    }

    @Test
    public void surroundingWhitespaceOfARangeIsConsumed() {
        assertEquals("AndQuery(RangeQuery(Gen),BaseQuery( love))", parse("  [Gen]  love"));
    }

    @Test
    public void rangeAloneLeavesASingleSpaceAsTheText() {
        // sought becomes " " (length 1), so it is a BaseQuery and not the NullQuery
        assertEquals("AndQuery(RangeQuery(Gen),BaseQuery( ))", parse("[Gen]"));
    }

    @Test
    public void rangeAndBlurCombine() {
        // range removed first (" love ~ joy"), then blur at 5..8, then the range ANDed outside
        assertEquals("AndQuery(RangeQuery(Gen),BlurQuery(BaseQuery( love),BaseQuery(joy)|1))", parse("[Gen] love ~ joy"));
    }

    @Test
    public void rangeTextIsReplacedEverywhereItOccurs() {
        // String.replace replaces every occurrence of the matched "[A] ", not just the first
        assertEquals("AndQuery(RangeQuery(A),BaseQuery( x  y))", parse("[A] x [A] y"));
    }

    @Test
    public void nestedBracketsMatchTheInnerPair() {
        // [^\[\]]+ cannot contain brackets, so "[B]" is the range and "[A" + " " + "]" is left
        assertEquals("AndQuery(RangeQuery(B),BaseQuery([A ]))", parse("[A[B]]"));
    }

    // ---- query classes ----

    @Test
    public void accessorsReturnConstructorArguments() {
        BaseQuery l = new BaseQuery("l");
        RangeQuery r = new RangeQuery("Gen");
        BlurQuery b = new BlurQuery(l, r, 7);
        assertSame(l, b.getLeftQuery());
        assertSame(r, b.getRightQuery());
        assertEquals(7, b.getFactor());
        assertEquals("l", l.getQuery());
        assertEquals("Gen", r.getQuery());
        assertTrue(new AndQuery(l, r) instanceof BinaryQuery);
        assertTrue(new OrQuery(l, r) instanceof BinaryQuery);
        assertTrue(new AndNotQuery(l, r) instanceof BinaryQuery);
        assertFalse(new NullQuery() instanceof BinaryQuery);
    }

    @Test
    public void baseQueryAcceptsNullText() {
        assertNull(new BaseQuery(null).getQuery());
        assertEquals("BaseQuery(null)", dump(new BaseQuery(null)));
    }

    // ---- find() against a stub index ----

    /** Index whose find(String) maps the query text to a passage reference. */
    private static final class StubIndex implements Index {
        final Map<String, String> refs = new HashMap<String, String>();
        final Map<String, Integer> ranked = new HashMap<String, Integer>();
        final List<String> asked = new ArrayList<String>();
        SearchModifier modifier;

        public Key find(String query) throws BookException {
            asked.add(query);
            if (query == null) {
                return PassageKeyFactory.instance().createEmptyKeyList(V11N);
            }
            String ref = refs.get(query);
            if (ranked.containsKey(query)) {
                PassageTally t = new PassageTally(V11N);
                t.add(ref == null ? PassageKeyFactory.instance().createEmptyKeyList(V11N) : keyOf(ref), ranked.get(query));
                return t;
            }
            return ref == null ? PassageKeyFactory.instance().createEmptyKeyList(V11N) : keyOf(ref);
        }

        public Key getKey(String name) throws NoSuchKeyException {
            return PassageKeyFactory.instance().getKey(V11N, name);
        }

        public void setSearchModifier(SearchModifier m) {
            modifier = m;
        }

        public SearchModifier getSearchModifier() {
            return modifier;
        }

        public void close() {
        }

        private static Key keyOf(String ref) {
            try {
                return PassageKeyFactory.instance().getKey(V11N, ref);
            } catch (NoSuchKeyException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static StubIndex index() {
        StubIndex i = new StubIndex();
        i.refs.put("a", "Gen 1:1-5");
        i.refs.put("b", "Gen 1:3-8");
        i.refs.put("c", "Gen 1:3");
        return i;
    }

    @Test
    public void baseQueryPassesItsTextToTheIndex() throws BookException {
        StubIndex i = index();
        assertEquals(5, new BaseQuery("a").find(i).getCardinality());
        assertEquals("[a]", i.asked.toString());
    }

    @Test
    public void nullQueryAsksTheIndexForNull() throws BookException {
        StubIndex i = index();
        assertTrue(new NullQuery().find(i).isEmpty());
        assertEquals(1, i.asked.size());
        assertNull(i.asked.get(0));
    }

    @Test
    public void andIntersects() throws BookException {
        Key k = new AndQuery(new BaseQuery("a"), new BaseQuery("b")).find(index());
        assertEquals("Gen.1.3-Gen.1.5", k.getOsisRef());
    }

    @Test
    public void andStopsAtAnEmptyLeftWithoutAskingTheRight() throws BookException {
        StubIndex i = index();
        Key k = new AndQuery(new BaseQuery("none"), new BaseQuery("a")).find(i);
        assertTrue(k.isEmpty());
        assertEquals("[none]", i.asked.toString());
    }

    @Test
    public void andPrefersARankedRightResult() throws BookException {
        StubIndex i = index();
        i.ranked.put("b", 3);
        Key k = new AndQuery(new BaseQuery("a"), new BaseQuery("b")).find(i);
        assertTrue(k instanceof PassageTally);
        assertEquals("Gen.1.3-Gen.1.5", k.getOsisRef());
    }

    @Test
    public void orUnionsAndAsksBothSides() throws BookException {
        StubIndex i = index();
        Key k = new OrQuery(new BaseQuery("a"), new BaseQuery("c")).find(i);
        assertEquals("Gen.1.1-Gen.1.5", k.getOsisRef());
        // unlike And, Or evaluates the right side even if the left is empty
        i.asked.clear();
        Key r = new OrQuery(new BaseQuery("none"), new BaseQuery("c")).find(i);
        assertEquals("[none, c]", i.asked.toString());
        assertEquals("Gen.1.3", r.getOsisRef());
    }

    @Test
    public void andNotSubtracts() throws BookException {
        Key k = new AndNotQuery(new BaseQuery("a"), new BaseQuery("c")).find(index());
        assertEquals(4, k.getCardinality());
        assertEquals("Gen.1.1-Gen.1.2 Gen.1.4-Gen.1.5", k.getOsisRef());
    }

    @Test
    public void blurWidensTheRightByTheFactorBeforeIntersecting() throws BookException {
        // c = Gen 1:3, blurred by 1 (default restriction NONE) = Gen 1:2-4; a = Gen 1:1-5
        Key k = new BlurQuery(new BaseQuery("a"), new BaseQuery("c"), 1).find(index());
        assertEquals("Gen.1.2-Gen.1.4", k.getOsisRef());
    }

    @Test
    public void rangeQueryResolvesThroughIndexGetKey() throws BookException {
        Key k = new RangeQuery("Gen 1:1-2").find(index());
        assertEquals("Gen.1.1-Gen.1.2", k.getOsisRef());
    }

    @Test
    public void rangeQueryWrapsAnUnparsableRangeInABookException() {
        try {
            new RangeQuery("zzz 99").find(index());
            fail("expected BookException");
        } catch (BookException e) {
            assertTrue(e.getCause() instanceof NoSuchKeyException);
            assertTrue(e.getMessage(), e.getMessage().contains("zzz 99"));
        }
    }

    // ---- decorator (LuceneQueryDecorator via QueryDecorator.plugin) ----

    private static final QueryDecorator DECORATOR = QueryDecoratorFactory.getSearchSyntax();

    @Test
    public void decoratorsOnWords() {
        String w = "in the beginning";
        assertEquals("\"in the beginning\"", DECORATOR.decoratePhrase(w));
        assertEquals("+in +the +beginning", DECORATOR.decorateAllWords(w));
        assertEquals("in the beginning", DECORATOR.decorateAnyWords(w));
        assertEquals("-in -the -beginning", DECORATOR.decorateNotWords(w));
        assertEquals("in* the* beginning*", DECORATOR.decorateStartWords(w));
        assertEquals("in~ the~ beginning~", DECORATOR.decorateSpellWords(w));
        assertEquals("+[in the beginning]", DECORATOR.decorateRange(w));
    }

    @Test
    public void decoratorsOnTwoWords() {
        assertEquals("\"a b\"", DECORATOR.decoratePhrase("a b"));
        assertEquals("+a +b", DECORATOR.decorateAllWords("a b"));
        assertEquals("a b", DECORATOR.decorateAnyWords("a b"));
        assertEquals("-a -b", DECORATOR.decorateNotWords("a b"));
        assertEquals("a* b*", DECORATOR.decorateStartWords("a b"));
        assertEquals("a~ b~", DECORATOR.decorateSpellWords("a b"));
        assertEquals("+[a b]", DECORATOR.decorateRange("a b"));
    }

    @Test
    public void decoratorsOnTheEmptyString() {
        // "".split(" ") is [""], so the word decorators still add their one marker
        assertEquals("\"\"", DECORATOR.decoratePhrase(""));
        assertEquals("+", DECORATOR.decorateAllWords(""));
        assertEquals("", DECORATOR.decorateAnyWords(""));
        assertEquals("-", DECORATOR.decorateNotWords(""));
        assertEquals("*", DECORATOR.decorateStartWords(""));
        assertEquals("~", DECORATOR.decorateSpellWords(""));
        assertEquals("+[]", DECORATOR.decorateRange(""));
    }

    @Test
    public void decoratorsSplitOnEverySingleSpace() {
        // "a  b".split(" ") is ["a", "", "b"]: the empty middle word is decorated too
        assertEquals("+a + +b", DECORATOR.decorateAllWords("a  b"));
        assertEquals("a* * b*", DECORATOR.decorateStartWords("a  b"));
        // trailing empty strings are dropped by split
        assertEquals("+a", DECORATOR.decorateAllWords("a "));
    }
}
