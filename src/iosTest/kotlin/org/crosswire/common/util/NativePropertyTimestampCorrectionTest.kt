package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertTrue

class NativePropertyTimestampCorrectionTest {
    @Test fun storedTimestampUsesLegacyDateCommentRatherThanEpochNumber() {
        val bytes = PropertyMap().store(null)
        val comment = buildString { bytes.forEach { append((it.toInt() and 255).toChar()) } }.lineSequence().first()
        assertTrue(Regex("#[A-Z][a-z]{2} [A-Z][a-z]{2} [ 0-9][0-9] [0-9]{2}:[0-9]{2}:[0-9]{2} .+ [0-9]{4}").matches(comment), comment)
    }
}
