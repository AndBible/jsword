package org.crosswire.jsword.passage

import org.crosswire.jsword.passage.baseline.BaselineDistinctPassage
import org.crosswire.jsword.passage.baseline.BaselineRangedPassage
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

/** Observational comparison, not a timing threshold: both stores run in the same test JVM. */
class PassageStoreTimingProbe {
    @Test
    fun addAndRemoveOverTheNewTestament() {
        val kjv = Versifications.instance().getVersification("KJV")!!
        val first = kjv.getOrdinal(Verse(kjv, BibleBook.MATT, 1, 1))
        val last = kjv.maximumOrdinal()
        val samples = linkedMapOf<String, MutableList<Double>>()
        println("K3 timing setup: baseline=12b8d39e warmups=2 measured=8 first=$first last=$last expected=${(last - first + 1) / 2}")
        for (round in -2 until 8) {
            for (kind in listOf("Distinct", "Ranged")) {
                // Reverse baseline/current order each round to balance JIT/GC and ordering effects.
                val versions = if (round % 2 == 0) listOf("old", "new") else listOf("new", "old")
                for (version in versions) {
                    val passage: Passage = when (kind to version) {
                        "Distinct" to "old" -> BaselineDistinctPassage(kjv)
                        "Distinct" to "new" -> DistinctPassage(kjv)
                        "Ranged" to "old" -> BaselineRangedPassage(kjv)
                        else -> RangedPassage(kjv)
                    }
                    val add = measureTime { for (o in first..last) passage.add(kjv.decodeOrdinal(o)) }
                    val remove = measureTime { for (o in first..last step 2) passage.remove(kjv.decodeOrdinal(o)) }
                    val count = passage.countVerses()
                    assertEquals((last - first + 1) / 2, count, "$kind $version round=$round")
                    println("K3 timing sample: round=$round kind=$kind version=$version addMs=${add.inWholeNanoseconds / 1e6} removeMs=${remove.inWholeNanoseconds / 1e6} count=$count")
                    if (round >= 0) {
                        samples.getOrPut("$kind $version add") { mutableListOf() }.add(add.inWholeNanoseconds / 1e6)
                        samples.getOrPut("$kind $version remove") { mutableListOf() }.add(remove.inWholeNanoseconds / 1e6)
                    }
                }
            }
        }
        for ((phase, values) in samples) {
            val sorted = values.sorted()
            val median = (sorted[3] + sorted[4]) / 2
            println("K3 timing summary: phase=$phase medianMs=$median minMs=${sorted.first()} maxMs=${sorted.last()} n=${sorted.size}")
        }
    }
}
