package org.crosswire.jsword.book.sword

/**
 * The key title a daily devotion shows for today, in the same date style as the key titles
 * ([DevotionDateFormat], which formats a fixed leap year).
 *
 * Common code has no local-time calendar to take today's (month, day) from, so each platform supplies it:
 * the JVM formats the current instant with the default date style (the old body), iOS takes the month and day
 * from the current calendar and calls [DevotionDateFormat.format]. The leap-year rollover of 29 February to
 * 1 March only matters when parsing a title back (equality of strings), which the shared leap-year test pins.
 */
internal expect fun todayDevotionKey(): String
