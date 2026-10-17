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
 *
 */
package org.crosswire.common.util

import java.io.File
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.ArrayList
import java.util.HashSet
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Characterization of [IniSection] and [Ini]: pins how they parse, what they
 * keep and their quirks before they are converted to Kotlin.
 * Expected values were derived by reading the parsers, not by observing them.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class IniCharacterizationTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun bytes(s: String): ByteArray {
        return s.toByteArray(StandardCharsets.UTF_8)
    }

    private fun loaded(): IniSection {
        val section = IniSection()
        section.load(bytes(CONF), UTF8)
        return section
    }

    private fun list(values: Collection<String>?): List<String> {
        return ArrayList(values!!)
    }

    @Test
    fun testSectionParse() {
        val s = loaded()
        // The name is the text between the brackets, not trimmed
        assertEquals(" Name ", s.name)
        assertEquals("value", s.get("key"))
        // Keys are case sensitive
        assertEquals("other", s.get("Key"))
        assertFalse(s.containsKey("KEY"))
        // Only the first '=' splits
        assertEquals("a=b=c", s.get("eq"))
        // Whitespace around key and value is dropped
        assertEquals("spaced value", s.get("spaced key"))
        // A trailing backslash continues the value on the next line; both parts trimmed, joined by newline
        assertEquals("line1\nline2", s.get("cont"))
        assertEquals("", s.get("empty"))
        assertTrue(s.containsKey("empty"))
        assertEquals(LOGOS, s.get("greek"))
        assertEquals(AAKKOSET, s.get("fin"))
        // Comment, blank and '='-less lines make no keys
        assertFalse(s.containsKey("no equals here"))
        assertEquals(HashSet(Arrays.asList("key", "Key", "dup", "eq", "spaced key", "cont", "empty", "greek", "fin")),
                HashSet(s.keys))
        assertEquals(9, s.size())
        assertFalse(s.isEmpty)
    }

    @Test
    fun testSectionDuplicateKeysKeepDistinctValuesInOrder() {
        val s = loaded()
        assertEquals(2, s.size("dup"))
        assertEquals(Arrays.asList("one", "two"), list(s.getValues("dup")))
        assertEquals("one", s.get("dup", 0))
        assertEquals("two", s.get("dup", 1))
        assertEquals("one", s.get("dup"))
        assertEquals("one", s.get("dup", "default"))
        assertEquals(0, s.size("missing"))
        assertEquals(1, s.size("key"))
    }

    @Test
    fun testSectionReportIsOnceAndNamesTheIssues() {
        val s = loaded()
        // The repeated value "one" is not added again and the line without '=' is skipped, in file order
        assertEquals("Duplicate value: dup = one\nSkipping: Expected to see '=' in: no equals here\n", s.report())
        assertEquals("", s.report())
    }

    @Test
    fun testSectionLookupsOfMissingAndOutOfRange() {
        val s = loaded()
        assertNull(s.get("missing"))
        assertNull(s.get("missing", 3))
        assertEquals("default", s.get("missing", "default"))
        assertNull(s.getValues("missing"))
        assertFailsWith<IndexOutOfBoundsException> {
            // ArrayList.get, despite the Javadoc naming ArrayIndexOutOfBoundsException
            s.get("dup", 5)
        }
        assertFailsWith<UnsupportedOperationException> {
            // values are unmodifiable
            (s.getValues("dup") as MutableCollection<String>).add("x")
        }
        assertFailsWith<UnsupportedOperationException> {
            // keys are unmodifiable
            (s.keys as MutableCollection<String>).add("x")
        }
    }

    @Test
    fun testSectionContains() {
        val s = loaded()
        assertTrue(s.containsKey("dup"))
        assertFalse(s.containsKey("nope"))
        assertTrue(s.containsValue("two"))
        assertFalse(s.containsValue("three"))
        assertTrue(s.containsValue("dup", "two"))
        assertFalse(s.containsValue("dup", "three"))
        assertFalse(s.containsValue("key", "two"))
        assertFalse(s.containsValue("nope", "two"))
    }

    @Test
    fun testSectionRemoveAndReplace() {
        val s = loaded()
        assertTrue(s.remove("dup", "one"))
        assertEquals(Arrays.asList("two"), list(s.getValues("dup")))
        assertFalse(s.remove("dup", "one"), "already gone")
        assertTrue(s.remove("dup", "two"))
        assertFalse(s.containsKey("dup"), "the last value removes the key")
        assertFalse(s.remove("dup", "two"))
        assertFalse(s.remove("missing", "x"))

        assertTrue(s.remove("eq"))
        assertFalse(s.containsKey("eq"))
        assertFalse(s.remove("eq"))

        assertTrue(s.replace("key", "new"))
        assertEquals(Arrays.asList("new"), list(s.getValues("key")))
        assertTrue(s.replace("fresh", "v"))
        assertEquals("v", s.get("fresh"))
    }

    @Test
    fun testSectionAddRejectsNullAndEmptyKeys() {
        val s = IniSection("S")
        assertFalse(s.add("", "v"))
        assertFalse(s.add(null, "v"))
        assertFalse(s.add("k", null))
        assertFalse(s.replace(null, "v"))
        assertFalse(s.replace("k", null))
        assertTrue(s.isEmpty)
        // The rejections are only reported by load, so a fresh section has no report at all
        assertNull(s.report())
    }

    @Test
    fun testSectionAddReturnsTrueForDuplicateValue() {
        val s = IniSection("S")
        assertTrue(s.add("k", "v"))
        assertTrue(s.add("k", "v"), "a duplicate counts as present")
        assertEquals(1, s.size("k"))
    }

    @Test
    fun testSectionContinuationAtEndOfInput() {
        val s = IniSection()
        // After the continuation there is no further line: advance() returns null and getSplitPos(null) throws
        assertFailsWith<NullPointerException> {
            // quirk kept
            s.load(bytes("[S]\nk=v \\"), UTF8)
        }
    }

    @Test
    fun testSectionContinuationFollowedByKeyLineWarns() {
        val s = IniSection()
        s.load(bytes("[S]\na=x \\\nb=y\n"), UTF8)
        // The continuation swallows "b=y" into a's value but the possible mistake is reported
        assertEquals("x\nb=y", s.get("a"))
        assertFalse(s.containsKey("b"))
        assertEquals("Possible trailing continuation on previous line. Found: b=y\n", s.report())
    }

    @Test
    fun testSectionByteOrderMarkIsStripped() {
        val s = IniSection()
        s.load(bytes("\uFEFF[Bom]\nk=v\n"), UTF8)
        assertEquals("Bom", s.name)
        assertEquals("v", s.get("k"))
    }

    @Test
    fun testSectionLoadFilter() {
        val s = IniSection()
        s.load(bytes("[S]\nkeep=1\ndrop=2\n"), UTF8, Filter<String> { key -> "keep" == key })
        assertTrue(s.containsKey("keep"))
        assertFalse(s.containsKey("drop"))
    }

    @Test
    fun testSectionCopyConstructorQuirks() {
        val copy = IniSection(loaded())
        assertEquals(" Name ", copy.name)
        assertEquals(Arrays.asList("one", "two"), list(copy.getValues("dup")))
        assertEquals("line1\nline2", copy.get("cont"))
        // The copy constructor leaves warnings and report unset
        assertNull(copy.report())
        assertFailsWith<NullPointerException> {
            // warnings is null in a copy
            copy.add("k", null)
        }
    }

    @Test
    fun testSectionClear() {
        val s = loaded()
        s.clear()
        assertTrue(s.isEmpty)
        assertEquals("", s.report())
        s.name = "Other"
        assertEquals("Other", s.name)
    }

    @Test
    fun testSectionSaveWriter() {
        val s = IniSection("S")
        s.add("k", "a")
        s.add("k", "b")
        s.add("m", "x\ny")
        val out = StringWriter()
        s.save(out)
        // Keys come out in hash order; check the pieces that are order independent.
        val text = out.toString()
        assertTrue(text.startsWith("[S]" + NL), text)
        // Values of one key keep insertion order, no blank line between entries, newline becomes " \" + tab
        assertTrue(text.contains("k = a" + NL + "k = b" + NL), text)
        assertTrue(text.contains("m = x \\\n\ty" + NL), text)
    }

    @Test
    fun testSectionSaveNullName() {
        val out = StringWriter()
        IniSection().save(out)
        assertEquals("[null]" + NL, out.toString())
    }

    @Test
    fun testSectionSaveAndReloadRoundTrip() {
        val s = loaded()
        val file = folder.newFile("round.conf")
        IniSectionJvm.save(s, file, UTF8)
        val back = IniSection()
        IniSectionJvm.load(back, file, UTF8)
        assertEquals(s.name, back.name)
        assertEquals(HashSet(s.keys), HashSet(back.keys))
        for (key in s.keys) {
            assertEquals(list(s.getValues(key)), list(back.getValues(key)), key)
        }

        // save() writes back to the file it was loaded from
        back.replace("key", "changed")
        back.save()
        val again = IniSection()
        IniSectionJvm.load(again, file, UTF8)
        assertEquals("changed", again.get("key"))
    }

    // ---------------------------------------------------------------- Ini

    private fun loadedIni(text: String): Ini {
        val ini = Ini()
        ini.load(bytes(text), UTF8)
        return ini
    }

    @Test
    fun testIniSections() {
        val ini = loadedIni(TWO)
        assertEquals(2, ini.size())
        assertEquals(Arrays.asList("A", "B"), ini.sections)
        assertEquals("A", ini.sectionName)
        assertEquals("A", ini.getSectionName(0))
        assertEquals("B", ini.getSectionName(1))
        assertEquals("2", ini.getValue("A", "k", 1))
        assertEquals("1", ini.getValue("A", "k"))
        assertEquals("3", ini.getValue("B", "k"))
        assertEquals(2, ini.getValueSize("A", "k"))
        // Section names are case insensitive
        assertEquals("1", ini.getValue("a", "k", 0))
        // The first-section variants
        assertEquals(2, ini.getValueSize("k"))
        assertEquals("1", ini.getValue("k"))
        assertEquals("2", ini.getValue("k", 1))
        assertEquals(HashSet(Arrays.asList("k", "other")), HashSet(ini.keys))
        assertEquals(Arrays.asList("1", "2"), list(ini.getValues("k")))
        assertSame(ini.section, ini.section)
        assertEquals("A", ini.section!!.name)
    }

    @Test
    fun testIniMissingAndOutOfRange() {
        val ini = loadedIni(TWO)
        assertNull(ini.getValue("Z", "k", 0))
        assertNull(ini.getValue("Z", "k"))
        assertEquals(0, ini.getValueSize("Z", "k"))
        assertNull(ini.getValue("A", "nope"))
        assertNull(ini.getValue("nope"))
        assertFailsWith<IndexOutOfBoundsException> {
            // from the ArrayList of values
            ini.getValue("A", "k", 9)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            // from the section list
            ini.getSectionName(5)
        }
        assertFailsWith<UnsupportedOperationException> {
            // unmodifiable
            (ini.sections as MutableList<String>).add("x")
        }
    }

    @Test
    fun testIniParseDetails() {
        val ini = loadedIni("# c\n; c\n\nlead=1\n[ Name ]\n  spaced  =  v w  \neq=a=b\ncont=x \\\n  y\nempty=\nbad line\nKey=1\nkey=2\ngreek=" + LOGOS + "\n")
        // A key before any header lives in the section named ""
        assertEquals(Arrays.asList("", " Name "), ini.sections)
        assertEquals("1", ini.getValue("", "lead"))
        assertEquals("v w", ini.getValue(" Name ", "spaced"))
        assertEquals("a=b", ini.getValue(" Name ", "eq"))
        assertEquals("x\ny", ini.getValue(" Name ", "cont"))
        assertEquals("", ini.getValue(" Name ", "empty"))
        assertNull(ini.getValue(" Name ", "bad line"))
        assertEquals("1", ini.getValue(" Name ", "Key"))
        assertEquals("2", ini.getValue(" Name ", "key"))
        assertEquals(LOGOS, ini.getValue(" Name ", "greek"))
    }

    @Test
    fun testIniContinuationAtEndKeepsTheNewline() {
        val ini = loadedIni("[S]\nk=v \\")
        assertEquals("v\n", ini.getValue("S", "k"))
    }

    @Test
    fun testIniDoesNotStripByteOrderMark() {
        // Unlike IniSection, "\uFEFF[Bom]" is not a section line; it has no '=' so it is skipped
        val ini = loadedIni("\uFEFF[Bom]\nk=v\n")
        assertEquals(Arrays.asList(""), ini.sections)
        assertEquals("v", ini.getValue("", "k"))
    }

    @Test
    fun testIniEmptyKeyStillCreatesTheSection() {
        val ini = loadedIni("[S]\n=v\n")
        assertEquals(Arrays.asList("S"), ini.sections)
        assertNull(ini.getValue("S", ""))
        assertEquals(0, ini.section!!.size())
    }

    @Test
    fun testIniAddReplaceRemove() {
        val ini = loadedIni(TWO)
        // "a" finds section A case-insensitively, so no section is added
        assertTrue(ini.add("a", "k", "9"))
        assertEquals(Arrays.asList("A", "B"), ini.sections)
        assertEquals(3, ini.getValueSize("A", "k"))
        assertTrue(ini.add("C", "n", "v"))
        assertEquals(Arrays.asList("A", "B", "C"), ini.sections)
        assertFalse(ini.add("C", "", "v"))
        assertTrue(ini.replace("A", "k", "only"))
        assertEquals(1, ini.getValueSize("A", "k"))
        assertEquals("only", ini.getValue("A", "k"))

        // removing the last value of the last key removes the section
        assertTrue(ini.remove("B", "k", "3"))
        assertEquals(Arrays.asList("A", "C"), ini.sections)
        assertEquals(2, ini.size())
        assertFalse(ini.remove("B", "k", "3"))
        assertFalse(ini.remove("A", "k", "nope"))
        assertEquals(2, ini.size())
    }

    @Test
    fun testIniRemoveKeyRemovesSectionEvenWhenKeyIsMissing() {
        val ini = loadedIni(TWO)
        // Quirk: the section is dropped whether or not the key existed
        assertFalse(ini.remove("A", "nope"))
        assertEquals(Arrays.asList("B"), ini.sections)
        assertTrue(ini.remove("B", "k"))
        assertEquals(0, ini.size())
        assertFalse(ini.remove("B", "k"))
    }

    @Test
    fun testIniRemoveWithDifferentCaseLeavesAStaleListEntry() {
        val ini = loadedIni("[A]\nk=1\n")
        // The case-insensitive map finds and drops the section, but the list removes by exact equals
        assertTrue(ini.remove("a", "k", "1"))
        assertEquals(0, ini.size())
        assertEquals(Arrays.asList("A"), ini.sections)
        assertNull(ini.section)
    }

    @Test
    fun testIniFirstSectionHelpersOnEmptyIni() {
        val ini = Ini()
        assertEquals(0, ini.size())
        assertNull(ini.sectionName)
        assertNull(ini.section)
        assertNull(ini.keys)
        assertNull(ini.getValues("k"))
        assertNull(ini.getValue("k"))
        assertEquals(0, ini.getValueSize("k"))
        // These claim success without doing anything
        assertTrue(ini.addValue("k", "v"))
        assertTrue(ini.removeValue("k", "v"))
        assertTrue(ini.removeValue("k"))
        assertTrue(ini.replaceValue("k", "v"))
        assertEquals(0, ini.size())
        assertFailsWith<IndexOutOfBoundsException> {
            // empty list
            ini.getSectionName(0)
        }
    }

    @Test
    fun testIniFirstSectionHelpers() {
        val ini = loadedIni(TWO)
        assertTrue(ini.addValue("k", "x"))
        assertEquals(Arrays.asList("1", "2", "x"), list(ini.getValues("k")))
        assertTrue(ini.replaceValue("k", "y"))
        assertEquals(Arrays.asList("y"), list(ini.getValues("k")))
        assertTrue(ini.removeValue("k", "y"))
        assertNull(ini.getValues("k"))
        assertTrue(ini.removeValue("other"))
        // A has no keys left, but removeValue(key) dropped the section
        assertEquals(Arrays.asList("B"), ini.sections)
    }

    @Test
    fun testIniClear() {
        val ini = loadedIni(TWO)
        ini.clear()
        assertEquals(0, ini.size())
        assertTrue(ini.sections.isEmpty())
    }

    @Test
    fun testIniSaveAndReloadRoundTrip() {
        val ini = loadedIni("[A]\nk=1\nk=2\ncont=l1 \\\n l2\ngreek=" + LOGOS + "\n[ B b ]\nk=" + AAKKOSET + "\nempty=\n")
        val file = folder.newFile("round.ini")
        ini.save(file, UTF8)
        val back = Ini()
        back.load(file, UTF8)
        assertEquals(ini.sections, back.sections)
        assertEquals("2", back.getValue("A", "k", 1))
        assertEquals("l1\nl2", back.getValue("A", "cont"))
        assertEquals(LOGOS, back.getValue("A", "greek"))
        assertEquals(AAKKOSET, back.getValue(" B b ", "k"))
        assertEquals("", back.getValue(" B b ", "empty"))

        // load(InputStream)
        val fromFile = Ini()
        fromFile.load(java.io.ByteArrayInputStream(bytes(TWO)), UTF8)
        assertEquals(Arrays.asList("A", "B"), fromFile.sections)
    }

    companion object {
        private const val UTF8 = "UTF-8"
        private val NL = System.getProperty("line.separator")

        /** Greek lambda omicron gamma omicron sigma */
        private const val LOGOS = "λόγος"
        private const val AAKKOSET = "Ääkköset"

        private val CONF = "\uFEFF".substring(1) +
            "[ Name ]\n" +
            "# a comment\n" +
            "; another comment\n" +
            "\n" +
            "key=value\n" +
            "Key=other\n" +
            "dup=one\n" +
            "dup=two\n" +
            "dup=one\n" +
            "eq=a=b=c\n" +
            "   spaced key  =   spaced value   \n" +
            "cont=line1 \\\n" +
            "   line2\n" +
            "empty=\n" +
            "no equals here\n" +
            "greek=" + LOGOS + "\n" +
            "fin=" + AAKKOSET + "\n"

        private const val TWO = "[A]\nk=1\nk=2\nother=o\n[B]\nk=3\n"
    }
}
