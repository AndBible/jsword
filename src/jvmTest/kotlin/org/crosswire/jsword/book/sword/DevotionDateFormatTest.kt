package org.crosswire.jsword.book.sword

import org.crosswire.common.icu.DateFormatter
import org.crosswire.jsword.book.sword.state.RawLDBackendState
import org.junit.Test
import java.lang.reflect.InvocationTargetException
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DevotionDateFormatTest {
    private val backend = RawLDBackend<RawLDBackendState>(SwordBookMetaData(
        "[D]\nDataPath=./modules/x/\nModDrv=RawLD\nCategory=Daily Devotional\nLang=en\n".toByteArray(), "d"
    ), 2)

    @Test fun everyLeapYearDayMatchesCurrentYearNormalizedReference() {
        val previous = Locale.getDefault()
        try {
            for (locale in listOf(Locale.US, Locale.GERMANY, Locale.FRANCE)) {
                Locale.setDefault(locale)
                val days = intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
                for (month in 1..12) for (day in 1..days[month - 1]) {
                    val text = format(month, day)
                    assertEquals(referenceFormat(month, day), text, "$locale $month.$day")
                    assertEquals(referenceParse(text), parse(text), "$locale $text")
                    assertEquals(referenceFormat(month, day), DevotionDateFormat.format(month, day))
                    assertEquals(referenceParse(text), DevotionDateFormat.parseMonthDay(text))
                }
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun malformedInputPreservesAssertionFailure() {
        assertFailsWith<AssertionError> { referenceParse("not a date") }
        assertFailsWith<AssertionError> { parse("not a date") }
        assertFailsWith<AssertionError> { DevotionDateFormat.parseMonthDay("not a date") }
    }

    private fun format(month: Int, day: Int): String = invoke("internal2external", "$month.$day") as String
    private fun parse(text: String): Pair<Int, Int> {
        val internal = invoke("external2internal", text, "") as String
        val parts = internal.split('.')
        return parts[0].toInt() to parts[1].toInt()
    }

    private fun invoke(name: String, vararg args: String): Any? {
        val method = RawLDBackend::class.java.getDeclaredMethod(name, *Array(args.size) { String::class.java })
        method.isAccessible = true
        try {
            return method.invoke(backend, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun referenceFormat(month: Int, day: Int): String {
        val greg = GregorianCalendar()
        val formatter = DateFormatter.getDateInstance()
        greg.set(Calendar.MONTH, month - 1)
        greg.set(Calendar.DATE, day)
        return formatter.format(greg.time)!!
    }

    private fun referenceParse(text: String): Pair<Int, Int> {
        val greg = GregorianCalendar()
        val formatter = DateFormatter.getDateInstance()
        formatter.setLenient(true)
        greg.time = formatter.parse(text)
        return 1 + greg.get(Calendar.MONTH) to greg.get(Calendar.DATE)
    }
}
