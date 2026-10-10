package org.crosswire.jsword.passage

import org.crosswire.jsword.versification.system.Versifications
import org.junit.Test
import kotlin.test.assertEquals

/** Catches dropped CR separators, changed description bytes, and an unusable common boundary. */
class CommonBoundaryTest {
    @Test fun descriptionPreservesAllLineEndingsAndLfOutput() {
        val v11n = Versifications.instance().getVersification("KJV")!!
        for (separator in listOf("\n", "\r", "\r\n")) {
            val passage = RangedPassage(v11n)
            val read = Passage::class.java.getMethod("readDescription", String::class.java)
            read.invoke(passage, "KJV${separator}Gen 1:1${separator}Gen 1:3${separator}")
            assertEquals("Gen.1.1 Gen.1.3", passage.getOsisRef())
            val output = StringBuilder()
            Passage::class.java.getMethod("writeDescription", Appendable::class.java).invoke(passage, output)
            assertEquals("KJV\nGenesis 1:1\nGenesis 1:3\n", output.toString())
        }
    }

    @Test fun emptyDescriptionAndHeaderOnlyRetainOldSuppressionBehavior() {
        val v11n = Versifications.instance().getVersification("KJV")!!
        for (text in listOf("", "KJV", "KJV\n", "KJV\r", "KJV\r\n")) {
            val passage = RangedPassage(v11n)
            var added = 0
            passage.addPassageListener(object : PassageListener {
                override fun versesAdded(ev: PassageEvent) { added++ }
                override fun versesRemoved(ev: PassageEvent) {}
                override fun versesChanged(ev: PassageEvent) {}
            })
            passage.readDescription(text)
            passage.add(Verse(v11n, org.crosswire.jsword.versification.BibleBook.GEN, 1, 1))
            assertEquals(1, passage.countVerses())
            // The old zero-data-line early return leaves event suppression raised.
            assertEquals(0, added)
        }
    }

    @Test fun dumpAndOptimizePreserveExactFormatting() {
        val books = org.crosswire.jsword.versification.BibleBookList(arrayOf(org.crosswire.jsword.versification.BibleBook.GEN))
        val out = StringBuilder()
        org.crosswire.jsword.versification.Versification.dump(out, "sample", books, arrayOf(intArrayOf(0, 31), intArrayOf(33)))
        assertEquals("    private final int[][] sample =\n    {\n        // Gen\n        { \n                0,    31, \n        },\n        // Sentinel\n        { \n               33, \n        },\n    };\n", out.toString())
        out.clear()
        org.crosswire.jsword.versification.Versification.optimize(out, books, arrayOf(intArrayOf(0, 31)))
        assertEquals("    private final int[][] chapterStarts =\n    {\n        // Gen\n        { \n                0,     1, \n        },\n        // Sentinel\n        { \n               33, \n        },\n    };\n\n    /** The last ordinal number of the Old Testament */\n    private int otMaxOrdinal = -1;\n    /** The last ordinal number of the New Testament and the maximum ordinal number of this Reference System */\n    private int ntMaxOrdinal = 32;\n", out.toString())
    }

    @Test fun listenerReceivesOriginalSourceAndChangedBounds() {
        val v11n = Versifications.instance().getVersification("KJV")!!
        val passage = RangedPassage(v11n)
        var received: PassageEvent? = null
        passage.addPassageListener(object : PassageListener {
            override fun versesAdded(ev: PassageEvent) { received = ev }
            override fun versesRemoved(ev: PassageEvent) {}
            override fun versesChanged(ev: PassageEvent) {}
        })
        passage.add(Verse(v11n, org.crosswire.jsword.versification.BibleBook.GEN, 1, 3))
        assertEquals(passage, received!!.getSource())
        assertEquals("Gen.1.3", received!!.getLowerIndex()!!.getOsisRef())
        assertEquals(PassageEvent.EventType.ADDED, received!!.getType())
    }
}
