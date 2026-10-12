package org.crosswire.jsword.book.sword

import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookException
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.regex.Pattern

/** Characterizes the JVM baseline before the common-ready replacements. */
class ConfigEntryTypeCommonReadyTest {
    @Test fun datesMatchTheJdkPatternsIncludingNullFailures() {
        val patterns = listOf(
            ConfigEntryType.SWORD_VERSION_DATE to "\\d{4}-\\d{2}-\\d{2}",
            ConfigEntryType.COPYRIGHT_DATE to "\\d{4}(\\s*-\\s*\\d{4})?(\\s*,\\s*\\d{4}(\\s*-\\s*\\d{4})?)*"
        )
        val values = listOf("", "2026", "2026-10-12", "0000-99-99", "2020 - 2026, 1999",
            "2020\t-\r\n2026", "2020 -2026", "٢٠٢٦", "2026\n", "x2026")
        for ((entry, pattern) in patterns) {
            for (value in values) assertEquals(value, Pattern.matches(pattern, value), entry.isAllowed(value))
            try {
                entry.isAllowed(null)
                fail("null date must throw")
            } catch (_: NullPointerException) { }
        }
    }

    @Test fun numericAndBooleanConversionsKeepNullableJvmBehavior() {
        val numeric = listOf(ConfigEntryType.BLOCK_COUNT, ConfigEntryType.DISPLAY_LEVEL, ConfigEntryType.INSTALL_SIZE)
        assertTrue("numeric characterization must exercise an entry", numeric.isNotEmpty())
        for (entry in numeric) {
            for (value in listOf(null, "", " 1", "2147483648", "-2147483649")) {
                assertFalse(entry.isAllowed(value))
                assertEquals(if (entry == ConfigEntryType.BLOCK_COUNT) entry.getDefault() else null, entry.convert(value))
            }
            for (value in listOf("+1", "-2147483648", "2147483647", "١٢")) {
                assertTrue(entry.isAllowed(value))
                assertEquals(Integer.valueOf(value), entry.convert(value))
            }
        }
        for (entry in listOf(ConfigEntryType.CASE_SENSITIVE_KEYS, ConfigEntryType.OSIS_Q_TO_TICK)) {
            for (value in listOf(null, "TRUE", "true", "False", " true", "1")) {
                assertEquals(java.lang.Boolean.valueOf(value), entry.convert(value))
            }
        }
    }

    @Test fun enumSerializationPreservesIdentityAndNames() {
        for (category in BookCategory.values()) {
            val bytes = ByteArrayOutputStream().also { output ->
                ObjectOutputStream(output).use { it.writeObject(category) }
            }.toByteArray()
            val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
            assertSame(category, restored)
            assertEquals(category, BookCategory.fromString(category.getName()))
            assertEquals(category, BookCategory.fromExternalString(category.toString()))
        }
    }

    @Test fun bookExceptionPreservesMessageAndCause() {
        val cause = IllegalArgumentException("cause")
        val error = BookException("message", cause)
        assertEquals("message", error.message)
        assertSame(cause, error.cause)
    }
}
