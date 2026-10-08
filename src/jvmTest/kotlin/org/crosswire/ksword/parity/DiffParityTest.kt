package org.crosswire.ksword.parity

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * common.diff: current vs baseline (K0 pilot). Inputs cover the branches a Bible-text diff meets.
 *
 * Identical pairs are left out of the round trip: their patch text is empty, and `PatchEntry.fromText("")`
 * throws `IllegalStateException("No match found")` on both sides (Matcher.group without a match). Both
 * sides throwing is parity, but the probe cannot return an exception, so the cases are omitted rather than caught.
 */
class DiffParityTest {
    private val pairs = listOf(
        "" to "",
        "" to "In the beginning",
        "In the beginning God created" to "In the beginning God created",
        "In the beginning God created the heaven" to "In the beginning God made the heavens",
        "line one\nline two\nline three\n" to "line one\nline 2\nline three\n\n\n",   // trailing newlines
        "בְּרֵאשִׁית בָּרָא אֱלֹהִים" to "בְּרֵאשִׁית בָּרָא אֱלֹהִים אֵת",                    // RTL, combining marks
        "emoji 😀 and 𠀀 rare CJK" to "emoji 😃 and 𠀁 rare CJK!",                          // surrogate pairs
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
            for (applyTo in listOf(source, source + " (edited)", "")) {
                assertEquals(
                    Oracle.call("patch.roundTrip", source, target, applyTo),
                    Current.call("patch.roundTrip", source, target, applyTo),
                    "source=$source applyTo=$applyTo",
                )
            }
        }
    }
}
