package org.crosswire.jsword.book.sword

import java.text.DecimalFormat

/** Preserve the legacy default FORMAT locale, including its decimal digits. */
internal fun backendZeroPad(number: Int, width: Int): String =
    DecimalFormat("0".repeat(width)).format(number.toLong())
