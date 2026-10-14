package org.crosswire.common.util

import kotlin.test.*

class IniLiveViewTest {
    @Test fun keyIteratorCannotRemoveFromBackingMap() {
        val backing = linkedMapOf("first" to "one", "second" to "two")
        val view = iniLiveView(backing.keys)
        rejectRemoval(view)
        assertEquals(listOf("first", "second"), backing.keys.toList())
        backing["third"] = "three"
        assertEquals(listOf("first", "second", "third"), view.toList())
        assertEquals(3, view.size)
        assertTrue(view.contains("third"))
    }

    @Test fun valueIteratorCannotRemoveFromBackingList() {
        val backing = arrayListOf("one", "two")
        val view = iniLiveView(backing)
        rejectRemoval(view)
        assertEquals(listOf("one", "two"), backing)
        backing.add("three")
        assertEquals(listOf("one", "two", "three"), view.toList())
        assertEquals(3, view.size)
        assertTrue(view.contains("three"))
    }

    private fun rejectRemoval(view: Collection<String>) {
        val iterator = view.iterator()
        assertTrue(iterator.hasNext())
        iterator.next()
        val failure = runCatching { (iterator as MutableIterator<String>).remove() }.exceptionOrNull()
        assertTrue(failure is ClassCastException || failure is UnsupportedOperationException,
            "Iterator mutation must be rejected, got $failure")
        assertTrue(iterator.hasNext())
        iterator.next()
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }
}
