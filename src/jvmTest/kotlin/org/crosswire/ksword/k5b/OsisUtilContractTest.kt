package org.crosswire.ksword.k5b

import fi.sykero.kdom.Element
import fi.sykero.kdom.Text
import org.crosswire.common.diff.Difference
import org.crosswire.common.diff.EditType
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.study.StrongsNumber
import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * K5b Task 7 characterization of `OSISUtil` and `StrongsNumber`. The expected values in [OsisUtilContractData]
 * were captured from the pre-conversion code and must stay green after the common-ready conversion.
 * Verse fragments come from the K5 oracle rows (KJV, ESV2011, FinRK); RTF snippets come from the `About` fields
 * of the test modules (only `\par` and `\pard` occur there, the other commands use hand-written snippets).
 */
class OsisUtilContractTest {
    @Test fun dataSetsAreComplete() {
        assertTrue(OsisUtilContractData.VERSES.count { it.book != "SYN" } >= 30)
        assertEquals(setOf("KJV", "ESV2011", "FinRK", "SYN"), OsisUtilContractData.VERSES.map { it.book }.toSet())
        assertTrue(OsisUtilContractData.VERSES.any { it.xml.contains("<w lemma") && it.xml.contains("<note") })
        assertTrue(OsisUtilContractData.VERSES.any { it.xml.contains("morph=") })
        assertTrue(OsisUtilContractData.VERSES.any { it.xml.contains("<title") })
        assertTrue(OsisUtilContractData.VERSES.any { it.xml.contains("who=\"Jesus\"") })
        assertEquals(10, OsisUtilContractData.RTF.count { !it.label.startsWith("edge") })
        assertTrue(OsisUtilContractData.STRONGS.size >= 20)
    }

    @Test fun extractorsMatchCapturedValuesForVerses() {
        for (case in OsisUtilContractData.VERSES) {
            val actual = OsisUtilContractProbe.verseFields(case.xml)
            assertEquals("${case.book} ${case.key}", case.expected.size, actual.size)
            for (i in actual.indices) {
                assertEquals("${case.book} ${case.key} ${OsisUtilContractProbe.fieldNames[i]}", case.expected[i], actual[i])
            }
        }
    }

    @Test fun rtfToOsisMatchesCapturedOutput() {
        for (case in OsisUtilContractData.RTF) {
            assertEquals(case.label, case.expected, OsisUtilContractProbe.rtfOutput(case.rtf))
        }
    }

    @Test fun strongsNumberMatchesCapturedValues() {
        for (case in OsisUtilContractData.STRONGS) {
            assertEquals("input=${case.input}", case.expected, OsisUtilContractProbe.strongsFields(case.input))
        }
    }

    @Test fun strongsNumberEqualityIsClassAndValueBased() {
        val a = StrongsNumber("G25")
        assertEquals(a, StrongsNumber("g0025"))
        assertEquals(a.hashCode(), StrongsNumber("g0025").hashCode())
        assertFalse(a.equals(StrongsNumber("H25")))
        assertFalse(a.equals(StrongsNumber("G26")))
        assertFalse(a.equals("G25"))
        assertFalse(a.equals(null))
        // The part is ignored by equals, and a subclass is never equal to its base (javaClass comparison).
        assertEquals(StrongsNumber("G3588a"), StrongsNumber("G3588"))
        val sub = object : StrongsNumber("G25") {}
        assertFalse(a.equals(sub))
        assertFalse(sub.equals(a))
        assertEquals("G0001", StrongsNumber("G1").getStrongsNumber())
        assertEquals("H12345", StrongsNumber("H12345").getStrongsNumber())
    }

    @Test fun strongsNumberCharShortConstructor() {
        val n = StrongsNumber('G', 25.toShort(), "a")
        assertEquals("G0025", n.getStrongsNumber())
        assertEquals("G0025a", n.fullStrongsNumber)
        assertTrue(n.isPart)
        assertTrue(n.isGreek)
    }

    @Test fun osisFrameworkAndFactoryElements() {
        val f = OSISUtil.factory()
        assertEquals("w", f.createW().name)
        assertEquals("div", f.createDiv().name)
        assertEquals("lb", f.createLB().name)
        val hdr = f.createHeaderCell()
        assertEquals("label", hdr.getAttributeValue("role"))
        assertEquals("center", hdr.getAttributeValue("align"))
        assertEquals("hi", f.createHI().name)
        assertEquals("x", f.createText("x").text)
    }

    @Test fun diffToOsisWrapsEditsInHi() {
        val diffs = mutableListOf(
            Difference(EditType.EQUAL, "same "), Difference(EditType.DELETE, "gone "), Difference(EditType.INSERT, "new"),
        )
        val out = XMLOutputter(Format.getRawFormat()).outputString(OSISUtil.diffToOsis(diffs))
        assertEquals("same <hi type=\"line-through\">gone </hi><hi type=\"underline\">new</hi>", out)
    }

    @Test fun canonicalTextIgnoresAdjacentNoBreakSpaceAsWhitespace() {
        val div = OSISUtil.factory().createDiv()
        div.addContent(Text("a "))
        div.addContent(Text("b"))
        // Java Character.isWhitespace(U+00A0) is false: a space is inserted between adjacent text nodes.
        assertEquals("a  b", OSISUtil.getCanonicalText(div))
    }

    @Test fun createOsisFrameworkNestsWorkInHeaderInOsisText() {
        val root: Element = OSISUtil.createOsisFramework(org.crosswire.jsword.book.Books.installed().getBook("KJV")!!.bookMetaData)
        assertEquals("osis", root.name)
        val text = root.getChild("osisText")!!
        assertEquals("Bible.KJV", text.getAttributeValue("osisIDWork"))
        assertEquals("KJV", text.getChild("header")!!.getChild("work")!!.getAttributeValue("osisWork"))
    }
}
