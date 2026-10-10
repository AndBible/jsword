package org.crosswire.jsword.passage

import org.crosswire.jsword.versification.FileVersificationMapping
import org.crosswire.jsword.versification.QualifiedKey
import org.crosswire.jsword.versification.VersificationToKJVMapper
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Literal real-path JVM capture in task-13-numeric-prechange.xml, before the helper. */
class ReferenceNumericParityTest {
    private val kjv = Versifications.instance().getVersification("KJV")!!
    private fun mappedOffset(text: String): String {
        val mapping = FileVersificationMapping().apply { addProperty("Gen.1.1-Gen.1.2", "+$text") }
        val mapper = VersificationToKJVMapper(kjv, mapping)
        return mapper.map(QualifiedKey(OsisParser().parseOsisRef(kjv, "Gen.1.1"))).joinToString { it.toString() }
    }

    @Test fun acceptedSignsLeadingZerosAndBmpDigits() {
        for (text in listOf("1", "+1", "01", "١", "１")) {
            assertEquals("Gen.1.1", OsisParser().parseOsisID(kjv, "Gen.$text.1")!!.getOsisRef())
            assertEquals("Gen.1.1", OsisParser().parseOsisID(kjv, "Gen.1.$text")!!.getOsisRef())
            assertEquals("Gen.1", OsisParser().parseOsisRef(kjv, "Gen.1.1-Gen.$text")!!.getOsisRef())
            assertEquals("Gen.1.2", mappedOffset(text))
        }
        assertEquals("Gen.0.1", OsisParser().parseOsisID(kjv, "Gen.-0.1")!!.getOsisRef())
        assertEquals("Gen.1.0", OsisParser().parseOsisID(kjv, "Gen.1.-0")!!.getOsisRef())
        assertEquals("Gen.1.1", mappedOffset("-0"))
    }

    @Test fun rawNumericErrorsKeepJdkMessages() {
        for (text in listOf("+", "-", " 1", "1 ", "x", "2147483648", "-2147483649", "𝟙")) {
            val expected = "For input string: \"$text\""
            assertEquals(expected, assertFailsWith<NumberFormatException> {
                OsisParser().parseOsisID(kjv, "Gen.$text.1")
            }.message)
            assertEquals(expected, assertFailsWith<NumberFormatException> {
                OsisParser().parseOsisID(kjv, "Gen.1.$text")
            }.message)
            // A '-' in an OSIS range is a delimiter, not a numeric sign.
            if (!text.startsWith('-')) {
                assertEquals(expected, assertFailsWith<NumberFormatException> {
                    OsisParser().parseOsisRef(kjv, "Gen.1.1-Gen.$text")
                }.message)
            }
            assertEquals(expected, assertFailsWith<NumberFormatException> { mappedOffset(text) }.message)
        }
        assertEquals("For input string: \"\"", assertFailsWith<NumberFormatException> { mappedOffset("") }.message)
        // The strict OSIS parser rejects empty parts before integer conversion.
        assertEquals(null, OsisParser().parseOsisID(kjv, "Gen..1"))
        assertEquals(null, OsisParser().parseOsisID(kjv, "Gen.1."))
    }
}
