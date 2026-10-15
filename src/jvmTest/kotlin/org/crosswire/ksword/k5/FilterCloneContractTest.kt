package org.crosswire.ksword.k5

import org.crosswire.jsword.book.filter.gbf.GBFFilter
import org.crosswire.jsword.book.filter.osis.OSISFilter
import org.crosswire.jsword.book.filter.plaintext.PlainTextFilter
import org.crosswire.jsword.book.filter.thml.THMLFilter
import org.junit.Assert.*
import org.junit.Test

class FilterCloneContractTest {
    private class CustomThml(val state: Any) : THMLFilter()
    private class CustomOsis(val state: Any) : OSISFilter()
    private class CustomGbf(val state: Any) : GBFFilter()
    private class CustomPlain(val state: Any) : PlainTextFilter()

    @Test fun unknownJvmSubclassesRetainRuntimeTypeAndShallowFields() {
        val state = Any()
        val originals = listOf(CustomThml(state), CustomOsis(state), CustomGbf(state), CustomPlain(state))
        for (original in originals) {
            val clone = original.clone()
            assertNotSame(original, clone)
            assertEquals(original.javaClass, clone.javaClass)
            val field = original.javaClass.getDeclaredField("state").apply { isAccessible = true }
            assertSame(state, field.get(clone))
        }
    }
}
