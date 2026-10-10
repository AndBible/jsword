package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissingResourceExceptionTypealiasTest {
    @Test
    fun isTheJdkClassOnTheJvm() {
        val e: Throwable = MissingResourceException("m", "C", "k")
        assertTrue(e is java.util.MissingResourceException)
        assertEquals("k", (e as java.util.MissingResourceException).key)
    }
}
