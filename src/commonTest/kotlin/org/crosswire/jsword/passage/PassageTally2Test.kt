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
 * JUnit Test.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class PassageTally2Test {
    /** Control the output of names */
    private var storedCase: CaseType? = null
    private var fullName: Boolean = false
    private lateinit var v11n: Versification

    /**
     * How we create Passages
     */
    private var keyf: PassageKeyFactory = PassageKeyFactory.instance()

    private lateinit var genC1V1r: VerseRange
    private lateinit var genC1V12r: VerseRange
    private lateinit var genC1V2r: VerseRange
    private lateinit var exoC2V1r: VerseRange

    private lateinit var genC1V1: Verse
    private lateinit var genC1V2: Verse
    private lateinit var genC1V3: Verse
    private lateinit var genC1V5: Verse

    private lateinit var genC1V135: Passage
    private lateinit var genC123V1: Passage
    private lateinit var genC1V157: Passage

    // AV11N(DMS): Update test to test all V11Ns
    private var tally: PassageTally = PassageTally(Versifications.instance().getVersification("KJV")!!)
    private var empty: PassageTally = PassageTally(Versifications.instance().getVersification("KJV")!!)
    private lateinit var temp: PassageTally

    @BeforeTest
    fun setUp(){
        storedCase = BookName.getDefaultCase()
        BookName.setCase(CaseType.SENTENCE)
        fullName = BookName.isFullBookName()
        BookName.setFullBookName(false)
        // AV11N(DMS): Update test to test all V11Ns
        v11n = Versifications.instance().getVersification("KJV")!!
        genC1V1r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 1)
        genC1V12r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 2)
        genC1V2r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 2), 1)
        exoC2V1r = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.EXOD, 2, 1), 1)
        genC1V1 = Verse(v11n, BibleBook.GEN, 1, 1)
        genC1V2 = Verse(v11n, BibleBook.GEN, 1, 2)
        genC1V3 = Verse(v11n, BibleBook.GEN, 1, 3)
        genC1V5 = Verse(v11n, BibleBook.GEN, 1, 5)
        genC1V135 = keyf.getKey(v11n, "Gen 1:1, Gen 1:3, Gen 1:5")
        genC123V1 = keyf.getKey(v11n, "Gen 1:1, Gen 2:1, Gen 3:1")
        genC1V157 = keyf.getKey(v11n, "Gen 1:1, Gen 1:5, Gen 1:7")
        tally.ordering = PassageTally.Order.TALLY
        empty.ordering = PassageTally.Order.TALLY
        tally.addAll(genC1V135)
        tally.addAll(genC123V1)
        tally.addAll(genC1V157)
    }

    @AfterTest
    fun tearDown() {
        BookName.setCase(storedCase)
        BookName.setFullBookName(fullName)
    }

    @Test
    fun testGetName() {
        assertEquals("Gen 1:1, 5, 3, 7, 2:1, 3:1", tally.getName(0))
        assertEquals("Gen 1:1", tally.getName(1))
        assertEquals("Gen 1:1, 5", tally.getName(2))
        assertEquals("Gen 1:1, 5, 3", tally.getName(3))
        assertEquals("Gen 1:1, 5, 3, 7", tally.getName(4))
        assertEquals("Gen 1:1, 5, 3, 7, 2:1", tally.getName(5))
        assertEquals("Gen 1:1, 5, 3, 7, 2:1, 3:1", tally.getName(6))
        assertEquals("Gen 1:1, 5, 3, 7, 2:1, 3:1", tally.getName(7))
        assertEquals("Gen 1:1, 5, 3, 7, 2:1, 3:1", tally.getName(8))
        assertEquals("Gen 1:1, 5, 3, 7, 2:1, 3:1", tally.getName(9))
        assertEquals("Gen 1:1, 5, 3, 7, 2:1, 3:1", tally.getName(10))
        assertEquals("", empty.getName(0))
        assertEquals("", empty.getName(1))
        assertEquals("", empty.getName(2))
        assertEquals("", empty.getName(3))
        assertEquals("", empty.getName(4))
        assertEquals("", empty.getName(5))
        assertEquals("", empty.getName(6))
        assertEquals("", empty.getName(7))
        assertEquals("", empty.getName(8))
        assertEquals("", empty.getName(9))
        assertEquals("", empty.getName(10))
    }

    @Test
    fun testToString() {
        assertEquals("Gen 1:1, 5, 3, 7, 2:1, 3:1", tally.toString())
        assertEquals("", empty.toString())
    }

    @Test
    fun testGetOrderedNameAndTally() {
        assertEquals("Gen 1:1 (100%), Gen 1:5 (66%), Gen 1:3 (33%), Gen 1:7 (33%), Gen 2:1 (33%), Gen 3:1 (33%)", tally.getNameAndTally())
        assertEquals("", empty.getNameAndTally())
    }

    @Test
    fun testAddPassageListener(){
        var li: FixturePassageListener = FixturePassageListener()
        temp = tally.clone()
        temp.addPassageListener(li)
        assertTrue(li.check(0, 0, 0))
        temp.add(VerseFactory.fromString(v11n, "Gen 1:7"))
        assertTrue(li.check(1, 0, 0))
        temp.add(VerseFactory.fromString(v11n, "Gen 1:9"))
        assertTrue(li.check(2, 0, 0))
        temp.removePassageListener(li)
        temp.add(VerseFactory.fromString(v11n, "Gen 1:11"))
        assertTrue(li.check(2, 0, 0))
    }

    @Test
    fun testClone() {
        assertTrue(tally !== tally.clone())
        assertEquals(tally, tally.clone())
        assertTrue(empty !== empty.clone())
        assertEquals(empty, empty.clone())
    }

    @Test
    fun testVerseIterator(){
        var it: Iterator<Key> = tally.iterator()
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 1:1"), it.next())
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 1:5"), it.next())
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 1:3"), it.next())
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 1:7"), it.next())
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 2:1"), it.next())
        assertTrue(it.hasNext())
        assertEquals<Key?>(VerseFactory.fromString(v11n, "Gen 3:1"), it.next())
        assertTrue(!it.hasNext())
        it = empty.iterator()
        assertTrue(!it.hasNext())
    }

    @Test
    fun testRangeIterator(){
        var it: Iterator<VerseRange> = tally.rangeIterator(RestrictionType.NONE)
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:1"), it.next())
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:5"), it.next())
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:3"), it.next())
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 1:7"), it.next())
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 2:1"), it.next())
        assertTrue(it.hasNext())
        assertEquals(VerseRangeFactory.fromString(v11n, "Gen 3:1"), it.next())
        assertTrue(!it.hasNext())
        it = empty.rangeIterator(RestrictionType.NONE)
        assertTrue(!it.hasNext())
    }

    @Test
    fun testIsEmpty() {
        assertTrue(!tally.isEmpty())
        assertTrue(empty.isEmpty())
    }

    @Test
    fun testCountVerses() {
        assertEquals(6, tally.countVerses())
        assertEquals(0, empty.countVerses())
    }

    @Test
    fun testCountRanges() {
        assertEquals(6, tally.countRanges(RestrictionType.NONE))
        assertEquals(0, empty.countRanges(RestrictionType.NONE))
    }

    @Test
    fun testContainsVerse() {
        assertTrue(!empty.contains(genC1V1))
        assertTrue(tally.contains(genC1V1))
        assertTrue(!tally.contains(genC1V2))
        assertTrue(tally.contains(genC1V3))
        assertTrue(tally.contains(genC1V5))
    }

    @Test
    fun testContainsVerseRange() {
        assertTrue(!empty.contains(genC1V1r))
        assertTrue(tally.contains(genC1V1r))
        assertTrue(!tally.contains(genC1V12r))
        assertTrue(!tally.contains(genC1V2r))
        assertTrue(!tally.contains(exoC2V1r))
    }

    @Test
    fun testAdd(){
        temp = tally.clone()
        temp.add(VerseFactory.fromString(v11n, "Gen 1:2"))
        assertEquals("Gen 1:1, 5, 2, 3, 7, 2:1, 3:1", temp.getName())
        temp.add(VerseFactory.fromString(v11n, "Gen 1:4"))
        assertEquals("Gen 1:1, 5, 2, 3, 4, 7, 2:1, 3:1", temp.getName())
        temp = tally.clone()
        temp.add(VerseRangeFactory.fromString(v11n, "Gen 1:2-4"))
        assertEquals("Gen 1:1, 3, 5, 2, 4, 7, 2:1, 3:1", temp.getName())
        assertFailsWith<NullPointerException> {
            temp.addAll((null as Key))
        }
    }

    @Test
    fun testUnAdd(){
        temp = tally.clone()
        temp.unAdd(VerseFactory.fromString(v11n, "Gen 1:5"))
        assertEquals("Gen 1:1, 3, 5, 7, 2:1, 3:1", temp.getName())
        temp.unAdd(VerseFactory.fromString(v11n, "Gen 1:5"))
        assertEquals("Gen 1:1, 3, 7, 2:1, 3:1", temp.getName())
        temp.unAdd(VerseFactory.fromString(v11n, "Gen 1:5"))
        assertEquals("Gen 1:1, 3, 7, 2:1, 3:1", temp.getName())
    }

    @Test
    fun testAddAll(){
        temp = tally.clone()
        temp.addAll(keyf.getKey(v11n, "Gen 1:2, Gen 1:4"))
        assertEquals("Gen 1:1, 5, 2, 3, 4, 7, 2:1, 3:1", temp.getName())
    }

    @Test
    fun testClear() {
        temp = tally.clone()
        temp.clear()
        assertEquals("", temp.getName())
        temp.clear()
        assertEquals("", temp.getName())
        temp = empty.clone()
        temp.clear()
        assertEquals("", temp.getName())
    }

    @Test
    fun testBlur() {
        temp = tally.clone()
        temp.blur(1, RestrictionType.NONE)
        assertEquals("Gen 1:1 (100%), Gen 1:2 (100%), Gen 1:0 (75%), Gen 1:4 (75%), Gen 1:5 (75%), Gen 1:6 (75%), Gen 1:3 (50%), Gen 1:7 (50%), Gen 2:1 (50%), Gen 3:1 (50%), Gen 1:8 (25%), Gen 2:0 (25%), Gen 2:2 (25%), Gen 3:0 (25%), Gen 3:2 (25%)", temp.getNameAndTally())
    }

    @Test
    fun testFlatten() {
        temp = tally.clone()
        temp.flatten()
        assertEquals("Gen 1:1, 3, 5, 7, 2:1, 3:1", temp.getName())
    }

}
