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
 * The NRSV Versification is nearly the same as the KJV versification.
 * It differs in that 3 John has 15 verses not 14 and Revelation 12
 * has 18 verses not 17.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
/**
 * Build the "NRSV" Versification.
 */
open class SystemNRSV internal constructor() : Versification(V11N_NAME, BOOKS_OT, BOOKS_NT, LAST_VERSE_OT, LAST_VERSE_NT) {
    companion object {
        const val V11N_NAME = "NRSV"

        internal val BOOKS_OT: Array<BibleBook> = SystemDefault.BOOKS_OT

        internal val BOOKS_NT: Array<BibleBook> = SystemDefault.BOOKS_NT

        internal val LAST_VERSE_OT: Array<IntArray> = SystemKJV.LAST_VERSE_OT

        internal val LAST_VERSE_NT: Array<IntArray> = arrayOf(
            // Matthew
            intArrayOf(
               25,  23,  17,  25,  48,  34,  29,  34,  38,  42,
               30,  50,  58,  36,  39,  28,  27,  35,  30,  34,
               46,  46,  39,  51,  46,  75,  66,  20,
            ),
            // Mark
            intArrayOf(
               45,  28,  35,  41,  43,  56,  37,  38,  50,  52,
               33,  44,  37,  72,  47,  20,
            ),
            // Luke
            intArrayOf(
               80,  52,  38,  44,  39,  49,  50,  56,  62,  42,
               54,  59,  35,  35,  32,  31,  37,  43,  48,  47,
               38,  71,  56,  53,
            ),
            // John
            intArrayOf(
               51,  25,  36,  54,  47,  71,  53,  59,  41,  42,
               57,  50,  38,  31,  27,  33,  26,  40,  42,  31,
               25,
            ),
            // Acts
            intArrayOf(
               26,  47,  26,  37,  42,  15,  60,  40,  43,  48,
               30,  25,  52,  28,  41,  40,  34,  28,  41,  38,
               40,  30,  35,  27,  27,  32,  44,  31,
            ),
            // Romans
            intArrayOf(
               32,  29,  31,  25,  21,  23,  25,  39,  33,  21,
               36,  21,  14,  23,  33,  27,
            ),
            // I Corinthians
            intArrayOf(
               31,  16,  23,  21,  13,  20,  40,  13,  27,  33,
               34,  31,  13,  40,  58,  24,
            ),
            // II Corinthians
            intArrayOf(
               24,  17,  18,  18,  21,  18,  16,  24,  15,  18,
               33,  21,  14,
            ),
            // Galatians
            intArrayOf(
               24,  21,  29,  31,  26,  18,
            ),
            // Ephesians
            intArrayOf(
               23,  22,  21,  32,  33,  24,
            ),
            // Philippians
            intArrayOf(
               30,  30,  21,  23,
            ),
            // Colossians
            intArrayOf(
               29,  23,  25,  18,
            ),
            // I Thessalonians
            intArrayOf(
               10,  20,  13,  18,  28,
            ),
            // II Thessalonians
            intArrayOf(
               12,  17,  18,
            ),
            // I Timothy
            intArrayOf(
               20,  15,  16,  16,  25,  21,
            ),
            // II Timothy
            intArrayOf(
               18,  26,  17,  22,
            ),
            // Titus
            intArrayOf(
               16,  15,  15,
            ),
            // Philemon
            intArrayOf(
               25,
            ),
            // Hebrews
            intArrayOf(
               14,  18,  19,  16,  14,  20,  28,  13,  28,  39,
               40,  29,  25,
            ),
            // James
            intArrayOf(
               27,  26,  18,  17,  20,
            ),
            // I Peter
            intArrayOf(
               25,  25,  22,  19,  14,
            ),
            // II Peter
            intArrayOf(
               21,  22,  18,
            ),
            // I John
            intArrayOf(
               10,  29,  24,  21,  21,
            ),
            // II John
            intArrayOf(
               13,
            ),
            // III John
            intArrayOf(
               15,
            ),
            // Jude
            intArrayOf(
               25,
            ),
            // Revelation of John
            intArrayOf(
               20,  29,  22,  11,  14,  17,  17,  13,  21,  11,
               19,  18,  18,  20,   8,  21,  18,  24,  21,  15,
               27,  21,
            ),
        )
    }
}
