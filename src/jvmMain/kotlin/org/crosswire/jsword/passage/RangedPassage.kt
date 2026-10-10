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

import java.util.NoSuchElementException
import java.util.TreeSet

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.versification.Versification

/**
 * A Passage that is implemented using a TreeSet of VerseRanges. The attributes
 * of the style are:
 * <ul>
 * <li>Compact storage of large amounts of data
 * <li>Fast getName()
 * <li>Slow manipulation
 * </ul>
 *
 * <p>
 * When to normalize()? This is a slow process, but one that is perhaps done
 * bit-by-bit instead of killing everything just to do getName(). The options
 * are:
 * <ul>
 * <li>Before every read</li>
 * <li>Before reads with a background thread</li>
 * <li>After every change</li>
 * <li>After every change with a caching scheme</li>
 * </ul>
 * I'm not sure which will be best. So I'm starting with 1 and optimizing later
 * ... Maybe the best is to allow the user to choose?
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class RangedPassage : AbstractPassage {
    /**
     * Create an empty RangedPassage. There are no ctors from either Verse or
     * VerseRange so you need to do new <code>RangedPassage().add(...);</code>
     *
     * @param refSystem
     *            The Versification to which this Passage belongs.
     */
    constructor(refSystem: Versification?) : super(refSystem) {
        store = TreeSet<VerseRange>()
    }

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
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param refs
     *            A String containing the text of the RangedPassage
     * @param basis
     *           The basis by which to interpret refs
     * @throws NoSuchVerseException
     *             if refs is invalid
     */
    // was protected: PassageType (same package) needs it, and Kotlin protected has no package access
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?, basis: Key?) : super(v11n, refs) {
        store = TreeSet<VerseRange>()
        addVerses(refs, basis)
        normalize()
    }

    // was protected: see above
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?) : this(v11n, refs, null)

    override fun clone(): RangedPassage {
        // This gets us a shallow copy
        val copy = super.clone() as RangedPassage

        // I want to just do the following
        // copy.store = (SortedSet) store.clone();
        // However SortedSet is not Cloneable so I can't
        // Watch out for this, I'm not sure if it breaks anything.
        copy.store = TreeSet<VerseRange>()
        copy.store.addAll(store)

        return copy
    }

    override fun countRanges(restrict: RestrictionType?): Int {
        if (restrict!!.equals(RestrictionType.NONE)) {
            return store.size
        }

        return super.countRanges(restrict)
    }

    override fun countVerses(): Int {
        val it = rangeIterator(RestrictionType.NONE)
        var count = 0

        while (it.hasNext()) {
            val range = it.next()
            count += range.getCardinality()
        }

        return count
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return VerseIterator(versification, rangeIterator(RestrictionType.NONE))
    }

    final override fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange> {
        if (restrict!!.equals(RestrictionType.NONE)) {
            return store.iterator()
        }

        return VerseRangeIterator(store.iterator(), restrict)
    }

    override fun isEmpty(): Boolean {
        return store.isEmpty()
    }

    override fun contains(that: Key?): Boolean {
        // Even for the contains(VerseRange) case, the simple
        // 'return store.contains(that);' will not work because
        // VerseRanges can contain others but not be equal to them.

        val thatRange = toVerseRange(versification, that)

        val it = rangeIterator(RestrictionType.NONE)
        while (it.hasNext()) {
            val thisRange = it.next()
            if (thisRange.contains(thatRange)) {
                return true
            }
        }

        // If it is not a Verse or a VerseRange then it's not here,
        // this also copes with the searches failing.
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#add(org.crosswire.jsword.passage.Key)
     */
    override fun add(that: Key?) {
        optimizeWrites()

        val thatRange = toVerseRange(versification, that)
        store.add(thatRange)

        normalize()

        // we do an extra check here because the cost of calculating the
        // params is non-zero an may be wasted
        if (suppressEvents == 0) {
            fireIntervalAdded(this, thatRange.start, thatRange.end)
        }
    }

    override fun clear() {
        optimizeWrites()

        store.clear()
        fireIntervalRemoved(this, null, null)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#remove(org.crosswire.jsword.passage.Key)
     */
    override fun remove(that: Key?) {
        optimizeWrites()

        val thatRange = toVerseRange(versification, that)
        var removed = false

        // This allows us to modify store which iterating through a copy
        val newStore: MutableSet<Key> = TreeSet<Key>()
        newStore.addAll(store)

        // go through all the VerseRanges
        for (aKey in newStore) {
            // if this range touches the range to be removed ...
            val thisRange = aKey as VerseRange
            if (thisRange.overlaps(thatRange)) {
                // ... remove it and add the remainder
                store.remove(thisRange)
                val vra = VerseRange.remainder(thisRange, thatRange)

                for (i in 0 until vra.size) {
                    store.add(vra[i])
                }

                removed = true
            }
        }

        if (removed) {
            normalize()
        }

        // we do an extra check here because the cost of calculating the
        // params is non-zero an may be wasted
        if (suppressEvents == 0) {
            fireIntervalRemoved(this, thatRange.start, thatRange.end)
        }
    }

    override fun retainAll(key: Key?) {
        optimizeWrites()

        val newStore: MutableSet<VerseRange> = TreeSet<VerseRange>()

        if (key is RangedPassage) {
            val thatIter = key.rangeIterator(RestrictionType.CHAPTER)
            while (thatIter.hasNext()) {
                val thatRange = thatIter.next()

                // go through all the VerseRanges
                val thisIter = rangeIterator(RestrictionType.NONE)
                while (thisIter.hasNext()) {
                    // if this range touches the range to be removed ...
                    val thisRange = thisIter.next()
                    if (thisRange.overlaps(thatRange)) {
                        // ... remove it and add the remainder
                        val interstect = VerseRange.intersection(thisRange, thatRange)
                        if (interstect != null) {
                            newStore.add(interstect)
                        }
                    }
                }
            }
        } else {
            val thatIter = key!!.iterator()
            while (thatIter.hasNext()) {
                val thatRange = toVerseRange(versification, thatIter.next())

                // go through all the VerseRanges
                val thisIter = rangeIterator(RestrictionType.NONE)
                while (thisIter.hasNext()) {
                    // if this range touches the range to be removed ...
                    val thisRange = thisIter.next()
                    if (thisRange.overlaps(thatRange)) {
                        // ... remove it and add the remainder
                        val interstect = VerseRange.intersection(thisRange, thatRange)
                        if (interstect != null) {
                            newStore.add(interstect)
                        }
                    }
                }
            }
        }

        store = newStore
        normalize()

        fireIntervalRemoved(this, null, null)
    }

    /**
     * We sometimes need to sort ourselves out ...
     * <p>
     * I don't think we need to be synchronized since we are private and we
     * could check that all public calling of normalize() are synchronized,
     * however this is safe, and I don't think there is a cost associated with a
     * double synchronize. (?)
     */
    /* protected */ // was package-private
    internal final override fun normalize() {
        if (skipNormalization != 0) {
            return
        }

        var last: VerseRange? = null
        var next: VerseRange? = null
        val newStore: MutableSet<VerseRange> = TreeSet<VerseRange>()

        val it = rangeIterator(RestrictionType.NONE)
        while (it.hasNext()) {
            next = it.next()

            if (last != null && next.adjacentTo(last)) {
                val merge = VerseRange(last, next)

                newStore.remove(last)
                newStore.add(merge)

                last = merge
            } else {
                newStore.add(next)
                last = next
            }
        }

        store = newStore
    }

    /**
     * This class is here to prevent users of RangedPassage.iterator() from
     * altering the underlying store and getting us out of sync. Right now there
     * are no issues with someone else removing a RangedPassage without telling
     * us, however there may be some day, and I'm not sure that we need the
     * functionality right now. Also buy using this we get to ensure
     * synchronization. Everything is final so to save the proxying performace
     * hit.
     */
    private class VerseIterator
    /**
     * Create a basic iterator that is a proxy for the RangedPassage
     * Passages iterator, with remove() overridden.
     * @param v11n
     *            the versification to which this reference pertains
     * @param it
     */
    constructor(v11n: Versification, it: MutableIterator<VerseRange>) : MutableIterator<Key> {
        /**
         * The Iterator that we are proxying to
         */
        private var real: MutableIterator<Key>

        init {
            val temp: MutableSet<Key> = TreeSet<Key>()

            while (it.hasNext()) {
                val range = it.next()
                val start = range.start.ordinal
                val end = range.getCardinality()

                for (i in 0 until end) {
                    temp.add(v11n.decodeOrdinal(start + i))
                }
            }

            real = temp.iterator()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return real.hasNext()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Throws(NoSuchElementException::class)
        override fun next(): Key {
            return real.next()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Throws(UnsupportedOperationException::class)
        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    /**
     * Loop over the VerseRanges and check that they do not require digging into
     */
    private class VerseRangeIterator
    /**
     * Simple ctor
     * @param it
     * @param restrict
     */
    constructor(it: MutableIterator<VerseRange>, restrict: RestrictionType?) : MutableIterator<VerseRange> {
        /**
         * What are we going to reply with next?
         */
        private var next: VerseRange? = null

        /**
         * Where do we break ranges
         */
        private var restrict: RestrictionType?

        /**
         * Where we read our base ranges from
         */
        private var real: MutableIterator<VerseRange>

        init {
            this.restrict = restrict
            this.real = it
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        override fun remove() {
            throw UnsupportedOperationException()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return next != null || real.hasNext()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        override fun next(): VerseRange {
            if (next == null) {
                next = real.next()
            }

            if (next == null) {
                throw NoSuchElementException()
            }

            // So we know what is broadly next, however the range might need
            // splitting according to restrict
            if (restrict!!.isSameScope(next!!.versification, next!!.start, next!!.end)) {
                return replyNext()
            }
            return splitNext()
        }

        /**
         * The next object is correct, use that one
         */
        private fun replyNext(): VerseRange {
            val reply = next
            next = null
            return reply!!
        }

        /**
         * The next object is too big, so cut it up
         */
        private fun splitNext(): VerseRange {
            val chop = next!!.rangeIterator(restrict)
            val first = chop.next()
            val ranges = VerseRange.remainder(next, first)

            jswordAssert(ranges.size == 1) { "" }
            next = ranges[0]

            return first
        }
    }

    /**
     * The place the real data is stored
     */
    private var store: MutableSet<VerseRange>

}
