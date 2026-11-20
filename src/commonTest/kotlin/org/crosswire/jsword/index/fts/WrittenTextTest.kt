package org.crosswire.jsword.index.fts

import fi.sykero.kdom.Element
import org.crosswire.jsword.book.OSISUtil
import kotlin.test.Test
import kotlin.test.assertEquals

/** [writtenText] over hand-built OSIS fragments: only the space the canonical-text walk inserts is ever dropped. */
class WrittenTextTest {
    /** A verse whose children are [parts]: a `w` element for a String starting with `<`, otherwise a text node. */
    private fun verse(vararg parts: String): Element {
        val f = OSISUtil.factory()
        val div = f.createDiv()
        val verse = f.createVerse()
        div.addContent(verse)
        for (p in parts) {
            if (p.startsWith("<")) {
                val w = f.createW()
                w.addContent(p.substring(1))
                verse.addContent(w)
            } else verse.addContent(p)
        }
        return div
    }

    @Test fun kjvMarkupReadsAsWritten() {
        // Gen.1.3 as KJV stores it: words in <w>, punctuation in the text nodes between them.
        assertEquals(
            "And God said, Let there be light: and there was light.",
            writtenText(verse("<And God", " ", "<said", ", ", "<Let there be", " ", "<light", ": ", "<and there was light", ".")),
        )
        assertEquals("a sabbath day's journey.", writtenText(verse("<a sabbath day", "'", "<s", " ", "<journey", ".")))
    }

    @Test fun punctuationAndBracketsAtAnElementBoundary() {
        assertEquals("light: and", writtenText(verse("<light", ":", "<and")))
        assertEquals("(word) and [other]", writtenText(verse("(", "<word", ")", " and ", "[", "<other", "]")))
        assertEquals("“Hi,” he said.", writtenText(verse("“", "<Hi", ",”", " he said", ".")))
    }

    @Test fun wordsMeetingAtAnElementBoundaryStaySeparated() {
        assertEquals("the Lord", writtenText(verse("<the", "<Lord")))
        assertEquals("I don't know", writtenText(verse("I ", "<do", "n't", " know")))
    }

    @Test fun afrikaansAndDutchApostrophesKeepTheirSpaces() {
        assertEquals("en ’n man", writtenText(verse("en ", "’n man")))
        assertEquals("en ’n man", writtenText(verse("<en", " ", "<’n", " ", "<man")))
        assertEquals("het ’t huis", writtenText(verse("het ’t huis")))
        assertEquals("in huis 's Heeren", writtenText(verse("in huis 's Heeren")))
        assertEquals("in huis 's Heeren", writtenText(verse("<in huis", " ", "<'s", " ", "<Heeren")))
    }

    @Test fun spacesThatAreInTheTextAreNeverRemoved() {
        assertEquals("mot : mot", writtenText(verse("mot : mot")))
        assertEquals("In the beginning  God", writtenText(verse("In the beginning  God")))
        assertEquals("a ( b ) c", writtenText(verse("a ( b ) c")))
    }
}
