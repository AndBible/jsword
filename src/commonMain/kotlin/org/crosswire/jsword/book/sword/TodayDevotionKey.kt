package org.crosswire.jsword.book.sword

/**
 * The key a daily devotion shows for today: today's date in the platform's default date style.
 *
 * This cannot be `DevotionDateFormat.format(month, day)` of today's local month and day: that formats a
 * date of the current year, so for 29 February in a non-leap year it would roll over to 1 March, and common code has no
 * local-time calendar to take (month, day) from. The platform formats the current instant itself.
 */
internal expect fun todayDevotionKey(): String
