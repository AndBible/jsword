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

import java.util.Collections
import java.util.SortedSet
import java.util.TreeSet

import org.crosswire.jsword.versification.Versification

/**
 * A Passage that is implemented using a TreeSet of Verses. The attributes of
 * the style are:
 * <ul>
 * <li>Fairly fast manipulation
 * <li>Slow getName()
 * <li>Bloated for storing large numbers of Verses
 * </ul>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class DistinctPassage : AbstractPassage {
    /** Copies the fields and preserves the deep-copy steps of the original clone. */
    protected constructor(copyFrom: DistinctPassage) : super(copyFrom) {
        store = TreeSet<Key>()
        store.addAll(copyFrom.store)
    }

    /**
     * Create an empty DistinctPassage. There are no ctors from either Verse or
     * VerseRange so you need to do new <code>DistinctPassage().add(...);</code>
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     */
    constructor(v11n: Versification?) : super(v11n)

    /**
     * Create a Verse from a human readable string. The opposite of toString(),
     * Given any DistinctPassage v1, and the following
     * <code>DistinctPassage v2 = new DistinctPassage(v1.toString());</code>
     * Then <code>v1.equals(v2);</code> Theoretically, since there are many ways
     * of representing a DistinctPassage as text string comparison along the
     * lines of: <code>v1.toString().equals(v2.toString())</code> could be
     * false. Practically since toString() is standardized this will be true
     * however. We don't need to worry about thread safety in a ctor since we
     * don't exist yet.
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param refs
     *            A String containing the text of the DistinctPassage
     * @param basis
     *           The basis by which to interpret refs
     * @throws NoSuchVerseException
     *             If the string is not valid
     */
    // was protected: PassageType (same package) needs it, and Kotlin protected has no package access
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?, basis: Key?) : super(v11n, refs) {
        store = Collections.synchronizedSortedSet(TreeSet<Key>())
        addVerses(refs, basis)
    }

    // was protected: see above
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?) : this(v11n, refs, null)

    override fun clone(): DistinctPassage {
        check(this::class == DistinctPassage::class) { "${this::class} must override clone()" }
        return DistinctPassage(this)
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return store.iterator()
    }

    override fun isEmpty(): Boolean {
        return store.isEmpty()
    }

    override fun countVerses(): Int {
        return store.size
    }

    override fun contains(that: Key?): Boolean {
        for (aKey in that!!) {
            if (!store.contains(aKey)) {
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
            store.add(lastVerse)
        }

        // we do an extra check here because the cost of calculating the
        // params is non-zero an may be wasted
        if (suppressEvents == 0) {
            fireIntervalAdded(this, firstVerse, lastVerse)
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
            store.remove(lastVerse)
        }

        // we do an extra check here because the cost of calculating the
        // params is non-zero an may be wasted
        if (suppressEvents == 0) {
            fireIntervalAdded(this, firstVerse, lastVerse)
        }
    }

    override fun clear() {
        optimizeWrites()

        store.clear()
        fireIntervalRemoved(this, null, null)
    }

    /**
     * The place the real data is stored
     */
    private var store: SortedSet<Key> = TreeSet<Key>()

}
