/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2012 - 2016
 *
 */
package org.crosswire.jsword.versification.system

import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.Versification


/**
 *
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
/**
 * Build the "KJVA" Versification.
 */
open class SystemKJVA internal constructor() : Versification(V11N_NAME, BOOKS_OT, BOOKS_NT, LAST_VERSE_OT, LAST_VERSE_NT) {
    companion object {
        const val V11N_NAME = "KJVA"

        private fun <T> concat(array1: Array<T>, array2: Array<T>): Array<T> {
            val result = array1.copyOf(array1.size + array2.size)
            array2.copyInto(result, array1.size)
            @Suppress("UNCHECKED_CAST")
            return result as Array<T>
        }

        // All KJV old testament books followed by all deuterocanonical books.
        internal val BOOKS_OT: Array<BibleBook> = concat(SystemKJV.BOOKS_OT, arrayOf(
            BibleBook.ESD1,
            BibleBook.ESD2,
            BibleBook.TOB,
            BibleBook.JDT,
            BibleBook.ADD_ESTH,
            BibleBook.WIS,
            BibleBook.SIR,
            BibleBook.BAR,
            BibleBook.PR_AZAR,
            BibleBook.SUS,
            BibleBook.BEL,
            BibleBook.PR_MAN,
            BibleBook.MACC1,
            BibleBook.MACC2,
        ))

        internal val BOOKS_NT: Array<BibleBook> = SystemDefault.BOOKS_NT

        // Same verse numbering as KJV for regular OT, followed by additional verse numberings for deuterocanonical books.
        internal val LAST_VERSE_OT: Array<IntArray> = concat(SystemKJV.LAST_VERSE_OT, arrayOf(
            // I Esdras
            intArrayOf(
                58,  30,  24,  63,  73,  34,  15,  96,  55,
            ),
            // II Esdras
            intArrayOf(
                40,  48,  36,  52,  56,  59,  70,  63,  47,  59,
                46,  51,  58,  48,  63,  78,
            ),
            // Tobit
            intArrayOf(
                22,  14,  17,  21,  22,  17,  18,  21,   6,  12,
                19,  22,  18,  15,
            ),
            // Judith
            intArrayOf(
                16,  28,  10,  15,  24,  21,  32,  36,  14,  23,
                23,  20,  20,  19,  13,  25,
            ),
            // Additions to Esther
            intArrayOf(
                1,   1,   1,   1,   1,   1,   1,   1,   1,  13,
                12,   6,  18,  19,  16,  24,
            ),
            // Wisdom
            intArrayOf(
                16,  24,  19,  20,  23,  25,  30,  21,  18,  21,
                26,  27,  19,  31,  19,  29,  21,  25,  22,
            ),
            // Sirach
            intArrayOf(
                30,  18,  31,  31,  15,  37,  36,  19,  18,  31,
                34,  18,  26,  27,  20,  30,  32,  33,  30,  32,
                28,  27,  28,  34,  26,  29,  30,  26,  28,  25,
                31,  24,  31,  26,  20,  26,  31,  34,  35,  30,
                24,  25,  33,  22,  26,  20,  25,  25,  16,  29,
                30,
            ),
            // Baruch
            intArrayOf(
                22,  35,  37,  37,   9,  73,
            ),
            // Prayer of Azariah
            intArrayOf(
                68,
            ),
            // Susanna
            intArrayOf(
                64,
            ),
            // Bel and the Dragon
            intArrayOf(
                42,
            ),
            // Prayer of Manasses
            intArrayOf(
                1,
            ),
            // I Maccabees
            intArrayOf(
                64,  70,  60,  61,  68,  63,  50,  32,  73,  89,
                74,  53,  53,  49,  41,  24,
            ),
            // II Maccabees
            intArrayOf(
                36,  32,  40,  50,  27,  31,  42,  36,  29,  38,
                38,  45,  26,  46,  39,
            ),
        ))

        internal val LAST_VERSE_NT: Array<IntArray> = SystemKJV.LAST_VERSE_NT
    }
}
