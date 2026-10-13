package org.crosswire.common.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.defaultTimeZone

internal actual fun legacyPropertyEntries(bytes: ByteArray): List<Pair<String, String>> = LegacyPropertyText.entries(bytes)

internal actual fun legacyPropertyBytes(map: PropertyMap, comments: String?): ByteArray {
    val formatter = NSDateFormatter().apply {
        locale = NSLocale("en_US_POSIX")
        timeZone = NSTimeZone.defaultTimeZone
        dateFormat = "EEE MMM dd HH:mm:ss zzz yyyy"
    }
    return LegacyPropertyText.bytes(map, comments, formatter.stringFromDate(NSDate()))
}
