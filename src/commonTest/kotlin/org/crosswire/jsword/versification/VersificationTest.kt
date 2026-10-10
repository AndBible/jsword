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

import org.crosswire.jsword.versification.*
import org.crosswire.jsword.versification.system.*
import kotlin.test.*
import org.crosswire.common.util.KLocale
import org.crosswire.common.util.MissingResourceException
import okio.IOException


import org.crosswire.jsword.book.CaseType
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.NoSuchVerseException
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.versification.system.SystemCalvin
import org.crosswire.jsword.versification.system.SystemCatholic
import org.crosswire.jsword.versification.system.SystemCatholic2
import org.crosswire.jsword.versification.system.SystemDarbyFR
import org.crosswire.jsword.versification.system.SystemGerman
import org.crosswire.jsword.versification.system.SystemKJV
import org.crosswire.jsword.versification.system.SystemKJVA
import org.crosswire.jsword.versification.system.SystemLeningrad
import org.crosswire.jsword.versification.system.SystemLuther
import org.crosswire.jsword.versification.system.SystemMT
import org.crosswire.jsword.versification.system.SystemNRSV
import org.crosswire.jsword.versification.system.SystemNRSVA
import org.crosswire.jsword.versification.system.SystemSegond
import org.crosswire.jsword.versification.system.SystemSynodal
import org.crosswire.jsword.versification.system.SystemSynodalProt
import org.crosswire.jsword.versification.system.SystemVulg
import org.crosswire.jsword.versification.system.Versifications

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class VersificationTest {
    private lateinit var v11n: Versification

    private fun forEachVersification(block: () -> Unit) {
        for (name in listOf(SystemCalvin.V11N_NAME, SystemCatholic.V11N_NAME, SystemCatholic2.V11N_NAME, SystemDarbyFR.V11N_NAME, SystemGerman.V11N_NAME, SystemKJV.V11N_NAME, SystemKJVA.V11N_NAME, SystemLeningrad.V11N_NAME, SystemLuther.V11N_NAME, SystemMT.V11N_NAME, SystemNRSV.V11N_NAME, SystemNRSVA.V11N_NAME, SystemSegond.V11N_NAME, SystemSynodal.V11N_NAME, SystemSynodalProt.V11N_NAME, SystemVulg.V11N_NAME)) {
            v11n = Versifications.instance().getVersification(name)!!
            block()
        }
    }
    private var storedCase: CaseType? = null

    @BeforeTest
    fun setUp() {
        storedCase = BookName.getDefaultCase()
    }

    @AfterTest
    fun tearDown() {
        BookName.setCase(storedCase)
    }

    @Test
    fun testIn(){
        forEachVersification {
            // Counts using loops
            var verseCount: Int = 0

            // For all the books
            for (b in generateSequence(v11n.firstBook) { v11n.getNextBook(it) }.asIterable()) {
                // Continue the verse counts for the whole Bible
                for (c in 0..v11n.getLastChapter(b)) {
                    verseCount += v11n.getLastVerse(b, c) + 1
                }
            }

            assertEquals(verseCount, v11n.maximumOrdinal() + 1)
            assertEquals(verseCount, v11n.getCount(null))
        }
    }

    @Test
    fun testOrdinal(){
        forEachVersification {
            var ordinal: Int = 0
            for (b in generateSequence(v11n.firstBook) { v11n.getNextBook(it) }.asIterable()) {
                var cib: Int = v11n.getLastChapter(b)
                for (c in 0..cib) {
                    var vic: Int = v11n.getLastVerse(b, c)
                    for (v in 0..vic) {
                        var verse: Verse = Verse(v11n, b, c, v)
                        assertEquals(ordinal++, v11n.getOrdinal(verse), verse.getOsisID())
                    }
                }
            }
        }
    }

    @Test
    fun testDecodeOrdinal(){
        forEachVersification {
            var ordinal: Int = 0
            for (b in generateSequence(v11n.firstBook) { v11n.getNextBook(it) }.asIterable()) {
                var cib: Int = v11n.getLastChapter(b)
                for (c in 0..cib) {
                    var vic: Int = v11n.getLastVerse(b, c)
                    for (v in 0..vic) {
                        var verse: Verse = Verse(v11n, b, c, v)
                        assertEquals(verse, v11n.decodeOrdinal(ordinal++), verse.getOsisID())
                    }
                }
            }
        }
    }

    @Test
    fun testValidate(){
        forEachVersification {
            for (b in generateSequence(v11n.firstBook) { v11n.getNextBook(it) }.asIterable()) {
                // Test that negative chapters are invalid
                try {
                    v11n.validate(b, -1, 0)
                    fail()
                } catch (ex: NoSuchVerseException) {
                    // This is allowed
                }

                for (c in 0..v11n.getLastChapter(b)) {
                    // Test that negative verse are invalid
                    try {
                        v11n.validate(b, c, -1)
                        fail()
                    } catch (ex: NoSuchVerseException) {
                        // This is allowed
                    }

                    // test that every verse, including introductions are valid
                    for (v in 0..v11n.getLastVerse(b, c)) {
                        v11n.validate(b, c, v)
                    }

                    // test that every verses past the end are invalid
                    try {
                        v11n.validate(b, c, v11n.getLastVerse(b, c) + 1)
                        fail()
                    } catch (ex: NoSuchVerseException) {
                        // This is allowed
                    }
                }

                // test that every chapters past the end are invalid
                try {
                    v11n.validate(b, v11n.getLastChapter(b) + 1, 0)
                    fail()
                } catch (ex: NoSuchVerseException) {
                    // This is allowed
                }
            }
        }
    }

    @Test
    fun testPatch(){
        forEachVersification {

            var all: Int = 0
            // For all the books
            for (b in generateSequence(v11n.firstBook) { v11n.getNextBook(it) }.asIterable()) {
                var cib: Int = v11n.getLastChapter(b)
                for (c in 0..cib) {
                    var vic: Int = v11n.getLastVerse(b, c)
                    for (v in 0..vic) {
                        var pv: Verse = v11n.patch(BibleBook.INTRO_BIBLE, 0, all)
                        assertEquals(b, pv.book, pv.getName())
                        assertEquals(c, pv.chapter, pv.getName())
                        assertEquals(v, pv.verse, pv.getName())
                        all++
                    }
                }
            }

            var gen11: Verse = Verse(v11n, BibleBook.GEN, 1, 1)
            assertEquals(gen11, v11n.patch(BibleBook.GEN, 1, 1))
            assertEquals(gen11, v11n.patch(BibleBook.GEN, 0, 2))
            assertEquals(gen11, v11n.patch(null, 3, 1))
            assertEquals(gen11, v11n.patch(null, 0, 4))
        }
    }

    @Test
    fun testVerseCount(){
        forEachVersification {
            var countUp: Int = 0
            var firstVerse: Verse = Verse(v11n, v11n.firstBook, 0, 0)
            var lastBook: BibleBook = v11n.lastBook
            var lastChapter: Int = v11n.getLastChapter(lastBook)
            var lastVerse: Verse = Verse(v11n, lastBook, lastChapter, v11n.getLastVerse(lastBook, lastChapter))
            for (b in generateSequence(v11n.firstBook) { v11n.getNextBook(it) }.asIterable()) {
                var cib: Int = v11n.getLastChapter(b)
                for (c in 0..cib) {
                    var vic: Int = v11n.getLastVerse(b, c)
                    for (v in 0..vic) {
                        var curVerse: Verse = Verse(v11n, b, c, v)
                        var up: Int = v11n.distance(firstVerse, curVerse) + 1
                        assertEquals(++countUp, up)
                        //                    assertEquals(verseCountSlow(gen00, curVerse), up)
                    }
                }

            }
            var countDown: Int = v11n.maximumOrdinal()
            assertEquals(v11n.getOrdinal(lastVerse), countDown)
            for (b in generateSequence(v11n.firstBook) { v11n.getNextBook(it) }.asIterable()) {
                var cib: Int = v11n.getLastChapter(b)
                for (c in 0..cib) {
                    var vic: Int = v11n.getLastVerse(b, c)
                    for (v in 0..vic) {
                        var curVerse: Verse = Verse(v11n, b, c, v)
                        var down: Int = v11n.distance(curVerse, lastVerse)
                        assertEquals(countDown--, down)
                    }
                }

            }
        }
    }

    @Test
    fun testNames() {
        forEachVersification {
            // AV11N(DMS): Is this right?
            assertEquals(2, BibleBook.GEN.ordinal)
            assertEquals(68, BibleBook.REV.ordinal)
        }
    }

    @Test
    fun testMTSystem() {
        forEachVersification {
            // The MT v11n is OT only and had a problem where this would have failed.
            // At this time all versifications have an OT and Gen 1:1
            var v11nMT: Versification = Versifications.instance().getVersification(SystemMT.V11N_NAME)!!
            var verse: Verse = Verse(v11n, BibleBook.GEN, 1, 1)
            var index: Int = verse.ordinal
            var testament: Testament = v11nMT.getTestament(index)
            assertEquals(Testament.OLD, testament, "Gen 1:1 is old testament")
            assertEquals(index, v11n.getTestamentOrdinal(index), "ordinals in OT do not change")
        }
    }

    @Test
    fun testGetBook() {
        forEachVersification {
            if (v11n.containsBook(BibleBook.GEN)) {
                assertEquals(BibleBook.GEN, v11n.getBook("Genesis"))
                assertEquals(BibleBook.GEN, v11n.getBook("Gene"))
                assertEquals(BibleBook.GEN, v11n.getBook("Gen"))
                assertEquals(BibleBook.GEN, v11n.getBook("GE"))
                assertEquals(BibleBook.GEN, v11n.getBook("ge"))
                assertEquals(BibleBook.GEN, v11n.getBook("GEN"))
                assertEquals(BibleBook.GEN, v11n.getBook("genesis"))
            }

            if (v11n.containsBook(BibleBook.PS)) {
                assertEquals(BibleBook.PS, v11n.getBook("psa"))
                assertEquals(BibleBook.PS, v11n.getBook("ps"))
                assertEquals(BibleBook.PS, v11n.getBook("pss"))
                assertEquals(BibleBook.PS, v11n.getBook("psalter"))
            }

            if (v11n.containsBook(BibleBook.ECCL)) {
                assertEquals(BibleBook.ECCL, v11n.getBook("ecc"))
                assertEquals(BibleBook.ECCL, v11n.getBook("Qohelot"))
            }

            if (v11n.containsBook(BibleBook.SONG)) {
                assertEquals(BibleBook.SONG, v11n.getBook("son"))
                assertEquals(BibleBook.SONG, v11n.getBook("song"))
                assertEquals(BibleBook.SONG, v11n.getBook("song of solomon"))
                assertEquals(BibleBook.SONG, v11n.getBook("songofsolomon"))
                assertEquals(BibleBook.SONG, v11n.getBook("ss"))
                assertEquals(BibleBook.SONG, v11n.getBook("canticle"))
                assertEquals(BibleBook.SONG, v11n.getBook("can"))
            }

            if (v11n.containsBook(BibleBook.PHIL)) {
                assertEquals(BibleBook.PHIL, v11n.getBook("phi"))
                assertEquals(BibleBook.PHIL, v11n.getBook("phil"))
                assertEquals(BibleBook.PHIL, v11n.getBook("phili"))
            }

            if (v11n.containsBook(BibleBook.PHLM)) {
                assertEquals(BibleBook.PHLM, v11n.getBook("phile"))
            }

            if (v11n.containsBook(BibleBook.REV)) {
                assertEquals(BibleBook.REV, v11n.getBook("revelations"))
                assertEquals(BibleBook.REV, v11n.getBook("rev"))
            }

            assertEquals(null, v11n.getBook("1"))
        }
    }

    @Test
    fun testGetLongBookName(){
        forEachVersification {
            BookName.setCase(CaseType.SENTENCE)
            if (v11n.containsBook(BibleBook.GEN)) {
                assertEquals("Genesis", v11n.getLongName(BibleBook.GEN))
            }
            if (v11n.containsBook(BibleBook.REV)) {
                assertEquals("Revelation of John", v11n.getLongName(BibleBook.REV))
            }

            BookName.setCase(CaseType.LOWER)
            if (v11n.containsBook(BibleBook.GEN)) {
                assertEquals("genesis", v11n.getLongName(BibleBook.GEN))
            }
            if (v11n.containsBook(BibleBook.REV)) {
                assertEquals("revelation of john", v11n.getLongName(BibleBook.REV))
            }

            BookName.setCase(CaseType.UPPER)
            if (v11n.containsBook(BibleBook.GEN)) {
                assertEquals("GENESIS", v11n.getLongName(BibleBook.GEN))
            }
            if (v11n.containsBook(BibleBook.REV)) {
                assertEquals("REVELATION OF JOHN", v11n.getLongName(BibleBook.REV))
            }
        }
    }

    @Test
    fun testGetShortBookName(){
        forEachVersification {
            BookName.setCase(CaseType.SENTENCE)
            if (v11n.containsBook(BibleBook.GEN)) {
                assertEquals("Gen", v11n.getShortName(BibleBook.GEN))
            }
            if (v11n.containsBook(BibleBook.EXOD)) {
                assertEquals("Exo", v11n.getShortName(BibleBook.EXOD))
            }
            if (v11n.containsBook(BibleBook.JUDG)) {
                assertEquals("Judg", v11n.getShortName(BibleBook.JUDG))
            }
            if (v11n.containsBook(BibleBook.MAL)) {
                assertEquals("Mal", v11n.getShortName(BibleBook.MAL))
            }
            if (v11n.containsBook(BibleBook.MATT)) {
                assertEquals("Mat", v11n.getShortName(BibleBook.MATT))
            }
            if (v11n.containsBook(BibleBook.PHIL)) {
                assertEquals("Phili", v11n.getShortName(BibleBook.PHIL))
            }
            if (v11n.containsBook(BibleBook.PHLM)) {
                assertEquals("Phile", v11n.getShortName(BibleBook.PHLM))
            }
            if (v11n.containsBook(BibleBook.JUDE)) {
                assertEquals("Jude", v11n.getShortName(BibleBook.JUDE))
            }
            if (v11n.containsBook(BibleBook.REV)) {
                assertEquals("Rev", v11n.getShortName(BibleBook.REV))
            }

            BookName.setCase(CaseType.LOWER)
            if (v11n.containsBook(BibleBook.GEN)) {
                assertEquals("gen", v11n.getShortName(BibleBook.GEN))
            }
            if (v11n.containsBook(BibleBook.EXOD)) {
                assertEquals("exo", v11n.getShortName(BibleBook.EXOD))
            }
            if (v11n.containsBook(BibleBook.JUDG)) {
                assertEquals("judg", v11n.getShortName(BibleBook.JUDG))
            }
            if (v11n.containsBook(BibleBook.MAL)) {
                assertEquals("mal", v11n.getShortName(BibleBook.MAL))
            }
            if (v11n.containsBook(BibleBook.MATT)) {
                assertEquals("mat", v11n.getShortName(BibleBook.MATT))
            }
            if (v11n.containsBook(BibleBook.PHIL)) {
                assertEquals("phili", v11n.getShortName(BibleBook.PHIL))
            }
            if (v11n.containsBook(BibleBook.PHLM)) {
                assertEquals("phile", v11n.getShortName(BibleBook.PHLM))
            }
            if (v11n.containsBook(BibleBook.JUDE)) {
                assertEquals("jude", v11n.getShortName(BibleBook.JUDE))
            }
            if (v11n.containsBook(BibleBook.REV)) {
                assertEquals("rev", v11n.getShortName(BibleBook.REV))
            }

            BookName.setCase(CaseType.UPPER)
            if (v11n.containsBook(BibleBook.GEN)) {
                assertEquals("GEN", v11n.getShortName(BibleBook.GEN))
            }
            if (v11n.containsBook(BibleBook.EXOD)) {
                assertEquals("EXO", v11n.getShortName(BibleBook.EXOD))
            }
            if (v11n.containsBook(BibleBook.JUDG)) {
                assertEquals("JUDG", v11n.getShortName(BibleBook.JUDG))
            }
            if (v11n.containsBook(BibleBook.MAL)) {
                assertEquals("MAL", v11n.getShortName(BibleBook.MAL))
            }
            if (v11n.containsBook(BibleBook.MATT)) {
                assertEquals("MAT", v11n.getShortName(BibleBook.MATT))
            }
            if (v11n.containsBook(BibleBook.PHIL)) {
                assertEquals("PHILI", v11n.getShortName(BibleBook.PHIL))
            }
            if (v11n.containsBook(BibleBook.PHLM)) {
                assertEquals("PHILE", v11n.getShortName(BibleBook.PHLM))
            }
            if (v11n.containsBook(BibleBook.JUDE)) {
                assertEquals("JUDE", v11n.getShortName(BibleBook.JUDE))
            }
            if (v11n.containsBook(BibleBook.REV)) {
                assertEquals("REV", v11n.getShortName(BibleBook.REV))
            }
        }
    }

    @Test
    fun testVerseListSortOrder() {
        forEachVersification {
            if (v11n.containsBook(BibleBook.GEN) && v11n.containsBook(BibleBook.REV)) {
                var keyList: MutableList<Key> = mutableListOf<Key>()
                var gen11: Verse = Verse(v11n, BibleBook.GEN, 1, 1)
                var rev11: Verse = Verse(v11n, BibleBook.REV, 1, 1)

                keyList.add(gen11)
                keyList.add(rev11)
                keyList.sort()
                assertEquals(gen11, keyList[0], "Genesis should be at start when sorted")
                assertEquals(rev11, keyList[1], "Revelation should be at end when sorted")
            }
        }
    }

    @Test
    fun testPassageListSortOrder() {
        forEachVersification {
            try {
                if (v11n.containsBook(BibleBook.GEN) && v11n.containsBook(BibleBook.REV)) {
                    var keyList: MutableList<Key> = mutableListOf<Key>()
                    var gen11: Key = PassageKeyFactory.instance().getKey(v11n, "Gen.1.1")
                    var rev11: Key = PassageKeyFactory.instance().getKey(v11n, "Rev.1.1")

                    keyList.add(gen11)
                    keyList.add(rev11)
                    keyList.sort()
                    assertEquals(gen11, keyList[0], "Genesis should be at start when sorted")
                    assertEquals(rev11, keyList[1], "Revelation should be at end when sorted")
                }
            } catch (e: NoSuchKeyException) {
                fail("Exception in testPassageListSort:" + e.message)
            }
        }
    }
}
