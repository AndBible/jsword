package org.crosswire.jsword.book.filter.gbf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GbfStackPortableContractTest {
    @Test fun emptyRemovalHasNoDiagnosticMessage() {
        assertNull(assertFailsWith<NoSuchElementException> { GbfStack<String>().removeFirst() }.message)
    }

    @Test fun emptyLastHasNoDiagnosticMessage() {
        assertNull(assertFailsWith<NoSuchElementException> { GbfStack<String>().getLast() }.message)
    }

    @Test fun indexedBoundsRetainIndexAndSize() {
        val stack = GbfStack<String>()
        assertEquals("Index: 0, Size: 0", assertFailsWith<IndexOutOfBoundsException> { stack[0] }.message)
        stack.addFirst("value")
        for (index in listOf(-1, 1)) {
            assertEquals("Index: $index, Size: 1", assertFailsWith<IndexOutOfBoundsException> { stack[index] }.message)
        }
    }

    @Test fun stackOrderSurvivesReadsAndRemoval() {
        val stack = GbfStack<String>()
        assertTrue(stack.isEmpty())
        stack.addFirst("first")
        stack.addFirst("second")
        assertEquals("second", stack[0])
        assertEquals("first", stack[1])
        assertEquals("first", stack.getLast())
        assertEquals("second", stack.removeFirst())
        assertEquals("first", stack.getLast())
        assertEquals("first", stack.removeFirst())
        assertTrue(stack.isEmpty())
    }
}
