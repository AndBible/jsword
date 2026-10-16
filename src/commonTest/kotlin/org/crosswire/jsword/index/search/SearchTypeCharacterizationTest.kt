package org.crosswire.jsword.index.search

import org.crosswire.jsword.index.query.QueryDecoratorFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Characterization of SearchType, DefaultSearchModifier and DefaultSearchRequest (port of the JVM test; runs on iOS too). */
class SearchTypeCharacterizationTest {
    @Test fun constantsInDeclarationOrderWithDisplayNames() {
        val all = SearchType.entries
        assertEquals(7, all.size)
        val order = listOf(SearchType.PHRASE, SearchType.ALL_WORDS, SearchType.ANY_WORDS, SearchType.NOT_WORDS,
            SearchType.START_WORDS, SearchType.SPELL_WORDS, SearchType.RANGE)
        val names = listOf("Phrase", "All", "Any", "Not", "Start", "Spell", "Range")
        for (i in order.indices) {
            assertSame(order[i], all[i])
            assertEquals(names[i], order[i].toString())
        }
        assertEquals("ALL_WORDS", SearchType.ALL_WORDS.name)
    }

    @Test fun decorateDelegatesPerConstant() {
        assertEquals("\"a b\"", SearchType.PHRASE.decorate("a b"))
        assertEquals("+a +b", SearchType.ALL_WORDS.decorate("a b"))
        assertEquals("a b", SearchType.ANY_WORDS.decorate("a b"))
        assertEquals("-a -b", SearchType.NOT_WORDS.decorate("a b"))
        assertEquals("a* b*", SearchType.START_WORDS.decorate("a b"))
        assertEquals("a~ b~", SearchType.SPELL_WORDS.decorate("a b"))
        assertEquals("+[a b]", SearchType.RANGE.decorate("a b"))
    }

    @Test fun decorateAgreesWithTheDefaultDecoratorForEveryConstant() {
        val w = "in the beginning"
        val d = QueryDecoratorFactory.getSearchSyntax()
        assertEquals(d.decoratePhrase(w), SearchType.PHRASE.decorate(w))
        assertEquals(d.decorateAllWords(w), SearchType.ALL_WORDS.decorate(w))
        assertEquals(d.decorateAnyWords(w), SearchType.ANY_WORDS.decorate(w))
        assertEquals(d.decorateNotWords(w), SearchType.NOT_WORDS.decorate(w))
        assertEquals(d.decorateStartWords(w), SearchType.START_WORDS.decorate(w))
        assertEquals(d.decorateSpellWords(w), SearchType.SPELL_WORDS.decorate(w))
        assertEquals(d.decorateRange(w), SearchType.RANGE.decorate(w))
    }

    @Test fun decorateTheEmptyString() {
        assertEquals("\"\"", SearchType.PHRASE.decorate(""))
        assertEquals("+", SearchType.ALL_WORDS.decorate(""))
        assertEquals("", SearchType.ANY_WORDS.decorate(""))
        assertEquals("-", SearchType.NOT_WORDS.decorate(""))
        assertEquals("*", SearchType.START_WORDS.decorate(""))
        assertEquals("~", SearchType.SPELL_WORDS.decorate(""))
        assertEquals("+[]", SearchType.RANGE.decorate(""))
    }

    @Test fun fromStringMatchesDisplayNamesIgnoringCase() {
        assertSame(SearchType.PHRASE, SearchType.fromString("Phrase"))
        assertSame(SearchType.PHRASE, SearchType.fromString("PHRASE"))
        assertSame(SearchType.ALL_WORDS, SearchType.fromString("all"))
        assertSame(SearchType.ANY_WORDS, SearchType.fromString("ANY"))
        assertSame(SearchType.NOT_WORDS, SearchType.fromString("Not"))
        assertSame(SearchType.START_WORDS, SearchType.fromString("start"))
        assertSame(SearchType.SPELL_WORDS, SearchType.fromString("sPeLl"))
        assertSame(SearchType.RANGE, SearchType.fromString("range"))
        for (t in SearchType.entries) assertSame(t, SearchType.fromString(t.toString()))
    }

    @Test fun fromStringDoesNotAcceptConstantNames() {
        for (bad in listOf("ALL_WORDS", "ANY_WORDS", "NOT_WORDS", "START_WORDS", "SPELL_WORDS", "nope", "", "Phrase ")) {
            val e = assertFailsWith<ClassCastException>(bad) { SearchType.fromString(bad) }
            assertEquals("Not a valid search type", e.message)
        }
    }

    @Test fun fromStringNullIsAnUnknownName() {
        val e = assertFailsWith<ClassCastException> { SearchType.fromString(null) }
        assertEquals("Not a valid search type", e.message)
    }

    @Test fun defaultModifierIsUnrankedAndUnlimited() {
        val m = DefaultSearchModifier()
        assertFalse(m.isRanked)
        assertEquals(Int.MAX_VALUE, m.maxResults)
    }

    @Test fun modifierSettersAreIndependent() {
        val m = DefaultSearchModifier()
        m.isRanked = true
        assertTrue(m.isRanked)
        assertEquals(Int.MAX_VALUE, m.maxResults)
        m.maxResults = 25
        assertEquals(25, m.maxResults)
        assertTrue(m.isRanked)
        m.isRanked = false
        assertFalse(m.isRanked)
        assertEquals(25, m.maxResults)
        m.maxResults = 0
        assertEquals(0, m.maxResults)
        m.maxResults = -1
        assertEquals(-1, m.maxResults)
    }

    @Test fun requestKeepsTextAndModifier() {
        val m = DefaultSearchModifier()
        val r = DefaultSearchRequest("love joy", m)
        assertEquals("love joy", r.request)
        assertSame(m, r.searchModifier)
    }

    @Test fun requestWithoutModifierHasNullModifier() {
        val r = DefaultSearchRequest("love")
        assertEquals("love", r.request)
        assertNull(r.searchModifier)
    }

    @Test fun requestAcceptsNulls() {
        val r = DefaultSearchRequest(null, null)
        assertNull(r.request)
        assertNull(r.searchModifier)
    }
}
