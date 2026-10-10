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
class VerseTest {

    private lateinit var v11n: Versification
    private lateinit var gen00: Verse
    private lateinit var gen10: Verse
    private lateinit var gen11: Verse
    private lateinit var gen11a: Verse
    private lateinit var gen11s: Verse
    private lateinit var gen12: Verse
    private lateinit var gen20: Verse
    private lateinit var gen21: Verse
    private lateinit var gen22: Verse
    private lateinit var rev11: Verse
    private lateinit var rev12: Verse
    private lateinit var rev21: Verse
    private lateinit var rev22: Verse
    private lateinit var rev99: Verse
    private lateinit var jude0: Verse
    private lateinit var jude1: Verse
    private lateinit var jude2: Verse
    private lateinit var jude9: Verse
    private lateinit var ssa00: Verse
    private lateinit var ssa10: Verse
    private lateinit var ssa11: Verse
    private var fullName: Boolean = false

    @BeforeTest
    fun setUp(){
        fullName = BookName.isFullBookName()
        BookName.setFullBookName(false)
        // AV11N(DMS): Update test to test all V11Ns
        v11n = Versifications.instance().getVersification("KJV")!!
        gen00 = Verse(v11n, BibleBook.GEN, 0, 0)
        gen10 = Verse(v11n, BibleBook.GEN, 1, 0)
        gen11 = Verse(v11n, BibleBook.GEN, 1, 1)
        gen11a = Verse(v11n, BibleBook.GEN, 1, 1)
        gen11s = Verse(v11n, BibleBook.GEN, 1, 1, "sub")
        gen12 = Verse(v11n, BibleBook.GEN, 1, 2)
        gen20 = Verse(v11n, BibleBook.GEN, 2, 0)
        gen21 = Verse(v11n, BibleBook.GEN, 2, 1)
        gen22 = Verse(v11n, BibleBook.GEN, 2, 2)
        rev11 = Verse(v11n, BibleBook.REV, 1, 1)
        rev12 = Verse(v11n, BibleBook.REV, 1, 2)
        rev21 = Verse(v11n, BibleBook.REV, 2, 1)
        rev22 = Verse(v11n, BibleBook.REV, 2, 2)
        rev99 = Verse(v11n, BibleBook.REV, 22, 21)
        jude0 = Verse(v11n, BibleBook.JUDE, 0, 0)
        jude1 = Verse(v11n, BibleBook.JUDE, 1, 1)
        jude2 = Verse(v11n, BibleBook.JUDE, 1, 2)
        jude9 = Verse(v11n, BibleBook.JUDE, 1, 25)
        ssa00 = Verse(v11n, BibleBook.SAM2, 0, 0)
        ssa10 = Verse(v11n, BibleBook.SAM2, 1, 0)
        ssa11 = Verse(v11n, BibleBook.SAM2, 1, 1)
    }

    @AfterTest
    fun tearDown() {
        BookName.setFullBookName(fullName)
    }

   @Test
    fun testNewViaString(){
       assertEquals(gen11s, VerseFactory.fromString(v11n, "Gen.1.1!sub"))
        assertEquals(Verse.DEFAULT, gen11)
        assertEquals(gen11, VerseFactory.fromString(v11n, "Genesis 1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "Gen 1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "Ge 1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "genesis 1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "genesi 1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "GENESIS 1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "GENESI 1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "ge 1 1"))
        assertEquals(gen10, VerseFactory.fromString(v11n, "ge 1"))
        assertEquals(gen00, VerseFactory.fromString(v11n, "ge"))
        assertEquals(gen00, VerseFactory.fromString(v11n, "gen"))
        assertEquals(rev99, VerseFactory.fromString(v11n, "rev 22 21"))
        assertEquals(rev99, VerseFactory.fromString(v11n, "REVE 22 21"))
        assertEquals(rev99, VerseFactory.fromString(v11n, "REVELATIONS 22 21"))
        assertEquals(gen20, VerseFactory.fromString(v11n, "ge 2"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "ge.1.1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "ge 1.1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "ge.1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "ge.1:1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "ge:1:1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "ge:1 1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, " ge 1 1 "))
        assertEquals(gen10, VerseFactory.fromString(v11n, "gen1"))
        assertEquals(gen10, VerseFactory.fromString(v11n, "GEN1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "GENESIS1:1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, "GE1    1"))
        assertEquals(gen11, VerseFactory.fromString(v11n, " GEN  1  1  "))
        assertEquals(gen11, VerseFactory.fromString(v11n, " gen 1 1 "))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "2Sa 1:1"))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "2Sa 1 1"))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "2Sa1 1"))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "2 Sa 1 1"))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "2 Sa1 1"))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "2Sa1:1"))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "2 Sa 1 1"))
        assertEquals(ssa10, VerseFactory.fromString(v11n, "  2  Sa  1  "))
        assertEquals(ssa00, VerseFactory.fromString(v11n, "  2  Sa  "))
        assertEquals(ssa10, VerseFactory.fromString(v11n, "  2  Sa1  "))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "  2  Sa1  1  "))
        assertEquals(ssa11, VerseFactory.fromString(v11n, "  2 : Sa1  1  "))
        assertEquals(rev99, VerseFactory.fromString(v11n, "Rev 22:$"))
        assertEquals(rev99, VerseFactory.fromString(v11n, " Rev 22 ff "))
        assertEquals(rev99, VerseFactory.fromString(v11n, "  Rev  22  ff  "))
        assertEquals(rev99, VerseFactory.fromString(v11n, "  Rev  22  $  "))
        assertEquals(jude0, VerseFactory.fromString(v11n, "Jude"))
        assertEquals(jude9, VerseFactory.fromString(v11n, "Jude $"))
        assertEquals(jude9, VerseFactory.fromString(v11n, " Jude  $ "))
        assertEquals(jude9, VerseFactory.fromString(v11n, "Jude ff"))
        assertEquals(jude9, VerseFactory.fromString(v11n, "  Jude  ff  "))
        assertEquals(VerseFactory.fromString(v11n, "Deu 1:1"), VerseFactory.fromString(v11n, "Dt 1:1"))
        assertEquals(VerseFactory.fromString(v11n, "Mat 1:1"), VerseFactory.fromString(v11n, "Mt 1:1"))
        assertFailsWith<NoSuchVerseException> {
            VerseFactory.fromString(v11n, "gen 1 1 1")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseFactory.fromString(v11n, "gen.1.1.1")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseFactory.fromString(v11n, "gen.1.1:1")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseFactory.fromString(v11n, "gen 1 1 1")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseFactory.fromString(v11n, "gen 1 1 1")
        }
        assertFailsWith<NoSuchVerseException> {
            VerseFactory.fromString(v11n, "gen 1 1 1")
        }
        assertFailsWith<NullPointerException> { VerseFactory.fromString(v11n, null) }
        assertEquals(jude1, VerseFactory.fromString(v11n, "jude 1"))
        assertEquals(jude2, VerseFactory.fromString(v11n, "jude 2"))
        assertEquals(jude9, VerseFactory.fromString(v11n, "jude 25"))
    }

    @Test
    fun testGetName(){
        assertEquals("Gen 1:1", VerseFactory.fromString(v11n, "Genesis 1 1")!!.getName())
        assertEquals("Gen 1:1", VerseFactory.fromString(v11n, "Gen 1 1")!!.getName())
        assertEquals("Gen 1:1", VerseFactory.fromString(v11n, "Genesis 1:1")!!.getName())
        assertEquals("Gen 1:1", VerseFactory.fromString(v11n, "Gen 1 1")!!.getName())
        assertEquals("Gen 1:1", VerseFactory.fromString(v11n, "ge 1 1")!!.getName())
        assertEquals("Gen 0:0", VerseFactory.fromString(v11n, "ge")!!.getName())
        assertEquals("Gen 1:1", VerseFactory.fromString(v11n, "Ge:1:1")!!.getName())
        assertEquals("Jude 1", VerseFactory.fromString(v11n, "Jude 1")!!.getName())
        assertEquals("Jude 0", VerseFactory.fromString(v11n, "Jude")!!.getName())
        assertEquals("Jude 1", VerseFactory.fromString(v11n, "Jude 1:1")!!.getName())
    }

    @Test
    fun testGetNameVerse(){
        assertEquals("2", VerseFactory.fromString(v11n, "Gen 1:2")!!.getName(gen11))
        assertEquals("2:1", VerseFactory.fromString(v11n, "Gen 2:1")!!.getName(gen11))
        assertEquals("Gen 2:1", VerseFactory.fromString(v11n, "Gen 2:1")!!.getName(jude9))
        assertEquals("Gen 2:1", VerseFactory.fromString(v11n, "Gen 2:1")!!.getName(null))
    }

    @Test
    fun testNewViaIntIntIntBoolean() {
        assertEquals(gen00, Verse(v11n, null, 1, 1, true))
        assertEquals(gen10, Verse(v11n, BibleBook.GEN, 0, 1, true))
        assertEquals(gen10, Verse(v11n, BibleBook.GEN, 1, 0, true))
        assertEquals(rev99, Verse(v11n, BibleBook.REV, 22, 22, true))
        assertEquals(rev99, Verse(v11n, BibleBook.REV, 23, 21, true))
        assertEquals(rev99, Verse(v11n, BibleBook.REV, 23, 22, true))
        assertEquals(rev99, Verse(v11n, BibleBook.GEN, 999999, 0, true))
        assertEquals(rev99, Verse(v11n, BibleBook.GEN, 0, 999999, true))
    }

    @Test
    fun testClone() {
        assertEquals(gen11, gen11.clone())
        assertEquals(gen11, gen11.clone())
        assertEquals(rev99, rev99.clone())
        assertEquals(rev99, rev99.clone())
    }

    @Test
    fun testEquals() {
        assertTrue(!gen11.equals(null))
        assertTrue(!gen11.equals(0))
        assertTrue(!gen11.equals("org.crosswire.jsword.passage.Verse"))
        assertTrue(gen11.equals(gen11a))
        assertTrue(!gen11.equals(gen12))
        assertTrue(!gen11.equals(rev99))
        assertTrue(!gen11.equals(gen12))
    }

    @Test
    fun testHashCode() {
        assertEquals(gen11.hashCode(), gen11a.hashCode())
        assertTrue(gen11.hashCode() != 0)
    }

    @Test
    fun testCompareTo() {
        assertTrue(gen11.compareTo(rev99) < 0)
        assertTrue(rev99.compareTo(gen11) > 0)
        assertEquals(0, gen11.compareTo(gen11))
    }

    @Test
    fun testAddSubtract() {
        assertEquals(1, v11n.distance(gen11, gen12))
        assertEquals(0, v11n.distance(gen11, gen11))
        assertEquals(-1, v11n.distance(gen12, gen11))
        var last: Verse = gen11.clone()
        for (i in 0 until v11n.maximumOrdinal() step 99) {
            var next: Verse = v11n.add(last, i)!!
            assertEquals(v11n.distance(last, next), i)
            var next2: Verse = v11n.subtract(next, i)!!
            assertEquals(gen11, next2, gen11.getOsisID())
        }
        assertEquals(gen11, v11n.subtract(gen11, 0), gen11.getOsisID())
        assertEquals(gen10, v11n.subtract(gen11, 1), gen11.getOsisID())
        assertEquals(gen00, v11n.subtract(gen11, 2), gen11.getOsisID())
        assertEquals(gen11, v11n.add(gen11, 0), gen11.getOsisID())
        assertEquals(rev99, v11n.add(rev99, 0), rev99.getOsisID())
        assertEquals(rev99, v11n.add(rev99, 1), rev99.getOsisID())
        assertEquals(rev99, v11n.add(rev99, 2), rev99.getOsisID())
    }

    @Test
    fun testToString() {
        assertEquals("Gen 1:1", gen11.toString())
        assertEquals("Gen 1:2", gen12.toString())
        assertEquals("Gen 2:1", gen21.toString())
        assertEquals("Gen 2:2", gen22.toString())
        assertEquals("Rev 1:1", rev11.toString())
        assertEquals("Rev 1:2", rev12.toString())
        assertEquals("Rev 2:1", rev21.toString())
        assertEquals("Rev 2:2", rev22.toString())
        assertEquals("Rev 22:21", rev99.toString())
    }

    @Test
    fun testGetBook() {
        assertEquals(BibleBook.GEN, gen11.book)
        assertEquals(BibleBook.GEN, gen12.book)
        assertEquals(BibleBook.GEN, gen21.book)
        assertEquals(BibleBook.GEN, gen22.book)
        assertEquals(BibleBook.REV, rev11.book)
        assertEquals(BibleBook.REV, rev12.book)
        assertEquals(BibleBook.REV, rev21.book)
        assertEquals(BibleBook.REV, rev22.book)
        assertEquals(BibleBook.REV, rev99.book)
    }

    @Test
    fun testGetChapter() {
        assertEquals(1, gen11.chapter)
        assertEquals(1, gen12.chapter)
        assertEquals(2, gen21.chapter)
        assertEquals(2, gen22.chapter)
        assertEquals(1, rev11.chapter)
        assertEquals(1, rev12.chapter)
        assertEquals(2, rev21.chapter)
        assertEquals(2, rev22.chapter)
        assertEquals(22, rev99.chapter)
    }

    @Test
    fun testGetVerse() {
        assertEquals(1, gen11.verse)
        assertEquals(2, gen12.verse)
        assertEquals(1, gen21.verse)
        assertEquals(2, gen22.verse)
        assertEquals(1, rev11.verse)
        assertEquals(2, rev12.verse)
        assertEquals(1, rev21.verse)
        assertEquals(2, rev22.verse)
        assertEquals(21, rev99.verse)
    }

    @Test
    fun testGetOrdinal() {
        assertEquals(4, gen11.ordinal)
        assertEquals(5, gen12.ordinal)
        assertEquals(36, gen21.ordinal)
        assertEquals(37, gen22.ordinal)
        assertEquals(31935, rev11.ordinal)
        assertEquals(31936, rev12.ordinal)
        assertEquals(31956, rev21.ordinal)
        assertEquals(31957, rev22.ordinal)
        assertEquals(32359, rev99.ordinal)
    }

    @Test
    fun testGetAccuracy(){
        var vr: VerseRange = VerseRange(v11n, gen11, gen11)
        assertEquals(AccuracyType.BOOK_VERSE, AccuracyType.fromText(v11n, "Gen 1:1", AccuracyType.tokenize("Gen 1:1"), vr))
        assertEquals(AccuracyType.BOOK_CHAPTER, AccuracyType.fromText(v11n, "Gen 1", AccuracyType.tokenize("Gen 1"), vr))
        assertEquals(AccuracyType.BOOK_VERSE, AccuracyType.fromText(v11n, "Jude 1", AccuracyType.tokenize("Jude 1"), vr))
        assertEquals(AccuracyType.BOOK_VERSE, AccuracyType.fromText(v11n, "Jude 1:1", AccuracyType.tokenize("Jude 1:1"), vr))
        assertEquals(AccuracyType.BOOK_ONLY, AccuracyType.fromText(v11n, "Gen", AccuracyType.tokenize("Gen"), vr))
        assertEquals(AccuracyType.CHAPTER_VERSE, AccuracyType.fromText(v11n, "1:1", AccuracyType.tokenize("1:1"), vr))
        assertEquals(AccuracyType.VERSE_ONLY, AccuracyType.fromText(v11n, "1", AccuracyType.tokenize("1"), vr))
        assertFailsWith<NoSuchVerseException> {
            AccuracyType.fromText(v11n, "Komplete and utter rubbish", AccuracyType.tokenize("Komplete and utter rubbish"), vr)
        }
        assertFailsWith<NoSuchVerseException> {
            AccuracyType.fromText(v11n, "x 1 1", AccuracyType.tokenize("x 1 1"), vr)
        }
    }

    @Test
    fun testIsStartEndOfChapterBook(){
        assertTrue(v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen 1:1")))
        assertTrue(!v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen 1:10")))
        assertTrue(!v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen 1:$")))
        assertTrue(v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen 10:0")))
        assertTrue(!v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen 10:10")))
        assertTrue(!v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen 10:$")))
        assertTrue(v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen $:0")))
        assertTrue(!v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen $:10")))
        assertTrue(!v11n.isStartOfChapter(VerseFactory.fromString(v11n, "Gen $:$")))
        assertTrue(!v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen 1:1")))
        assertTrue(!v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen 1:10")))
        assertTrue(v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen 1:$")))
        assertTrue(!v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen 10:1")))
        assertTrue(!v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen 10:10")))
        assertTrue(v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen 10:$")))
        assertTrue(!v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen $:1")))
        assertTrue(!v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen $:10")))
        assertTrue(v11n.isEndOfChapter(VerseFactory.fromString(v11n, "Gen $:$")))
        assertTrue(v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen 0:0")))
        assertTrue(!v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen 1:10")))
        assertTrue(!v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen 1:$")))
        assertTrue(!v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen 10:1")))
        assertTrue(!v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen 10:10")))
        assertTrue(!v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen 10:$")))
        assertTrue(!v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen $:1")))
        assertTrue(!v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen $:10")))
        assertTrue(!v11n.isStartOfBook(VerseFactory.fromString(v11n, "Gen $:$")))
        assertTrue(!v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen 1:1")))
        assertTrue(!v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen 1:10")))
        assertTrue(!v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen 1:$")))
        assertTrue(!v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen 10:1")))
        assertTrue(!v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen 10:10")))
        assertTrue(!v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen 10:$")))
        assertTrue(!v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen $:1")))
        assertTrue(!v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen $:10")))
        assertTrue(v11n.isEndOfBook(VerseFactory.fromString(v11n, "Gen $:$")))
    }

    @Test
    fun testMax() {
        assertEquals(v11n.max(gen11, gen12), gen12)
        assertEquals(v11n.max(gen11, rev99), rev99)
        assertEquals(v11n.max(gen11, gen11a), gen11)
        assertEquals(v11n.max(gen12, gen11), gen12)
        assertEquals(v11n.max(rev99, gen11), rev99)
        assertEquals(v11n.max(gen11a, gen11), gen11a)
    }

    @Test
    fun testMin() {
        assertEquals(v11n.min(gen11, gen12), gen11)
        assertEquals(v11n.min(gen11, rev99), gen11)
        assertEquals(v11n.min(gen11, gen11a), gen11)
        assertEquals(v11n.min(gen12, gen11), gen11)
        assertEquals(v11n.min(rev99, gen11), gen11)
        assertEquals(v11n.min(gen11a, gen11a), gen11a)
    }

    @Test
    fun testToVerseArray() {
        assertEquals(1, gen11.toVerseArray().size)
    }

    @Test
    fun testVerseHasName() {
        var v: Verse = Verse(Versifications.instance().getVersification(Versifications.DEFAULT_V11N)!!, 5)
        assertEquals("Gen 1:2", v.getName())
    }
}
