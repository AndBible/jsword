package org.crosswire.jsword.book

import fi.sykero.kdom.input.KdomBuilder
import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.jsword.book.study.StrongsNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the production StrongsNumber and OSISUtil on every platform (so the iOS CI run executes them too).
 * Expected values are the ones OsisUtilContractTest captured from the pre-conversion JVM code.
 */
class StrongsOsisUtilCommonTest {
    private val matthew = "<div><verse osisID=\"Matt.4.19\" verseOrdinal=\"24204\"><w lemma=\"strong:G2532\" morph=\"robinson:CONJ\" src=\"1\">And</w> <w lemma=\"strong:G3004\" morph=\"robinson:V-PAI-3S\" src=\"2\">he saith</w> <w lemma=\"strong:G846\" morph=\"robinson:P-DPM\" src=\"3\">unto them</w>, <q who=\"Jesus\"><w lemma=\"strong:G1205\" morph=\"robinson:V-XXM-2P\" src=\"4\">Follow</w> <w lemma=\"strong:G3694\" morph=\"robinson:ADV\" src=\"5\" /><w lemma=\"strong:G3450\" morph=\"robinson:P-1GS\" src=\"6\">me</w>, <w lemma=\"strong:G2532\" morph=\"robinson:CONJ\" src=\"7\">and</w> <w lemma=\"strong:G4160\" morph=\"robinson:V-FAI-1S\" src=\"8\">I will make</w> <w lemma=\"strong:G5209\" morph=\"robinson:P-2AP\" src=\"9\">you</w> <w lemma=\"strong:G231\" morph=\"robinson:N-APM\" src=\"10\">fishers</w> <w lemma=\"strong:G444\" morph=\"robinson:N-GPM\" src=\"11\">of men</w>.</q><milestone resp=\"pdy 2003-12-14-08:55\" type=\"x-strongsMarkup\" /></verse></div>"

    @Test fun strongsNumberPadsToFourAsciiDigits() {
        assertEquals("H0430", StrongsNumber("H430").getStrongsNumber())
        assertEquals("G0025", StrongsNumber("g25").getStrongsNumber())
        assertEquals("H12345", StrongsNumber("H12345").getStrongsNumber())
        assertEquals("G3588a", StrongsNumber("G3588!a").fullStrongsNumber)
        for (input in listOf("H430", "G1", "G3588a")) {
            val number = StrongsNumber(input).getStrongsNumber()
            assertTrue(number.drop(1).all { it in '0'..'9' }, "ASCII digits only: $number")
        }
    }

    @Test fun osisExtractorsReadAVerseFragment() {
        val root = KdomBuilder().build(matthew).rootElement
        assertEquals("And he saith unto them, Follow me, and I will make you fishers of men.", OSISUtil.getPlainText(root))
        assertEquals("And he saith unto them , Follow me , and I will make you fishers of men .", OSISUtil.getCanonicalText(root))
        assertEquals("G2532 G3004 G846 G1205 G3694 G3450 G2532 G4160 G5209 G231 G444", OSISUtil.getStrongsNumbers(root))
        assertEquals(11, OSISUtil.getDeepContent(root, OSISUtil.OSIS_ELEMENT_W).size)
    }

    @Test fun osisHeadingOnlyFragment() {
        val root = KdomBuilder().build(
            "<div><div osisID=\"Gen.0.0\" type=\"x-gen\"><title type=\"main\">THE FIRST BOOK OF MOSES CALLED GENESIS</title></div></div>").rootElement
        assertEquals("", OSISUtil.getCanonicalText(root))
        assertEquals("THE FIRST BOOK OF MOSES CALLED GENESIS", OSISUtil.getHeadings(root))
    }

    @Test fun rtfToOsisConvertsStyleCommands() {
        val out = XMLOutputter(Format.getRawFormat()).outputString(OSISUtil.rtfToOsis("Plain \\i italic\\i0  then \\b bold\\b0  end\\par"))
        assertEquals("Plain <hi type=\"italic\">italic</hi> then <hi type=\"bold\">bold</hi> end<lb />", out)
    }
}
