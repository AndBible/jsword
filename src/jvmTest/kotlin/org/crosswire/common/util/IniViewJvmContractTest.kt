package org.crosswire.common.util

import kotlin.test.*

/** Characterizes the real JVM Collections wrappers independently of the Native implementation. */
class IniViewJvmContractTest {
    @Test fun realSectionKeyViewRejectsRemovalAndRetainsLiveContents() {
        val section = IniSection("test")
        section.add("first", "one")
        section.add("second", "two")
        val keys = section.keys
        val iterator = keys.iterator() as MutableIterator<String>
        iterator.next()
        assertFailsWith<UnsupportedOperationException> { iterator.remove() }
        assertEquals(setOf("first", "second"), section.keys.toSet())
        section.add("third", "three")
        assertEquals(setOf("first", "second", "third"), keys.toSet())
        assertEquals(3, keys.size)
    }

    @Test fun realSectionValueViewRejectsRemovalAndRetainsLiveContents() {
        val section = IniSection("test")
        section.add("key", "one")
        section.add("key", "two")
        val values = section.getValues("key")!!
        val iterator = values.iterator() as MutableIterator<String>
        assertEquals("one", iterator.next())
        assertFailsWith<UnsupportedOperationException> { iterator.remove() }
        assertEquals(listOf("one", "two"), section.getValues("key")!!.toList())
        assertEquals("two", iterator.next())
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
        section.add("key", "three")
        assertEquals(listOf("one", "two", "three"), values.toList())
        assertEquals(3, values.size)
    }
}
