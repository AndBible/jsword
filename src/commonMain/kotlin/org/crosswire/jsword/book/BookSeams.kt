package org.crosswire.jsword.book

import org.crosswire.common.activate.Activatable

/** K4 forward declarations: common code only passes facades around until K5b.
 * Activatable is added to Book when it moves in Task 16.
 */
expect interface Book : Activatable, Comparable<Book>

expect interface BookDriver {
    fun getDriverName(): String
}
