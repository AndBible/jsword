package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals

class KLocaleTest {
    @Test fun normalisesCase() { assertEquals("fi_FI", KLocale("FI", "fi").toString()) }
    @Test fun legacyCodesMapToCurrent() {
        assertEquals("he", KLocale("iw").language); assertEquals("id", KLocale("in").language); assertEquals("yi", KLocale("ji").language)
    }
    @Test fun toStringLikeJava() {
        assertEquals("", KLocale.ROOT.toString()); assertEquals("en", KLocale("en").toString())
        assertEquals("en_US", KLocale("en", "US").toString()); assertEquals("en__POSIX", KLocale("en", "", "POSIX").toString())
        assertEquals("_US", KLocale("", "US").toString())
    }
    @Test fun equality() { assertEquals(KLocale("pt", "BR"), KLocale("PT", "br")) }
}
