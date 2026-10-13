@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.jsword.book.sword

import platform.Foundation.*

actual object DevotionDateFormat {
    actual fun format(month: Int, day: Int): String {
        val components = NSDateComponents().apply {
            year = 2000
            this.month = month.toLong()
            this.day = day.toLong()
        }
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            lenient = true
        }
        return formatter.stringFromDate(NSCalendar.currentCalendar.dateFromComponents(components)!!)
    }

    actual fun parseMonthDay(text: String): Pair<Int, Int>? {
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            lenient = true
        }
        val date = formatter.dateFromString(text) ?: return null
        val parts = NSCalendar.currentCalendar.components(NSCalendarUnitMonth or NSCalendarUnitDay, date)
        return parts.month.toInt() to parts.day.toInt()
    }
}
