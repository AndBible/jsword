package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ArrayIndexOutOfBoundsExceptionTest {
    @Test fun indexConstructorRetainsJvmMessage() {
        for (index in listOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE)) {
            val failure = ArrayIndexOutOfBoundsException(index)
            assertIs<IndexOutOfBoundsException>(failure)
            assertEquals("Array index out of range: $index", failure.message)
        }
    }

    @Test fun messageConstructorDoesNotFormatItsArgument() {
        assertEquals("custom", ArrayIndexOutOfBoundsException("custom").message)
        assertNull(ArrayIndexOutOfBoundsException(null).message)
    }
}
