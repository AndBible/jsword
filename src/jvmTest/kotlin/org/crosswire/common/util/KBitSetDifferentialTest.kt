package org.crosswire.common.util

import java.util.BitSet
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class KBitSetDifferentialTest {
    private fun assertSame(j: BitSet, k: KBitSet, ctx: String) {
        assertEquals(j.cardinality(), k.cardinality(), "$ctx cardinality")
        assertEquals(j.isEmpty, k.isEmpty, "$ctx isEmpty")
        assertEquals(j.hashCode(), k.hashCode(), "$ctx hashCode")
        assertEquals(j.toString(), k.toString(), "$ctx toString")
        var a = j.nextSetBit(0); var b = k.nextSetBit(0)
        while (a >= 0 || b >= 0) { assertEquals(a, b, "$ctx nextSetBit"); if (a < 0) break; a = j.nextSetBit(a + 1); b = k.nextSetBit(b + 1) }
    }

    @Test
    fun randomOperationsMatchJavaUtilBitSet() {
        val rnd = Random(42)
        repeat(200) { round ->
            val n = 1 + rnd.nextInt(32000)   // KJV maximumOrdinal is ~32k
            val j1 = BitSet(n); val k1 = KBitSet(n); val j2 = BitSet(n); val k2 = KBitSet(n)
            repeat(rnd.nextInt(500)) { val i = rnd.nextInt(n); j1.set(i); k1.set(i) }
            repeat(rnd.nextInt(500)) { val i = rnd.nextInt(n); j2.set(i); k2.set(i) }
            repeat(rnd.nextInt(50)) { val i = rnd.nextInt(n); j1.clear(i); k1.clear(i) }
            for (i in 0 until n step 97) assertEquals(j1.get(i), k1[i], "get $i")
            assertSame(j1, k1, "r$round base")
            when (round % 3) {
                0 -> { j1.and(j2); k1.and(k2) }
                1 -> { j1.or(j2); k1.or(k2) }
                else -> { j1.andNot(j2); k1.andNot(k2) }
            }
            assertSame(j1, k1, "r$round op")
            val kc = k1.copy(); kc.set(0); kc.clear(1)
            assertSame(j1, k1, "r$round copy is independent")
            j1.clear(); k1.clear(); assertSame(j1, k1, "r$round clear")
        }
    }

    @Test
    fun equalityDependsOnContentNotCapacity() {
        val small = KBitSet(0).apply { set(63); set(64) }
        val large = KBitSet(4096).apply { set(63); set(64) }
        assertEquals(small, large)
        assertEquals(large, small)
        assertEquals(small.hashCode(), large.hashCode())
        assertEquals(KBitSet(0), KBitSet(4096))
        large.set(4095)
        kotlin.test.assertNotEquals(small, large)
        kotlin.test.assertNotEquals(large, small)
        kotlin.test.assertNotEquals<Any>(small, "{63, 64}")
    }

    @Test
    fun algebraHandlesDifferentCapacitiesAndSelfAliasing() {
        for (operation in 0..2) {
            for (reverse in listOf(false, true)) {
                val small = KBitSet(0).apply { set(0); set(63) }
                val large = KBitSet(1024).apply { set(63); set(64); set(1000) }
                val a = if (reverse) large else small
                val b = if (reverse) small else large
                when (operation) {
                    0 -> { a.and(b); assertEquals("{63}", a.toString()) }
                    1 -> { a.or(b); assertEquals("{0, 63, 64, 1000}", a.toString()) }
                    else -> {
                        a.andNot(b)
                        assertEquals(if (reverse) "{64, 1000}" else "{0}", a.toString())
                    }
                }
            }
        }
        val s = KBitSet(0).apply { set(0); set(63); set(64); set(1000) }
        s.and(s)
        assertEquals("{0, 63, 64, 1000}", s.toString())
        s.or(s)
        assertEquals("{0, 63, 64, 1000}", s.toString())
        s.andNot(s)
        assertEquals("{}", s.toString())
    }

    @Test
    fun copyPreservesContentWithBidirectionalIndependence() {
        val original = KBitSet(0).apply { set(0); set(63); set(64); set(1000) }
        val copy = original.copy()
        assertEquals("{0, 63, 64, 1000}", copy.toString())
        assertEquals(original, copy)
        copy.clear(63)
        copy.set(2000)
        assertEquals("{0, 63, 64, 1000}", original.toString())
        original.clear(64)
        original.set(3)
        assertEquals("{0, 64, 1000, 2000}", copy.toString())
        assertEquals("{0, 3, 63, 1000}", original.toString())
    }

    @Test
    fun setBeyondTheInitialSizeGrows() {
        val j = BitSet(10); val k = KBitSet(10)
        j.set(1000); k.set(1000)
        assertSame(j, k, "grow")
        assertEquals(-1, k.nextSetBit(1001))
    }
}
