package org.crosswire.jsword.book.sword

import org.crosswire.common.icu.DateFormatter
import java.util.Date

/** The original `SwordDailyDevotion` body: default-locale date instance formatting the current instant. */
internal actual fun todayDevotionKey(): String = DateFormatter.getDateInstance().format(Date())!!
