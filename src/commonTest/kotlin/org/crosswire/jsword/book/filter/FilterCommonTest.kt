package org.crosswire.jsword.book.filter

import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.common.util.PluginRegistry
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.DataPolice
import org.crosswire.jsword.passage.DefaultLeafKeyList
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.*

internal expect fun filterTestBook(initials: String): Book

class FilterCommonTest {
    @Test fun frozenOsis() = verify("osis")
    @Test fun frozenThml() = verify("thml")
    @Test fun frozenGbf() = verify("gbf")
    @Test fun frozenPlainText() = verify("plaintext")

    private fun verify(type: String) {
        val cases = frozenFilterCases.filter { it.type == type }
        assertEquals(10, cases.size)
        val output = XMLOutputter(Format.getRawFormat())
        for (case in cases) {
            val book = filterTestBook(case.module)
            val key = if (type == "plaintext") DefaultLeafKeyList(case.key) else
                PassageKeyFactory.instance().getKey(Versifications.instance().getVersification("KJV"), case.key).getVerseAt(0)
            val filter = SourceFilterFactory.getFilter(type)
            val actual = output.outputString(filter.toOSIS(book, key, case.input))
            assertEquals(case.expected, actual, "${case.module} ${case.key}, frozen accepted stage ${case.acceptedStage}")
        }
    }

    @Test fun factoryUsesProductionRegistryAndFreshPortableClones() {
        val expected = listOf("default", "gbf", "osis", "plaintext", "tei", "thml")
        assertEquals(expected, PluginRegistry.entries(SourceFilter::class).map { it.name })
        for (name in expected) {
            val first = SourceFilterFactory.getFilter(name)
            val second = SourceFilterFactory.getFilter(name.uppercase())
            assertNotSame(first, second)
            assertEquals(first::class, second::class)
        }
        assertEquals(SourceFilterFactory.getDefaultFilter()::class, SourceFilterFactory.getFilter("unknown-format")::class)
    }

    @Test fun frozenFallbackCasesAreNotLost() {
        assertTrue(frozenFilterCases.any { it.module == "ISV" && it.key == "1Cor.14.33" && it.acceptedStage == 3 })
        assertTrue(frozenFilterCases.any { it.module == "NASB" && it.key == "Mark.10.34" && it.acceptedStage == 2 && "<q sID=" in it.input })
    }

    @Test fun reportingHasRealStateAndAcceptsRealCommonBookContext() {
        val original = DataPolice.isReporting
        try {
            DataPolice.isReporting = true
            assertTrue(DataPolice.isReporting)
            DataPolice.report(filterTestBook("K5"), DefaultLeafKeyList("case"), "portable filter diagnostic")
            assertFailsWith<NullPointerException> { DataPolice.report(null, null, "invalid reporting context") }
            DataPolice.isReporting = false
            assertFalse(DataPolice.isReporting)
            DataPolice.report(null, null, "disabled reporting does not dereference context")
        } finally {
            DataPolice.isReporting = original
        }
    }
}
