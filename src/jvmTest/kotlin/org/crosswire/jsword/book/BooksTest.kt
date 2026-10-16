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
package org.crosswire.jsword.book

import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.jsword.book.basic.AbstractPassageBook
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.VerseRangeFactory
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.system.Versifications
import org.junit.Assume
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class BooksTest {
    protected lateinit var gen11: Array<Key>
    protected lateinit var bmds: Array<BookMetaData>
    protected lateinit var bibles: Array<Book>

    //protected val ignorebibles: Array<Class<*>> = arrayOf()

    @BeforeTest
    fun setUp() {
        val lbmds = Books.installed().getBooks(BookFilters.getOnlyBibles())
        val numBibles = lbmds.size
        bibles = Array(numBibles) { lbmds[it] }
        bmds = Array(numBibles) { bibles[it].bookMetaData }
        gen11 = Array(numBibles) { bibles[it].getKey("Gen 1:1") }
    }

    @Test
    fun testGetBible() {
        for (i in bibles.indices) {
            val bible = bibles[i]
            assertTrue(bible != null)
        }
    }

    @Test
    fun testGetBibleMetaData() {
        for (i in bibles.indices) {
            val bible = bibles[i]
            val bmd = bible.bookMetaData
            assertEquals(bmds[i], bmd)
        }
    }

    @Test
    fun testMetaData() {
        for (i in bmds.indices) {
            val bmd = bmds[i]

            assertTrue(bmd.initials != null)
            assertTrue(bmd.initials.length > 0)
            assertTrue(bmd.name != null)
            assertTrue(bmd.name.length > 0)
        }
    }

    @Test
    fun testGetBookMetaData() {
        for (i in bibles.indices) {
            val bible = bibles[i]
            val bmd = bible.bookMetaData
            assertEquals(bmds[i], bmd)
        }
    }

    @Test
    fun testGetDataKey() {
        for (i in bibles.indices) {
            val bible = bibles[i]
            val key = bible.getKey("Gen 1:1")
            val data = BookData(bible, key)
            assertNotNull(data)
        }
    }

    @Test
    fun testGetDataPassage() {
        for (i in bibles.indices) {
            val bible = bibles[i]
            val data = BookData(bible, gen11[i])
            assertNotNull(data.osisFragment)
        }
    }

    /** Bibles like TurNTB contain merged (linked) verses which are duplicated when chapters are displayed- see JS-224.
     * This tests the deduplication code in AbstractPassageBook.
     */
    @Test
    fun testBookList() {
        //part of the pre-requisites
        val kjv = Books.installed().getBook("KJV") as AbstractPassageBook?
        Assume.assumeTrue("KJV is installed", kjv != null)
        assertTrue(kjv!!.bibleBooks.contains(BibleBook.ACTS))
    }


    /**
     * Bibles like TurNTB contain merged (linked) verses which are duplicated when chapters are displayed- see JS-224.
     * This tests the deduplication code in AbstractPassageBook.
     */
    @Test
    fun testLinkedVersesNotDuplicated() {
        val turNTB = Books.installed().getBook("TurNTB")

        Assume.assumeTrue("TurNTB is installed", turNTB != null)

        // Eph 2:4,5 are merged/linked in TurNTB
        val eph245 = VerseRangeFactory.fromString(Versifications.instance().getVersification("KJV"), "Eph 2:4-5")
        val bookData = BookData(turNTB, eph245)
        val osisFragment = bookData.osisFragment

        val xmlOutputter = XMLOutputter(Format.getPrettyFormat())
        val xml = xmlOutputter.outputString(osisFragment)

        assertTrue(xml.length < 300, "Probable duplicate text")
    }

    /**
     * Books like Josephus have hierarchical chapters.  Only the current chapter should be returned, not child chapters.
     */
    @Test
    fun testHierarchicalBook() {
        val josephus = Books.installed().getBook("Josephus")
        val section1Text = "THOSE who undertake to write histories"
        if (josephus != null) {
            // navigate down to the Preface
            val theAntiquitiesOfTheJewsKey = josephus.getGlobalKeyList().get(1)
            val prefaceKey = theAntiquitiesOfTheJewsKey!!.get(0)
            val prefaceText = josephus.getRawText(prefaceKey)
            assertFalse(prefaceText.contains(section1Text), "Child keys returned in raw text")

            // Now attempt to parse a key but get the problem is that all child text is returned too
            val bookData = BookData(josephus, prefaceKey)
            val osisFragment = bookData.osisFragment

            val xmlOutputter = XMLOutputter(Format.getPrettyFormat())
            val prefaceXml = xmlOutputter.outputString(osisFragment)
            assertFalse(prefaceXml.contains(section1Text), "Child keys returned in xml")
        }
    }

    /*
     * FIXME: These are only valid if all bibles are English fun
     * testGetFind() { // This only checks that find() does
     * something vaguely sensible // I assume that find() just calls
     * findPassage(), where the real tests are for (i in
     * bibles.indices) { val bible = bibles[i]; val key = bible.find(Search("aaron", false))
     * assertNotNull(key, "bible=" + bible.getFullName()) } }
     *
     * FIXME: These are only valid if all bibles are English fun
     * testFindPassage() { for (i in bibles.indices) {
     *     val ver = bibles[i]
     *
     *     val key = ver.find(Search("aaron", false))
     *     assertTrue(key != null) } }
     *
     * FIXME: These are only valid if all bibles are English fun
     * testFindPassage2() { for (i in bibles.indices) {
     *     // Check that this is a type that we expect to return real Bible
     *     // data
     *     val ver = bibles[i]
     *     var skip = false
     *     for (j in ignorebibles.indices) {
     *         // if (ver instanceof fullbibles[j])
     *         if (ignorebibles[j].isAssignableFrom(ver.javaClass)) skip = true
     *     }
     *     if (skip) continue
     *     log.debug("thorough testing bible: {}", ver.getFullName())
     *
     *     var key = ver.find(Search("aaron", false))
     *     var ref = KeyUtil.getPassage(key)
     *     assertTrue(ref.countVerses() > 10)
     *     key = ver.find(Search("jerusalem", false))
     *     ref = KeyUtil.getPassage(key)
     *     assertTrue(ref.countVerses() > 10)
     *     key = ver.find(Search("god", false))
     *     ref = KeyUtil.getPassage(key)
     *     assertTrue(ref.countVerses() > 10)
     *     key = ver.find(Search("GOD", false))
     *     ref = KeyUtil.getPassage(key)
     *     assertTrue(ref.countVerses() > 10)
     *     key = ver.find(Search("brother's", false))
     *     ref = KeyUtil.getPassage(key)
     *     assertTrue(ref.countVerses() > 2)
     *     key = ver.find(Search("BROTHER'S", false))
     *     ref = KeyUtil.getPassage(key)
     *     assertTrue(ref.countVerses() > 2)
     *
     *     key = ver.find(Search("maher-shalal-hash-baz", false))
     *     ref = KeyUtil.getPassage(key)
     *     if (ref.isEmpty()) {
     *         key = ver.find(Search("mahershalalhashbaz", false))
     *         ref = KeyUtil.getPassage(key)
     *     }
     *     if (ref.isEmpty()) {
     *         key = ver.find(Search("maher*", false))
     *         ref = KeyUtil.getPassage(key)
     *     }
     *     assertEquals(2, ref.countVerses())
     *     assertEquals(Verse("Isa 8:1"), ref.getVerseAt(0))
     *     assertEquals(Verse("Isa 8:3"), ref.getVerseAt(1))
     *
     *     key = ver.find(Search("MAHER-SHALAL-HASH-BAZ", false))
     *     ref = KeyUtil.getPassage(key)
     *     if (ref.isEmpty()) {
     *         key = ver.find(Search("MAHERSHALALHASHBAZ", false))
     *         ref = KeyUtil.getPassage(key)
     *     }
     *     assertEquals(2, ref.countVerses())
     *     assertEquals(Verse("Isa 8:1"), ref.getVerseAt(0))
     *     assertEquals(Verse("Isa 8:3"), ref.getVerseAt(1))
     * } }
     */
    /*
     * fun testGetStartsWith() { for (i in bibles.indices) {
     *     val ver = bibles[i]
     *
     *     if (ver is SearchableBible) {
     *         val sa = Sentance2Util.toStringArray(ver.getSearcher().getStartsWith("a"))
     *         assertTrue(sa != null)
     *     }
     * } }
     *
     * fun testGetStartsWith2() { for (i in bibles.indices) {
     *     // Check that this is a type that we expect to return real Bible data
     *     val origver = bibles[i]
     *     var skip = false
     *     for (j in ignorebibles.indices) {
     *         // if (ver instanceof fullbibles[j])
     *         if (origver.javaClass.isAssignableFrom(ignorebibles[j])) skip = true
     *     }
     *     if (skip) continue
     *     log.debug("thorough testing bible: {}", origver.getBibleMetaData().getFullName())
     *
     *     if (origver is SearchableBible) {
     *         val ver = origver as SearchableBible
     *         var sa = Sentance2Util.toStringArray(ver.getSearcher().getStartsWith("jos"))
     *         assertTrue(sa.size > 5)
     *         sa = Sentance2Util.toStringArray(ver.getSearcher().getStartsWith("jerusale"))
     *         assertEquals("jerusalem", sa[0])
     *         sa = Sentance2Util.toStringArray(ver.getSearcher().getStartsWith("maher-shalal"))
     *         if (sa.size == 0) {
     *             sa = Sentance2Util.toStringArray(ver.getSearcher().getStartsWith("mahershalal"))
     *             assertEquals("mahershalalhashbaz", sa[0])
     *         } else {
     *             assertEquals("maher-shalal-hash-baz", sa[0])
     *         }
     *         assertEquals(1, sa.size)
     *         sa = Sentance2Util.toStringArray(ver.getSearcher().getStartsWith("MAHER-SHALAL"))
     *         if (sa.size == 0) {
     *             sa = Sentance2Util.toStringArray(ver.getSearcher().getStartsWith("MAHERSHALAL"))
     *             assertEquals("mahershalalhashbaz", sa[0])
     *         } else {
     *             assertEquals("maher-shalal-hash-baz", sa[0])
     *         }
     *         assertEquals(1, sa.size)
     *         sa = Sentance2Util.toStringArray(ver.getSearcher().getStartsWith("XXX"))
     *         assertEquals(0, sa.size)
     *     }
     * } }
     */
}
