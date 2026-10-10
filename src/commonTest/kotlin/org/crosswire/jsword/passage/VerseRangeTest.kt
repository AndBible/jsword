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
 * @author DM Smith
 */
class VerseRangeTest {
    /** Control the output of names */
    private var storedCase: CaseType? = null
    private var fullName: Boolean = false

    private lateinit var v11n: Versification
    private lateinit var rangeGenC1V0to31: VerseRange
    private lateinit var rangeGenC1V01: VerseRange
    private lateinit var rangeGenC1V1: VerseRange
    private lateinit var rangeGenC1V02: VerseRange
    private lateinit var rangeGenC1V12: VerseRange
    private lateinit var rangeGenC1V1to31: VerseRange
    private lateinit var rangeGenC1V0to34: VerseRange
    private lateinit var rangeGenC1V1to33: VerseRange
    private lateinit var rangeGenC1V2: VerseRange
    private lateinit var rangeGenC0V0toEnd: VerseRange
    private lateinit var rangeGenC0V0toExoC1V1: VerseRange
    private lateinit var rangeGenC0V0toExoEnd: VerseRange
    private lateinit var rangeGenC1V1toRevEnd: VerseRange
    private lateinit var rangeRevC22V0toEnd: VerseRange
    private lateinit var rangeRevC0V0toEnd: VerseRange
    private lateinit var rangeRevC22V21: VerseRange

    private lateinit var genC0V0: Verse
    private lateinit var genC1V1: Verse
    private lateinit var genC1V2: Verse
    private lateinit var genC1V31: Verse
    private lateinit var genC2V1: Verse
    private lateinit var genC50V26: Verse
    private lateinit var exoC1V1: Verse
    private lateinit var revC0V0: Verse
    private lateinit var revC1V1: Verse
    private lateinit var revC1V2: Verse
    private lateinit var revC22V21: Verse
    private lateinit var revC22V0: Verse
    private lateinit var revC22V1: Verse

    @BeforeTest
    fun setUp(){
        storedCase = BookName.getDefaultCase()
        BookName.setCase(CaseType.SENTENCE)
        fullName = BookName.isFullBookName()
        BookName.setFullBookName(false)
        // AV11N(DMS): Update test to test all V11Ns
        v11n = Versifications.instance().getVersification("KJV")!!
        rangeGenC1V0to31 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 0), 32)
        rangeGenC1V01 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 0), 2)
        rangeGenC1V1 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 1)
        rangeGenC1V02 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 0), 3)
        rangeGenC1V12 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 2)
        rangeGenC1V1to31 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 31)
        rangeGenC1V0to34 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 0), 34)
        rangeGenC1V1to33 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 33)
        rangeGenC1V2 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 2), 1)
        rangeGenC0V0toEnd = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 0, 0), 1584)
        rangeGenC0V0toExoC1V1 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 0, 0), 1587)
        rangeGenC0V0toExoEnd = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 0, 0), 2838)
        rangeGenC1V1toRevEnd = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.GEN, 1, 1), 32356)
        rangeRevC22V0toEnd = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.REV, 22, 0), 22)
        rangeRevC0V0toEnd = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.REV, 0, 0), 428)
        rangeRevC22V21 = RestrictionType.NONE.toRange(v11n, Verse(v11n, BibleBook.REV, 22, 21), 1)
        genC0V0 = Verse(v11n, BibleBook.GEN, 0, 0)
        genC1V1 = Verse(v11n, BibleBook.GEN, 1, 1)
        genC1V2 = Verse(v11n, BibleBook.GEN, 1, 2)
        genC1V31 = Verse(v11n, BibleBook.GEN, 1, 31)
        genC2V1 = Verse(v11n, BibleBook.GEN, 2, 1)
        genC50V26 = Verse(v11n, BibleBook.GEN, 50, 26)
        exoC1V1 = Verse(v11n, BibleBook.EXOD, 1, 1)
        revC0V0 = Verse(v11n, BibleBook.REV, 0, 0)
        revC1V1 = Verse(v11n, BibleBook.REV, 1, 1)
        revC1V2 = Verse(v11n, BibleBook.REV, 1, 2)
        revC22V21 = Verse(v11n, BibleBook.REV, 22, 21)
        revC22V0 = Verse(v11n, BibleBook.REV, 22, 0)
        revC22V1 = Verse(v11n, BibleBook.REV, 22, 1)
    }

    @AfterTest
    fun tearDown() {
        BookName.setCase(storedCase)
        BookName.setFullBookName(fullName)
    }

    @Test
    fun testNewViaString(){
        assertEquals(rangeGenC1V1, VerseRangeFactory.fromString(v11n, "Gen 1:1-1"))
        assertEquals(rangeGenC1V12, VerseRangeFactory.fromString(v11n, "Gen 1:1-2"))
        assertEquals(rangeGenC1V12, VerseRangeFactory.fromString(v11n, "Gen 1:1-1:2"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-50:26"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-50:$"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-50:ff"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-$:26"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-ff:26"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-$:$"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-$:ff"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-ff:$"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-ff:ff"))
        assertEquals(rangeGenC0V0toExoC1V1, VerseRangeFactory.fromString(v11n, "Gen 0:0-Exo 1:1"))
        assertEquals(rangeGenC0V0toExoEnd, VerseRangeFactory.fromString(v11n, "Gen-Exo"))
        assertEquals(rangeGenC1V1toRevEnd, VerseRangeFactory.fromString(v11n, "Gen 1:1-Rev 22:21"))
        assertEquals(rangeGenC1V1toRevEnd, VerseRangeFactory.fromString(v11n, "Gen 1:1-Rev 22:$"))
        assertEquals(rangeGenC1V1toRevEnd, VerseRangeFactory.fromString(v11n, "Gen 1:1-Rev 22:ff"))
        assertEquals(rangeGenC1V1, VerseRangeFactory.fromString(v11n, "Gen 1:1"))
        assertEquals(rangeRevC22V21, VerseRangeFactory.fromString(v11n, "Rev 22:21"))
        assertEquals(rangeRevC22V21, VerseRangeFactory.fromString(v11n, "Rev 22:21-21"))
        assertEquals(rangeGenC1V1, VerseRangeFactory.fromString(v11n, "Gen 1:1-Gen 1:1"))
        assertEquals(rangeGenC1V12, VerseRangeFactory.fromString(v11n, "Gen 1:1-Gen 1:2"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-50:26"))
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen 0:0-Gen 50:26"))
        assertEquals(rangeRevC22V21, VerseRangeFactory.fromString(v11n, "Rev 22:21-Rev 22:21"))
        assertEquals(rangeRevC22V21, VerseRangeFactory.fromString(v11n, "Rev 22:21-Rev 22:ff"))
        assertEquals(rangeRevC22V21, VerseRangeFactory.fromString(v11n, "Rev 22:21-Rev 22:$"))
        assertEquals(rangeRevC22V21, VerseRangeFactory.fromString(v11n, "Rev 22:21-$"))
        assertEquals(rangeRevC22V21, VerseRangeFactory.fromString(v11n, "Rev 22:21-21"))
        assertEquals(rangeGenC1V1, VerseRangeFactory.fromString(v11n, "Gen 1:1-1:1"))
        assertEquals(rangeGenC0V0toExoC1V1, VerseRangeFactory.fromString(v11n, "ge 0 0-ex 1 1"))
        assertEquals(rangeGenC0V0toExoC1V1, VerseRangeFactory.fromString(v11n, "ge -ex 1 1"))
        assertEquals(rangeGenC0V0toExoC1V1, VerseRangeFactory.fromString(v11n, "Genesis 0:0-ex 1 1"))
        assertEquals(rangeGenC0V0toExoC1V1, VerseRangeFactory.fromString(v11n, "ge-ex:1:1"))
        assertFailsWith<NoSuchVerseException> {
            VerseRangeFactory.fromString(v11n, "Gen 1:1-Gen 1:2-Gen 1:3")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseRangeFactory.fromString(v11n, "Gen 1:1-2-3")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseRangeFactory.fromString(v11n, "b 1:1-2")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseRangeFactory.fromString(v11n, "g-f 1 2")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseRangeFactory.fromString(v11n, (null as String?))
        }
        assertEquals(rangeGenC0V0toEnd, VerseRangeFactory.fromString(v11n, "Gen"))
        assertEquals(rangeRevC22V0toEnd, VerseRangeFactory.fromString(v11n, "Rev 22"))
        assertEquals(rangeRevC0V0toEnd, VerseRangeFactory.fromString(v11n, "Rev"))
    }

    @Test
    fun testToString() {
        assertEquals("Gen 1:1", rangeGenC1V1.toString())
        assertEquals("Gen 1:1-2", rangeGenC1V12.toString())
        assertEquals("Gen 1", rangeGenC1V1to31.toString())
        assertEquals("Gen 1:1-2:1", rangeGenC1V1to33.toString())
        assertEquals("Gen 1:2", rangeGenC1V2.toString())
        assertEquals("Gen", rangeGenC0V0toEnd.toString())
        assertEquals("Gen-Exo 1:1", rangeGenC0V0toExoC1V1.toString())
        assertEquals("Gen-Rev", rangeGenC1V1toRevEnd.toString())
        assertEquals("Rev 22", rangeRevC22V0toEnd.toString())
        assertEquals("Rev", rangeRevC0V0toEnd.toString())
        assertEquals("Rev 22:21", rangeRevC22V21.toString())
    }

    @Test
    fun testNewViaVerseInt(){
        assertEquals(rangeGenC1V1, RestrictionType.NONE.toRange(v11n, VerseFactory.fromString(v11n, "Gen 1:1"), 1))
    }

    @Test
    fun testNewViaVerseIntBoolean() {
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.toRange(v11n, genC1V1, 999999))
        assertEquals(rangeGenC1V1, RestrictionType.NONE.toRange(v11n, genC1V1, 0))
        assertEquals(rangeGenC1V1, RestrictionType.NONE.toRange(v11n, genC1V1, -1))
    }

    @Test
    fun testNewViaVerse() {
        assertEquals(rangeGenC1V1, VerseRange(v11n, genC1V1))
        assertEquals(rangeRevC22V21, VerseRange(v11n, revC22V21))
    }

    @Test
    fun testNewViaVerseVerse(){
        assertEquals(rangeGenC1V1, VerseRange(v11n, genC1V1, genC1V1))
        assertEquals(rangeGenC1V12, VerseRange(v11n, genC1V1, genC1V2))
        assertEquals(rangeGenC1V12, VerseRange(v11n, genC1V2, genC1V1))
        assertEquals(rangeGenC1V1toRevEnd, VerseRange(v11n, genC1V1, revC22V21))
        assertEquals(rangeGenC1V1toRevEnd, VerseRange(v11n, revC22V21, genC1V1))
        assertEquals(rangeGenC0V0toEnd, VerseRange(v11n, genC0V0, genC50V26))
        assertEquals(rangeGenC0V0toEnd, VerseRange(v11n, genC50V26, genC0V0))
        assertEquals(rangeGenC0V0toExoC1V1, VerseRange(v11n, genC0V0, exoC1V1))
        assertEquals(rangeGenC0V0toExoC1V1, VerseRange(v11n, exoC1V1, genC0V0))
        assertEquals(rangeGenC1V1, VerseRange(v11n, genC1V1, Verse(v11n, BibleBook.GEN, 1, 1)))
    }

    @Test
    fun testNewViaVerseIntIntBoolean() {
        assertEquals(rangeGenC1V1, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 0))
        assertEquals(rangeGenC1V1, RestrictionType.NONE.blur(v11n, genC1V1, 0, 0))
        assertEquals(rangeGenC1V12, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 1))
        assertEquals(rangeGenC1V12, RestrictionType.NONE.blur(v11n, genC1V1, 0, 1))
        assertEquals(rangeGenC1V01, RestrictionType.CHAPTER.blur(v11n, genC1V1, 1, 0))
        assertEquals(rangeGenC1V1, RestrictionType.NONE.blur(v11n, genC1V1, 0, 0))
        assertEquals(rangeGenC1V01, RestrictionType.CHAPTER.blur(v11n, genC1V1, 9, 0))
        assertEquals(rangeGenC1V1, RestrictionType.NONE.blur(v11n, genC1V1, 0, 0))
        assertEquals(rangeRevC22V21, RestrictionType.CHAPTER.blur(v11n, revC22V21, 0, 1))
        assertEquals(rangeRevC22V21, RestrictionType.NONE.blur(v11n, revC22V21, 0, 1))
        assertEquals(rangeRevC22V21, RestrictionType.CHAPTER.blur(v11n, revC22V21, 0, 9))
        assertEquals(rangeRevC22V21, RestrictionType.NONE.blur(v11n, revC22V21, 0, 9))
        assertEquals(rangeGenC1V1to31, RestrictionType.NONE.blur(v11n, genC1V1, 0, 30))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 30))
        assertEquals(rangeGenC1V0to31, RestrictionType.NONE.blur(v11n, genC1V1, 1, 30))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 1, 30))
        assertEquals(rangeGenC1V1to31, RestrictionType.NONE.blur(v11n, genC1V1, 0, 30))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 9, 30))
        assertEquals(rangeGenC1V1to33, RestrictionType.NONE.blur(v11n, genC1V1, 0, 32))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 32))
        assertEquals(rangeGenC1V0to34, RestrictionType.NONE.blur(v11n, genC1V1, 1, 32))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 1, 32))
        assertEquals(rangeGenC1V1to33, RestrictionType.NONE.blur(v11n, genC1V1, 0, 32))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 9, 32))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 1581))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 3, 1581))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 9, 1581))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 1533))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 1, 1584))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 2, 1584))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 32356))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 32356))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 32356))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 1, 32356))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 32356))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 9, 32356))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 32357))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 32357))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 32357))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 1, 32357))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 32357))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 9, 32357))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 99999))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 0, 99999))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 99999))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 1, 99999))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, genC1V1, 0, 99999))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, genC1V1, 9, 99999))
        assertEquals(rangeGenC1V12, RestrictionType.CHAPTER.blur(v11n, genC1V2, 1, 0))
        assertEquals(rangeGenC1V12, RestrictionType.NONE.blur(v11n, genC1V2, 1, 0))
        assertEquals(rangeGenC1V02, RestrictionType.CHAPTER.blur(v11n, genC1V2, 9, 0))
        assertEquals(rangeGenC1V12, RestrictionType.NONE.blur(v11n, genC1V2, 1, 0))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, revC22V21, 21, 0))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.NONE.blur(v11n, revC22V21, 21, 0))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, revC22V21, 21, 1))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.NONE.blur(v11n, revC22V21, 21, 1))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, revC22V21, 21, 9))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.NONE.blur(v11n, revC22V21, 21, 9))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, revC22V21, 425, 0))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, revC22V21, 427, 1))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, revC22V21, 427, 9))
        assertEquals(rangeGenC0V0toEnd, RestrictionType.NONE.blur(v11n, genC1V1, 2, 1581))
        assertEquals(rangeGenC0V0toEnd, RestrictionType.NONE.blur(v11n, genC1V1, 2, 1581))
        assertEquals(rangeGenC0V0toExoC1V1, RestrictionType.NONE.blur(v11n, genC1V1, 2, 1584))
        assertEquals(rangeGenC0V0toExoC1V1, RestrictionType.NONE.blur(v11n, genC1V1, 2, 1584))
        assertEquals(rangeRevC0V0toEnd, RestrictionType.NONE.blur(v11n, revC22V21, 426, 0))
        assertEquals(rangeRevC0V0toEnd, RestrictionType.NONE.blur(v11n, revC22V21, 426, 1))
        assertEquals(rangeRevC0V0toEnd, RestrictionType.NONE.blur(v11n, revC22V21, 426, 9))
    }

    @Test
    fun testNewViaVerseRangeIntIntBoolean() {
        assertEquals(rangeGenC1V1, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 0))
        assertEquals(rangeGenC1V1, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 0))
        assertEquals(rangeGenC1V12, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 1))
        assertEquals(rangeGenC1V12, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 1))
        assertEquals(rangeGenC1V01, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 1, 0))
        assertEquals(rangeGenC1V01, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 1, 0))
        assertEquals(rangeGenC1V01, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 9, 0))
        assertEquals(rangeRevC22V21, RestrictionType.CHAPTER.blur(v11n, rangeRevC22V21, 0, 1))
        assertEquals(rangeRevC22V21, RestrictionType.NONE.blur(v11n, rangeRevC22V21, 0, 1))
        assertEquals(rangeRevC22V21, RestrictionType.CHAPTER.blur(v11n, rangeRevC22V21, 0, 9))
        assertEquals(rangeRevC22V21, RestrictionType.NONE.blur(v11n, rangeRevC22V21, 0, 9))
        assertEquals(rangeGenC1V1to31, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 30))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 30))
        assertEquals(rangeGenC1V0to31, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 1, 30))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 1, 30))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 9, 30))
        assertEquals(rangeGenC1V1to33, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 32))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 33))
        assertEquals(rangeGenC1V0to34, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 1, 32))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 1, 31))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 9, 31))
        assertEquals(rangeGenC0V0toEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 2, 1581))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 1581))
        assertEquals(rangeGenC0V0toEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 2, 1581))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 1, 1581))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 9, 1581))
        assertEquals(rangeGenC0V0toExoC1V1, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 2, 1584))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 1584))
        assertEquals(rangeGenC0V0toExoC1V1, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 2, 1584))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 1, 1584))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 9, 1584))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 32356))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 32356))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 32356))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 1, 32356))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 32356))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 9, 32356))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 32357))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 32357))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 32357))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 1, 32357))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 32357))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 9, 32357))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 99999))
        assertEquals(rangeGenC1V1to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 0, 99999))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 99999))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 1, 99999))
        assertEquals(rangeGenC1V1toRevEnd, RestrictionType.NONE.blur(v11n, rangeGenC1V1, 0, 99999))
        assertEquals(rangeGenC1V0to31, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V1, 9, 99999))
        assertEquals(rangeGenC1V12, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V2, 1, 0))
        assertEquals(rangeGenC1V12, RestrictionType.NONE.blur(v11n, rangeGenC1V2, 1, 0))
        assertEquals(rangeGenC1V02, RestrictionType.CHAPTER.blur(v11n, rangeGenC1V2, 9, 0))
        assertEquals(rangeGenC1V02, RestrictionType.NONE.blur(v11n, rangeGenC1V2, 2, 0))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, rangeRevC22V21, 21, 0))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.NONE.blur(v11n, rangeRevC22V21, 21, 0))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, rangeRevC22V21, 21, 1))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.NONE.blur(v11n, rangeRevC22V21, 21, 1))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, rangeRevC22V21, 21, 9))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.NONE.blur(v11n, rangeRevC22V21, 21, 9))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, rangeRevC22V21, 426, 0))
        assertEquals(rangeRevC0V0toEnd, RestrictionType.NONE.blur(v11n, rangeRevC22V21, 426, 0))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, rangeRevC22V21, 426, 1))
        assertEquals(rangeRevC0V0toEnd, RestrictionType.NONE.blur(v11n, rangeRevC22V21, 426, 1))
        assertEquals(rangeRevC22V0toEnd, RestrictionType.CHAPTER.blur(v11n, rangeRevC22V21, 426, 9))
        assertEquals(rangeRevC0V0toEnd, RestrictionType.NONE.blur(v11n, rangeRevC22V21, 426, 9))
    }

    @Test
    fun testNewViaVerseRangeVerseRange() {
        assertEquals(rangeGenC1V1toRevEnd, VerseRange(rangeGenC1V1, rangeRevC22V0toEnd))
        assertEquals(rangeGenC1V1toRevEnd, VerseRange(rangeGenC1V1toRevEnd, rangeRevC22V0toEnd))
        assertEquals(rangeGenC1V1toRevEnd, VerseRange(rangeRevC0V0toEnd, rangeGenC1V1toRevEnd))
        assertEquals(rangeGenC1V1toRevEnd, VerseRange(rangeGenC1V1toRevEnd, rangeGenC1V1toRevEnd))
        assertFailsWith<NullPointerException> {
            VerseRange(rangeGenC1V1toRevEnd, null)
        }
        assertFailsWith<NullPointerException> {
            VerseRange((null as VerseRange?), rangeGenC1V1toRevEnd)
        }
        assertFailsWith<NullPointerException> {
            VerseRange((null as VerseRange?), (null as VerseRange?))
        }
    }

    @Test
    fun testGetName() {
        assertEquals("Gen 1:1", rangeGenC1V1.getName())
        assertEquals("Gen 1:1-2", rangeGenC1V12.getName())
        assertEquals("Gen 1", rangeGenC1V1to31.getName())
        assertEquals("Gen 1:1-2:1", rangeGenC1V1to33.getName())
        assertEquals("Gen 1:2", rangeGenC1V2.getName())
        assertEquals("Gen", rangeGenC0V0toEnd.getName())
        assertEquals("Gen-Exo 1:1", rangeGenC0V0toExoC1V1.getName())
        assertEquals("Gen-Rev", rangeGenC1V1toRevEnd.getName())
        assertEquals("Rev 22", rangeRevC22V0toEnd.getName())
        assertEquals("Rev", rangeRevC0V0toEnd.getName())
        assertEquals("Rev 22:21", rangeRevC22V21.getName())
    }

    @Test
    fun testGetNameVerse() {
        assertEquals("1-2", rangeGenC1V12.getName(genC1V1))
        assertEquals("2", rangeGenC1V2.getName(genC1V1))
        assertEquals("Rev 22", rangeRevC22V0toEnd.getName(genC1V1))
        assertEquals("Rev 22", rangeRevC22V0toEnd.getName(null))
    }

    @Test
    fun testGetStart() {
        assertEquals(genC1V1, rangeGenC1V1.start)
        assertEquals(genC1V1, rangeGenC1V12.start)
        assertEquals(genC1V1, rangeGenC1V1to31.start)
        assertEquals(genC1V1, rangeGenC1V1to33.start)
        assertEquals(genC1V2, rangeGenC1V2.start)
        assertEquals(genC0V0, rangeGenC0V0toEnd.start)
        assertEquals(genC0V0, rangeGenC0V0toExoC1V1.start)
        assertEquals(genC1V1, rangeGenC1V1toRevEnd.start)
        assertEquals(revC22V0, rangeRevC22V0toEnd.start)
        assertEquals(revC0V0, rangeRevC0V0toEnd.start)
        assertEquals(revC22V21, rangeRevC22V21.start)
    }

    @Test
    fun testGetEnd() {
        assertEquals(genC1V1, rangeGenC1V1.end)
        assertEquals(genC1V2, rangeGenC1V12.end)
        assertEquals(genC1V31, rangeGenC1V1to31.end)
        assertEquals(genC2V1, rangeGenC1V1to33.end)
        assertEquals(genC1V2, rangeGenC1V2.end)
        assertEquals(genC50V26, rangeGenC0V0toEnd.end)
        assertEquals(exoC1V1, rangeGenC0V0toExoC1V1.end)
        assertEquals(revC22V21, rangeGenC1V1toRevEnd.end)
        assertEquals(revC22V21, rangeRevC22V0toEnd.end)
        assertEquals(revC22V21, rangeRevC0V0toEnd.end)
        assertEquals(revC22V21, rangeRevC22V21.end)
    }

    @Test
    fun testGetVerseCount() {
        assertEquals(1, rangeGenC1V1.getCardinality())
        assertEquals(2, rangeGenC1V12.getCardinality())
        assertEquals(31, rangeGenC1V1to31.getCardinality())
        assertEquals(33, rangeGenC1V1to33.getCardinality())
        assertEquals(1, rangeGenC1V2.getCardinality())
        assertEquals(1584, rangeGenC0V0toEnd.getCardinality())
        assertEquals(1587, rangeGenC0V0toExoC1V1.getCardinality())
        assertEquals(32356, rangeGenC1V1toRevEnd.getCardinality())
        assertEquals(22, rangeRevC22V0toEnd.getCardinality())
        assertEquals(427, rangeRevC0V0toEnd.getCardinality())
        assertEquals(1, rangeRevC22V21.getCardinality())
    }

    @Test
    fun testClone() {
        assertTrue(rangeGenC1V1 !== rangeGenC1V1.clone())
        assertTrue(rangeGenC1V1.equals(rangeGenC1V1.clone()))
        assertTrue(rangeRevC22V21 !== rangeRevC22V21.clone())
        assertTrue(rangeRevC22V21.equals(rangeRevC22V21.clone()))
    }

    @Test
    fun testCompareTo() {
        assertTrue(rangeRevC22V21.compareTo(rangeGenC1V1) > 0)
        assertTrue(rangeGenC1V1.compareTo(rangeRevC22V21) < 0)
        assertTrue(rangeGenC1V1.compareTo(rangeGenC1V1) == 0)
        assertTrue(rangeGenC1V1.compareTo(rangeGenC1V12) < 0)
        assertTrue(rangeGenC1V1.compareTo(rangeGenC1V1to31) < 0)
        assertTrue(rangeGenC1V1.compareTo(rangeGenC1V1to33) < 0)
        assertTrue(rangeGenC1V1.compareTo(rangeGenC0V0toEnd) > 0)
        assertTrue(rangeGenC1V2.compareTo(rangeGenC1V1) > 0)
        assertTrue(rangeGenC1V2.compareTo(rangeGenC1V12) > 0)
        assertTrue(rangeGenC1V2.compareTo(rangeGenC1V1toRevEnd) > 0)
        assertTrue(rangeGenC1V2.compareTo(rangeGenC1V2) == 0)
        assertTrue(rangeGenC1V2.compareTo(rangeRevC22V21) < 0)
        assertTrue(rangeGenC1V2.compareTo(rangeRevC22V0toEnd) < 0)
        assertFailsWith<NullPointerException> {
            rangeGenC1V2.compareTo(null)
        }
    }

    @Test
    fun testAdjacentTo(){
        assertTrue(!rangeGenC1V1.adjacentTo(rangeRevC22V0toEnd))
        assertTrue(!rangeGenC1V1.adjacentTo(rangeRevC0V0toEnd))
        assertTrue(!rangeGenC1V2.adjacentTo(rangeRevC22V0toEnd))
        assertTrue(!rangeRevC22V21.adjacentTo(rangeGenC1V1))
        assertTrue(rangeGenC1V1.adjacentTo(rangeGenC1V2))
        assertTrue(rangeGenC0V0toEnd.adjacentTo(rangeGenC0V0toExoC1V1))
        assertTrue(rangeGenC0V0toEnd.adjacentTo(rangeGenC1V1toRevEnd))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:10-11")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:12-13")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:10-12")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:11-13")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:10-13")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:11-12")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:10-13")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:10-13")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:11-12")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:10-13")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:11-13")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:10-12")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:12-13")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:10-11")))
        assertTrue(!VerseRangeFactory.fromString(v11n, "Gen 1:10-11")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:13-14")))
        assertTrue(!VerseRangeFactory.fromString(v11n, "Gen 1:13-14")!!.adjacentTo(VerseRangeFactory.fromString(v11n, "Gen 1:10-11")))
        assertFailsWith<NullPointerException> {
            rangeGenC0V0toEnd.adjacentTo(null)
        }
    }

    @Test
    fun testOverlaps(){
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:10-11")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:11-12")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:10-12")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:11-13")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:10-13")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:11-12")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:10-13")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:10-13")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:11-12")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:10-13")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:11-13")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:10-12")))
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:11-12")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:10-11")))
        assertTrue(!VerseRangeFactory.fromString(v11n, "Gen 1:10-11")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:12-13")))
        assertTrue(!VerseRangeFactory.fromString(v11n, "Gen 1:12-13")!!.overlaps(VerseRangeFactory.fromString(v11n, "Gen 1:10-11")))
        assertFailsWith<NullPointerException> {
            rangeGenC0V0toEnd.overlaps(null)
        }
    }

    @Test
    fun testContainsVerse() {
        assertTrue(rangeGenC0V0toEnd.contains(genC1V1))
        assertTrue(rangeGenC0V0toEnd.contains(genC1V2))
        assertTrue(rangeGenC0V0toEnd.contains(genC50V26))
        assertTrue(rangeGenC1V1.contains(genC1V1))
        assertTrue(rangeGenC1V12.contains(genC1V1))
        assertTrue(rangeGenC1V12.contains(genC1V2))
        assertTrue(rangeGenC1V2.contains(genC1V2))
        assertTrue(rangeRevC22V0toEnd.contains(revC22V1))
        assertTrue(rangeRevC22V0toEnd.contains(revC22V21))
        assertTrue(!rangeRevC22V0toEnd.contains(genC1V1))
        assertTrue(!rangeGenC1V1.contains(genC1V2))
        assertTrue(!rangeGenC0V0toEnd.contains(exoC1V1))
        assertTrue(!rangeRevC22V0toEnd.contains(revC1V1))
        assertFailsWith<NullPointerException> {
            rangeGenC0V0toEnd.contains((null as Verse?))
        }
    }

    @Test
    fun testContainsVerseRange() {
        assertTrue(rangeGenC0V0toEnd.contains(rangeGenC1V1))
        assertTrue(rangeGenC0V0toEnd.contains(rangeGenC1V12))
        assertTrue(rangeGenC0V0toEnd.contains(rangeGenC1V2))
        assertTrue(rangeGenC1V1.contains(rangeGenC1V1))
        assertTrue(rangeGenC1V12.contains(rangeGenC1V1))
        assertTrue(!rangeGenC1V12.contains(rangeGenC0V0toEnd))
        assertTrue(!rangeGenC1V2.contains(rangeGenC1V12))
        assertTrue(rangeRevC22V0toEnd.contains(rangeRevC22V0toEnd))
        assertTrue(!rangeRevC22V0toEnd.contains(rangeGenC0V0toEnd))
        assertTrue(rangeRevC22V0toEnd.contains(rangeRevC22V21))
        assertTrue(!rangeGenC1V1.contains(rangeRevC22V0toEnd))
        assertTrue(rangeGenC0V0toEnd.contains(rangeGenC1V2))
        assertTrue(!rangeRevC22V0toEnd.contains(rangeRevC0V0toEnd))
        assertFailsWith<NullPointerException> {
            rangeGenC0V0toEnd.contains((null as VerseRange?))
        }
    }

    @Test
    fun testIsChapter(){
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1")!!.isWholeChapter)
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:0-ff")!!.isWholeChapter)
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 1:0-$")!!.isWholeChapter)
        assertTrue(VerseRangeFactory.fromString(v11n, "Exo 2")!!.isWholeChapter)
        assertTrue(VerseRangeFactory.fromString(v11n, "Exo 2:0-ff")!!.isWholeChapter)
        assertTrue(VerseRangeFactory.fromString(v11n, "Exo 2:0-$")!!.isWholeChapter)
        assertTrue(!VerseRangeFactory.fromString(v11n, "Num 3:1")!!.isWholeChapter)
        assertTrue(!VerseRangeFactory.fromString(v11n, "Num 4:1-5:1")!!.isWholeChapter)
        assertTrue(!VerseRangeFactory.fromString(v11n, "Num 5:1-6:ff")!!.isWholeChapter)
        assertTrue(!VerseRangeFactory.fromString(v11n, "Lev")!!.isWholeChapter)
    }

    @Test
    fun testIsBook(){
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen")!!.isWholeBook)
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 0:0-Gen 50:ff")!!.isWholeBook)
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 0:0-Gen 50:$")!!.isWholeBook)
        assertTrue(VerseRangeFactory.fromString(v11n, "Gen 0-50:ff")!!.isWholeBook)
        assertTrue(!VerseRangeFactory.fromString(v11n, "Num 1:2-Num $:$")!!.isWholeBook)
        assertTrue(!VerseRangeFactory.fromString(v11n, "Num 4:1-5:1")!!.isWholeBook)
        assertTrue(!VerseRangeFactory.fromString(v11n, "Num 5:1-6:ff")!!.isWholeBook)
        assertTrue(!VerseRangeFactory.fromString(v11n, "Lev-Deu 1:1")!!.isWholeBook)
    }

    @Test
    fun testToVerseArray() {
        assertEquals(1, rangeGenC1V1.toVerseArray().size)
        assertEquals(2, rangeGenC1V12.toVerseArray().size)
        assertEquals(31, rangeGenC1V1to31.toVerseArray().size)
        assertEquals(33, rangeGenC1V1to33.toVerseArray().size)
        assertEquals(1, rangeGenC1V2.toVerseArray().size)
        assertEquals(1584, rangeGenC0V0toEnd.toVerseArray().size)
        assertEquals(1587, rangeGenC0V0toExoC1V1.toVerseArray().size)
        assertEquals(427, rangeRevC0V0toEnd.toVerseArray().size)
        assertEquals(genC1V1, rangeGenC1V1.toVerseArray()[0])
        assertEquals(genC1V1, rangeGenC1V12.toVerseArray()[0])
        assertEquals(genC1V2, rangeGenC1V12.toVerseArray()[1])
        assertEquals(genC1V1, rangeGenC1V1to31.toVerseArray()[0])
        assertEquals(genC1V31, rangeGenC1V1to31.toVerseArray()[30])
        assertEquals(genC1V2, rangeGenC1V2.toVerseArray()[0])
        assertEquals(genC1V1, rangeGenC0V0toEnd.toVerseArray()[2])
        assertEquals(genC1V1, rangeGenC0V0toExoC1V1.toVerseArray()[2])
        assertEquals(exoC1V1, rangeGenC0V0toExoC1V1.toVerseArray()[1586])
        assertEquals(revC1V1, rangeRevC0V0toEnd.toVerseArray()[2])
        assertEquals(revC1V2, rangeRevC0V0toEnd.toVerseArray()[3])
        assertEquals(revC22V21, rangeRevC0V0toEnd.toVerseArray()[426])
    }

    @Test
    fun testVerseElements() {
        var it: Iterator<Key> = rangeGenC1V1.iterator()
        while (it.hasNext()) {
            assertTrue(it.hasNext())
            var v: Verse = it.next() as Verse
            assertEquals(genC1V1, v)
            assertTrue(!it.hasNext())
        }
        it = rangeGenC1V12.iterator()
        while (it.hasNext()) {
            assertTrue(it.hasNext())
            var v: Verse = it.next() as Verse
            assertEquals(genC1V1, v)
            assertTrue(it.hasNext())
            v = it.next() as Verse
            assertEquals(genC1V2, v)
            assertTrue(!it.hasNext())
        }
    }

    /**
     * Test fix related to JS-274 to ensure key.contains(verse) works correctly
     */
    @Test
    fun testKeyContainsVerse() {
        // this passes
        assertTrue(rangeGenC0V0toEnd.contains(genC1V1))
        // this fails
        var genAllKey: Key = rangeGenC0V0toEnd
        assertTrue(genAllKey.contains(genC1V1))
    }

    @Test
    fun testIntersection() {
        assertTrue(VerseRange.intersection(rangeGenC0V0toEnd, rangeGenC0V0toExoEnd)!!.contains(rangeGenC0V0toEnd))
    }
}
