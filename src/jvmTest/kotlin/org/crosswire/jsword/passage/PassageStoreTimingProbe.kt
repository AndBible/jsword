package org.crosswire.jsword.passage

import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

class PassageStoreTimingProbe {
    @Test
    fun addAndRemoveOverTheNewTestament() {
        val kjv = Versifications.instance().getVersification("KJV")!!
        val first = kjv.getOrdinal(Verse(kjv, BibleBook.MATT, 1, 1))
        val last = kjv.maximumOrdinal()
        for (p in listOf<Passage>(DistinctPassage(kjv), RangedPassage(kjv))) {
            val add = measureTime { for (o in first..last) p.add(kjv.decodeOrdinal(o)) }
            val remove = measureTime { for (o in first..last step 2) p.remove(kjv.decodeOrdinal(o)) }
            assertEquals((last - first + 1) / 2, p.countVerses(), p::class.simpleName)
            println("K3 timing ${p::class.simpleName}: add=$add remove=$remove")
        }
    }
}
