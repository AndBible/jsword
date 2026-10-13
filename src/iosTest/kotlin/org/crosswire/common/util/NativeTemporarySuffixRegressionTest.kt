package org.crosswire.common.util

import platform.posix.unlink
import kotlin.test.*

class NativeTemporarySuffixRegressionTest {
    @Test fun longDotNonDotAndMixedSuffixRetainJdkFixedOverflowSemantics() {
        for ((prefix, suffix) in listOf("abc" to "." + "s".repeat(300), "abc" to "s".repeat(300),
            "x".repeat(200) to "." + "s".repeat(300))) {
            val uri = netTemporaryUri(prefix, suffix)
            try {
                val name = NativeLexicalFiles.name(uri.getPath()!!)
                val retainedPrefix = if (prefix == "abc") 3 else name.takeWhile { it == 'x' }.length
                val digits = name.substring(retainedPrefix).takeWhile { it in '0'..'9' }
                assertNotNull(digits.toULongOrNull())
                fun shorten(length: Int, excess: Int, minimum: Int) = minOf(length, maxOf(minimum, length - excess))
                var p = prefix.length
                val r = digits.length
                var s = suffix.length
                val initial = p + r + s - 255
                if (initial > 0) p = shorten(p, initial, 3)
                val suffixExcess = p + r + s - 255
                if (suffixExcess > 0) {
                    s = shorten(s, suffixExcess, if (suffix.startsWith('.')) 4 else 0)
                    s = shorten(s, suffixExcess, 3)
                }
                assertEquals(prefix.take(p) + digits + suffix.take(s), name)
            } finally { unlink(uri.getPath()!!) }
        }
    }
}
