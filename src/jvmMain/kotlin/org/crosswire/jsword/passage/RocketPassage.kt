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

import org.crosswire.jsword.versification.Versification

/**
 * A RocketPassage is a bit and heavy implementation of Passage that goes fairly
 * quickly once let of the leash. It manages its speed by creating contained
 * instances of DistinctPassage and RangedPassage and selects the fastest
 * implementation for each of its methods from the 3 available.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class RocketPassage : BitwisePassage {
    /**
     * Create a new RocketPassage
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     */
    constructor(v11n: Versification?) : super(v11n)

    /**
     * Create a Verse from a human readable string. The opposite of getName(),
     * Given any RangedPassage v1, and the following
     * <code>RangedPassage v2 = new RangedPassage(v1.getName());</code> Then
     * <code>v1.equals(v2);</code> Theoretically, since there are many ways of
     * representing a RangedPassage as text string comparison along the lines
     * of: <code>v1.getName().equals(v2.getName())</code> could be false.
     * However since getName() is standardized this will be true. We don't need
     * to worry about thread safety in a ctor since we don't exist yet.
     *
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param refs
     *            A String containing the text of the RangedPassage
     * @param basis
     *           The basis by which to interpret refs
     * @throws NoSuchVerseException
     *           if refs is invalid
     */
    // was protected: PassageType (same package) needs it, and Kotlin protected has no package access
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?, basis: Key?) : super(v11n, refs, basis)

    /**
     * Create a Verse from a human readable string. The opposite of getName(),
     * Given any RangedPassage v1, and the following
     * <code>RangedPassage v2 = new RangedPassage(v1.getName());</code> Then
     * <code>v1.equals(v2);</code> Theoretically, since there are many ways of
     * representing a RangedPassage as text string comparison along the lines
     * of: <code>v1.getName().equals(v2.getName())</code> could be false.
     * However since getName() is standardized this will be true. We don't need
     * to worry about thread safety in a ctor since we don't exist yet.
     *
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param refs
     *            A String containing the text of the RangedPassage
     * @throws NoSuchVerseException
     *            if refs is invalid
     */
    // was protected: see above
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?) : this(v11n, refs, null)

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.AbstractPassage#optimizeReads()
     */
    override fun optimizeReads() {
        raiseEventSuppresion()

        // We have to create the cached versions of these separately
        // so that the calculations made by addAll(this) can
        // safely call methods like countVerses() without any
        // danger of them being optimized before the optimizations
        // are ready for use.

        val dtemp = DistinctPassage(versification)
        dtemp.raiseEventSuppresion()
        dtemp.addAll(this)
        dtemp.lowerEventSuppressionAndTest()

        val rtemp = RangedPassage(versification)
        rtemp.raiseEventSuppresion()
        rtemp.addAll(this)
        rtemp.lowerEventSuppressionAndTest()

        distinct = dtemp
        ranged = rtemp

        // This is just an optimization so we dont need to fire any events
        lowerEventSuppressionAndTest()
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.AbstractPassage#optimizeWrites()
     */
    override fun optimizeWrites() {
        distinct = null
        ranged = null
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Passage#countRanges(int)
     */
    override fun countRanges(restrict: RestrictionType?): Int {
        val ranged = ranged
        if (ranged != null) {
            return ranged.countRanges(restrict)
        }

        return super.countRanges(restrict)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Passage#countVerses()
     */
    override fun countVerses(): Int {
        val distinct = distinct
        if (distinct != null) {
            return distinct.countVerses()
        }

        return super.countVerses()
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        val distinct = distinct
        if (distinct != null) {
            return distinct.iterator()
        }

        return super.iterator()
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Passage#rangeIterator(int)
     */
    override fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange> {
        val ranged = ranged
        if (ranged != null) {
            return ranged.rangeIterator(restrict)
        }

        return super.rangeIterator(restrict)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Passage#isEmpty()
     */
    override fun isEmpty(): Boolean {
        val distinct = distinct
        if (distinct != null) {
            return distinct.isEmpty()
        }

        return super.isEmpty()
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Passage#getVerseAt(int)
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getVerseAt(offset: Int): Verse {
        val distinct = distinct
        if (distinct != null) {
            return distinct.getVerseAt(offset)
        }

        return super.getVerseAt(offset)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Passage#getVerseRangeAt(int, int)
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getRangeAt(offset: Int, restrict: RestrictionType?): VerseRange {
        val ranged = ranged
        if (ranged != null) {
            return ranged.getRangeAt(offset, restrict)
        }

        return super.getRangeAt(offset, restrict)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Passage#booksInPassage()
     */
    override fun booksInPassage(): Int {
        val distinct = distinct
        if (distinct != null) {
            return distinct.booksInPassage()
        }

        return super.booksInPassage()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Passage#containsAll(org.crosswire.jsword
     * .passage.Passage)
     */
    override fun containsAll(that: Passage?): Boolean {
        val ranged = ranged
        if (ranged != null) {
            return ranged.containsAll(that)
        }

        return super.containsAll(that)
    }

    /**
     * The contained DistinctPassage
     */
    private var distinct: DistinctPassage? = null

    /**
     * The contained RangedPassage
     */
    private var ranged: RangedPassage? = null

}
