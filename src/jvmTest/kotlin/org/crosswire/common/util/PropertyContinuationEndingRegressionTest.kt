package org.crosswire.common.util

import java.util.Properties
import org.junit.Test
import kotlin.test.assertEquals

class PropertyContinuationEndingRegressionTest {
    private fun check(text: String) {
        assertEquals("17", System.getProperty("java.specification.version"))
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        val reference = Properties().apply { load(bytes.inputStream()) }
        assertEquals(reference.entries.associate { it.key as String to it.value as String },
            LegacyPropertyText.entries(bytes).toMap(), text.replace("\r", "<CR>").replace("\n", "<LF>"))
    }

    @Test fun emptyBackslashCrLfAtEofMatchesIndependentLineReader() { check("\\\r\n") }
    @Test fun emptyBackslashCrAtEofMatchesIndependentLineReader() { check("\\\r") }
    @Test fun emptyBackslashLfAtEofMatchesIndependentLineReader() { check("\\\n") }
    @Test fun nonemptyBackslashCrLfAtEofMatchesIndependentLineReader() { check("key=value\\\r\n") }
    @Test fun nonemptyBackslashCrAtEofMatchesIndependentLineReader() { check("key=value\\\r") }
    @Test fun nonemptyBackslashLfAtEofMatchesIndependentLineReader() { check("key=value\\\n") }
}
