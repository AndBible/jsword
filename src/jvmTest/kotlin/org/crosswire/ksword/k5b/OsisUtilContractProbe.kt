package org.crosswire.ksword.k5b

import fi.sykero.kdom.Element
import fi.sykero.kdom.input.KdomBuilder
import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.study.StrongsNumber
import org.crosswire.jsword.versification.system.Versifications

/** One verse (or synthetic) OSIS fragment plus the values the old `OSISUtil` produced for it. */
internal class VerseCase(val book: String, val key: String, val xml: String, val expected: List<String>)

/** One RTF snippet plus the old `rtfToOsis` output (raw XML of the returned content) or an exception tag. */
internal class RtfCase(val label: String, val rtf: String, val expected: String)

/** One `StrongsNumber` input plus the old observable values. */
internal class StrongsCase(val input: String?, val expected: List<String>)

/** Computes the observable values of the `OSISUtil` extractors and `StrongsNumber`, shared by the test and the capture. */
internal object OsisUtilContractProbe {
    val fieldNames = listOf(
        "canonical", "plain", "strongs", "morphStrongs", "lexicalNoMorph", "notes", "headings",
        "references", "wCount", "fragmentSize",
    )

    fun parse(xml: String): Element = KdomBuilder().build(xml).rootElement

    fun verseFields(xml: String): List<String> {
        val root = parse(xml)
        val v11n = Versifications.instance().getVersification("KJV")
        return listOf(
            OSISUtil.getCanonicalText(root),
            OSISUtil.getPlainText(root),
            OSISUtil.getStrongsNumbers(root),
            OSISUtil.getMorphologiesWithStrong(root),
            OSISUtil.getLexicalInformation(root, false),
            OSISUtil.getNotes(root),
            OSISUtil.getHeadings(root),
            OSISUtil.getReferences(null, null, v11n, root),
            OSISUtil.getDeepContent(root, OSISUtil.OSIS_ELEMENT_W).size.toString(),
            OSISUtil.getFragment(root).size.toString(),
        )
    }

    /** Raw XML of the content list, or a tag for the exceptions the converter may throw. */
    fun rtfOutput(rtf: String): String = try {
        XMLOutputter(Format.getRawFormat()).outputString(OSISUtil.rtfToOsis(rtf))
    } catch (e: NumberFormatException) {
        "EXC:NumberFormatException"
    } catch (e: RuntimeException) {
        // java.util.EmptyStackException on the old code, NoSuchElementException after the conversion.
        "EXC:StackUnderflow"
    }

    fun strongsFields(input: String?): List<String> = try {
        val n = StrongsNumber(input)
        listOf(
            n.getStrongsNumber(), n.fullStrongsNumber, n.isValid().toString(), n.isGreek.toString(),
            n.isHebrew.toString(), n.isPart.toString(), n.toString(), n.hashCode().toString(),
        )
    } catch (e: NullPointerException) {
        listOf("EXC:NullPointerException")
    }
}
