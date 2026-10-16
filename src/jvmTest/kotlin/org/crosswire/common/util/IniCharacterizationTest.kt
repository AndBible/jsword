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
 * \u00A9 CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Characterization of {@link IniSection} and {@link Ini}: pins how they parse, what they
 * keep and their quirks before they are converted to Kotlin.
 * Expected values were derived by reading the parsers, not by observing them.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class IniCharacterizationTest {
    private static final String UTF8 = "UTF-8";
    private static final String NL = System.getProperty("line.separator");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Greek lambda omicron gamma omicron sigma */
    private static final String LOGOS = "\u03BB\u03CC\u03B3\u03BF\u03C2";
    private static final String AAKKOSET = "\u00C4\u00E4kk\u00F6set";

    private static final String CONF = "\uFEFF".substring(1)
            + "[ Name ]\n"
            + "# a comment\n"
            + "; another comment\n"
            + "\n"
            + "key=value\n"
            + "Key=other\n"
            + "dup=one\n"
            + "dup=two\n"
            + "dup=one\n"
            + "eq=a=b=c\n"
            + "   spaced key  =   spaced value   \n"
            + "cont=line1 \\\n"
            + "   line2\n"
            + "empty=\n"
            + "no equals here\n"
            + "greek=" + LOGOS + "\n"
            + "fin=" + AAKKOSET + "\n";

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static IniSection loaded() throws IOException {
        IniSection section = new IniSection();
        section.load(bytes(CONF), UTF8);
        return section;
    }

    private static List<String> list(Collection<String> values) {
        return new ArrayList<String>(values);
    }

    @Test
    public void testSectionParse() throws IOException {
        IniSection s = loaded();
        // The name is the text between the brackets, not trimmed
        Assert.assertEquals(" Name ", s.getName());
        Assert.assertEquals("value", s.get("key"));
        // Keys are case sensitive
        Assert.assertEquals("other", s.get("Key"));
        Assert.assertFalse(s.containsKey("KEY"));
        // Only the first '=' splits
        Assert.assertEquals("a=b=c", s.get("eq"));
        // Whitespace around key and value is dropped
        Assert.assertEquals("spaced value", s.get("spaced key"));
        // A trailing backslash continues the value on the next line; both parts trimmed, joined by newline
        Assert.assertEquals("line1\nline2", s.get("cont"));
        Assert.assertEquals("", s.get("empty"));
        Assert.assertTrue(s.containsKey("empty"));
        Assert.assertEquals(LOGOS, s.get("greek"));
        Assert.assertEquals(AAKKOSET, s.get("fin"));
        // Comment, blank and '='-less lines make no keys
        Assert.assertFalse(s.containsKey("no equals here"));
        Assert.assertEquals(new HashSet<String>(Arrays.asList("key", "Key", "dup", "eq", "spaced key", "cont", "empty", "greek", "fin")),
                new HashSet<String>(s.getKeys()));
        Assert.assertEquals(9, s.size());
        Assert.assertFalse(s.isEmpty());
    }

    @Test
    public void testSectionDuplicateKeysKeepDistinctValuesInOrder() throws IOException {
        IniSection s = loaded();
        Assert.assertEquals(2, s.size("dup"));
        Assert.assertEquals(Arrays.asList("one", "two"), list(s.getValues("dup")));
        Assert.assertEquals("one", s.get("dup", 0));
        Assert.assertEquals("two", s.get("dup", 1));
        Assert.assertEquals("one", s.get("dup"));
        Assert.assertEquals("one", s.get("dup", "default"));
        Assert.assertEquals(0, s.size("missing"));
        Assert.assertEquals(1, s.size("key"));
    }

    @Test
    public void testSectionReportIsOnceAndNamesTheIssues() throws IOException {
        IniSection s = loaded();
        // The repeated value "one" is not added again and the line without '=' is skipped, in file order
        Assert.assertEquals("Duplicate value: dup = one\nSkipping: Expected to see '=' in: no equals here\n", s.report());
        Assert.assertEquals("", s.report());
    }

    @Test
    public void testSectionLookupsOfMissingAndOutOfRange() throws IOException {
        IniSection s = loaded();
        Assert.assertNull(s.get("missing"));
        Assert.assertNull(s.get("missing", 3));
        Assert.assertEquals("default", s.get("missing", "default"));
        Assert.assertNull(s.getValues("missing"));
        try {
            s.get("dup", 5);
            Assert.fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // ArrayList.get, despite the Javadoc naming ArrayIndexOutOfBoundsException
        }
        try {
            s.getValues("dup").add("x");
            Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // values are unmodifiable
        }
        try {
            s.getKeys().add("x");
            Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // keys are unmodifiable
        }
    }

    @Test
    public void testSectionContains() throws IOException {
        IniSection s = loaded();
        Assert.assertTrue(s.containsKey("dup"));
        Assert.assertFalse(s.containsKey("nope"));
        Assert.assertTrue(s.containsValue("two"));
        Assert.assertFalse(s.containsValue("three"));
        Assert.assertTrue(s.containsValue("dup", "two"));
        Assert.assertFalse(s.containsValue("dup", "three"));
        Assert.assertFalse(s.containsValue("key", "two"));
        Assert.assertFalse(s.containsValue("nope", "two"));
    }

    @Test
    public void testSectionRemoveAndReplace() throws IOException {
        IniSection s = loaded();
        Assert.assertTrue(s.remove("dup", "one"));
        Assert.assertEquals(Arrays.asList("two"), list(s.getValues("dup")));
        Assert.assertFalse("already gone", s.remove("dup", "one"));
        Assert.assertTrue(s.remove("dup", "two"));
        Assert.assertFalse("the last value removes the key", s.containsKey("dup"));
        Assert.assertFalse(s.remove("dup", "two"));
        Assert.assertFalse(s.remove("missing", "x"));

        Assert.assertTrue(s.remove("eq"));
        Assert.assertFalse(s.containsKey("eq"));
        Assert.assertFalse(s.remove("eq"));

        Assert.assertTrue(s.replace("key", "new"));
        Assert.assertEquals(Arrays.asList("new"), list(s.getValues("key")));
        Assert.assertTrue(s.replace("fresh", "v"));
        Assert.assertEquals("v", s.get("fresh"));
    }

    @Test
    public void testSectionAddRejectsNullAndEmptyKeys() {
        IniSection s = new IniSection("S");
        Assert.assertFalse(s.add("", "v"));
        Assert.assertFalse(s.add(null, "v"));
        Assert.assertFalse(s.add("k", null));
        Assert.assertFalse(s.replace(null, "v"));
        Assert.assertFalse(s.replace("k", null));
        Assert.assertTrue(s.isEmpty());
        // The rejections are only reported by load, so a fresh section has no report at all
        Assert.assertNull(s.report());
    }

    @Test
    public void testSectionAddReturnsTrueForDuplicateValue() {
        IniSection s = new IniSection("S");
        Assert.assertTrue(s.add("k", "v"));
        Assert.assertTrue("a duplicate counts as present", s.add("k", "v"));
        Assert.assertEquals(1, s.size("k"));
    }

    @Test
    public void testSectionContinuationAtEndOfInput() throws IOException {
        IniSection s = new IniSection();
        // After the continuation there is no further line: advance() returns null and getSplitPos(null) throws
        try {
            s.load(bytes("[S]\nk=v \\"), UTF8);
            Assert.fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            // quirk kept
        }
    }

    @Test
    public void testSectionContinuationFollowedByKeyLineWarns() throws IOException {
        IniSection s = new IniSection();
        s.load(bytes("[S]\na=x \\\nb=y\n"), UTF8);
        // The continuation swallows "b=y" into a's value but the possible mistake is reported
        Assert.assertEquals("x\nb=y", s.get("a"));
        Assert.assertFalse(s.containsKey("b"));
        Assert.assertEquals("Possible trailing continuation on previous line. Found: b=y\n", s.report());
    }

    @Test
    public void testSectionByteOrderMarkIsStripped() throws IOException {
        IniSection s = new IniSection();
        s.load(bytes("\uFEFF[Bom]\nk=v\n"), UTF8);
        Assert.assertEquals("Bom", s.getName());
        Assert.assertEquals("v", s.get("k"));
    }

    @Test
    public void testSectionLoadFilter() throws IOException {
        IniSection s = new IniSection();
        s.load(bytes("[S]\nkeep=1\ndrop=2\n"), UTF8, new Filter<String>() {
            public boolean test(String key) {
                return "keep".equals(key);
            }
        });
        Assert.assertTrue(s.containsKey("keep"));
        Assert.assertFalse(s.containsKey("drop"));
    }

    @Test
    public void testSectionCopyConstructorQuirks() throws IOException {
        IniSection copy = new IniSection(loaded());
        Assert.assertEquals(" Name ", copy.getName());
        Assert.assertEquals(Arrays.asList("one", "two"), list(copy.getValues("dup")));
        Assert.assertEquals("line1\nline2", copy.get("cont"));
        // The copy constructor leaves warnings and report unset
        Assert.assertNull(copy.report());
        try {
            copy.add("k", null);
            Assert.fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            // warnings is null in a copy
        }
    }

    @Test
    public void testSectionClear() throws IOException {
        IniSection s = loaded();
        s.clear();
        Assert.assertTrue(s.isEmpty());
        Assert.assertEquals("", s.report());
        s.setName("Other");
        Assert.assertEquals("Other", s.getName());
    }

    @Test
    public void testSectionSaveWriter() {
        IniSection s = new IniSection("S");
        s.add("k", "a");
        s.add("k", "b");
        s.add("m", "x\ny");
        StringWriter out = new StringWriter();
        s.save(out);
        // Keys come out in hash order; check the pieces that are order independent.
        String text = out.toString();
        Assert.assertTrue(text, text.startsWith("[S]" + NL));
        // Values of one key keep insertion order, no blank line between entries, newline becomes " \" + tab
        Assert.assertTrue(text, text.contains("k = a" + NL + "k = b" + NL));
        Assert.assertTrue(text, text.contains("m = x \\\n\ty" + NL));
    }

    @Test
    public void testSectionSaveNullName() {
        StringWriter out = new StringWriter();
        new IniSection().save(out);
        Assert.assertEquals("[null]" + NL, out.toString());
    }

    @Test
    public void testSectionSaveAndReloadRoundTrip() throws IOException {
        IniSection s = loaded();
        File file = folder.newFile("round.conf");
        IniSectionJvm.save(s, file, UTF8);
        IniSection back = new IniSection();
        IniSectionJvm.load(back, file, UTF8);
        Assert.assertEquals(s.getName(), back.getName());
        Assert.assertEquals(new HashSet<String>(s.getKeys()), new HashSet<String>(back.getKeys()));
        for (String key : s.getKeys()) {
            Assert.assertEquals(key, list(s.getValues(key)), list(back.getValues(key)));
        }

        // save() writes back to the file it was loaded from
        back.replace("key", "changed");
        back.save();
        IniSection again = new IniSection();
        IniSectionJvm.load(again, file, UTF8);
        Assert.assertEquals("changed", again.get("key"));
    }

    // ---------------------------------------------------------------- Ini

    private static Ini loadedIni(String text) throws IOException {
        Ini ini = new Ini();
        ini.load(bytes(text), UTF8);
        return ini;
    }

    private static final String TWO = "[A]\nk=1\nk=2\nother=o\n[B]\nk=3\n";

    @Test
    public void testIniSections() throws IOException {
        Ini ini = loadedIni(TWO);
        Assert.assertEquals(2, ini.size());
        Assert.assertEquals(Arrays.asList("A", "B"), ini.getSections());
        Assert.assertEquals("A", ini.getSectionName());
        Assert.assertEquals("A", ini.getSectionName(0));
        Assert.assertEquals("B", ini.getSectionName(1));
        Assert.assertEquals("2", ini.getValue("A", "k", 1));
        Assert.assertEquals("1", ini.getValue("A", "k"));
        Assert.assertEquals("3", ini.getValue("B", "k"));
        Assert.assertEquals(2, ini.getValueSize("A", "k"));
        // Section names are case insensitive
        Assert.assertEquals("1", ini.getValue("a", "k", 0));
        // The first-section variants
        Assert.assertEquals(2, ini.getValueSize("k"));
        Assert.assertEquals("1", ini.getValue("k"));
        Assert.assertEquals("2", ini.getValue("k", 1));
        Assert.assertEquals(new HashSet<String>(Arrays.asList("k", "other")), new HashSet<String>(ini.getKeys()));
        Assert.assertEquals(Arrays.asList("1", "2"), list(ini.getValues("k")));
        Assert.assertSame(ini.getSection(), ini.getSection());
        Assert.assertEquals("A", ini.getSection().getName());
    }

    @Test
    public void testIniMissingAndOutOfRange() throws IOException {
        Ini ini = loadedIni(TWO);
        Assert.assertNull(ini.getValue("Z", "k", 0));
        Assert.assertNull(ini.getValue("Z", "k"));
        Assert.assertEquals(0, ini.getValueSize("Z", "k"));
        Assert.assertNull(ini.getValue("A", "nope"));
        Assert.assertNull(ini.getValue("nope"));
        try {
            ini.getValue("A", "k", 9);
            Assert.fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // from the ArrayList of values
        }
        try {
            ini.getSectionName(5);
            Assert.fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // from the section list
        }
        try {
            ini.getSections().add("x");
            Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // unmodifiable
        }
    }

    @Test
    public void testIniParseDetails() throws IOException {
        Ini ini = loadedIni("# c\n; c\n\nlead=1\n[ Name ]\n  spaced  =  v w  \neq=a=b\ncont=x \\\n  y\nempty=\nbad line\nKey=1\nkey=2\ngreek=" + LOGOS + "\n");
        // A key before any header lives in the section named ""
        Assert.assertEquals(Arrays.asList("", " Name "), ini.getSections());
        Assert.assertEquals("1", ini.getValue("", "lead"));
        Assert.assertEquals("v w", ini.getValue(" Name ", "spaced"));
        Assert.assertEquals("a=b", ini.getValue(" Name ", "eq"));
        Assert.assertEquals("x\ny", ini.getValue(" Name ", "cont"));
        Assert.assertEquals("", ini.getValue(" Name ", "empty"));
        Assert.assertNull(ini.getValue(" Name ", "bad line"));
        Assert.assertEquals("1", ini.getValue(" Name ", "Key"));
        Assert.assertEquals("2", ini.getValue(" Name ", "key"));
        Assert.assertEquals(LOGOS, ini.getValue(" Name ", "greek"));
    }

    @Test
    public void testIniContinuationAtEndKeepsTheNewline() throws IOException {
        Ini ini = loadedIni("[S]\nk=v \\");
        Assert.assertEquals("v\n", ini.getValue("S", "k"));
    }

    @Test
    public void testIniDoesNotStripByteOrderMark() throws IOException {
        // Unlike IniSection, "\uFEFF[Bom]" is not a section line; it has no '=' so it is skipped
        Ini ini = loadedIni("\uFEFF[Bom]\nk=v\n");
        Assert.assertEquals(Arrays.asList(""), ini.getSections());
        Assert.assertEquals("v", ini.getValue("", "k"));
    }

    @Test
    public void testIniEmptyKeyStillCreatesTheSection() throws IOException {
        Ini ini = loadedIni("[S]\n=v\n");
        Assert.assertEquals(Arrays.asList("S"), ini.getSections());
        Assert.assertNull(ini.getValue("S", ""));
        Assert.assertEquals(0, ini.getSection().size());
    }

    @Test
    public void testIniAddReplaceRemove() throws IOException {
        Ini ini = loadedIni(TWO);
        // "a" finds section A case-insensitively, so no section is added
        Assert.assertTrue(ini.add("a", "k", "9"));
        Assert.assertEquals(Arrays.asList("A", "B"), ini.getSections());
        Assert.assertEquals(3, ini.getValueSize("A", "k"));
        Assert.assertTrue(ini.add("C", "n", "v"));
        Assert.assertEquals(Arrays.asList("A", "B", "C"), ini.getSections());
        Assert.assertFalse(ini.add("C", "", "v"));
        Assert.assertTrue(ini.replace("A", "k", "only"));
        Assert.assertEquals(1, ini.getValueSize("A", "k"));
        Assert.assertEquals("only", ini.getValue("A", "k"));

        // removing the last value of the last key removes the section
        Assert.assertTrue(ini.remove("B", "k", "3"));
        Assert.assertEquals(Arrays.asList("A", "C"), ini.getSections());
        Assert.assertEquals(2, ini.size());
        Assert.assertFalse(ini.remove("B", "k", "3"));
        Assert.assertFalse(ini.remove("A", "k", "nope"));
        Assert.assertEquals(2, ini.size());
    }

    @Test
    public void testIniRemoveKeyRemovesSectionEvenWhenKeyIsMissing() throws IOException {
        Ini ini = loadedIni(TWO);
        // Quirk: the section is dropped whether or not the key existed
        Assert.assertFalse(ini.remove("A", "nope"));
        Assert.assertEquals(Arrays.asList("B"), ini.getSections());
        Assert.assertTrue(ini.remove("B", "k"));
        Assert.assertEquals(0, ini.size());
        Assert.assertFalse(ini.remove("B", "k"));
    }

    @Test
    public void testIniRemoveWithDifferentCaseLeavesAStaleListEntry() throws IOException {
        Ini ini = loadedIni("[A]\nk=1\n");
        // The case-insensitive map finds and drops the section, but the list removes by exact equals
        Assert.assertTrue(ini.remove("a", "k", "1"));
        Assert.assertEquals(0, ini.size());
        Assert.assertEquals(Arrays.asList("A"), ini.getSections());
        Assert.assertNull(ini.getSection());
    }

    @Test
    public void testIniFirstSectionHelpersOnEmptyIni() throws IOException {
        Ini ini = new Ini();
        Assert.assertEquals(0, ini.size());
        Assert.assertNull(ini.getSectionName());
        Assert.assertNull(ini.getSection());
        Assert.assertNull(ini.getKeys());
        Assert.assertNull(ini.getValues("k"));
        Assert.assertNull(ini.getValue("k"));
        Assert.assertEquals(0, ini.getValueSize("k"));
        // These claim success without doing anything
        Assert.assertTrue(ini.addValue("k", "v"));
        Assert.assertTrue(ini.removeValue("k", "v"));
        Assert.assertTrue(ini.removeValue("k"));
        Assert.assertTrue(ini.replaceValue("k", "v"));
        Assert.assertEquals(0, ini.size());
        try {
            ini.getSectionName(0);
            Assert.fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // empty list
        }
    }

    @Test
    public void testIniFirstSectionHelpers() throws IOException {
        Ini ini = loadedIni(TWO);
        Assert.assertTrue(ini.addValue("k", "x"));
        Assert.assertEquals(Arrays.asList("1", "2", "x"), list(ini.getValues("k")));
        Assert.assertTrue(ini.replaceValue("k", "y"));
        Assert.assertEquals(Arrays.asList("y"), list(ini.getValues("k")));
        Assert.assertTrue(ini.removeValue("k", "y"));
        Assert.assertNull(ini.getValues("k"));
        Assert.assertTrue(ini.removeValue("other"));
        // A has no keys left, but removeValue(key) dropped the section
        Assert.assertEquals(Arrays.asList("B"), ini.getSections());
    }

    @Test
    public void testIniClear() throws IOException {
        Ini ini = loadedIni(TWO);
        ini.clear();
        Assert.assertEquals(0, ini.size());
        Assert.assertTrue(ini.getSections().isEmpty());
    }

    @Test
    public void testIniSaveAndReloadRoundTrip() throws IOException {
        Ini ini = loadedIni("[A]\nk=1\nk=2\ncont=l1 \\\n l2\ngreek=" + LOGOS + "\n[ B b ]\nk=" + AAKKOSET + "\nempty=\n");
        File file = folder.newFile("round.ini");
        ini.save(file, UTF8);
        Ini back = new Ini();
        back.load(file, UTF8);
        Assert.assertEquals(ini.getSections(), back.getSections());
        Assert.assertEquals("2", back.getValue("A", "k", 1));
        Assert.assertEquals("l1\nl2", back.getValue("A", "cont"));
        Assert.assertEquals(LOGOS, back.getValue("A", "greek"));
        Assert.assertEquals(AAKKOSET, back.getValue(" B b ", "k"));
        Assert.assertEquals("", back.getValue(" B b ", "empty"));

        // load(InputStream)
        Ini fromFile = new Ini();
        fromFile.load(new java.io.ByteArrayInputStream(bytes(TWO)), UTF8);
        Assert.assertEquals(Arrays.asList("A", "B"), fromFile.getSections());
    }
}
