@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.jsword.book.sword

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSDate

/**
 * Today's month and day in the current calendar, formatted the way key titles are ([DevotionDateFormat] uses
 * the year 2000), so the result can equal a devotion key title.
 */
internal actual fun todayDevotionKey(): String {
    val parts = NSCalendar.currentCalendar.components(NSCalendarUnitMonth or NSCalendarUnitDay, NSDate())
    return DevotionDateFormat.format(parts.month.toInt(), parts.day.toInt())
}
