package org.crosswire.jsword.book.filter

import fi.sykero.kdom.Content
import fi.sykero.kdom.Element
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.filter.osis.OSISFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OsisChapterEndNoteTest {
    private val book = filterTestBook("ChapterEndNotes")
    private val key = book.getKey("Isa.66.24")

    private val note = """<note type="study" n="48"><hi type="italic">sn</hi> A final study note.</note>"""
    private val chapterEnd = """<chapter eID="Isa.66"/>"""
    private val verseEnd = """<verse eID="Isa.66.24"/>"""

    @Test
    fun noteAtEndOfWellFormedVerseKeepsItsMarkup() {
        assertNotePreserved("The final verse.$note")
    }

    @Test
    fun noteAfterLastVerseMilestoneKeepsItsMarkup() {
        assertNotePreserved("The final verse.$verseEnd$note$chapterEnd")
    }

    @Test
    fun bookClosingTagsAroundChapterEndDoNotFlattenNotes() {
        // SWORD entries are per-verse fragments: the book/section starts are in earlier entries.
        assertNotePreserved("The final verse.$note</div> $chapterEnd</div>")
    }

    @Test
    fun bookClosingTagsAfterLastVerseMilestoneDoNotFlattenNotes() {
        assertNotePreserved("The final verse.$verseEnd$note</div> $chapterEnd</div>")
    }

    private fun assertNotePreserved(input: String) {
        val content = OSISFilter().toOSIS(book, key, input)
        val parsedNote = assertNotNull(content.filterIsInstance<Element>().singleOrNull { it.name == "note" })
        assertEquals("study", parsedNote.getAttributeValue("type"))
        assertEquals("48", parsedNote.getAttributeValue("n"))
        assertEquals("italic", parsedNote.getChild("hi")?.getAttributeValue("type"))
        // The canonical reading/copy/speech path must not receive the note as ordinary verse text.
        val verse = Element("verse").addContent(content as List<Content>)
        assertEquals("The final verse.", OSISUtil.getCanonicalText(verse))
    }
}
