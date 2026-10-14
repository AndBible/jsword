package org.crosswire.jsword.book.sword

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.crosswire.common.util.IniSection
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.ksword.fixtures.withTestDirectory

class ConfigEntryTableTest {
    @Test fun testCreateConfigEntryTableInstance() {
        val table = ConfigEntryTable("TestBook", true)
        assertNotNull(table)
    }

    @Test fun testSaveConfigEntryTable() = withTestDirectory { root ->
        val table = IniSection("TestBook")
        assertNotNull(table)
        table.add(BookMetaData.KEY_LANG, "de")
        val lang = table.get(BookMetaData.KEY_LANG)
        assertNotNull(lang)
        assertEquals("de", lang)
        storageSave(table, StorageLocation((root / "testconfig.conf").toString()), "UTF-8")
    }
}
