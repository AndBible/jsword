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

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

import org.crosswire.common.util.KBitSet

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.versification.Versification

/**
 * A Passage that is implemented using a BitSet - one for each verse. The
 * attributes of the style are:
 * <ul>
 * <li>Fairly fast manipulation
 * <li>Fairly getName()
 * <li>Static size, poor for small Passages, good for large Passages
 * </ul>
 *
 * <p>
 * The BitSet has one more bit than the number of verses in the Bible. This
 * would waste 1 bit per BitSet but since this doesn't cause BitSet to need an
 * extra long it doesn't, and it saves us some maths.
 * </p>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class BitwisePassage : AbstractPassage {
    private val lock = SynchronizedObject()
    /** Copies the fields and preserves the deep-copy steps of the original clone. */
    protected constructor(copyFrom: BitwisePassage) : super(copyFrom) {
        store = copyFrom.store.copy()
    }

    /**
     * Create an empty BitwisePassage. There are no ctors from either Verse or
     * VerseRange so you need to do new <code>DistinctPassage().add(...);</code>
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     */
    constructor(v11n: Versification?) : super(v11n) {
        store = KBitSet(v11n!!.maximumOrdinal() + 1)
    }

    /**
     * Create a Verse from a human readable string. The opposite of toString(),
     * Given any BitwisePassage v1, and the following
     * <code>DistinctPassage v2 = new BitwisePassage(v1.toString());</code> Then
     * <code>v1.equals(v2);</code> Theoretically, since there are many ways of
     * representing a BitwisePassage as text string comparison along the lines
     * of: <code>v1.toString().equals(v2.toString())</code> could be false.
     * Practically since toString() is standardized this will be true however.
     * We don't need to worry about thread safety in a ctor since we don't exist
     * yet.
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param refs
     *            A String containing the text of the BitwisePassage
     * @param basis
     *           The basis by which to interpret refs
     * @throws NoSuchVerseException
     *             If the string is not parsable
     */
    // was protected: PassageType (same package) needs it, and Kotlin protected has no package access
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?, basis: Key?) : super(v11n, refs) {
        store = KBitSet(v11n!!.maximumOrdinal() + 1)
        addVerses(refs, basis)
    }

    /**
     * Create a Verse from a human readable string. The opposite of toString(),
     * Given any BitwisePassage v1, and the following
     * <code>DistinctPassage v2 = new BitwisePassage(v1.toString());</code> Then
     * <code>v1.equals(v2);</code> Theoretically, since there are many ways of
     * representing a BitwisePassage as text string comparison along the lines
     * of: <code>v1.toString().equals(v2.toString())</code> could be false.
     * Practically since toString() is standardized this will be true however.
     * We don't need to worry about thread safety in a ctor since we don't exist
     * yet.
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param refs
     *            A String containing the text of the BitwisePassage
     * @throws NoSuchVerseException
     *             If the string is not parsable
     */
    // was protected: see above
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?) : this(v11n, refs, null)

    override fun clone(): BitwisePassage {
        check(this::class == BitwisePassage::class) { "${this::class} must override clone()" }
        return BitwisePassage(this)
    }

    override fun countVerses(): Int {
        return store.cardinality()
    }

    override fun isEmpty(): Boolean {
        return store.isEmpty
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return VerseIterator()
    }

    override fun contains(that: Key?): Boolean {
        for (aKey in that!!) {
            val verse = aKey as Verse
            if (!store[verse.ordinal]) {
                return false
            }
        }

        return true
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#add(org.crosswire.jsword.passage.Key)
     */
    override fun add(that: Key?) {
        optimizeWrites()

        var firstVerse: Verse? = null
        var lastVerse: Verse? = null
        for (aKey in that!!) {
            lastVerse = aKey as Verse
            if (firstVerse == null) {
                firstVerse = lastVerse
            }
            store.set(lastVerse.ordinal)
        }

        // we do an extra check here because the cost of calculating the
        // params is non-zero and may be wasted
        if (suppressEvents == 0) {
            fireIntervalAdded(this, firstVerse, lastVerse)
        }
    }

    /**
     * A shortcut to adding a key, by ordinal. The ordinal needs to be taken
     * from the same versification as the passage being created.
     *
     * @param ordinal
     *            the ordinal
     */
    open fun addVersifiedOrdinal(ordinal: Int) {
        optimizeWrites()
        store.set(ordinal)

        // we do an extra check here because the cost of calculating the
        // params is non-zero and may be wasted
        if (suppressEvents == 0) {
            val verse = versification.decodeOrdinal(ordinal)
            fireIntervalAdded(this, verse, verse)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#remove(org.crosswire.jsword.passage.Key)
     */
    override fun remove(that: Key?) {
        optimizeWrites()

        var firstVerse: Verse? = null
        var lastVerse: Verse? = null
        for (aKey in that!!) {
            lastVerse = aKey as Verse
            if (firstVerse == null) {
                firstVerse = lastVerse
            }
            store.clear(lastVerse.ordinal)
        }

        // we do an extra check here because the cost of calculating the
        // params is non-zero and may be wasted
        if (suppressEvents == 0) {
            fireIntervalAdded(this, firstVerse, lastVerse)
        }
    }

    override fun addAll(key: Key?) {
        //check for key empty. This avoids the AIOBounds with that.getVerseAt, during event firing
        if (key!!.isEmpty()) {
            //nothing to add
            return
        }

        optimizeWrites()

        if (key is BitwisePassage) {
            val thatRef: BitwisePassage = key
            store.or(thatRef.store)
        } else {
            super.addAll(key)
        }

        // we do an extra check here because the cost of calculating the
        // params is non-zero and may be wasted
        if (suppressEvents == 0 && !key.isEmpty()) {
            if (key is Passage) {
                val that: Passage = key
                fireIntervalAdded(this, that.getVerseAt(0), that.getVerseAt(that.countVerses() - 1))
            } else if (key is VerseRange) {
                val that: VerseRange = key
                fireIntervalAdded(this, that.start, that.end)
            } else if (key is Verse) {
                val that: Verse = key
                fireIntervalAdded(this, that, that)
            }
        }
    }

    override fun removeAll(key: Key?) {
        optimizeWrites()

        if (key is BitwisePassage) {
            val thatRef: BitwisePassage = key

            store.andNot(thatRef.store)
        } else {
            super.removeAll(key)
        }

        // we do an extra check here because the cost of calculating the
        // params is non-zero and may be wasted
        if (suppressEvents == 0 && !key!!.isEmpty()) {
            if (key is Passage) {
                val that: Passage = key
                fireIntervalRemoved(this, that.getVerseAt(0), that.getVerseAt(that.countVerses() - 1))
            } else if (key is VerseRange) {
                val that: VerseRange = key
                fireIntervalRemoved(this, that.start, that.end)
            } else if (key is Verse) {
                val that: Verse = key
                fireIntervalRemoved(this, that, that)
            }
        }
    }

    override fun retainAll(key: Key?) {
        optimizeWrites()

        var thatStore: KBitSet? = null
        if (key is BitwisePassage) {
            thatStore = key.store
        } else {
            val v11n = versification
            thatStore = KBitSet(v11n.maximumOrdinal() + 1)

            for (aKey in key!!) {
                val ord = (aKey as Verse).ordinal
                if (store[ord]) {
                    thatStore.set(ord)
                }
            }
        }
        store.and(thatStore)

        fireIntervalRemoved(this, null, null)
    }

    override fun clear() {
        optimizeWrites()

        store.clear()

        fireIntervalRemoved(this, null, null)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(verses: Int, restrict: RestrictionType?) {
        blur(verses, restrict, true, true)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType, boolean, boolean)
     */
    override fun blur(verses: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        synchronized(lock) {
            jswordAssert(verses >= 0) { "" }
            optimizeWrites()
            raiseNormalizeProtection()

            if (!restrict!!.equals(RestrictionType.NONE)) {
                super.blur(verses, restrict, blurDown, blurUp)
            } else {
                optimizeWrites()
                raiseEventSuppresion()
                raiseNormalizeProtection()

                val maximumOrdinal = versification.maximumOrdinal()
                val newStore = KBitSet(maximumOrdinal + 1)

                var i = store.nextSetBit(0)
                while (i >= 0) {
                    // Ordinal values go from 0 to maximumOrdinal.
                    val start = Math.max(0, i - (if (blurDown) verses else 0))
                    val end = Math.min(maximumOrdinal, i + (if (blurUp) verses else 0))

                    for (j in start..end) {
                        newStore.set(j)
                    }
                    i = store.nextSetBit(i + 1)
                }

                store = newStore

                lowerNormalizeProtection()
                if (lowerEventSuppressionAndTest()) {
                    fireIntervalAdded(this, null, null)
                }
            }
        }
    }

    /**
     * Iterate over the Verses
     *
     * @author Joe Walker
     * @author DM Smith
     */
    private inner class VerseIterator : MutableIterator<Key> {
        /**
         * What is the next Verse to be considered
         */
        private var next: Int

        /**
         * Find the first unused verse
         */
        init {
            next = -1
            calculateNext()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return next >= 0
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Throws(NoSuchElementException::class)
        override fun next(): Key {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            val retcode: Key = versification.decodeOrdinal(next)
            calculateNext()

            return retcode
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Throws(UnsupportedOperationException::class)
        override fun remove() {
            store.clear(next)
        }

        /**
         * Find the next bit
         */
        private fun calculateNext() {
            next = store.nextSetBit(next + 1)
        }
    }

    /**
     * The place the real data is stored
     */
    @JvmField
    protected var store: KBitSet

}
