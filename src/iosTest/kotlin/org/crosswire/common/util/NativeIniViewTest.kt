package org.crosswire.common.util

import kotlin.test.*

class NativeIniViewTest {
    @Test fun keysRemainLiveButIteratorCannotMutateSection() {
        val section = IniSection("test")
        section.add("first", "one")
        section.add("second", "two")
        val keys = section.keys
        rejectRemoval(keys.iterator())
        assertEquals(setOf("first", "second"), section.keys.toSet())
        assertEquals(listOf("one"), section.getValues("first")!!.toList())
        assertEquals(listOf("two"), section.getValues("second")!!.toList())
        section.add("third", "three")
        assertEquals(setOf("first", "second", "third"), keys.toSet())
        assertEquals(3, keys.size)
        assertTrue(keys.contains("third"))
    }

    @Test fun valuesRemainLiveButIteratorCannotMutateSection() {
        val section = IniSection("test")
        section.add("key", "one")
        section.add("key", "two")
        val values = section.getValues("key")!!
        rejectRemoval(values.iterator())
        assertEquals(listOf("one", "two"), section.getValues("key")!!.toList())
        section.add("key", "three")
        assertEquals(listOf("one", "two", "three"), values.toList())
        assertEquals(3, values.size)
        assertTrue(values.contains("three"))
    }

    private fun rejectRemoval(iterator: Iterator<String>) {
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
