package org.crosswire.jsword.index.fts

import kotlin.test.Test
import kotlin.test.assertEquals

class WrittenTextTest {
    @Test fun removesSpaceBeforeClosingPunctuation() {
        assertEquals("light: and there was light.", writtenText("light : and there was light ."))
        assertEquals("a, b; c! d? e", writtenText("a , b ; c ! d ? e"))
    }

    @Test fun removesSpaceAroundBrackets() {
        assertEquals("(word) and [other]", writtenText("( word ) and [ other ]"))
    }

    @Test fun handlesQuotes() {
        assertEquals("“Hi,” he said.", writtenText("“ Hi , ” he said ."))
        assertEquals("‘a’ and «b»", writtenText("‘ a ’ and « b »"))
    }

    @Test fun collapsesWholeWhitespaceRuns() {
        assertEquals("a: b", writtenText("a   \t: b"))
    }

    @Test fun keepsTextWithoutPunctuationAndOtherSpaces() {
        assertEquals("In the beginning  God", writtenText("In the beginning  God"))
        assertEquals("", writtenText(""))
        assertEquals("said:", writtenText("said :"))
    }

    @Test fun joinsApostropheSAndNtSplitAtAnElementBoundary() {
        assertEquals("a sabbath day's journey.", writtenText("a sabbath day ' s journey ."))
        assertEquals("the Lord's passover", writtenText("the Lord 's passover"))
        assertEquals("the Lord’s passover", writtenText("the Lord ’s passover"))
        assertEquals("the Lord’s passover", writtenText("the Lord ’ s passover"))
        assertEquals("I don't know", writtenText("I do n't know"))
    }

    @Test fun keepsPluralPossessivesAndOrdinaryWords() {
        assertEquals("his sons' wives and his sons' sons", writtenText("his sons' wives and his sons' sons"))
        assertEquals("it is so", writtenText("it is so"))
        assertEquals("Jesus' sake", writtenText("Jesus' sake"))
        assertEquals("brother's keeper", writtenText("brother's keeper"))
    }
}
