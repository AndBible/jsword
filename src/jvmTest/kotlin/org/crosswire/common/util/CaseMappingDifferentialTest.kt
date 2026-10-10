package org.crosswire.common.util

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class CaseMappingDifferentialTest {
    private val locales = listOf("en", "tr", "az", "lt", "el", "de", "fi", "nl", "sr_LT", "he", "zh_TW")
    private val words = listOf("Isaiah", "İşaya", "ırmak", "IRMAK", "Ιησούς", "ΟΔΟΣ", "Straße", "IJsselmeer",
        "Įstatymas", "Ìì", "1 Kings", "", "ǅ")

    @Test
    fun matchesJavaLocaleCaseMapping() {
        for (tag in locales) {
            val parts = tag.split('_')
            val k = KLocale(parts[0], parts.getOrElse(1) { "" })
            val j = Locale(parts[0], parts.getOrElse(1) { "" })
            for (w in words) {
                assertEquals(w.lowercase(j), w.lowercase(k), "lower $tag $w")
                assertEquals(w.uppercase(j), w.uppercase(k), "upper $tag $w")
            }
        }
    }
}
