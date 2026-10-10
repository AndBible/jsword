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
 * The SystemDefault versification (v11n) is that of the Protestant KJV.
 * This is the first v11n defined within JSword and SWORD.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class SystemDefault : Versification() {
    companion object {
        const val V11N_NAME = ""

        // Default Books for other Testament in single Testament Bibles
        internal val BOOKS_NONE: Array<BibleBook> = arrayOf(
        )

        // Default offsets for other Testament in single Testament Bibles
        internal val LAST_VERSE_NONE: Array<IntArray> = arrayOf(
        )

        // Default NT list is the most common
        internal val BOOKS_NT: Array<BibleBook> = arrayOf(
            BibleBook.MATT,
            BibleBook.MARK,
            BibleBook.LUKE,
            BibleBook.JOHN,
            BibleBook.ACTS,
            BibleBook.ROM,
            BibleBook.COR1,
            BibleBook.COR2,
            BibleBook.GAL,
            BibleBook.EPH,
            BibleBook.PHIL,
            BibleBook.COL,
            BibleBook.THESS1,
            BibleBook.THESS2,
            BibleBook.TIM1,
            BibleBook.TIM2,
            BibleBook.TITUS,
            BibleBook.PHLM,
            BibleBook.HEB,
            BibleBook.JAS,
            BibleBook.PET1,
            BibleBook.PET2,
            BibleBook.JOHN1,
            BibleBook.JOHN2,
            BibleBook.JOHN3,
            BibleBook.JUDE,
            BibleBook.REV,
        )

        internal val BOOKS_OT: Array<BibleBook> = arrayOf(
            BibleBook.GEN,
            BibleBook.EXOD,
            BibleBook.LEV,
            BibleBook.NUM,
            BibleBook.DEUT,
            BibleBook.JOSH,
            BibleBook.JUDG,
            BibleBook.RUTH,
            BibleBook.SAM1,
            BibleBook.SAM2,
            BibleBook.KGS1,
            BibleBook.KGS2,
            BibleBook.CHR1,
            BibleBook.CHR2,
            BibleBook.EZRA,
            BibleBook.NEH,
            BibleBook.ESTH,
            BibleBook.JOB,
            BibleBook.PS,
            BibleBook.PROV,
            BibleBook.ECCL,
            BibleBook.SONG,
            BibleBook.ISA,
            BibleBook.JER,
            BibleBook.LAM,
            BibleBook.EZEK,
            BibleBook.DAN,
            BibleBook.HOS,
            BibleBook.JOEL,
            BibleBook.AMOS,
            BibleBook.OBAD,
            BibleBook.JONAH,
            BibleBook.MIC,
            BibleBook.NAH,
            BibleBook.HAB,
            BibleBook.ZEPH,
            BibleBook.HAG,
            BibleBook.ZECH,
            BibleBook.MAL,
        )
    }
}
