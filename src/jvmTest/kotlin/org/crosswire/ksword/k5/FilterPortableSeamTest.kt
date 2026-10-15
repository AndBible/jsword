package org.crosswire.ksword.k5

import org.crosswire.common.xml.xmlCharacters
import org.junit.Test
import org.junit.Assert.*

class FilterPortableSeamTest {
    @Test fun callbackSlicesRetainJvmConstructorFailureContract() {
        val chars = charArrayOf('a', '\u0000', '\uD800', 'z')
        for ((offset, length) in listOf(0 to 4, 1 to 2, 4 to 0, -1 to 1, 0 to -1,
            3 to 2, Int.MAX_VALUE to 1, 1 to Int.MAX_VALUE)) {
            val expected = runCatching { String(chars, offset, length) }
            val actual = runCatching { xmlCharacters(chars, offset, length) }
            assertEquals(expected.getOrNull(), actual.getOrNull())
            assertEquals(expected.exceptionOrNull()?.javaClass, actual.exceptionOrNull()?.javaClass)
            assertEquals(expected.exceptionOrNull()?.message, actual.exceptionOrNull()?.message)
        }
    }
}
