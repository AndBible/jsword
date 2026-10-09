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
package org.crosswire.jsword.book;

import java.util.List;

import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.system.Versifications;
import org.jdom2.Content;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

/**
 * Characterization of the text extraction statics of {@link OSISUtil} on
 * hand-built OSIS trees. Non-ASCII text is written with unicode escapes.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class OSISUtilCharacterizationTest {
    private static final String DEJA = "d\u00e9j\u00e0";

    private static Element element(Element parent, Element child) {
        parent.addContent(child);
        return child;
    }

    private static Element withText(Element e, String text) {
        e.addContent(text);
        return e;
    }

    /**
     * div > verse(osisID=Gen.1.1) with children, in order: text "In the beginning ",
     * w (lemma strong:G3056, morph robinson:N-NSM) "Word", text " was ", hi > hi "deja",
     * note "a note", text " end ", title "Heading",
     * note(type=crossReference) > reference(osisRef=Gen.1.2) "Gen 1:2".
     */
    private static Element mainTree() {
        OSISUtil.OSISFactory f = OSISUtil.factory();
        Element div = f.createDiv();
        Element verse = element(div, f.createVerse());
        verse.setAttribute(OSISUtil.OSIS_ATTR_OSISID, "Gen.1.1");
        verse.addContent("In the beginning ");
        Element w = element(verse, f.createW());
        w.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, "strong:G3056");
        w.setAttribute(OSISUtil.ATTRIBUTE_W_MORPH, "robinson:N-NSM");
        w.addContent("Word");
        verse.addContent(" was ");
        Element outer = element(verse, f.createHI());
        outer.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_ITALIC);
        Element inner = element(outer, f.createHI());
        inner.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_BOLD);
        inner.addContent(DEJA);
        withText(element(verse, f.createNote()), "a note");
        verse.addContent(" end ");
        withText(element(verse, f.createTitle()), "Heading");
        Element xref = element(verse, f.createNote());
        xref.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.NOTETYPE_REFERENCE);
        Element ref = element(xref, f.createReference());
        ref.setAttribute(OSISUtil.OSIS_ATTR_REF, "Gen.1.2");
        ref.addContent("Gen 1:2");
        return div;
    }

    @Test
    public void testCanonicalTextSkipsNotesAndTitles() {
        // note, title and reference are extra-biblical: skipped unless canonical="true".
        // Whitespace already present at a text boundary suppresses the inserted space.
        Assert.assertEquals("In the beginning Word was " + DEJA + " end", OSISUtil.getCanonicalText(mainTree()));
    }

    @Test
    public void testCanonicalTextKeepsCanonicalExtraBiblical() {
        OSISUtil.OSISFactory f = OSISUtil.factory();
        Element div = f.createDiv();
        div.addContent("A");
        Element kept = withText(element(div, f.createTitle()), "T");
        kept.setAttribute(OSISUtil.OSIS_ATTR_CANONICAL, "true");
        Element dropped = withText(element(div, f.createTitle()), "U");
        dropped.setAttribute(OSISUtil.OSIS_ATTR_CANONICAL, "false");
        // Adjacent text without whitespace is joined by one space
        Assert.assertEquals("A T", OSISUtil.getCanonicalText(div));
    }

    @Test
    public void testCanonicalTextOfNonCanonicalRootIsEmpty() {
        Element title = withText(OSISUtil.factory().createTitle(), "Heading");
        Assert.assertEquals("", OSISUtil.getCanonicalText(title));
    }

    @Test
    public void testPlainTextIncludesEverything() {
        Assert.assertEquals("In the beginning Word was " + DEJA + "a note end HeadingGen 1:2", OSISUtil.getPlainText(mainTree()));
    }

    @Test
    public void testStrongsNumbers() {
        Assert.assertEquals("G3056", OSISUtil.getStrongsNumbers(mainTree()));
        Assert.assertEquals("G3056@robinson:N-NSM", OSISUtil.getMorphologiesWithStrong(mainTree()));
    }

    private static Element lexicalTree() {
        OSISUtil.OSISFactory f = OSISUtil.factory();
        Element div = f.createDiv();
        Element w1 = withText(element(div, f.createW()), "one");
        w1.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, "strong:G3056");
        w1.setAttribute(OSISUtil.ATTRIBUTE_W_MORPH, "robinson:N-NSM");
        Element w2 = withText(element(div, f.createW()), "two");
        // two numbers in one lemma; "G5!a" shows the optional ! and letter suffix
        w2.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, "lemma.TR:x strong:H1234 strong:G5!a");
        w2.setAttribute(OSISUtil.ATTRIBUTE_W_MORPH, "a b");
        // no strong number: ignored
        Element w3 = withText(element(div, f.createW()), "three");
        w3.setAttribute(OSISUtil.ATTRIBUTE_W_LEMMA, "lemma.TR:x");
        element(div, f.createW());
        return div;
    }

    @Test
    public void testLexicalInformationOnSeveralWords() {
        Assert.assertEquals("G3056 H1234 G5!a", OSISUtil.getStrongsNumbers(lexicalTree()));
        // spaces inside a morph become '@', and the morph follows each strong number
        Assert.assertEquals("G3056@robinson:N-NSM H1234@a@b G5!a@a@b", OSISUtil.getMorphologiesWithStrong(lexicalTree()));
        Assert.assertEquals("G3056 H1234 G5!a", OSISUtil.getLexicalInformation(lexicalTree(), false));
    }

    @Test
    public void testNotesSkipCrossReferences() {
        Assert.assertEquals("a note", OSISUtil.getNotes(mainTree()));

        OSISUtil.OSISFactory f = OSISUtil.factory();
        Element div = f.createDiv();
        withText(element(div, f.createNote()), "n1");
        Element other = withText(element(div, f.createNote()), "n2");
        other.setAttribute(OSISUtil.OSIS_ATTR_TYPE, "study");
        Element xref = withText(element(div, f.createNote()), "n3");
        xref.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.NOTETYPE_REFERENCE);
        Assert.assertEquals("n1 n2", OSISUtil.getNotes(div));
    }

    @Test
    public void testHeadings() {
        Assert.assertEquals("Heading", OSISUtil.getHeadings(mainTree()));

        OSISUtil.OSISFactory f = OSISUtil.factory();
        Element div = f.createDiv();
        withText(element(div, f.createTitle()), "H1");
        withText(element(div, f.createTitle()), "H2\u00e9");
        Assert.assertEquals("H1 H2\u00e9", OSISUtil.getHeadings(div));
    }

    @Test
    public void testReferences() {
        Versification v11n = Versifications.instance().getVersification("KJV");
        Assert.assertEquals("Gen.1.2", OSISUtil.getReferences(null, null, v11n, mainTree()));

        // Several references are collected into one sorted passage
        OSISUtil.OSISFactory f = OSISUtil.factory();
        Element div = f.createDiv();
        element(div, f.createReference()).setAttribute(OSISUtil.OSIS_ATTR_REF, "Gen.1.4");
        element(div, f.createReference()).setAttribute(OSISUtil.OSIS_ATTR_REF, "Gen.1.2");
        // reference without osisRef and one that cannot be parsed are ignored (reporting is off)
        element(div, f.createReference());
        element(div, f.createReference()).setAttribute(OSISUtil.OSIS_ATTR_REF, "Nonexistent.1.1");
        Assert.assertEquals("Gen.1.2 Gen.1.4", OSISUtil.getReferences(null, null, v11n, div));
    }

    @Test
    public void testEmptyDiv() {
        Versification v11n = Versifications.instance().getVersification("KJV");
        Element div = OSISUtil.factory().createDiv();
        Assert.assertEquals("", OSISUtil.getCanonicalText(div));
        Assert.assertEquals("", OSISUtil.getPlainText(div));
        Assert.assertEquals("", OSISUtil.getStrongsNumbers(div));
        Assert.assertEquals("", OSISUtil.getMorphologiesWithStrong(div));
        Assert.assertEquals("", OSISUtil.getNotes(div));
        Assert.assertEquals("", OSISUtil.getHeadings(div));
        Assert.assertEquals("", OSISUtil.getReferences(null, null, v11n, div));
        Assert.assertTrue(OSISUtil.getFragment(div).isEmpty());
    }

    @Test
    public void testFragmentDigsPastOsisWrappers() {
        OSISUtil.OSISFactory f = OSISUtil.factory();
        Element osis = f.createOsis();
        Element osisText = element(osis, f.createOsisText());
        Element div = element(osisText, f.createDiv());
        div.addContent("x");

        List<Content> fromOsis = OSISUtil.getFragment(osis);
        Assert.assertEquals(1, fromOsis.size());
        Assert.assertEquals("x", fromOsis.get(0).getValue());
        Assert.assertEquals("x", OSISUtil.getFragment(osisText).get(0).getValue());
        Assert.assertEquals("x", OSISUtil.getPlainText(osis));

        // A null root gives an empty fragment
        Assert.assertTrue(OSISUtil.getFragment(null).isEmpty());
        // A single nested div is dug into
        Element outer = f.createDiv();
        Element nested = element(outer, f.createDiv());
        nested.addContent("y");
        Assert.assertEquals("y", OSISUtil.getFragment(outer).get(0).getValue());
    }

    @Test
    public void testDeepContentFindsNestedElementsInDocumentOrder() {
        OSISUtil.OSISFactory f = OSISUtil.factory();
        Element div = f.createDiv();
        Element a = withText(element(div, f.createTitle()), "a");
        Element b = withText(element(element(div, f.createHI()), f.createTitle()), "b");
        Object[] found = OSISUtil.getDeepContent(div, OSISUtil.OSIS_ELEMENT_TITLE).toArray();
        Assert.assertArrayEquals(new Object[] {a, b}, found);
    }
}
