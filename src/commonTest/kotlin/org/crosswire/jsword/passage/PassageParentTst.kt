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
package org.crosswire.jsword.passage

import kotlin.test.*

import org.crosswire.jsword.book.CaseType
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.BookName
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications

/**
 * This would be called TestPassage however then people might think it was a
 * separate TestCase, which it is not, needing to be inherited from to customize
 * the type of test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */

abstract class PassageParentTst(private val ptype: PassageType, private val optimize: Boolean) {
    /** Control the output of names */
    private var storedCase: CaseType? = null
    private var fullName: Boolean = false
    private lateinit var v11n: Versification

    /**
     * How we create Passages
     */
    private var keyf: PassageKeyFactory = PassageKeyFactory.instance()

    private lateinit var genC1V135r: Passage
    private lateinit var exoC2V1To10C2V1To11r: Passage
    private lateinit var genToRev: Passage
    private lateinit var grace: Passage
    private lateinit var empty: Passage
    private lateinit var temp: Passage

    private lateinit var genC1V1r: VerseRange
    private lateinit var genC1V12r: VerseRange
    private lateinit var genC1V2r: VerseRange
    private lateinit var exoC2V1r: VerseRange
    private lateinit var exoC2V12r: VerseRange
    private lateinit var exoC2V2r: VerseRange

    private lateinit var genC1V1: Verse
    private lateinit var genC1V2: Verse
    private lateinit var genC1V3: Verse
    private lateinit var genC1V5: Verse
    private lateinit var exoC2V1: Verse
    private lateinit var exoC2V2: Verse
    private lateinit var exoC2V3: Verse
    private lateinit var exoC2V11: Verse
    private lateinit var revC22V21: Verse

    /**
     * Some of these tests fail with an out of memory exception. The optimize
     * bit of these tests is wrong so we never optimize for write, and
     * performing tests aimed at writing after optimizing for reads is allowed
     * to be a bad performer (which includes using lots of memory) So the
     * problem is with the tests and not with the Passages.
     *
     * @throws Exception
     */
    @BeforeTest
    fun setUp(){
        PassageKeyFactory.setDefaultPassage(PassageType.toInteger(ptype))
        storedCase = BookName.getDefaultCase()
        BookName.setCase(CaseType.SENTENCE)
        fullName = BookName.isFullBookName()
        BookName.setFullBookName(false)
        // AV11N(DMS): Update test to test all V11Ns
        v11n = Versifications.instance().getVersification("KJV")!!
        genC1V135r = keyf.getKey(v11n, "Gen 1:1, Gen 1:3, Gen 1:5")
        exoC2V1To10C2V1To11r = keyf.getKey(v11n, "Exo 2:1-10, Exo 3:1-11")
        genToRev = keyf.getKey(v11n, "Gen-Rev 22:21")
        grace = (keyf.createEmptyKeyList(v11n) as Passage)
        grace.addAll(keyf.getKey(v11n, "Gen 6:8, 19:19, 32:5, 33:8, 10, 15, 39:4, 47:25, 29, 50:4, Exo 33:12-13, 16-17, 34:9, Num 32:5, Judg 6:17, Rut 2:2, 10, 1Sa 1:18, 20:3, 27:5"))
        grace.addAll(keyf.getKey(v11n, "2Sa 14:22, 16:4, Ezr 9:8, Est 2:17, Psa 45:2, 84:11, Pro 1:9, 3:22, 34, 4:9, 22:11, Jer 31:2, Zec 4:7, 12:10, Luk 2:40, Joh 1:14, 16-17"))
        grace.addAll(keyf.getKey(v11n, "Act 4:33, 11:23, 13:43, 14:3, 26, 15:11, 40, 18:27, 20:24, 32, Rom 1:5, 7, 3:24, 4:4, 16, 5:2, 15, 17, 20-6:1, 6:14-15, 11:5-6, 12:3, 6, 15:15, 16:20, 24"))
        grace.addAll(keyf.getKey(v11n, "1Co 1:3-4, 3:10, 10:30, 15:10, 16:23, 2Co 1:2, 12, 4:15, 6:1, 8:1, 6-7, 9, 19, 9:8, 14, 12:9, 13:14, Gal 1:3, 6, 15, 2:9, 21, 5:4, 6:18"))
        grace.addAll(keyf.getKey(v11n, "Eph 1:2, 6-7, 2:5, 7-8, 3:2, 7-8, 4:7, 29, 6:24, Phili 1:2, 7, 4:23, Col 1:2, 6, 3:16, 4:6, 18-1Th 1:1, 1Th 5:28, 2Th 1:2, 12, 2:16, 3:18"))
        grace.addAll(keyf.getKey(v11n, "1Ti 1:2, 14, 6:21, 2Ti 1:2, 9, 2:1, 4:22, Tit 1:4, 2:11, 3:7, 15, Phile 3, 25, Heb 2:9, 4:16, 10:29, 12:15, 28, 13:9, 25, Jam 1:11, 4:6"))
        grace.addAll(keyf.getKey(v11n, "1Pe 1:2, 10, 13, 3:7, 4:10, 5:5, 10, 12, 2Pe 1:2, 3:18, 2Jo 3, Jude 4, Rev 1:4, 22:21"))
        empty = (keyf.createEmptyKeyList(v11n) as Passage)
        if (optimize) {
            genC1V135r.optimizeReads()
            exoC2V1To10C2V1To11r.optimizeReads()
            genToRev.optimizeReads()
            grace.optimizeReads()
            empty.optimizeReads()
        }

        genC1V1r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 1)
        genC1V12r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 2)
        genC1V2r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 2), 1)
        exoC2V1r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.EXOD, 2, 1), 1)
        exoC2V12r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.EXOD, 2, 1), 2)
        exoC2V2r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.EXOD, 2, 2), 1)
        genC1V1 = Verse(v11n, BibleBook.GEN, 1, 1)
        genC1V2 = Verse(v11n, BibleBook.GEN, 1, 2)
        genC1V3 = Verse(v11n, BibleBook.GEN, 1, 3)
        genC1V5 = Verse(v11n, BibleBook.GEN, 1, 5)
        exoC2V1 = Verse(v11n, BibleBook.EXOD, 2, 1)
        exoC2V2 = Verse(v11n, BibleBook.EXOD, 2, 2)
        exoC2V3 = Verse(v11n, BibleBook.EXOD, 2, 3)
        exoC2V11 = Verse(v11n, BibleBook.EXOD, 3, 11)
        revC22V21 = VerseFactory.fromString(v11n, "Rev 22:21")!!
    }

    @AfterTest
    fun tearDown() {
        BookName.setCase(storedCase)
        BookName.setFullBookName(fullName)
        PassageKeyFactory.setDefaultPassage(PassageType.toInteger(PassageType.SPEED))
    }

    @Test
    fun testWholeBible(){
        var it: Iterator<VerseRange> = genToRev.rangeIterator(RestrictionType.NONE)
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen-Rev"))
        assertTrue(!it.hasNext())
        // it = gen_rev.rangeIterator(RestrictionType.BOOK)
        it = genToRev.rangeIterator(RestrictionType.CHAPTER)
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 0"))
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 1"))
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 2"))
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 3"))
        it = empty.rangeIterator(RestrictionType.NONE)
        assertTrue(!it.hasNext())
    }

    @Test
    fun testReadAddPassageListener(){
        var li1: FixturePassageListener = FixturePassageListener()
        var li2: FixturePassageListener = FixturePassageListener()
        temp = (genC1V135r.clone() as Passage)
        temp.addPassageListener(li1)
        temp.addPassageListener(li2)
        assertTrue(li1.check(0, 0, 0))
        assertTrue(li2.check(0, 0, 0))
        temp.add(VerseFactory.fromString(v11n, "Gen 1:7"))
        assertTrue(li1.check(1, 0, 0))
        assertTrue(li2.check(1, 0, 0))
        temp.add(VerseFactory.fromString(v11n, "Gen 1:9"))
        assertTrue(li1.check(2, 0, 0))
        assertTrue(li2.check(2, 0, 0))
        temp.removePassageListener(li1)
        temp.add(VerseFactory.fromString(v11n, "Gen 1:11"))
        assertTrue(li1.check(2, 0, 0))
        assertTrue(li2.check(3, 0, 0))
    }

    @Test
    fun testReadRangeIterator(){
        // We used to check for a UnsupportedOperationException until
        // throwing IllegalStateExceptions here. I'm not too bothered about the
        // exact type of the
        // exception right now. Maybe we ought to be more thorough at some
        // stage.
        var it: Iterator<VerseRange> = genC1V135r.rangeIterator(RestrictionType.NONE)
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:1"), it.next())
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:3"), it.next())
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:5"), it.next())
        assertTrue(!it.hasNext())
        it = empty.rangeIterator(RestrictionType.NONE)
        assertTrue(!it.hasNext())
    }

    @Test
    fun testReadVerseIterator(){
        var it: Iterator<Key> = genC1V135r.iterator()
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 1:1"), it.next())
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 1:3"), it.next())
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 1:5"), it.next())
        assertTrue(!it.hasNext())
        it = empty.iterator()
        assertTrue(!it.hasNext())
    }

    @Test
    fun testReadIsEmpty() {
        assertTrue(!genC1V135r.isEmpty())
        assertTrue(!exoC2V1To10C2V1To11r.isEmpty())
        assertTrue(empty.isEmpty())
    }

    @Test
    fun testReadCountVerses() {
        assertEquals(3, genC1V135r.countVerses())
        assertEquals(21, exoC2V1To10C2V1To11r.countVerses())
        assertEquals(0, empty.countVerses())
    }

    @Test
    fun testReadCountRanges() {
        assertEquals(3, genC1V135r.countRanges(RestrictionType.NONE))
        assertEquals(2, exoC2V1To10C2V1To11r.countRanges(RestrictionType.NONE))
        assertEquals(0, empty.countVerses())
    }

    @Test
    fun testReadGetVerseAt(){
        assertEquals(genC1V1, genC1V135r.getVerseAt(0))
        assertEquals(genC1V3, genC1V135r.getVerseAt(1))
        assertEquals(genC1V5, genC1V135r.getVerseAt(2))
        assertEquals(exoC2V1, exoC2V1To10C2V1To11r.getVerseAt(0))
        assertEquals(exoC2V2, exoC2V1To10C2V1To11r.getVerseAt(1))
        assertEquals(exoC2V3, exoC2V1To10C2V1To11r.getVerseAt(2))
        assertEquals(exoC2V11, exoC2V1To10C2V1To11r.getVerseAt(20))
    }

    @Test
    fun testReadGetVerseRangeAt(){
        assertEquals(genC1V1r, genC1V135r.getRangeAt(0, RestrictionType.NONE))
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:3"), genC1V135r.getRangeAt(1, RestrictionType.NONE))
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:5"), genC1V135r.getRangeAt(2, RestrictionType.NONE))
        assertEquals(VerseRangeFactory.fromString(v11n, "Exo 2:1-10"), exoC2V1To10C2V1To11r.getRangeAt(0, RestrictionType.NONE))
        assertEquals(VerseRangeFactory.fromString(v11n, "Exo 3:1-11"), exoC2V1To10C2V1To11r.getRangeAt(1, RestrictionType.NONE))
    }

    @Test
    fun testReadBooksInPassage() {
        assertEquals(1, genC1V135r.booksInPassage())
        assertEquals(1, exoC2V1To10C2V1To11r.booksInPassage())
    }

    @Test
    fun testReadContainsVerse() {
        assertTrue(!empty.contains(genC1V1))
        assertTrue(genC1V135r.contains(genC1V1))
        assertTrue(!genC1V135r.contains(genC1V2))
        assertTrue(genC1V135r.contains(genC1V3))
        assertTrue(genC1V135r.contains(genC1V5))
        assertTrue(genToRev.contains(genC1V1))
        assertTrue(genToRev.contains(genC1V2))
        assertTrue(genToRev.contains(revC22V21))
    }

    @Test
    fun testReadContainsVerseRange() {
        assertTrue(genC1V135r.contains(genC1V1r))
        assertTrue(!genC1V135r.contains(genC1V12r))
        assertTrue(!genC1V135r.contains(genC1V2r))
        assertTrue(!genC1V135r.contains(exoC2V1r))
        assertTrue(!exoC2V1To10C2V1To11r.contains(genC1V1r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V1r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V12r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V2r))
        assertTrue(genToRev.contains(VerseRange(v11n, revC22V21)))
    }

    @Test
    fun testReadContainsAll() {
        assertTrue(!genC1V135r.containsAll(exoC2V1To10C2V1To11r))
        assertTrue(genC1V135r.containsAll(empty))
        assertTrue(genC1V135r.containsAll((genC1V135r.clone() as Passage)))
        assertTrue(!exoC2V1To10C2V1To11r.containsAll(genC1V135r))
        assertTrue(exoC2V1To10C2V1To11r.containsAll(empty))
        assertTrue(exoC2V1To10C2V1To11r.containsAll((exoC2V1To10C2V1To11r.clone() as Passage)))
    }

    @Test
    fun testReadContainsVerseBase() {
        assertTrue(!empty.contains(genC1V1))
        assertTrue(genC1V135r.contains(genC1V1))
        assertTrue(!genC1V135r.contains(genC1V2))
        assertTrue(genC1V135r.contains(genC1V3))
        assertTrue(genC1V135r.contains(genC1V5))
        assertTrue(genC1V135r.contains(genC1V1r))
        assertTrue(!genC1V135r.contains(genC1V12r))
        assertTrue(!genC1V135r.contains(genC1V2r))
        assertTrue(!genC1V135r.contains(exoC2V1r))
        assertTrue(!exoC2V1To10C2V1To11r.contains(genC1V1r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V1r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V12r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V2r))
    }

    // ==========================================================================

    @Test
    fun testWriteCreatePassage() {
        assertEquals(0, ((keyf.createEmptyKeyList(v11n) as Passage)).countVerses())
    }

    @Test
    fun testWriteToString(){
        assertEquals("Gen 1:1-3, Rev 22:21", keyf.getKey(v11n, "gen 1 1,gen 1 3,rev 22 21,gen 1 2").toString())
        assertEquals("Gen 1:1-3, 22:2-10, Rev 22", keyf.getKey(v11n, "Gen 1 3;gen 22 2;rev 22 21;gen 22 3-10; rev 22 19;gen 1 1;rev 22 10-18; gen 1 2; rev 22 1-21").toString())
        assertEquals("", keyf.getKey(v11n, "").toString())
        assertEquals("Gen-Exo", keyf.getKey(v11n, "gen 1 1-50:26,ex,ex 1 2,ex 1 3-10").toString())
        assertEquals("", keyf.getKey(v11n, null).toString())
    }

    @Test
    fun testWriteGetName(){
        assertEquals("Gen 1:1-3, Rev 22:21", keyf.getKey(v11n, "gen 1 1,gen 1 3,rev 22 21,gen 1 2").getName())
        assertEquals("Gen 1:1-3, 22:2-10, Rev 22", keyf.getKey(v11n, "Gen 1 3;gen 22 2;rev 22 21;gen 22 3-10; rev 22 19;gen 1 1;rev 22 10-18; gen 1 2; rev 22 1-21").getName())
        assertEquals("", keyf.getKey(v11n, "").getName())
        assertEquals("Gen-Exo", keyf.getKey(v11n, "gen 1 1-50:26,ex,ex 1 2,ex 1 3-10").getName())
        assertEquals("Exo 1:1, 4", keyf.getKey(v11n, "exo 1:1, 4").getName())
        assertEquals("Exo 1:1-4, 6-22", keyf.getKey(v11n, "exo 1:1, 4, 2-3, 11-ff, 6-10").getName())
        assertEquals("Num 1-2", keyf.getKey(v11n, "Num 1, 2").getName())
        // Test for the separator being a space. This comes from "Clarke"
        assertEquals("Gen 1:26, 3:22, 11:7, 20:13, 31:7, 53, 35:7", keyf.getKey(v11n, "Ge 1:26  3:22  11:7  20:13  31:7, 53  35:7").getName())
    }

    @Test
    fun testWriteBlur(){
        temp = (genC1V135r.clone() as Passage)
        temp.blur(0, RestrictionType.CHAPTER)
        assertEquals(temp, genC1V135r)
        temp = (genC1V135r.clone() as Passage)
        temp.blur(0, RestrictionType.NONE)
        assertEquals(temp, genC1V135r)
        temp = (genC1V135r.clone() as Passage)
        temp.blur(1, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Gen 1:0-6"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(1, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Gen 1:0-6"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(2, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Gen 1:0-7"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(2, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Gen 0:0-1:7"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(12, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Gen 1:0-17"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(12, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Bible.Intro-Gen 1:17"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(26, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Gen 1:0-31"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(26, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Bible.Intro-Gen 1:31"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(27, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Gen 1:0-31"))
        temp = (genC1V135r.clone() as Passage)
        temp.blur(27, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Bible.Intro-Gen 2:0"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(0, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:1-10, Exo 3:1-11"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(0, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:1-10, Exo 3:1-11"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(1, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:0-11, Exo 3:0-12"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(1, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:0-2:11, Exo 3:0-3:12"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(2, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:0-12, Exo 3:0-13"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(2, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Exo 1:22-2:12, Exo 2:25-3:13"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(3, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:0-13, Exo 3:0-14"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(3, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Exo 1:21-2:13, Exo 2:24-3:14"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(14, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:0-24, Exo 3:0-22"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(14, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Exo 1:10-2:24, Exo 2:12-4:2"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(15, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:0-25, Exo 3:0-22"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(15, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Exo 1:9-2:25, Exo 2:11-4:3"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(16, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:1-3:22"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(16, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Exo 1:8-4:4"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(99999, RestrictionType.CHAPTER)
        assertEquals(temp, keyf.getKey(v11n, "Exo 2:1-3:22"))
        temp = (exoC2V1To10C2V1To11r.clone() as Passage)
        temp.blur(99999, RestrictionType.NONE)
        assertEquals(temp, keyf.getKey(v11n, "Bible.Intro-Rev 22:21"))
    }

    @Test
    fun testWriteAddPassageListener(){
        var li1: FixturePassageListener = FixturePassageListener()
        var li2: FixturePassageListener = FixturePassageListener()
        temp = (genC1V135r.clone() as Passage)
        temp.addPassageListener(li1)
        temp.addPassageListener(li2)
        assertTrue(li1.check(0, 0, 0))
        assertTrue(li2.check(0, 0, 0))
        temp.add(VerseFactory.fromString(v11n, "Gen 1:7"))
        assertTrue(li1.check(1, 0, 0))
        assertTrue(li2.check(1, 0, 0))
        temp.add(VerseFactory.fromString(v11n, "Gen 1:9"))
        assertTrue(li1.check(2, 0, 0))
        assertTrue(li2.check(2, 0, 0))
        temp.removePassageListener(li1)
        temp.add(VerseFactory.fromString(v11n, "Gen 1:11"))
        assertTrue(li1.check(2, 0, 0))
        assertTrue(li2.check(3, 0, 0))
    }

    @Test
    fun testWriteClone() {
        assertTrue(genC1V135r !== genC1V135r.clone())
        assertEquals(genC1V135r, genC1V135r.clone())
        assertTrue(exoC2V1To10C2V1To11r !== exoC2V1To10C2V1To11r.clone())
        assertEquals(exoC2V1To10C2V1To11r, exoC2V1To10C2V1To11r.clone())
    }

    @Test
    fun testWriteRangeIterator(){
        var it: Iterator<VerseRange> = genC1V135r.rangeIterator(RestrictionType.NONE)
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 1:1"))
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 1:3"))
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 1:5"))
        assertTrue(!it.hasNext())
        it = genToRev.rangeIterator(RestrictionType.NONE)
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen-Rev"))
        assertTrue(!it.hasNext())
        // No longer have RestrictionType.BOOK
        // it = gen_rev.rangeIterator(RestrictionType.BOOK)
        it = genToRev.rangeIterator(RestrictionType.CHAPTER)
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 0"))
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 1"))
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 2"))
        assertTrue(it.hasNext())
        assertEquals(it.next(), VerseRangeFactory.fromString(v11n, "Gen 3"))
        it = empty.rangeIterator(RestrictionType.NONE)
        assertTrue(!it.hasNext())
    }

    @Test
    fun testWriteVerseIterator(){
        var it: Iterator<Key> = genC1V135r.iterator()
        assertTrue(it.hasNext())
        assertEquals<Key?>(it.next(), VerseFactory.fromString(v11n, "Gen 1:1"))
        assertTrue(it.hasNext())
        assertEquals<Key?>(it.next(), VerseFactory.fromString(v11n, "Gen 1:3"))
        assertTrue(it.hasNext())
        assertEquals<Key?>(it.next(), VerseFactory.fromString(v11n, "Gen 1:5"))
        assertTrue(!it.hasNext())
        it = empty.iterator()
        assertTrue(!it.hasNext())
    }

    @Test
    fun testWriteIsEmpty() {
        assertTrue(!genC1V135r.isEmpty())
        assertTrue(!exoC2V1To10C2V1To11r.isEmpty())
        assertTrue(empty.isEmpty())
    }

    @Test
    fun testWriteCountVerses() {
        assertEquals(3, genC1V135r.countVerses())
        assertEquals(21, exoC2V1To10C2V1To11r.countVerses())
        assertEquals(0, empty.countVerses())
    }

    @Test
    fun testWriteCountRanges() {
        assertEquals(3, genC1V135r.countRanges(RestrictionType.NONE))
        assertEquals(2, exoC2V1To10C2V1To11r.countRanges(RestrictionType.NONE))
        assertEquals(0, empty.countVerses())
    }

    @Test
    fun testWriteGetVerseAt(){
        assertEquals(genC1V135r.getVerseAt(0), genC1V1)
        assertEquals(genC1V135r.getVerseAt(1), genC1V3)
        assertEquals(genC1V135r.getVerseAt(2), genC1V5)
        assertEquals(exoC2V1To10C2V1To11r.getVerseAt(0), exoC2V1)
        assertEquals(exoC2V1To10C2V1To11r.getVerseAt(1), exoC2V2)
        assertEquals(exoC2V1To10C2V1To11r.getVerseAt(2), exoC2V3)
        assertEquals(exoC2V1To10C2V1To11r.getVerseAt(20), exoC2V11)
    }

    @Test
    fun testWriteGetVerseRangeAt(){
        assertEquals(genC1V135r.getRangeAt(0, RestrictionType.NONE), genC1V1r)
        assertEquals(genC1V135r.getRangeAt(1, RestrictionType.NONE), VerseRangeFactory.fromString(v11n, "Gen 1:3"))
        assertEquals(genC1V135r.getRangeAt(2, RestrictionType.NONE), VerseRangeFactory.fromString(v11n, "Gen 1:5"))
        assertEquals(exoC2V1To10C2V1To11r.getRangeAt(0, RestrictionType.NONE), VerseRangeFactory.fromString(v11n, "Exo 2:1-10"))
        assertEquals(exoC2V1To10C2V1To11r.getRangeAt(1, RestrictionType.NONE), VerseRangeFactory.fromString(v11n, "Exo 3:1-11"))
    }

    @Test
    fun testWriteBooksInPassage() {
        assertEquals(1, genC1V135r.booksInPassage())
        assertEquals(1, exoC2V1To10C2V1To11r.booksInPassage())
    }

    @Test
    fun testWriteContainsVerse() {
        assertTrue(!empty.contains(genC1V1))
        assertTrue(genC1V135r.contains(genC1V1))
        assertTrue(!genC1V135r.contains(genC1V2))
        assertTrue(genC1V135r.contains(genC1V3))
        assertTrue(genC1V135r.contains(genC1V5))
        assertTrue(genToRev.contains(genC1V1))
        assertTrue(genToRev.contains(genC1V2))
        assertTrue(genToRev.contains(revC22V21))
    }

    @Test
    fun testWriteContainsVerseRange() {
        assertTrue(genC1V135r.contains(genC1V1r))
        assertTrue(!genC1V135r.contains(genC1V12r))
        assertTrue(!genC1V135r.contains(genC1V2r))
        assertTrue(!genC1V135r.contains(exoC2V1r))
        assertTrue(!exoC2V1To10C2V1To11r.contains(genC1V1r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V1r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V12r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V2r))
        assertTrue(genToRev.contains(VerseRange(v11n, revC22V21)))
    }

    @Test
    fun testWriteContainsAll() {
        assertTrue(!genC1V135r.containsAll(exoC2V1To10C2V1To11r))
        assertTrue(genC1V135r.containsAll(empty))
        assertTrue(genC1V135r.containsAll((genC1V135r.clone() as Passage)))
        assertTrue(!exoC2V1To10C2V1To11r.containsAll(genC1V135r))
        assertTrue(exoC2V1To10C2V1To11r.containsAll(empty))
        assertTrue(exoC2V1To10C2V1To11r.containsAll((exoC2V1To10C2V1To11r.clone() as Passage)))
        assertTrue(!empty.containsAll(genC1V135r))
        assertTrue(empty.containsAll(empty))
        assertTrue(!empty.containsAll(exoC2V1To10C2V1To11r))
        assertTrue(genToRev.containsAll(genToRev))
    }

    @Test
    fun testWriteContainsVerseBase() {
        assertTrue(!empty.contains(genC1V1))
        assertTrue(genC1V135r.contains(genC1V1))
        assertTrue(!genC1V135r.contains(genC1V2))
        assertTrue(genC1V135r.contains(genC1V3))
        assertTrue(genC1V135r.contains(genC1V5))
        assertTrue(genC1V135r.contains(genC1V1r))
        assertTrue(!genC1V135r.contains(genC1V12r))
        assertTrue(!genC1V135r.contains(genC1V2r))
        assertTrue(!genC1V135r.contains(exoC2V1r))
        assertTrue(!exoC2V1To10C2V1To11r.contains(genC1V1r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V1r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V12r))
        assertTrue(exoC2V1To10C2V1To11r.contains(exoC2V2r))
    }

    @Test
    fun testWriteAdd(){
        temp = (genC1V135r.clone() as Passage)
        temp.add(VerseFactory.fromString(v11n, "Gen 1:2"))
        assertEquals("Gen 1:1-3, 5", temp.getName())
        temp.add(VerseFactory.fromString(v11n, "Gen 1:4"))
        assertEquals("Gen 1:1-5", temp.getName())
        temp = (genC1V135r.clone() as Passage)
        temp.add(VerseRangeFactory.fromString(v11n, "Gen 1:2-4"))
        assertEquals("Gen 1:1-5", temp.getName())
        temp = (genC1V135r.clone() as Passage)
        temp.add(VerseRangeFactory.fromString(v11n, "Gen 1:2"))
        assertEquals("Gen 1:1-3, 5", temp.getName())
        temp.add(VerseRangeFactory.fromString(v11n, "Gen 1:4"))
        assertEquals("Gen 1:1-5", temp.getName())
        temp = (genC1V135r.clone() as Passage)
        temp.add(VerseRangeFactory.fromString(v11n, "Gen 1:1-5"))
        assertEquals("Gen 1:1-5", temp.getName())
    }

    @Test
    fun testWriteAddAll(){
        temp = (genC1V135r.clone() as Passage)
        temp.addAll(keyf.getKey(v11n, "Gen 1:2, Gen 1:4"))
        assertEquals("Gen 1:1-5", temp.getName())
    }

    @Test
    fun testWriteClear() {
        temp = (genC1V135r.clone() as Passage)
        temp.clear()
        assertEquals("", temp.getName())
        temp.clear()
        assertEquals("", temp.getName())
    }

    @Test
    fun testWriteRemove(){
        temp = (genC1V135r.clone() as Passage)
        temp.remove(VerseFactory.fromString(v11n, "Gen 1:3"))
        assertEquals("Gen 1:1, 5", temp.getName())
        temp.remove(VerseFactory.fromString(v11n, "Gen 1:5"))
        assertEquals("Gen 1:1", temp.getName())
        temp.remove(VerseFactory.fromString(v11n, "Gen 1:1"))
        assertEquals("", temp.getName())
        temp = keyf.getKey(v11n, "Gen 1:1-5")
        temp.remove(VerseFactory.fromString(v11n, "Gen 1:3"))
        assertEquals("Gen 1:1-2, 4-5", temp.getName())
    }

    @Test
    fun testWriteRemoveAllCollection(){
        temp = keyf.getKey(v11n, "Gen 1:1-5")
        temp.removeAll(keyf.getKey(v11n, "Gen 1:2, Gen 1:4"))
        assertEquals("Gen 1:1, 3, 5", temp.getName())
        temp.removeAll(keyf.getKey(v11n, "Exo 1:2, Gen 1:4"))
        assertEquals("Gen 1:1, 3, 5", temp.getName())
        temp.removeAll(keyf.getKey(v11n, "Gen 1:2-Rev 22:21"))
        assertEquals("Gen 1:1", temp.getName())
        temp.removeAll(keyf.getKey(v11n, "Gen 1:1"))
        assertEquals("", temp.getName())
    }

    @Test
    fun testWriteRetainAllCollection(){
        temp = keyf.getKey(v11n, "Gen 1:1-5")
        temp.retainAll(keyf.getKey(v11n, "Gen 1:2, Gen 1:4"))
        assertEquals("Gen 1:2, 4", temp.getName())
        temp.retainAll(keyf.getKey(v11n, "Exo 1:2, Gen 1:4"))
        assertEquals("Gen 1:4", temp.getName())
        temp.retainAll(keyf.getKey(v11n, "Gen 1:2-Rev 22:21"))
        assertEquals("Gen 1:4", temp.getName())
        temp.retainAll(keyf.getKey(v11n, "Gen 1:1"))
        assertEquals("", temp.getName())
        temp.addAll(grace)
        assertEquals(temp.countVerses(), grace.countVerses())
        temp.retainAll(genToRev)
        assertEquals(temp, grace)
        temp.retainAll(keyf.getKey(v11n, "gen"))
        assertEquals(10, temp.countVerses())
        temp.retainAll(keyf.getKey(v11n, "gen 35:1-rev"))
        assertEquals(4, temp.countVerses())
        temp.retainAll(keyf.getKey(v11n, "exo-rev"))
        assertEquals("", temp.getName())
    }

    @Test
    fun testWriteDescription(){
        var out: StringBuilder = StringBuilder()
        genC1V135r.writeDescription(out)
        var lst: Passage = (keyf.createEmptyKeyList(v11n) as Passage)
        lst.readDescription(out.toString())
        assertEquals(genC1V135r, lst)
    }

    @Test
    fun testSpecial(){
        // Some special tests for known breakages
        var ich5l: Passage = keyf.getKey(v11n, "1ch 5")
        var ich5u: Passage = keyf.getKey(v11n, "1Ch 5")
        assertEquals(ich5l, ich5u)
    }
}
