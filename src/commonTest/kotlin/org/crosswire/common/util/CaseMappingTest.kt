package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CaseMappingTest {
    @Test
    fun turkishDottedI() {
        assertEquals("isaiah", "Isaiah".lowercase(KLocale("en")))
        assertEquals("ısaiah", "Isaiah".lowercase(KLocale("tr")))
        assertEquals("İSAİAH", "isaiah".uppercase(KLocale("tr")))
        assertEquals("ISAIAH", "isaiah".uppercase(KLocale("en")))
    }

    @Test
    fun greekFinalSigmaAndTonos() {
        // JDK 17 values: final sigma on lower-casing, tonos kept on upper-casing (U+038E).
        assertEquals("\u03BF\u03B4\u03BF\u03C2", "ΟΔΟΣ".lowercase(KLocale("el")))
        assertEquals("\u0399\u0397\u03A3\u039F\u038E\u03A3", "Ιησούς".uppercase(KLocale("el")))
    }

    @Test
    fun germanSharpS() {
        assertEquals("STRASSE", "Straße".uppercase(KLocale("de")))
    }
}
