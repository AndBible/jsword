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
 * Orthodox is based on the LXX versification system.
 * q.v. for more information on the method of its compilation.
 * However, this versification system differs from the LXX system
 * in that the book order follows that seen in modern Orthodox Bibles
 * and books seen in some editions of the LXX but absent from modern
 * Orthodox Bibles have been omitted.
 *  
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
/**
 * Build the "Orthodox" Versification.
 */
open class SystemOrthodox internal constructor() : Versification(V11N_NAME, BOOKS_OT, BOOKS_NT, LAST_VERSE_OT, LAST_VERSE_NT) {
    companion object {
        const val V11N_NAME = "Orthodox"

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
            BibleBook.ESD1,
            BibleBook.EZRA,
            BibleBook.NEH,
            BibleBook.TOB,
            BibleBook.JDT,
            BibleBook.ESTH,
            BibleBook.MACC1,
            BibleBook.MACC2,
            BibleBook.MACC3,
            BibleBook.PS,
            BibleBook.PR_MAN,
            BibleBook.JOB,
            BibleBook.PROV,
            BibleBook.ECCL,
            BibleBook.SONG,
            BibleBook.WIS,
            BibleBook.SIR,
            BibleBook.HOS,
            BibleBook.AMOS,
            BibleBook.MIC,
            BibleBook.JOEL,
            BibleBook.OBAD,
            BibleBook.JONAH,
            BibleBook.NAH,
            BibleBook.HAB,
            BibleBook.ZEPH,
            BibleBook.HAG,
            BibleBook.ZECH,
            BibleBook.MAL,
            BibleBook.ISA,
            BibleBook.JER,
            BibleBook.BAR,
            BibleBook.LAM,
    	BibleBook.EP_JER,
            BibleBook.EZEK,
    	BibleBook.SUS,
            BibleBook.DAN,
            BibleBook.BEL,
            BibleBook.MACC4,
        )

        internal val BOOKS_NT: Array<BibleBook> = SystemDefault.BOOKS_NT

        /** Constant for the max verse number in each chapter */
        internal val LAST_VERSE_OT: Array<IntArray> = arrayOf(
            // Genesis
            intArrayOf(
               31,  25,  25,  26,  32,  23,  24,  22,  29,  32,
               32,  20,  18,  24,  21,  16,  27,  33,  39,  18,
               34,  24,  20,  67,  34,  35,  46,  22,  35,  43,
               55,  33,  20,  31,  29,  44,  36,  30,  23,  23,
               57,  39,  34,  34,  28,  34,  31,  22,  33,  26,
            ),
            // Exodus
            intArrayOf(
               22,  25,  22,  31,  23,  30,  29,  32,  35,  29,
               10,  51,  22,  31,  27,  36,  16,  27,  25,  26,
               37,  31,  33,  18,  40,  37,  21,  43,  46,  38,
               18,  35,  23,  35,  35,  40,  21,  29,  23,  38,
            ),
            // Leviticus
            intArrayOf(
               17,  16,  17,  35,  26,  40,  38,  36,  24,  20,
               47,   8,  59,  57,  33,  34,  16,  30,  37,  27,
               24,  33,  44,  23,  55,  46,  34,
            ),
            // Numbers
            intArrayOf(
               54,  34,  51,  49,  31,  27,  89,  26,  23,  36,
               35,  16,  34,  45,  41,  50,  28,  32,  22,  29,
               35,  41,  30,  25,  18,  65,  23,  31,  40,  17,
               54,  42,  56,  29,  34,  13,
            ),
            // Deuteronomy
            intArrayOf(
               46,  37,  29,  49,  33,  25,  26,  20,  29,  22,
               32,  32,  19,  29,  23,  22,  20,  22,  21,  20,
               23,  30,  26,  24,  19,  19,  27,  69,  29,  20,
               30,  52,  29,  12,
            ),
            // Joshua
            intArrayOf(
               18,  24,  17,  24,  16,  27,  26,  35,  33,  43,
               23,  24,  33,  15,  64,  10,  18,  28,  54,   9,
               49,  34,  16,  36,
            ),
            // Judges
            intArrayOf(
               36,  23,  31,  24,  32,  40,  25,  35,  57,  18,
               40,  15,  25,  20,  20,  31,  13,  32,  30,  48,
               25,
            ),
            // Ruth
            intArrayOf(
               22,  23,  18,  22,
            ),
            // I Samuel
            intArrayOf(
               28,  36,  21,  22,  12,  21,  17,  22,  27,  27,
               15,  25,  23,  52,  35,  23,  58,  30,  24,  43,
               16,  23,  29,  23,  44,  25,  12,  25,  11,  32,
               13,
            ),
            // II Samuel
            intArrayOf(
               27,  32,  39,  12,  26,  23,  29,  18,  13,  19,
               27,  31,  39,  33,  37,  23,  29,  33,  44,  26,
               22,  51,  41,  25,
            ),
            // I Kings
            intArrayOf(
               53,  71,  39,  34,  32,  38,  51,  66,  28,  33,
               44,  54,  34,  31,  34,  42,  24,  46,  21,  43,
               43,  54,
            ),
            // II Kings
            intArrayOf(
               22,  25,  27,  44,  27,  35,  20,  29,  37,  36,
               21,  22,  25,  29,  38,  20,  41,  37,  37,  21,
               26,  20,  37,  20,  30,
            ),
            // I Chronicles
            intArrayOf(
               54,  55,  24,  43,  41,  81,  40,  40,  44,  14,
               47,  41,  14,  17,  29,  43,  27,  17,  19,   8,
               30,  19,  32,  31,  31,  32,  34,  21,  30,
            ),
            // II Chronicles
            intArrayOf(
               18,  18,  17,  23,  14,  42,  22,  18,  31,  19,
               23,  16,  23,  15,  19,  14,  19,  34,  11,  37,
               20,  12,  21,  27,  28,  23,   9,  27,  36,  27,
               21,  33,  25,  33,  31,  31,
            ),
            // I Esdras
            intArrayOf(
               58,  30,  24,  63,  73,  34,  15,  96,  55,
            ),
            // Ezra
            intArrayOf(
               11,  70,  13,  24,  17,  22,  28,  36,  15,  44,
            ),
            // Nehemiah
            intArrayOf(
               11,  20,  37,  23,  19,  19,  73,  18,  38,  40,
               36,  47,  31,
            ),
            // Tobit
            intArrayOf(
               22,  14,  17,  21,  23,  19,  18,  21,   6,  14,
               19,  22,  19,  15,
            ),
            // Judith
            intArrayOf(
               16,  28,  10,  15,  24,  21,  32,  36,  14,  23,
               23,  20,  20,  19,  14,  25,
            ),
            // Esther
            intArrayOf(
               22,  23,  15,  17,  22,  14,  10,  17,  35,  13,
               17,   7,  30,  19,  24,  24,
            ),
            // I Maccabees
            intArrayOf(
               64,  70,  60,  61,  68,  63,  50,  32,  73,  89,
               74,  53,  54,  49,  41,  24,
            ),
            // II Maccabees
            intArrayOf(
               36,  32,  40,  50,  27,  31,  42,  36,  29,  38,
               38,  46,  26,  46,  39,
            ),
            // III Maccabees
            intArrayOf(
               29,  33,  30,  21,  51,  41,  23,
            ),
            // Psalms
            intArrayOf(
                6,  13,   9,   9,  13,  11,  18,  10,  40,   8,
                9,   6,   7,   6,  11,  15,  51,  15,  10,  14,
               32,   6,  10,  22,  12,  14,   9,  11,  13,  25,
               11,  22,  23,  28,  13,  40,  23,  14,  18,  14,
               12,   6,  27,  18,  12,  10,  15,  21,  23,  21,
               11,   7,   9,  24,  14,  12,  12,  19,  14,   9,
               13,  12,  11,  14,  20,   8,  36,  37,   7,  24,
               20,  28,  23,  11,  13,  21,  72,  13,  20,  17,
                8,  19,  13,  14,  17,   7,  19,  53,  17,  16,
               16,   5,  23,  11,  13,  12,   9,   9,   5,   8,
               29,  22,  36,  45,  48,  43,  14,  31,   7,  10,
               10,   9,  26,  18,  19,   2,  29, 176,   7,   8,
                9,   4,   8,   5,   7,   5,   6,   8,   8,   3,
               18,   3,   3,  21,  26,   9,   8,  24,  15,  10,
                8,  12,  15,  22,  10,  11,  20,  14,   9,   6,
                7,
            ),
            // Prayer of Manasses
            intArrayOf(
               15,
            ),
            // Job
            intArrayOf(
               22,  18,  26,  21,  27,  30,  22,  22,  35,  22,
               20,  25,  28,  22,  35,  23,  16,  21,  29,  29,
               34,  30,  17,  25,   6,  14,  23,  28,  25,  31,
               40,  22,  33,  37,  16,  34,  24,  41,  35,  32,
               34,  22,
            ),
            // Proverbs
            intArrayOf(
               35,  23,  38,  28,  23,  40,  28,  37,  25,  33,
               31,  31,  27,  36,  38,  33,  30,  24,  29,  30,
               31,  31,  36,  77,  31,  29,  29,  30,  49,  35,
               31,
            ),
            // Ecclesiastes
            intArrayOf(
               18,  26,  22,  17,  20,  12,  30,  17,  18,  20,
               10,  14,
            ),
            // Song of Solomon
            intArrayOf(
               17,  17,  11,  16,  17,  13,  14,  15,
            ),
            // Wisdom
            intArrayOf(
               16,  25,  19,  20,  24,  27,  30,  21,  19,  21,
               27,  27,  19,  31,  19,  29,  21,  25,  22,
            ),
            // Sirach
            intArrayOf(
               30,  18,  31,  31,  15,  37,  36,  19,  18,  31,
               34,  18,  26,  27,  20,  30,  32,  33,  31,  32,
               28,  27,  28,  34,  26,  29,  30,  26,  28,  40,
               31,  26,  33,  31,  26,  31,  31,  35,  35,  30,
               27,  27,  33,  24,  26,  20,  25,  25,  16,  29,
               30,
            ),
            // Hosea
            intArrayOf(
               11,  25,   5,  19,  15,  12,  16,  14,  17,  15,
               12,  15,  16,  10,
            ),
            // Amos
            intArrayOf(
               15,  16,  15,  13,  27,  15,  17,  14,  15,
            ),
            // Micah
            intArrayOf(
               16,  13,  12,  14,  15,  16,  20,
            ),
            // Joel
            intArrayOf(
               20,  32,  21,  21,
            ),
            // Obadiah
            intArrayOf(
               21,
            ),
            // Jonah
            intArrayOf(
               17,  11,  10,  11,
            ),
            // Nahum
            intArrayOf(
               15,  14,  19,
            ),
            // Habakkuk
            intArrayOf(
               17,  20,  19,
            ),
            // Zephaniah
            intArrayOf(
               18,  15,  21,
            ),
            // Haggai
            intArrayOf(
               15,  24,
            ),
            // Zechariah
            intArrayOf(
               21,  17,  11,  14,  11,  15,  14,  23,  17,  12,
               17,  14,   9,  21,
            ),
            // Malachi
            intArrayOf(
               14,  17,  24,   6,
            ),
            // Isaiah
            intArrayOf(
               31,  22,  26,   6,  30,  13,  25,  23,  21,  34,
               16,   6,  22,  32,   9,  14,  14,   7,  25,   6,
               17,  25,  18,  23,  12,  21,  13,  29,  24,  33,
                9,  20,  24,  17,  10,  22,  38,  22,   8,  31,
               29,  25,  28,  28,  26,  13,  15,  22,  26,  11,
               23,  15,  12,  17,  13,  12,  21,  14,  21,  22,
               11,  12,  20,  12,  25,  24,
            ),
            // Jeremiah
            intArrayOf(
               19,  37,  25,  31,  31,  30,  34,  23,  26,  25,
               23,  17,  27,  22,  21,  21,  27,  23,  15,  18,
               14,  30,  42,  10,  39,  28,  46,  64,  31,  33,
               47,  44,  24,  22,  19,  32,  24,  40,  44,  26,
               22,  22,  32,  30,  28,  28,  16,  44,  38,  46,
               63,  34,
            ),
            // Baruch
            intArrayOf(
               22,  35,  38,  37,  9,
            ),
            // Lamentations
            intArrayOf(
               22,  22,  66,  22,  22,
            ),
            // Epistle of Jeremiah
            intArrayOf(
               73,
            ),
            // Ezekiel
            intArrayOf(
               28,  13,  27,  17,  17,  14,  27,  18,  11,  22,
               25,  28,  23,  23,   8,  63,  24,  32,  14,  49,
               37,  31,  49,  27,  17,  21,  36,  26,  21,  26,
               18,  32,  33,  31,  15,  38,  28,  23,  29,  49,
               26,  20,  27,  31,  25,  24,  23,  35,
            ),
            // Susanna
            intArrayOf(
               64,
            ),
            // Daniel
            intArrayOf(
               21,  49, 100,  37,  31,  29,  28,  27,  27,  21,
               45,  13,
            ),
            // Bel and the Dragon
            intArrayOf(
               42,
            ),
            // IV Maccabees
            intArrayOf(
               35,  24,  21,  26,  38,  35,  25,  29,  32,  21,
               27,  20,  27,  20,  32,  25,  24,  24,
            ),
        )

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
               52,  25,  36,  54,  47,  71,  53,  59,  41,  42,
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
               36,  21,  14,  26,  33,  27,
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
               19,  18,  18,  20,   9,  21,  18,  24,  21,  15,
               27,  21
            ),
        )

        /**
         * Serialization ID
         */
        private const val serialVersionUID = 3365882007485277007L
    }
}
