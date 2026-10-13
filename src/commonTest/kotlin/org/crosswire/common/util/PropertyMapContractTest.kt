package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PropertyMapContractTest {
    @Test fun replacementRetainsOrderAndReinsertionMovesToEnd() {
        val map = PropertyMap()
        assertNull(map.put("b", "two"))
        map.putAll(mapOf("a" to "one", "c" to "three"))
        assertEquals("one", map.put("a", "updated"))
        assertEquals(listOf("b", "a", "c"), map.keys.toList())
        assertEquals("two", map.remove("b"))
        map["b"] = "last"
        assertEquals(listOf("a", "c", "b"), map.keys.toList())
        assertEquals(listOf("updated", "three", "last"), map.values.toList())
        assertNull(map.remove("missing"))
    }

    @Test fun defaultsAffectLookupButNotExplicitMembershipOrViews() {
        val root = PropertyMap().apply { put("root", "inherited") }
        val defaults = PropertyMap(root).apply { put("shared", "default") }
        val map = PropertyMap(defaults).apply { put("shared", "explicit") }
        assertEquals("inherited", map["root"])
        assertEquals("explicit", map["shared"])
        assertEquals("fallback", map.get("missing", "fallback"))
        assertNull(map["missing"])
        assertFalse(map.containsKey("root"))
        assertFalse(map.containsValue("inherited"))
        assertEquals(1, map.size)
        assertEquals(listOf("shared"), map.keys.toList())
        assertEquals(listOf("explicit"), map.values.toList())
        assertEquals(listOf("shared" to "explicit"), map.entries.map { it.key to it.value })
        map.clear()
        assertTrue(map.isEmpty())
        assertEquals("default", map["shared"])
        assertEquals("inherited", map["root"])
    }

    @Test fun keyViewIsLiveAndRemovesBackingEntries() {
        val map = PropertyMap().apply { put("a", "one"); put("b", "two") }
        val keys = map.keys
        map["c"] = "three"
        assertEquals(listOf("a", "b", "c"), keys.toList())
        assertTrue(keys.remove("b"))
        assertFalse(map.containsKey("b"))
        val iterator = keys.iterator()
        assertEquals("a", iterator.next())
        iterator.remove()
        assertFailsWith<IllegalStateException> { iterator.remove() }
        assertEquals(listOf("c"), map.keys.toList())
        keys.clear()
        assertTrue(map.isEmpty())
    }

    @Test fun valueViewRemovesOnlyFirstDuplicateAndSupportsIteratorRemoval() {
        val map = PropertyMap().apply {
            put("a", "duplicate"); put("b", "duplicate"); put("c", "last")
        }
        val values = map.values
        assertTrue(values.remove("duplicate"))
        assertEquals(listOf("b", "c"), map.keys.toList())
        val iterator = values.iterator()
        assertEquals("duplicate", iterator.next())
        iterator.remove()
        map["d"] = "new"
        assertEquals(listOf("last", "new"), values.toList())
        assertFalse(values.remove("absent"))
        values.clear()
        assertTrue(map.isEmpty())
    }

    @Test fun entryViewUpdatesAndRemovesBackingEntries() {
        val map = PropertyMap().apply { put("a", "one"); put("b", "two") }
        val entries = map.entries
        val iterator = entries.iterator()
        val first = iterator.next()
        assertEquals("a", first.key)
        assertEquals("one", first.setValue("changed"))
        assertEquals("changed", map["a"])
        assertEquals(listOf("a", "b"), map.keys.toList())
        iterator.remove()
        assertFalse(map.containsKey("a"))
        val wrongValue = mutableMapOf("b" to "wrong").entries.single()
        assertFalse(entries.remove(wrongValue))
        val matching = mutableMapOf("b" to "two").entries.single()
        assertTrue(entries.contains(matching))
        assertTrue(entries.remove(matching))
        assertTrue(map.isEmpty())
        map["c"] = "three"
        assertEquals(listOf("c"), entries.map { it.key })
        entries.clear()
        assertTrue(map.isEmpty())
    }

    @Test fun equalityAndHashUseExplicitEntriesRegardlessOfOrderOrDefaults() {
        val defaults = PropertyMap().apply { put("default", "hidden") }
        val map = PropertyMap(defaults).apply { put("a", "one"); put("b", "two") }
        val reverse = linkedMapOf("b" to "two", "a" to "one")
        assertTrue(map == reverse)
        assertTrue(reverse == map)
        assertEquals(reverse.hashCode(), map.hashCode())
        assertFalse(map == mapOf("a" to "one"))
        assertFalse(map == mapOf("a" to "one", "b" to "wrong"))
        val empty = PropertyMap(defaults)
        assertTrue(empty == emptyMap<String, String>())
        assertEquals(0, empty.hashCode())
    }

    @Test fun iteratorBoundaryAndUnsupportedViewAdditions() {
        val map = PropertyMap().apply { put("a", "one") }
        val iterator = map.entries.iterator()
        assertFailsWith<IllegalStateException> { iterator.remove() }
        iterator.next()
        assertFailsWith<NoSuchElementException> { iterator.next() }
        assertFailsWith<UnsupportedOperationException> { map.keys.add("b") }
        assertFailsWith<UnsupportedOperationException> { map.values.add("two") }
        assertFailsWith<UnsupportedOperationException> {
            map.entries.add(mutableMapOf("b" to "two").entries.single())
        }
        assertEquals(mapOf("a" to "one"), map)
    }
}
