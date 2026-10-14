package org.crosswire.jsword.book.sword

import kotlin.test.Test
import kotlin.test.assertEquals

/** Preserve Java trim semantics: only characters up to U+0020 are stripped. */
class DataEntryCharacterizationTest {
    @Test fun rawTextTrimsEveryCharUpToSpace() {
        val data = " \u0001\u0002abc def\u001a \u0000".encodeToByteArray()
        assertEquals("abc def", DataEntry("test", data, "UTF-8").getRawText(null))
    }

    @Test fun rawTextKeepsNonAsciiWhitespace() {
        val data = " abc ".encodeToByteArray()
        assertEquals(" abc ", DataEntry("test", data, "UTF-8").getRawText(null))
    }
}
