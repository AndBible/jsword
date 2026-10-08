package org.crosswire.ksword.parity

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * common.diff: current vs baseline (K0 pilot). Inputs cover: empty and identical texts, insertion/replacement in prose,
 * trailing newlines, RTL with combining marks, surrogate pairs, a pattern longer than the Bitap word, a multi-line pair
 * over 250 chars (so `checkLines=true` really runs LineMap line mode and the re-diff loop in Diff.compute), and a
 * fuzzy `applyTo` (middle of the text altered, also shifted by a prefix) so Patch.apply reaches Match/Bitap inexact search and Diff.xIndex.
 *
 * Identical pairs are left out of the round trip: their patch text is empty, and `PatchEntry.fromText("")`
 * throws `IllegalStateException("No match found")` on both sides (Matcher.group without a match). Both
 * sides throwing is parity, but the probe cannot return an exception, so the cases are omitted rather than caught.
 */
class DiffParityTest {
    private val lineSource = (1..13).joinToString("") { "line number %02d of text\n".format(it) }
    private val lineTarget = (1..13).joinToString("") {
        if (it in listOf(1, 5, 9)) "LINE changed %02d of text!\n".format(it) else "line number %02d of text\n".format(it)
    }

    private val pairs = listOf(
        lineSource to lineTarget,                                                           // source+target >= 250: line mode
        "" to "",
        "" to "In the beginning",
        "In the beginning God created" to "In the beginning God created",
        "In the beginning God created the heaven" to "In the beginning God made the heavens",
        "line one\nline two\nline three\n" to "line one\nline 2\nline three\n\n\n",   // trailing newlines
        "בְּרֵאשִׁית בָּרָא אֱלֹהִים" to "בְּרֵאשִׁית בָּרָא אֱלֹהִים אֵת",                    // RTL, combining marks
        "emoji 😀 and 𠀀 rare CJK" to "emoji 😃 and 𠀁 rare CJK!",                          // surrogate pairs
        "a" to "ab",                                                                         // insert into 1-char text: empty-pattern lastIndexOf
        "a".repeat(40) + "XYZ" + "b".repeat(40) to "a".repeat(40) + "XQZ" + "b".repeat(41), // > Bitap word length
    )

    @Test
    fun compareMatchesBaseline() {
        for ((source, target) in pairs) {
            for (checkLines in listOf("false", "true")) {
                assertEquals(
                    Oracle.call("diff.compare", source, target, checkLines),
                    Current.call("diff.compare", source, target, checkLines),
                    "source=$source target=$target checkLines=$checkLines",
                )
            }
        }
    }

    @Test
    fun patchTextMatchesBaseline() {
        for ((source, target) in pairs) {
            assertEquals(Oracle.call("patch.make", source, target), Current.call("patch.make", source, target), "source=$source")
        }
    }

    @Test
    fun patchRoundTripMatchesBaseline() {
        for ((source, target) in pairs.filter { it.first != it.second }) {
            val fuzzy = if (source.length > 10) source.replaceRange(source.length / 2, source.length / 2 + 2, "##") else source
            for (applyTo in listOf(source, source + " (edited)", "", fuzzy, "ZZZZZZZ" + fuzzy)) {
                assertEquals(
                    Oracle.call("patch.roundTrip", source, target, applyTo),
                    Current.call("patch.roundTrip", source, target, applyTo),
                    "source=$source applyTo=$applyTo",
                )
            }
        }
    }
}
