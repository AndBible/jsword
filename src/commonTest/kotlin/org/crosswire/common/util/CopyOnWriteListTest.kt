package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CopyOnWriteListTest {
    private fun listOf3() = CopyOnWriteList(listOf("a", "b", "c"))

    @Test
    fun addRemoveSetAndIndexing() {
        val l = CopyOnWriteList<String>()
        assertTrue(l.isEmpty())
        assertTrue(l.add("a"))
        l.add(0, "z")
        assertEquals(listOf("z", "a"), l)
        assertEquals("a", l.set(1, "b"))
        assertEquals("z", l.removeAt(0))
        assertTrue(l.remove("b"))
        assertFalse(l.remove("b"))
        assertEquals(0, l.size)
        assertFailsWith<IndexOutOfBoundsException> { l[0] }
        assertFailsWith<IndexOutOfBoundsException> { l.add(1, "x") }
        assertFailsWith<IndexOutOfBoundsException> { l.removeAt(0) }
        assertFailsWith<IndexOutOfBoundsException> { l.set(0, "x") }
    }

    @Test
    fun iterationSeesTheSnapshotNotConcurrentAdds() {
        val l = listOf3()
        val seen = mutableListOf<String>()
        for (e in l) {
            seen += e
            if (e == "a") {
                l.add("d")
                l.remove("c")
            }
        }
        assertEquals(listOf("a", "b", "c"), seen)
        assertEquals(listOf("a", "b", "d"), l)
    }

    @Test
    fun iteratorsAreReadOnly() {
        val l = listOf3()
        val it = l.iterator()
        it.next()
        assertFailsWith<UnsupportedOperationException> { it.remove() }
        val li = l.listIterator(1)
        assertEquals(1, li.nextIndex())
        assertEquals("b", li.next())
        assertEquals("b", li.previous())
        assertFailsWith<UnsupportedOperationException> { li.set("x") }
        assertFailsWith<UnsupportedOperationException> { li.add("x") }
        assertFailsWith<UnsupportedOperationException> { li.remove() }
        assertEquals(listOf("a", "b", "c"), l)
    }

    @Test
    fun subListIsAReadOnlySnapshot() {
        val l = listOf3()
        val sub = l.subList(1, 3)
        l.add("d")
        assertEquals(listOf("b", "c"), sub)
        assertFailsWith<UnsupportedOperationException> { sub.add("x") }
        assertFailsWith<UnsupportedOperationException> { sub.removeAt(0) }
        assertFailsWith<UnsupportedOperationException> { sub[0] = "x" }
        assertFailsWith<IndexOutOfBoundsException> { l.subList(2, 9) }
    }

    @Test
    fun bulkOperations() {
        val l = CopyOnWriteList<Int>()
        assertTrue(l.addAll(listOf(1, 2, 3, 4)))
        assertFalse(l.addAll(emptyList()))
        assertTrue(l.addAll(1, listOf(9, 8)))
        assertEquals(listOf(1, 9, 8, 2, 3, 4), l)
        assertTrue(l.removeAll(listOf(9, 8)))
        assertFalse(l.removeAll(listOf(42)))
        assertTrue(l.retainAll(listOf(1, 2, 3)))
        assertEquals(listOf(1, 2, 3), l)
        assertTrue(l.removeAll { it % 2 == 1 })
        assertEquals(listOf(2), l)
        assertFalse(l.removeAll { it > 100 })
        l.clear()
        assertTrue(l.isEmpty())
    }

    @Test
    fun searchAndEquality() {
        val l = CopyOnWriteList(listOf("a", "b", "a"))
        assertEquals(0, l.indexOf("a"))
        assertEquals(2, l.lastIndexOf("a"))
        assertEquals(-1, l.indexOf("q"))
        assertTrue(l.contains("b"))
        assertTrue(l.containsAll(listOf("a", "b")))
        assertEquals(listOf("a", "b", "a"), l)
        assertEquals(l, listOf("a", "b", "a"))
        assertEquals(listOf("a", "b", "a").hashCode(), l.hashCode())
        assertEquals("[a, b, a]", l.toString())
        assertFalse(l == listOf("a"))
    }

    @Test
    fun constructorCopiesItsArgument() {
        val src = mutableListOf(1, 2)
        val l = CopyOnWriteList(src)
        src.add(3)
        assertEquals(listOf(1, 2), l)
    }

    @Test
    fun subListWithFromGreaterThanToThrowsIndexOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> { listOf3().subList(2, 1) }
        assertFailsWith<IndexOutOfBoundsException> { listOf3().subList(-1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { listOf3().subList(0, 4) }
        assertEquals(emptyList(), listOf3().subList(1, 1))
    }

    @Test
    fun removeAllAndRetainAllUseTheArgumentsOwnEquality() {
        val ignoreCase = CaseInsensitiveStrings("A", "c")
        val l = CopyOnWriteList(listOf("a", "b", "c"))
        assertTrue(l.removeAll(ignoreCase))
        assertEquals(listOf("b"), l)

        val m = CopyOnWriteList(listOf("a", "b", "c"))
        assertTrue(m.retainAll(ignoreCase))
        assertEquals(listOf("a", "c"), m)
    }

    /** A collection whose `contains` differs from `equals`/`hashCode` based lookup, like a TreeSet with a comparator. */
    private class CaseInsensitiveStrings(vararg items: String) : AbstractCollection<String>() {
        private val items = items.toList()
        override val size: Int get() = items.size
        override fun iterator(): Iterator<String> = items.iterator()
        override fun contains(element: String): Boolean = items.any { it.equals(element, ignoreCase = true) }
    }
}
