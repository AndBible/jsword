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
/**
 * <p>
 *   The core responsibility of the passage package is to store and collate
 *   information about the structure of the Bible - The most important classes are
 *   <code>Passage</code>, <code>PassageTally</code> and <code>Strongs</code>. A
 *   <code>Passage</code> stores lists of verses, for example
 *   &quot;<em>Gen 1:1-5, 10</em>&quot;. <code>PassageTally</code> is similar but
 *   stores verses ordered by a tally against each verse. A <code>Strongs</code>
 *   represents a Greek or Hebrew word as categorized by James Strong.
 * </p>
 *
 *
 * <h1>Passage</h1>
 * <p>
 *   A <code>Passage</code> is modeled after the JDK 2.0 <code>Collections</code>
 *   interface - so all the usual <code>add()</code>, <code>remove()</code> type
 *   actions are available. (A <code>PassageCollection</code> proxy class is
 *   available in order to treat a <code>Passage</code> exactly like a
 *   <code>Collection</code>)
 * </p>
 *
 * <p>In addition to this a <code>Passage</code> will do:
 * <ul>
 *   <li>List blurring (for find X within 5 verses of Y type requests)</li>
 *   <li>
 *     List change notification, so you can register to receive notification of
 *     changes to a <code>Passage</code>. This will be of most use with a
 *     multi-threaded search engine.
 *   </li>
 *   <li>
 *     An understanding of VerseRanges (see below) for range counting and iteration
 *     (in addition to Verse counting etc)
 *   </li>
 * </ul>
 *
 * <p>
 *   The <code>Passage</code> interface uses <code>Verse</code> and
 *   <code>VerseRange</code> in many of its methods. A <code>Verse</code> is
 *   obvious - a single Bible verse e.g. &quot;<em>Exo 2:4</em>&quot;, or
 *   &quot;<em>Jude 4</em>&quot;. A <code>VerseRange</code> has a start Verse and
 *   an end Verse e.g. &quot;<em>Exo 3:5-7</em>&quot;, or
 *   &quot;<em>Mat 25:1-Mar 2:4</em>&quot;.
 * </p>
 *
 * <p>
 *   <code>Verse</code> and <code>VerseRange</code> have a superclass interface of
 *   <code>VerseBase</code>, and this interface is collected and sorted by
 *   <code>Passage</code> .So an example <code>Passage</code> is
 *   &quot;<em>Exo 2:4, 3:5-7, Mat 25:1-Mar 2:4, Jude 4</em>&quot;.
 * </p>
 *
 * <p>
 *   The <code>Passage</code> interface is implemented by 3 concrete classes -
 *   <code>DistinctPassage</code> is a simple sorted collection of
 *   <code>Verse</code>s, <code>RangedPassage</code> is a sorted collection of
 *   <code>VerseRange</code>s, and <code>BitwisePassage</code> uses an array -
 *   essentially <code>boolean[31104]</code> to specify whether a verse is a member
 *   of the <code>Passage</code>. Obviously each of these implementations has
 *   different strengths, which the user should not need to be bothered with. So
 *   the <code>PassageFactory</code> class is responsible for creating
 *   <code>Passages</code> of a suitable type.
 * </p>
 *
 * <h2>PassageTally</h2>
 *
 * <p>
 *   The <code>PassageTally</code> class is-a to <code>Passage</code> however it's
 *   job is to store a rank to a <code>Verse</code>. This is for a best-match type
 *   application - &quot;find the verse that best matches these words&quot;. It is
 *   the intent to marry this with a Thesarus interface, because a gripe with the
 *   OLB is that I search for &quot;God &amp; loves &amp; world&quot; and expect to
 *   find John 3:16, but the search fails because John 3:16 uses the word
 *   &quot;loved&quot; and not &quot;loves&quot;.
 * </p>
 *
 * <p>
 *   The final aim is a fuzzy matching scheme to I can search for
 *   &quot;God loves us and gave Jesus to save us&quot; and correctly be told
 *   John 3:16.
 * </p>
 *
 * <h2>Strongs</h2>
 *
 * <p>
 *   The <code>Strongs</code> class represents a Hebrew or Greek word, or a
 *   parsing number indicating the way the verse is aimed.
 * </p>
 *
 * <h2>Other Stuff</h2>
 *
 * <p>
 *   This package has a <code>SelfTest</code> class that is designed to stress
 *   every line of code in the rest of the package to make it bug-free. Otherwise
 *   this package is largely complete. I still need to inspect
 *   [Bitwise|Distinct|Ranged]Passage and PassageTally.
 * </p>
 */
package org.crosswire.jsword.passage

import org.crosswire.jsword.versification.Versification

import okio.IOException

/**
 * A Passage is a specialized Collection of Verses. The additions are:
 * <ul>
 * <li>List blurring
 * <li>Range Counting and iteration (in addition to Verse counting etc)
 * <li>List change notification, so you can register to update yourself, and
 * this goes hand in hand with a added thread-safe contract.
 * <li>getName() to be more VerseBase like.
 * <li>Human readable serialization. So we can read and write to and from OLB
 * style Passage files.
 * </ul>
 *
 * <p>
 * Passage no longer extends the Collection interface to avoid J2SE 1.1/1.2
 * portability problems, and because many of the things that a Passage does rely
 * on consecutive Verses which are an alien concept to Collections. So users
 * would have to use the Passage interface anyway.
 *
 * <p>
 * Other arguments for and against.
 * <ul>
 * <li>The generic version will postpone some type errors to runtime. Is this a
 * huge problem? Are there many syntax errors that would be lost? Probably not.
 * <li>The specific version would stop enhancements like add("Gen 1:1"); (But
 * this is just syntactical sugar anyway).
 * <li>The specific version allows functionality by is-a as well as has-a. But a
 * Passage is fundamentally different so this is not that much use.
 * <li>At the end of the day I expect people to use getName() instead of
 * toString() and blur(), both of which are Passage things not Collection
 * things. So the general use of these classes is via a Passage interface not a
 * Collections one.
 * <li>Note that the implementations of Passage could not adhere strictly to the
 * Collections interface in returning false from add(), remove() etc, to specify
 * if the Collection was changed. Given ranges and the like this can get very
 * time consuming and complex.
 * </ul>
 *
 * <p>
 * The upshot of all this is that I am removing the Collections interface from
 * Passage.
 *
 * <p>
 * I considered giving Passages names to allow for a CLI that could use named
 * RangedPassages, however that is perhaps better left to another class.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
interface Passage : VerseKey<Passage> {
    /**
     * A summary of the verses in this Passage For example
     * "10 verses in 4 books"
     *
     * @return a String containing an overview of the verses
     */
    val overview: String

    /**
     * Returns the number of verses in this collection. Like Collection.size()
     * This does not mean the Passage needs to use Verses, just that it
     * understands the concept.
     *
     * @return the number of Verses in this collection
     * @see Verse
     */
    fun countVerses(): Int

    /**
     * Determine whether there are two or more ranges.
     *
     * @param restrict
     *            Do we break ranges at chapter/book boundaries
     * @return whether there are two or more ranges
     * @see VerseRange
     */
    fun hasRanges(restrict: RestrictionType?): Boolean

    /**
     * Like countVerses() that counts VerseRanges instead of Verses Returns the
     * number of fragments in this collection. This does not mean the Passage
     * needs to use VerseRanges, just that it understands the concept.
     *
     * @param restrict
     *            Do we break ranges at chapter/book boundaries
     * @return the number of VerseRanges in this collection
     * @see VerseRange
     */
    fun countRanges(restrict: RestrictionType?): Int

    /**
     * Ensures that there are a maximum of <code>count</code> Verses in this
     * Passage. If there were more than <code>count</code> Verses then a new
     * Passage is created containing the Verses from <code>count</code>+1
     * onwards. If there was not greater than <code>count</code> in the Passage,
     * then the passage remains unchanged, and null is returned.
     *
     * @param count
     *            The maximum number of Verses to allow in this collection
     * @return A new Passage containing the remaining verses or null
     * @see Verse
     */
    fun trimVerses(count: Int): Passage?

    /**
     * Ensures that there are a maximum of <code>count</code> VerseRanges in
     * this Passage. If there were more than <code>count</code> VerseRanges then
     * a new Passage is created containing the VerseRanges from
     * <code>count</code>+1 onwards. If there was not greater than
     * <code>count</code> in the Passage, then the passage remains unchanged,
     * and null is returned.
     *
     * @param count
     *            The maximum number of VerseRanges to allow in this collection
     * @param restrict
     *            Do we break ranges at chapter/book boundaries
     * @return A new Passage containing the remaining verses or null
     * @see VerseRange
     */
    fun trimRanges(count: Int, restrict: RestrictionType?): Passage?

    /**
     * How many books are there in this Passage
     *
     * @return The number of distinct books
     */
    fun booksInPassage(): Int

    /**
     * Get a specific Verse from this collection
     *
     * @param offset
     *            The verse offset (legal values are 0 to countVerses()-1)
     * @return The Verse
     * @throws ArrayIndexOutOfBoundsException
     *             If the offset is out of range
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    fun getVerseAt(offset: Int): Verse

    /**
     * Get a specific VerseRange from this collection
     *
     * @param offset
     *            The verse range offset (legal values are 0 to countRanges()-1)
     * @param restrict
     *            Do we break ranges at chapter/book boundaries
     * @return The Verse Range
     * @throws ArrayIndexOutOfBoundsException
     *             If the offset is out of range
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    fun getRangeAt(offset: Int, restrict: RestrictionType?): VerseRange

    /**
     * Like iterator() that iterates over VerseRanges instead of Verses.
     * Exactly the same data will be traversed, however using rangeIterator()
     * will usually give fewer iterations (and never more)
     *
     * @param restrict
     *            Do we break ranges over chapters
     * @return A list enumerator
     */
    fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange>

    override fun isValidIn(targetVersification: Versification?): Boolean {
        val rangeIterator = rangeIterator(RestrictionType.NONE)
        while (rangeIterator.hasNext()) {
            if (!rangeIterator.next().isValidIn(targetVersification)) {
                // If any of the included ranges is invalid, this passage is invalid.
                return false
            }
        }
        return true
    }

    /**
     * Returns true if this collection contains all the specified Verse
     *
     * @param that
     *            Verse or VerseRange that may exist in this Passage
     * @return true if this collection contains that
     */
    override fun contains(that: Key?): Boolean

    /**
     * Add this Verse/VerseRange to this Passage
     *
     * @param that
     *            The Verses to be added from this Passage
     */
    fun add(that: Key?)

    /**
     * Remove this Verse/VerseRange from this Passage
     *
     * @param that
     *            The Verses to be removed from this Passage
     */
    fun remove(that: Key?)

    /**
     * Returns true if this Passage contains all of the verses in that Passage
     *
     * @param that
     *            Passage to be checked for containment in this collection.
     * @return true if this reference contains all of the Verses in that Passage
     */
    fun containsAll(that: Passage?): Boolean

    /**
     * To be compatible with humans we read/write ourselves to a file that a
     * human can read and even edit. OLB verse.lst integration is a good goal
     * here.
     *
     * @param in
     *            The stream to read from
     * @exception java.io.IOException
     *                If the file/network etc breaks
     * @exception NoSuchVerseException
     *                If the file was invalid
     */
    @Throws(IOException::class, NoSuchVerseException::class)
    fun readDescription(text: String)

    /**
     * To be compatible with humans we read/write ourselves to a file that a
     * human can read and even edit. OLB verse.lst integration is a good goal
     * here.
     *
     * @param out
     *            The stream to write to
     * @exception java.io.IOException
     *                If the file/network etc breaks
     */
    @Throws(IOException::class)
    fun writeDescription(out: Appendable)

    /**
     * For performance reasons we may well want to hint to the Passage that we
     * have done editing it for now and that it is safe to cache certain values
     * to speed up future reads. Any action taken by this method will be undone
     * simply by making a future edit, and the only loss in calling
     * optimizeReads() is a loss of time if you then persist in writing to the
     * Passage.
     */
    fun optimizeReads()

    /**
     * Event Listeners - Add Listener
     *
     * @param li
     *            The listener to add
     */
    fun addPassageListener(li: PassageListener?)

    /**
     * Event Listeners - Remove Listener
     *
     * @param li
     *            The listener to remove
     */
    fun removePassageListener(li: PassageListener?)
}
