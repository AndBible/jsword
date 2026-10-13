package org.crosswire.jsword.book.sword

import org.crosswire.common.icu.DateFormatter
import java.util.Calendar
import java.util.GregorianCalendar

/** K4 date seam; becomes expect/actual in Task 16, retaining JVM locale and current year. */
actual object DevotionDateFormat {
    actual fun format(month: Int, day: Int): String {
        val greg = GregorianCalendar()
        val nameDF = DateFormatter.getDateInstance()
        greg.set(Calendar.MONTH, month - 1)
        greg.set(Calendar.DATE, day)
        return nameDF.format(greg.time)!!
    }

    actual fun parseMonthDay(text: String): Pair<Int, Int>? {
        val greg = GregorianCalendar()
        val nameDF = DateFormatter.getDateInstance()
        nameDF.setLenient(true)
        val date = nameDF.parse(text)
        greg.setTime(date)
        return (1 + greg.get(Calendar.MONTH)) to greg.get(Calendar.DATE)
    }
}
