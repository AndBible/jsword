package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.regex.Pattern

class MetadataVersionCharacterizationTest {
    @Test
    fun dottedVersionRetainsWildcardSeparatorsAndMissingParts() {
        val reference = Pattern.compile("^(\\d+)(?:.(\\d+))?(?:.(\\d+))?(?:.(\\d+))?$")
        val inputs = listOf("1", "1.2", "1x2", "1/2/3/4", "01.002", "1\n2", "", "1.2.3.4.5", "2147483648")
        for (input in inputs) {
            val matcher = reference.matcher(input)
            val expected = runCatching {
                require(matcher.matches())
                (1..matcher.groupCount()).map { matcher.group(it)?.toInt() ?: -1 }
            }
            val actual = runCatching { Version(input) }
            assertEquals(expected.isSuccess, actual.isSuccess, input)
            if (expected.isSuccess) {
                assertEquals(input, actual.getOrThrow().toString())
                val canonical = expected.getOrThrow().takeWhile { it >= 0 }.joinToString(".")
                assertEquals(Version(canonical), actual.getOrThrow(), input)
            }
        }
    }
}
