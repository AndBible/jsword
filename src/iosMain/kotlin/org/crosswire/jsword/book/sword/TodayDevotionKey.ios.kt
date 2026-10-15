package org.crosswire.jsword.book.sword

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle

/** Medium date style of the current instant, the same style [DevotionDateFormat] uses for the keys. */
internal actual fun todayDevotionKey(): String {
    val formatter = NSDateFormatter().apply { dateStyle = NSDateFormatterMediumStyle }
    return formatter.stringFromDate(NSDate())
}
