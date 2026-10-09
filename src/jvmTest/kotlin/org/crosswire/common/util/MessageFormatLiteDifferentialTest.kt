package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.MessageFormat
import java.util.Locale
import kotlin.test.assertFailsWith

class MessageFormatLiteDifferentialTest {
    private val patterns = listOf("{0}", "a {0} b {1}", "{0,number,integer}", "{0,number,00}.{1,number,00}",
        "{0,choice,0#verses|1#verse|1<verses}", "{0} {1,choice,0#books|1#book|1<books}", "it''s {0}", "'{0}' {0}", "{3,number,integer}", "{5}", "no args",
        "{0,number}", "{0,number,#,##0.0}", "{0,choice,0#none|1#one {0}|2#{0} items}", "'unterminated {0}", "a '' b '{' c {0}", "{ 0 }", "{0,choice,0#a''b|1#c}",
        "{0,number,'#'#}", "{1,choice,0#x|1<y {0,number,integer}}", "{0", "{x}", "}{0}", "{0,number,integer} {0,number,integer}",
        "{0,choice,x#a|1#b}", "{0,choice,1.2.3#a}")
    private val args = listOf<Array<Any?>>(arrayOf(0, 0), arrayOf(1, 1), arrayOf(2, 2), arrayOf(1234, 1L), arrayOf(-7, 2.5), arrayOf(1000000L, 0.5),
        arrayOf("s", 3), arrayOf(null, 1), arrayOf(1, 2, 3, 4567), arrayOf(1.5, 1.0), arrayOf(0.5, -1), arrayOf(1234567.891, 2.0))

    @Test fun matchesJavaMessageFormat() {
        for (tag in listOf("en", "fi", "de", "fa", "ar", "zh_CN"))
            for (p in patterns) for (a in args) {
                val jl = Locale(tag.substringBefore('_'), tag.substringAfter('_', ""))
                val expected = runCatching { MessageFormat(p, jl).format(a) }
                val actual = runCatching { MessageFormatLite.format(p, jl.toKLocale(), a) }
                assertEquals("$tag $p ${a.toList()}", expected.getOrNull(), actual.getOrNull())
                assertEquals("$tag $p ${a.toList()} throws", expected.exceptionOrNull()?.javaClass, actual.exceptionOrNull()?.javaClass)
            }
    }

    @Test fun emptyChoiceThrowsIndexOutOfBounds() {
        // java.text throws ArrayIndexOutOfBoundsException; common code can only promise the superclass.
        assertFailsWith<IndexOutOfBoundsException> { MessageFormat("{0,choice,bad}", Locale.ENGLISH).format(arrayOf<Any?>(1)) }
        assertFailsWith<IndexOutOfBoundsException> { MessageFormatLite.format("{0,choice,bad}", KLocale.ENGLISH, arrayOf(1)) }
    }

    @Test fun unsupportedTypeThrows() {
        assertFailsWith<UnsupportedOperationException> { MessageFormatLite.format("{0,date}", KLocale.ENGLISH, arrayOf(1)) }
    }
}
