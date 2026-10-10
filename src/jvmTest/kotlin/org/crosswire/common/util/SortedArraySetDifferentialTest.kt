package org.crosswire.common.util

import java.util.TreeSet
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SortedArraySetDifferentialTest {
    @Test
    fun emptyCopyOfSameOrderingDoesNotCompareEveryElementAgain() {
        var comparisons = 0
        val order = Comparator<Int> { a, b -> comparisons++; a.compareTo(b) }
        val original = SortedArraySet(order).apply { addAll(0 until 4096) }
        val copy = SortedArraySet(order)
        comparisons = 0
        assertEquals(true, copy.addAll(original))
        assertEquals(0, comparisons, "already sorted source needs no binary-search rebuild")
        assertEquals(original.toList(), copy.toList())
        copy.remove(0)
        original.remove(4095)
        assertEquals(true, original.contains(0))
        assertEquals(true, copy.contains(4095))
        assertEquals(false, copy.addAll(copy))
    }

    @Test
    fun bulkCopyInvalidatesExistingIteratorButEmptyCopyDoesNot() {
        val source = SortedArraySet<Int>().apply { add(1); add(2) }
        val target = SortedArraySet<Int>()
        val stale = target.iterator()
        assertEquals(true, target.addAll(source))
        kotlin.test.assertFailsWith<ConcurrentModificationException> { stale.next() }
        val current = target.iterator()
        assertEquals(false, target.addAll(emptyList()))
        assertEquals(1, current.next())
        assertEquals(2, current.next())
        assertEquals(false, current.hasNext())
    }

    @Test
    fun differentOrderingAndNonemptyCopiesStillMatchTreeSet() {
        val reverse = SortedArraySet<Int>(reverseOrder()).apply { addAll(listOf(3, 2, 1)) }
        val target = SortedArraySet<Int>()
        val expected = TreeSet<Int>()
        assertEquals(expected.addAll(reverse), target.addAll(reverse))
        assertEquals(expected.toList(), target.toList())
        assertEquals(expected.addAll(listOf(0, 2, 4)), target.addAll(listOf(0, 2, 4)))
        assertEquals(expected.toList(), target.toList())
        assertEquals(expected.addAll(emptyList()), target.addAll(emptyList()))
    }

    @Test
    fun randomOperationsMatchTreeSet() {
        val rnd = Random(7)
        repeat(300) { round ->
            val t = TreeSet<Int>(); val s = SortedArraySet<Int>()
            repeat(rnd.nextInt(400)) {
                val v = rnd.nextInt(200)
                when (rnd.nextInt(4)) {
                    0, 1 -> assertEquals(t.add(v), s.add(v), "add $v")
                    2 -> assertEquals(t.remove(v), s.remove(v), "remove $v")
                    else -> assertEquals(t.contains(v), s.contains(v), "contains $v")
                }
            }
            assertEquals(t.toList(), s.toList(), "r$round order")
            assertEquals(t.size, s.size)
            // iterator remove of every third element
            val ti = t.iterator(); val si = s.iterator(); var i = 0
            while (ti.hasNext()) { assertEquals(ti.next(), si.next()); if (i++ % 3 == 0) { ti.remove(); si.remove() } }
            assertEquals(false, si.hasNext())
            assertEquals(t.toList(), s.toList(), "r$round after iterator remove")
            assertEquals(t.addAll(listOf(5, 500, 5)), s.addAll(listOf(5, 500, 5)))
            assertEquals(t.toList(), s.toList())
        }
    }

    @Test
    fun comparatorOrder() {
        val t = TreeSet<String>(String.CASE_INSENSITIVE_ORDER); val s = SortedArraySet<String>(String.CASE_INSENSITIVE_ORDER)
        for (w in listOf("b", "A", "a", "C", "B")) assertEquals(t.add(w), s.add(w))
        assertEquals(t.toList(), s.toList())
    }
}
