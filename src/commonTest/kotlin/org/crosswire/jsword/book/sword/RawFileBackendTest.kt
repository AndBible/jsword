package org.crosswire.jsword.book.sword

import kotlin.test.Test
import kotlin.test.assertEquals
import org.crosswire.common.util.pathUri
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.state.RawFileBackendState
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseFactory
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.system.Versifications
import org.crosswire.ksword.fixtures.withTestDirectory

class RawFileBackendTest {
    private fun fixture(block: (RawFileBackend, RawFileBackendState) -> Unit) = withTestDirectory { root ->
        // The state pool keys metadata by name. Each isolated directory needs a distinct pool key.
        val conf = """
            [TestComment]
            Lang=de
            Description=MyNewBook-${root.name}
            ModDrv=RawFiles
            DataPath=test
            Encoding=UTF-8
        """.trimIndent().encodeToByteArray()
        val metadata = SwordBookMetaData(conf, "testconfig.conf").apply { library = pathUri(root) }
        val backend = RawFileBackend(metadata, 2)
        backend.create()
        val state = backend.initState()
        try { block(backend, state) } finally { state.releaseResources() }
    }

    @Test fun testSetRawText() = fixture { backend, state ->
        val v11n = Versifications.instance().getVersification(backend.bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION))!!
        val verses = listOf(
            Verse(v11n, BibleBook.GEN, 3, 1) to "Hello OT",
            Verse(v11n, BibleBook.LEV, 3, 5) to "Hello OT2",
            Verse(v11n, BibleBook.EXOD, 6, 4) to "Hello OT3",
            Verse(v11n, BibleBook.JUDG, 3, 1) to "Hello OT4",
            Verse(v11n, BibleBook.PET2, 1, 2) to "Hello NT",
            Verse(v11n, BibleBook.PET1, 1, 2) to "Hello NT2",
            Verse(v11n, BibleBook.REV, 22, 21) to "Hello NT3",
            Verse(v11n, BibleBook.JOHN3, 1, 2) to "Hello NT4")
        for ((verse, text) in verses) backend.setRawText(state, verse, text)
        for ((verse, text) in verses) assertEquals(text, backend.getRawText(state, verse))
    }

    @Test fun testSetAliasKey() = fixture { backend, state ->
        val v11n = Versifications.instance().getVersification(backend.bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION))!!
        val source = VerseFactory.fromString(v11n, "Gen 1:1")!!
        val alias1 = VerseFactory.fromString(v11n, "Gen 1:2")!!
        val alias2 = VerseFactory.fromString(v11n, "Gen 1:3")!!
        backend.setRawText(state, source, "Hello Alias test!")
        backend.setAliasKey(state, alias1, source)
        backend.setAliasKey(state, alias2, source)
        assertEquals("Hello Alias test!", backend.getRawText(state, source))
        assertEquals("Hello Alias test!", backend.getRawText(state, alias1))
        assertEquals("Hello Alias test!", backend.getRawText(state, alias2))
    }
}
