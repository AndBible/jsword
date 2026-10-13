package org.crosswire.common.util

import java.io.File
import org.junit.Test
import kotlin.test.*

class TemporarySuffixCharacterizationTest {
    @Test fun longDotNonDotAndMixedSuffixUseFixedPreSuffixOverflowTwice() {
        assertEquals("17", System.getProperty("java.specification.version"))
        for ((prefix, suffix) in listOf("abc" to "." + "s".repeat(300), "abc" to "s".repeat(300),
            "x".repeat(200) to "." + "s".repeat(300))) {
            val file = File.createTempFile(prefix, suffix)
            try { checkName(file.name, prefix, suffix) } finally { file.delete() }
        }
    }

    private fun checkName(name: String, prefix: String, suffix: String) {
        val retainedPrefix = name.takeWhile { it == prefix.first() }.let { if (prefix == "abc") 3 else it.length }
        val digits = name.substring(retainedPrefix).takeWhile { it in '0'..'9' }
        assertNotNull(digits.toULongOrNull())
        fun shorten(length: Int, excess: Int, minimum: Int) = minOf(length, maxOf(minimum, length - excess))
        var p = prefix.length
        var r = digits.length
        var s = suffix.length
        val initial = p + r + s - 255
        if (initial > 0) p = shorten(p, initial, 3)
        val suffixExcess = p + r + s - 255
        if (suffixExcess > 0) {
            s = shorten(s, suffixExcess, if (suffix.startsWith('.')) 4 else 0)
            s = shorten(s, suffixExcess, 3)
        }
        val randomExcess = p + r + s - 255
        if (randomExcess > 0 && randomExcess <= r - 5) r = shorten(r, randomExcess, 5)
        assertEquals(prefix.take(p) + digits.take(r) + suffix.take(s), name)
    }
}
