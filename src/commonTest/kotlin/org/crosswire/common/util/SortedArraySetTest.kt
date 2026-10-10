package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SortedArraySetTest {
    @Test fun ascendingAndDistinct() {
        val s = SortedArraySet<Int>()
        for (v in listOf(5, 1, 3, 1, 9)) s.add(v)
        assertEquals(listOf(1, 3, 5, 9), s.toList())
        assertFalse(s.add(3))
    }
    @Test fun iteratorContract() {
        val s = SortedArraySet<Int>().apply { addAll(listOf(1, 2, 3)) }
        val it = s.iterator()
        assertFailsWith<IllegalStateException> { it.remove() }
        it.next(); it.remove()
        assertFailsWith<IllegalStateException> { it.remove() }
        it.next(); it.next()
        assertFailsWith<NoSuchElementException> { it.next() }
        assertEquals(listOf(2, 3), s.toList())
    }
    @Test fun concurrentModificationIsDetected() {
        val s = SortedArraySet<Int>().apply { addAll(listOf(1, 2, 3)) }
        val it = s.iterator(); it.next()
        s.add(10)
        assertFailsWith<ConcurrentModificationException> { it.next() }
    }
}
