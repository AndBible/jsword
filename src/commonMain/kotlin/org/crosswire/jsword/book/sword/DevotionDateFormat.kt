package org.crosswire.jsword.book.sword

expect object DevotionDateFormat {
    fun format(month: Int, day: Int): String
    fun parseMonthDay(text: String): Pair<Int, Int>?
}
