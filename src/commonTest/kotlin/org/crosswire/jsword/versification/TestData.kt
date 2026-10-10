package org.crosswire.jsword.versification

import org.crosswire.jsword.versification.*
import org.crosswire.jsword.versification.system.*
import kotlin.test.*
import org.crosswire.common.util.KLocale
import org.crosswire.common.util.MissingResourceException
import okio.IOException

import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.SystemCalvin
import org.crosswire.jsword.versification.system.SystemCatholic
import org.crosswire.jsword.versification.system.SystemCatholic2
import org.crosswire.jsword.versification.system.SystemDarbyFR
import org.crosswire.jsword.versification.system.SystemGerman
import org.crosswire.jsword.versification.system.SystemKJV
import org.crosswire.jsword.versification.system.SystemKJVA
import org.crosswire.jsword.versification.system.SystemLXX
import org.crosswire.jsword.versification.system.SystemLeningrad
import org.crosswire.jsword.versification.system.SystemLuther
import org.crosswire.jsword.versification.system.SystemMT
import org.crosswire.jsword.versification.system.SystemNRSV
import org.crosswire.jsword.versification.system.SystemNRSVA
import org.crosswire.jsword.versification.system.SystemOrthodox
import org.crosswire.jsword.versification.system.SystemSegond
import org.crosswire.jsword.versification.system.SystemSynodal
import org.crosswire.jsword.versification.system.SystemSynodalProt
import org.crosswire.jsword.versification.system.SystemVulg
import org.crosswire.jsword.versification.system.Versifications

object TestData {
    val CALVIN: Versification = Versifications.instance().getVersification(SystemCalvin.V11N_NAME)!!
    val CATH1: Versification = Versifications.instance().getVersification(SystemCatholic.V11N_NAME)!!
    val CATH2: Versification = Versifications.instance().getVersification(SystemCatholic2.V11N_NAME)!!
    val DARBY: Versification = Versifications.instance().getVersification(SystemDarbyFR.V11N_NAME)!!
    val ORTHO: Versification = Versifications.instance().getVersification(SystemOrthodox.V11N_NAME)!!

    val KJV: Versification = Versifications.instance().getVersification(SystemKJV.V11N_NAME)!!
    val KJVA: Versification = Versifications.instance().getVersification(SystemKJVA.V11N_NAME)!!
    val SYNODAL_PROT: Versification = Versifications.instance().getVersification(SystemSynodalProt.V11N_NAME)!!
    val NRSV: Versification = Versifications.instance().getVersification(SystemNRSV.V11N_NAME)!!
    val NRSVA: Versification = Versifications.instance().getVersification(SystemNRSVA.V11N_NAME)!!
    val LXX: Versification = Versifications.instance().getVersification(SystemLXX.V11N_NAME)!!
    val SEGOND: Versification = Versifications.instance().getVersification(SystemSegond.V11N_NAME)!!
    val MT: Versification = Versifications.instance().getVersification(SystemMT.V11N_NAME)!!
    val GERMAN: Versification = Versifications.instance().getVersification(SystemGerman.V11N_NAME)!!
    val LENINGRAD: Versification = Versifications.instance().getVersification(SystemLeningrad.V11N_NAME)!!
    val LUTHER: Versification = Versifications.instance().getVersification(SystemLuther.V11N_NAME)!!
    val SYNODAL: Versification = Versifications.instance().getVersification(SystemSynodal.V11N_NAME)!!
    val VULGATE: Versification = Versifications.instance().getVersification(SystemVulg.V11N_NAME)!!

    // these verses should be equivalent
    val KJV_PS_14_2: Verse = Verse(KJV, BibleBook.PS, 14, 2)
    val KJV_PS_14_4: Verse = Verse(KJV, BibleBook.PS, 14, 4)
    val KJV_PS_14_2_4: VerseRange = VerseRange(KJV, KJV_PS_14_2, KJV_PS_14_4)

    val SYN_PROT_PS_13_2: Verse = Verse(SYNODAL_PROT, BibleBook.PS, 13, 2)
    val SYN_PROT_PS_13_4: Verse = Verse(SYNODAL_PROT, BibleBook.PS, 13, 4)
    val SYN_PROT_PS_13_2_4: VerseRange = VerseRange(SYNODAL_PROT, SYN_PROT_PS_13_2, SYN_PROT_PS_13_4)

    val KJVA_1MACCPS_1_2: Verse = Verse(KJVA, BibleBook.MACC1, 1, 2)
    val KJVA_1MACCPS_1_3: Verse = Verse(KJVA, BibleBook.MACC1, 1, 3)
    val KJVA_1MACC_1_2_3: VerseRange = VerseRange(KJVA, KJVA_1MACCPS_1_2, KJVA_1MACCPS_1_3)
}
