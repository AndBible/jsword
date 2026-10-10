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
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.versification

import org.crosswire.common.util.KLocale

/**
 * A BibleBook is a book of the Bible. It may or may not be canonical.
 * Note that the ordering of these books varies from one Versification to another.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
enum class BibleBook(
    /**
     * Get the OSIS representation of this BibleBook.
     *
     * @return the OSIS name
     */
    @get:JvmName("getOSIS") val osis: String,
    /**
     * Indicates that the book consists of a single chapter.
     *
     * @return true to indicate a 1-chapter book only
     */
    val isShortBook: Boolean = false
) {
    // JSword introduction to the Bible
    INTRO_BIBLE("Intro.Bible"),
    // JSword introduction to the Old Testament
    INTRO_OT("Intro.OT"),
    // Old Testament
    GEN("Gen"),
    EXOD("Exod"),
    LEV("Lev"),
    NUM("Num"),
    DEUT("Deut"),
    JOSH("Josh"),
    JUDG("Judg"),
    RUTH("Ruth"),
    SAM1("1Sam"),
    SAM2("2Sam"),
    KGS1("1Kgs"),
    KGS2("2Kgs"),
    CHR1("1Chr"),
    CHR2("2Chr"),
    EZRA("Ezra"),
    NEH("Neh"),
    ESTH("Esth"),
    JOB("Job"),
    PS("Ps"),
    PROV("Prov"),
    ECCL("Eccl"),
    SONG("Song"),
    ISA("Isa"),
    JER("Jer"),
    LAM("Lam"),
    EZEK("Ezek"),
    DAN("Dan"),
    HOS("Hos"),
    JOEL("Joel"),
    AMOS("Amos"),
    OBAD("Obad", true),
    JONAH("Jonah"),
    MIC("Mic"),
    NAH("Nah"),
    HAB("Hab"),
    ZEPH("Zeph"),
    HAG("Hag"),
    ZECH("Zech"),
    MAL("Mal"),
    // JSword introduction to the New Testament
    INTRO_NT("Intro.NT"),
    // New Testament
    MATT("Matt"),
    MARK("Mark"),
    LUKE("Luke"),
    JOHN("John"),
    ACTS("Acts"),
    ROM("Rom"),
    COR1("1Cor"),
    COR2("2Cor"),
    GAL("Gal"),
    EPH("Eph"),
    PHIL("Phil"),
    COL("Col"),
    THESS1("1Thess"),
    THESS2("2Thess"),
    TIM1("1Tim"),
    TIM2("2Tim"),
    TITUS("Titus"),
    PHLM("Phlm", true),
    HEB("Heb"),
    JAS("Jas"),
    PET1("1Pet"),
    PET2("2Pet"),
    JOHN1("1John"),
    JOHN2("2John", true),
    JOHN3("3John", true),
    JUDE("Jude", true),
    REV("Rev"),
    // Apocrypha
    TOB("Tob"),
    JDT("Jdt"),
    ADD_ESTH("AddEsth"),
    WIS("Wis"),
    SIR("Sir"),
    BAR("Bar"),
    EP_JER("EpJer", true),
    PR_AZAR("PrAzar"),
    SUS("Sus"),
    BEL("Bel"),
    MACC1("1Macc"),
    MACC2("2Macc"),
    MACC3("3Macc"),
    MACC4("4Macc"),
    PR_MAN("PrMan", true),
    ESD1("1Esd"),
    ESD2("2Esd"),
    PSS151("Ps151"),
    // Rahlfs' LXX
    ODES("Odes"),
    PSALM_SOL("PssSol"),
    // Vulgate & other later Latin mss
    EP_LAO("EpLao"),
    ESD3("3Esd"),
    ESD4("4Esd"),
    ESD5("5Esd"),
    // Ethiopian Orthodox Canon/Ge'ez Translation
    EN1("1En"),
    JUBS("Jub"),
    BAR4("4Bar"),
    ASCEN_ISA("AscenIsa"),
    PS_JOS("PsJos"),
    // Coptic Orthodox Canon
    APOSTOLIC("AposCon"),
    CLEM1("1Clem"),
    CLEM2("2Clem"),
    // Armenian Orthodox Canon
    COR3("3Cor"),
    EP_COR_PAUL("EpCorPaul"),
    JOS_ASEN("JosAsen"),
    T12PATR("T12Patr"),
    T12PATR_TASH("T12Patr.TAsh"),
    T12PATR_TBENJ("T12Patr.TBenj"),
    T12PATR_TDAN("T12Patr.TDan"),
    T12PATR_GAD("T12Patr.TGad"),
    T12PATR_TISS("T12Patr.TIss"),
    T12PATR_TJOS("T12Patr.TJos"),
    T12PATR_TJUD("T12Patr.TJud"),
    T12PATR_TLEVI("T12Patr.TLevi"),
    T12PATR_TNAPH("T12Patr.TNaph"),
    T12PATR_TREU("T12Patr.TReu"),
    T12PATR_TSIM("T12Patr.TSim"),
    T12PATR_TZeb("T12Patr.TZeb"),
    // Peshitta
    BAR2("2Bar"),
    EP_BAR("EpBar"),
    // Codex Sinaiticus
    BARN("Barn"),
    HERM("Herm"),
    HERM_MAND("Herm.Mand"),
    HERM_SIM("Herm.Sim"),
    HERM_VIS("Herm.Vis"),
    // Other books
    ADD_DAN("AddDan"),
    ADD_PS("AddPs"),
    ESTH_GR("EsthGr");

    /**
     * Get the OSIS representation of this BibleBook.
     *
     * @return the OSIS name
     */
    override fun toString(): String {
        return osis
    }

    companion object {
        /**
         * Case insensitive search for BibleBook for an OSIS name.
         *
         * @param osis
         * @return the matching BibleBook or null
         */
        @JvmStatic
        fun fromOSIS(osis: String?): BibleBook? {
            val match = BookName.normalize(osis, KLocale.ENGLISH)
            return osisMap[match]
        }

        /**
         * @param osis the osis book reference, case sensitive
         * @return the corresponding BibleBook
         */
        @JvmStatic
        fun fromExactOSIS(osis: String?): BibleBook? {
            return if (osis == null) null else exactMatches[osis]
        }

        /** A quick lookup based on OSIS name for the book  */
        private val osisMap: MutableMap<String, BibleBook> = HashMap(128)
        private val exactMatches: MutableMap<String, BibleBook> = HashMap(128)

        init {
            for (book in values()) {
                osisMap[BookName.normalize(book.osis, KLocale.ENGLISH)] = book
                exactMatches[book.osis] = book
            }
        }
    }
}
