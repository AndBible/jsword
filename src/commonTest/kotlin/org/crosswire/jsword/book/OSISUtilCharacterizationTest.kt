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
package org.crosswire.jsword.book

import fi.sykero.kdom.Element
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val DEJA = "d\u00E9j\u00E0"

/**
 * Characterization of the text extraction statics of [OSISUtil] on
 * hand-built OSIS trees. Non-ASCII text is written with unicode escapes.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class OSISUtilCharacterizationTest {

    private fun element(parent: Element, child: Element): Element {
        parent.addContent(child)
        return child
    }

    private fun withText(e: Element, text: String): Element {
        e.addContent(text)
        return e
    }

    /**
     * div > verse(osisID=Gen.1.1) with children, in order: text "In the beginning ",
     * w (lemma strong:G3056, morph robinson:N-NSM) "Word", text " was ", hi > hi "deja",
     * note "a note", text " end ", title "Heading",
     * note(type=crossReference) > reference(osisRef=Gen.1.2) "Gen 1:2".
     */
    private fun mainTree(): Element {
        val f = OSISUtil.factory()
        val div = f.createDiv()
        val verse = element(div, f.createVerse())
        verse.setAttribute(OSISUtil.OSIS_ATTR_OSISID, "Gen.1.1")
        verse.addContent("In the beginning ")
        val w = element(verse, f.createW())
        w.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, "strong:G3056")
        w.setAttribute(OSISUtil.ATTRIBUTE_W_MORPH, "robinson:N-NSM")
        w.addContent("Word")
        verse.addContent(" was ")
        val outer = element(verse, f.createHI())
        outer.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_ITALIC)
        val inner = element(outer, f.createHI())
        inner.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_BOLD)
        inner.addContent(DEJA)
        withText(element(verse, f.createNote()), "a note")
        verse.addContent(" end ")
        withText(element(verse, f.createTitle()), "Heading")
        val xref = element(verse, f.createNote())
        xref.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.NOTETYPE_REFERENCE)
        val ref = element(xref, f.createReference())
        ref.setAttribute(OSISUtil.OSIS_ATTR_REF, "Gen.1.2")
        ref.addContent("Gen 1:2")
        return div
    }

    @Test
    fun testCanonicalTextSkipsNotesAndTitles() {
        // note, title and reference are extra-biblical: skipped unless canonical="true".
        // Whitespace already present at a text boundary suppresses the inserted space.
        assertEquals("In the beginning Word was " + DEJA + " end", OSISUtil.getCanonicalText(mainTree()))
    }

    @Test
    fun testCanonicalTextKeepsCanonicalExtraBiblical() {
        val f = OSISUtil.factory()
        val div = f.createDiv()
        div.addContent("A")
        val kept = withText(element(div, f.createTitle()), "T")
        kept.setAttribute(OSISUtil.OSIS_ATTR_CANONICAL, "true")
        val dropped = withText(element(div, f.createTitle()), "U")
        dropped.setAttribute(OSISUtil.OSIS_ATTR_CANONICAL, "false")
        // Adjacent text without whitespace is joined by one space
        assertEquals("A T", OSISUtil.getCanonicalText(div))
    }

    @Test
    fun testCanonicalTextOfNonCanonicalRootIsEmpty() {
        val title = withText(OSISUtil.factory().createTitle(), "Heading")
        assertEquals("", OSISUtil.getCanonicalText(title))
    }

    @Test
    fun testPlainTextIncludesEverything() {
        assertEquals("In the beginning Word was " + DEJA + "a note end HeadingGen 1:2", OSISUtil.getPlainText(mainTree()))
    }

    @Test
    fun testStrongsNumbers() {
        assertEquals("G3056", OSISUtil.getStrongsNumbers(mainTree()))
        assertEquals("G3056@robinson:N-NSM", OSISUtil.getMorphologiesWithStrong(mainTree()))
    }

    private fun lexicalTree(): Element {
        val f = OSISUtil.factory()
        val div = f.createDiv()
        val w1 = withText(element(div, f.createW()), "one")
        w1.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, "strong:G3056")
        w1.setAttribute(OSISUtil.ATTRIBUTE_W_MORPH, "robinson:N-NSM")
        val w2 = withText(element(div, f.createW()), "two")
        // two numbers in one lemma; "G5!a" shows the optional ! and letter suffix
        w2.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, "lemma.TR:x strong:H1234 strong:G5!a")
        w2.setAttribute(OSISUtil.ATTRIBUTE_W_MORPH, "a b")
        // no strong number: ignored
        val w3 = withText(element(div, f.createW()), "three")
        w3.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, "lemma.TR:x")
        element(div, f.createW())
        return div
    }

    @Test
    fun testLexicalInformationOnSeveralWords() {
        assertEquals("G3056 H1234 G5!a", OSISUtil.getStrongsNumbers(lexicalTree()))
        // spaces inside a morph become '@', and the morph follows each strong number
        assertEquals("G3056@robinson:N-NSM H1234@a@b G5!a@a@b", OSISUtil.getMorphologiesWithStrong(lexicalTree()))
        assertEquals("G3056 H1234 G5!a", OSISUtil.getLexicalInformation(lexicalTree(), false))
    }

    @Test
    fun testNotesSkipCrossReferences() {
        assertEquals("a note", OSISUtil.getNotes(mainTree()))

        val f = OSISUtil.factory()
        val div = f.createDiv()
        withText(element(div, f.createNote()), "n1")
        val other = withText(element(div, f.createNote()), "n2")
        other.setAttribute(OSISUtil.OSIS_ATTR_TYPE, "study")
        val xref = withText(element(div, f.createNote()), "n3")
        xref.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.NOTETYPE_REFERENCE)
        assertEquals("n1 n2", OSISUtil.getNotes(div))
    }

    @Test
    fun testHeadings() {
        assertEquals("Heading", OSISUtil.getHeadings(mainTree()))

        val f = OSISUtil.factory()
        val div = f.createDiv()
        withText(element(div, f.createTitle()), "H1")
        withText(element(div, f.createTitle()), "H2\u00E9")
        assertEquals("H1 H2\u00E9", OSISUtil.getHeadings(div))
    }

    @Test
    fun testReferences() {
        val v11n: Versification? = Versifications.instance().getVersification("KJV")
        assertEquals("Gen.1.2", OSISUtil.getReferences(null, null, v11n, mainTree()))

        // Several references are collected into one sorted passage
        val f = OSISUtil.factory()
        val div = f.createDiv()
        element(div, f.createReference()).setAttribute(OSISUtil.OSIS_ATTR_REF, "Gen.1.4")
        element(div, f.createReference()).setAttribute(OSISUtil.OSIS_ATTR_REF, "Gen.1.2")
        // reference without osisRef and one that cannot be parsed are ignored (reporting is off)
        element(div, f.createReference())
        element(div, f.createReference()).setAttribute(OSISUtil.OSIS_ATTR_REF, "Nonexistent.1.1")
        assertEquals("Gen.1.2 Gen.1.4", OSISUtil.getReferences(null, null, v11n, div))
    }

    @Test
    fun testEmptyDiv() {
        val v11n: Versification? = Versifications.instance().getVersification("KJV")
        val div = OSISUtil.factory().createDiv()
        assertEquals("", OSISUtil.getCanonicalText(div))
        assertEquals("", OSISUtil.getPlainText(div))
        assertEquals("", OSISUtil.getStrongsNumbers(div))
        assertEquals("", OSISUtil.getMorphologiesWithStrong(div))
        assertEquals("", OSISUtil.getNotes(div))
        assertEquals("", OSISUtil.getHeadings(div))
        assertEquals("", OSISUtil.getReferences(null, null, v11n, div))
        assertTrue(OSISUtil.getFragment(div).isEmpty())
    }

    @Test
    fun testFragmentDigsPastOsisWrappers() {
        val f = OSISUtil.factory()
        val osis = f.createOsis()
        val osisText = element(osis, f.createOsisText())
        val div = element(osisText, f.createDiv())
        div.addContent("x")

        val fromOsis = OSISUtil.getFragment(osis)
        assertEquals(1, fromOsis.size)
        assertEquals("x", fromOsis[0].value)
        assertEquals("x", OSISUtil.getFragment(osisText)[0].value)
        assertEquals("x", OSISUtil.getPlainText(osis))

        // A null root gives an empty fragment
        assertTrue(OSISUtil.getFragment(null).isEmpty())
        // A single nested div is dug into
        val outer = f.createDiv()
        val nested = element(outer, f.createDiv())
        nested.addContent("y")
        assertEquals("y", OSISUtil.getFragment(outer)[0].value)
    }

    @Test
    fun testDeepContentFindsNestedElementsInDocumentOrder() {
        val f = OSISUtil.factory()
        val div = f.createDiv()
        val a = withText(element(div, f.createTitle()), "a")
        val b = withText(element(element(div, f.createHI()), f.createTitle()), "b")
        val found = OSISUtil.getDeepContent(div, OSISUtil.OSIS_ELEMENT_TITLE).toList()
        assertEquals(listOf<Any>(a, b), found)
    }
}
