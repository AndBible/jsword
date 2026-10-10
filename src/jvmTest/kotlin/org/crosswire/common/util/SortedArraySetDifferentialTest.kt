package org.crosswire.common.util

import java.util.TreeSet
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SortedArraySetDifferentialTest {
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
