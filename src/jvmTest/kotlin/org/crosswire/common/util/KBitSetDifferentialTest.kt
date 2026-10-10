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
    fun setBeyondTheInitialSizeGrows() {
        val j = BitSet(10); val k = KBitSet(10)
        j.set(1000); k.set(1000)
        assertSame(j, k, "grow")
        assertEquals(-1, k.nextSetBit(1001))
    }
}
