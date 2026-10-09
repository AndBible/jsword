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
 * \u00A9 CrossWire Bible Society, 2005 - 2016
 */
package org.crosswire.jsword.index.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.crosswire.jsword.index.query.QueryDecoratorFactory;
import org.junit.Test;

/**
 * Characterization of SearchType, DefaultSearchModifier and
 * DefaultSearchRequest. SearchType.decorate delegates to the plugin-resolved
 * QueryDecorator (LuceneQueryDecorator): the expected strings follow from its
 * source (PLUS/MINUS/QUOTE/FUZZY/WILD joined per space-separated word).
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class SearchTypeCharacterizationTest {

    @Test
    public void constantsInDeclarationOrderWithDisplayNames() {
        SearchType[] all = SearchType.values();
        assertEquals(7, all.length);
        SearchType[] order = {SearchType.PHRASE, SearchType.ALL_WORDS, SearchType.ANY_WORDS, SearchType.NOT_WORDS,
                SearchType.START_WORDS, SearchType.SPELL_WORDS, SearchType.RANGE};
        String[] names = {"Phrase", "All", "Any", "Not", "Start", "Spell", "Range"};
        for (int i = 0; i < order.length; i++) {
            assertSame(order[i], all[i]);
            // toString is the display name, name() stays the constant name
            assertEquals(names[i], order[i].toString());
        }
        assertEquals("ALL_WORDS", SearchType.ALL_WORDS.name());
    }

    @Test
    public void decorateDelegatesPerConstant() {
        assertEquals("\"a b\"", SearchType.PHRASE.decorate("a b"));
        assertEquals("+a +b", SearchType.ALL_WORDS.decorate("a b"));
        assertEquals("a b", SearchType.ANY_WORDS.decorate("a b"));
        assertEquals("-a -b", SearchType.NOT_WORDS.decorate("a b"));
        assertEquals("a* b*", SearchType.START_WORDS.decorate("a b"));
        assertEquals("a~ b~", SearchType.SPELL_WORDS.decorate("a b"));
        assertEquals("+[a b]", SearchType.RANGE.decorate("a b"));
    }

    @Test
    public void decorateAgreesWithTheDefaultDecoratorForEveryConstant() {
        String w = "in the beginning";
        assertEquals(QueryDecoratorFactory.getSearchSyntax().decoratePhrase(w), SearchType.PHRASE.decorate(w));
        assertEquals(QueryDecoratorFactory.getSearchSyntax().decorateAllWords(w), SearchType.ALL_WORDS.decorate(w));
        assertEquals(QueryDecoratorFactory.getSearchSyntax().decorateAnyWords(w), SearchType.ANY_WORDS.decorate(w));
        assertEquals(QueryDecoratorFactory.getSearchSyntax().decorateNotWords(w), SearchType.NOT_WORDS.decorate(w));
        assertEquals(QueryDecoratorFactory.getSearchSyntax().decorateStartWords(w), SearchType.START_WORDS.decorate(w));
        assertEquals(QueryDecoratorFactory.getSearchSyntax().decorateSpellWords(w), SearchType.SPELL_WORDS.decorate(w));
        assertEquals(QueryDecoratorFactory.getSearchSyntax().decorateRange(w), SearchType.RANGE.decorate(w));
    }

    @Test
    public void decorateTheEmptyString() {
        // "".split(" ") yields one empty word
        assertEquals("\"\"", SearchType.PHRASE.decorate(""));
        assertEquals("+", SearchType.ALL_WORDS.decorate(""));
        assertEquals("", SearchType.ANY_WORDS.decorate(""));
        assertEquals("-", SearchType.NOT_WORDS.decorate(""));
        assertEquals("*", SearchType.START_WORDS.decorate(""));
        assertEquals("~", SearchType.SPELL_WORDS.decorate(""));
        assertEquals("+[]", SearchType.RANGE.decorate(""));
    }

    @Test
    public void constantsAreDistinctSubclassBodies() {
        // every constant has its own body, so getClass() differs from getDeclaringClass()
        for (SearchType t : SearchType.values()) {
            assertSame(SearchType.class, t.getDeclaringClass());
            assertFalse(SearchType.class == t.getClass());
        }
    }

    @Test
    public void fromStringMatchesDisplayNamesIgnoringCase() {
        assertSame(SearchType.PHRASE, SearchType.fromString("Phrase"));
        assertSame(SearchType.PHRASE, SearchType.fromString("PHRASE"));
        assertSame(SearchType.ALL_WORDS, SearchType.fromString("all"));
        assertSame(SearchType.ANY_WORDS, SearchType.fromString("ANY"));
        assertSame(SearchType.NOT_WORDS, SearchType.fromString("Not"));
        assertSame(SearchType.START_WORDS, SearchType.fromString("start"));
        assertSame(SearchType.SPELL_WORDS, SearchType.fromString("sPeLl"));
        assertSame(SearchType.RANGE, SearchType.fromString("range"));
        // lookup by toString round-trips
        for (SearchType t : SearchType.values()) {
            assertSame(t, SearchType.fromString(t.toString()));
        }
    }

    @Test
    public void fromStringDoesNotAcceptConstantNames() {
        // the lookup is on the display name: "ALL_WORDS" is not "All"
        for (String bad : new String[] {"ALL_WORDS", "ANY_WORDS", "NOT_WORDS", "START_WORDS", "SPELL_WORDS", "nope", "", "Phrase "}) {
            try {
                SearchType.fromString(bad);
                fail("expected ClassCastException for '" + bad + "'");
            } catch (ClassCastException e) {
                assertEquals("Not a valid search type", e.getMessage());
            }
        }
    }

    @Test
    public void fromStringNullIsAnUnknownName() {
        // equalsIgnoreCase(null) is false for every constant
        try {
            SearchType.fromString(null);
            fail("expected ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Not a valid search type", e.getMessage());
        }
    }

    // ---- DefaultSearchModifier ----

    @Test
    public void defaultModifierIsUnrankedAndUnlimited() {
        DefaultSearchModifier m = new DefaultSearchModifier();
        assertFalse(m.isRanked());
        assertEquals(Integer.MAX_VALUE, m.getMaxResults());
    }

    @Test
    public void modifierSettersAreIndependent() {
        DefaultSearchModifier m = new DefaultSearchModifier();
        m.setRanked(true);
        assertTrue(m.isRanked());
        assertEquals(Integer.MAX_VALUE, m.getMaxResults());
        m.setMaxResults(25);
        assertEquals(25, m.getMaxResults());
        assertTrue(m.isRanked());
        m.setRanked(false);
        assertFalse(m.isRanked());
        assertEquals(25, m.getMaxResults());
        // no validation: zero and negative values are stored as given
        m.setMaxResults(0);
        assertEquals(0, m.getMaxResults());
        m.setMaxResults(-1);
        assertEquals(-1, m.getMaxResults());
    }

    @Test
    public void modifierSurvivesSerialization() throws Exception {
        DefaultSearchModifier m = new DefaultSearchModifier();
        m.setRanked(true);
        m.setMaxResults(42);
        DefaultSearchModifier copy = roundTrip(m);
        assertTrue(copy.isRanked());
        assertEquals(42, copy.getMaxResults());
    }

    // ---- DefaultSearchRequest ----

    @Test
    public void requestKeepsTextAndModifier() {
        DefaultSearchModifier m = new DefaultSearchModifier();
        DefaultSearchRequest r = new DefaultSearchRequest("love joy", m);
        assertEquals("love joy", r.getRequest());
        assertSame(m, r.getSearchModifier());
    }

    @Test
    public void requestWithoutModifierHasNullModifier() {
        DefaultSearchRequest r = new DefaultSearchRequest("love");
        assertEquals("love", r.getRequest());
        assertNull(r.getSearchModifier());
    }

    @Test
    public void requestAcceptsNulls() {
        DefaultSearchRequest r = new DefaultSearchRequest(null, null);
        assertNull(r.getRequest());
        assertNull(r.getSearchModifier());
    }

    @Test
    public void requestRoundTripsThroughSerialization() throws Exception {
        DefaultSearchModifier m = new DefaultSearchModifier();
        m.setRanked(true);
        m.setMaxResults(7);
        DefaultSearchRequest copy = roundTrip(new DefaultSearchRequest("caf\u00E9 \u03BB\u03CC\u03B3\u03BF\u03C2", m));
        assertEquals("caf\u00E9 \u03BB\u03CC\u03B3\u03BF\u03C2", copy.getRequest());
        assertTrue(copy.getSearchModifier().isRanked());
        assertEquals(7, copy.getSearchModifier().getMaxResults());
        assertNull(roundTrip(new DefaultSearchRequest("x")).getSearchModifier());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T o) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(o);
        out.close();
        return (T) new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
    }
}
