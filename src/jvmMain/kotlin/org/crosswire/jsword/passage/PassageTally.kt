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
 *     The copyright to this program is held by its authors. 
 *
 */
package org.crosswire.jsword.passage

import org.crosswire.common.util.SortedArraySet

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.versification.Versification
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * Similar to a Passage, but that stores a ranking for each of the Verses that
 * it contains.
 *
 * <p>
 * Currently there is no well defined spec for what the rank of a verse means -
 * it is just an int. Since this number is exposed in 2 places
 * (getNameAndTally() and getTallyFor()) we should specify what the numbers
 * mean. Trouble is most tallies come from searches where the numbers only have
 * relative meaning.
 * </p>
 *
 * <p>
 * This class exactly implements the Passage interface when the ordering is set
 * to Order.BIBLICAL, however an additional setting of Order.TALLY sorts the
 * verses by the rank in this tally.
 *
 * <p>
 * Calling <code>tally.add(Gen 1:1); tally.add(Gen 1:1);</code> is redundant for
 * a Passage however a PassageTally will increase the rank of Gen 1:1, there are
 * additional methods <code>unAdd()</code> and <code>unAddAll()</code> that do
 * the reverse, of decreasing the rank of the specified verse(s).
 * </p>
 *
 * <p>
 * The point is to allow a search for
 * "God loves us, and gave Jesus to die to save us" to correctly identify John
 * 3:16. So we are using fuzzy matching big style, but I think this will be very
 * useful.
 * </p>
 *
 * <p>
 * How should we rank VerseRanges? We could use a sum of the ranks of the verses
 * in a range or the maximum value of a range. The former would seem to be more
 * mathematically correct, but I think that the latter is better because: the
 * concept of max value is preserved, because a wide blurred match is generally
 * not as good as a sharply defined one.
 * </p>
 *
 * <p>
 * Should we be going for a PassageTallyFactory type approach? Of the 3
 * implementations of Passage, The RangedPassage does not make sense here, and a
 * PassageTally will not have the range of uses that a Passage has, so I think
 * there is more likely to be a correct answer. So right now the answer is no.
 * </p>
 *
 * <p>
 * Memory considerations: The BitSet approach will always use a
 * <code>int[31000]</code> = 128k of memory.<br>
 * The Distinct approach will be n * int[4] where n is the number of verses
 * stored. I expect most searches to have at least n=1000. Also 128k<br>
 * Given this, (A Distinct style PassageTally will usually use more memory than
 * a BitSet style PassageTally) And the intuitive result that the BitSet will be
 * faster, I'm going to start by implementing the latter only.
 * </p>
 *
 * <p>
 * To think about - I've upped the MAX_TALLY to 20000 to help the new mapper
 * program. I'm not sure why it was originally 100?
 *
 * <p>
 * LATER(joe): Specify how passage ranks work.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class PassageTally : AbstractPassage {
    /** Copies the fields and preserves the deep-copy steps of the original clone. */
    protected constructor(copyFrom: PassageTally) : super(copyFrom) {
        board = copyFrom.board.clone()
        totalState = copyFrom.totalState
        size = copyFrom.size
        max = copyFrom.max
        order = copyFrom.order
    }

    /**
     * Create an empty PassageTally
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     */
    constructor(v11n: Versification?) : super(v11n) {
        board = IntArray(v11n!!.maximumOrdinal() + 1)
    }

    /**
     * Create a Verse from a human readable string. The opposite of toString()
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param refs
     *            The text to interpret
     * @param basis
     *           The basis by which to interpret refs
     * @throws NoSuchVerseException
     *             If refs is invalid
     */
    // was protected: PassageType (same package) needs it, and Kotlin protected has no package access
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?, basis: Key?) : super(v11n, refs) {
        board = IntArray(v11n!!.maximumOrdinal() + 1)
        addVerses(refs, basis)
    }

    // was protected: see above
    @Throws(NoSuchVerseException::class)
    internal constructor(v11n: Versification?, refs: String?) : this(v11n, refs, null)

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun countVerses(): Int {
        return size
    }

    /**
     * Set how we sort the verses we output. The options are:
     * <ul>
     * <li>Order.BIBLICAL: Natural Biblical order</li>
     * <li>Order.TALLY: In an order specified by this class</li>
     * </ul>
     *
     * @param order
     *            the sort order
     */
    // The Java getter/setter pair is getOrdering/setOrdering over the serialized field `order`,
    // so this is a property without a backing field of its own.
    open var ordering: Order
        /**
         * Get how we sort the verses we output.
         *
         * @return the sort order
         */
        get() = order
        set(order) {
            this.order = order
        }

    /**
     * @return the total
     */
    open var total: Int
        get() = totalState
        set(value) { totalState = value }

    // Copies must bypass open accessors before subclass state has been initialized.
    private var totalState: Int = 0

    override fun clone(): PassageTally {
        check(this::class == PassageTally::class) { "${this::class} must override clone()" }
        return PassageTally(this)
    }

    override fun toString(): String {
        return getName(0)
    }

    override fun getName(): String {
        return getName(0)
    }

    /**
     * A Human readable version of the verse list. Uses short books names, and
     * the shortest possible rendering eg "Mat 3:1-4, 6"
     *
     * @param cnt
     *            The number of matches to return, 0 gives all matches
     * @return a String containing a description of the verses
     */
    open fun getName(cnt: Int): String {
        var maxCount = cnt
        val originalName = originalName
        if (PassageUtil.isPersistentNaming() && originalName != null) {
            return originalName
        }

        val retcode = StringBuilder()

        if (order == Order.BIBLICAL) {
            val it = rangeIterator(RestrictionType.NONE)
            var current: Verse? = null
            while (it.hasNext()) {
                val range = it.next()
                retcode.append(range.getName(current))

                if (it.hasNext()) {
                    retcode.append(REF_PREF_DELIM)
                }

                current = range.start
            }
        } else {
            if (maxCount == 0) {
                maxCount = Int.MAX_VALUE
            }

            val it: MutableIterator<Key> = OrderedVerseIterator(versification, board)
            var current: Key? = null
            var count = 0

            while (it.hasNext() && count < maxCount) {
                val verse = it.next()
                retcode.append(verse.getName(current))

                current = verse
                count++

                if (it.hasNext() && count < maxCount) {
                    retcode.append(REF_PREF_DELIM)
                }
            }
        }

        return retcode.toString()
    }

    /**
     * A Human readable version of the PassageTally. Uses short books names, and
     * the shortest possible rendering eg "Mat 3:1-4"
     *
     * @return a String containing a description of the verses
     */
    open fun getNameAndTally(): String {
        return getNameAndTally(0)
    }

    /**
     * A Human readable version of the PassageTally. Uses short books names, and
     * the shortest possible rendering eg "Mat 3:1-4"
     *
     * @param cnt
     *            The number of matches to return, 0 gives all matches
     * @return a String containing a description of the verses
     */
    open fun getNameAndTally(cnt: Int): String {
        var maxCount = cnt
        val retcode = StringBuilder()
        if (maxCount == 0) {
            maxCount = Int.MAX_VALUE
        }

        val it = OrderedVerseIterator(versification, board)
        var count = 0

        while (it.hasNext() && count < maxCount) {
            val verse = it.next()
            retcode.append(verse.getName())
            retcode.append(" (")
            retcode.append(100 * it.lastRank() / max)
            retcode.append("%)")

            count++

            if (it.hasNext() && count < maxCount) {
                retcode.append(REF_PREF_DELIM)
            }
        }

        return retcode.toString()
    }

    /**
     * Iterate through the verse elements in the current sort order
     *
     * @return A verse Iterator
     */
    override fun iterator(): MutableIterator<Key> {
        if (order == Order.BIBLICAL) {
            return VerseIterator()
        }
        return OrderedVerseIterator(versification, board)
    }

    override fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange> {
        if (order == Order.BIBLICAL) {
            return AbstractPassage.VerseRangeIterator(versification, iterator(), restrict)
        }
        return OrderedVerseRangeIterator(versification, iterator(), board)
    }

    /**
     * Does this tally contain all the specified verses?
     *
     * @param that
     *            The verses to test for
     * @return true if all the verses exist in this tally
     */
    override fun contains(that: Key?): Boolean {
        for (aKey in that!!) {
            val verse = aKey as Verse
            if (board[verse.ordinal] == 0) {
                return false
            }
        }

        return true
    }

    /**
     * The ranking given to a specific verse
     *
     * @param verse
     *            The verse to get the ranking of
     * @return The rank of the verse in question
     */
    open fun getTallyOf(verse: Verse?): Int {
        return board[verse!!.ordinal]
    }

    /**
     * What is the index of the give verse in the current ordering scheme
     *
     * @param verse
     *            The verse to get the index of
     * @return The index of the verse or -1 if the verse was not found
     */
    open fun getIndexOf(verse: Verse?): Int {
        val pos = verse!!.ordinal
        val tally = board[pos]
        return if (tally > 0) pos else -1
    }

    /**
     * Add/Increment this verses in the rankings
     *
     * @param that
     *            The verses to add/increment
     */
    override fun add(that: Key?) {
        optimizeWrites()

        alterVerseBase(that, 1)
        fireIntervalAdded(this, null, null)
    }

    /**
     * DONT USE THIS. It makes public something of the ratings scheme which is
     * not generally recommended. This method is likely to be removed at a
     * moments notice, and it only here to keep Mapper happy. Add/Increment this
     * verses in the rankings
     *
     * @param that
     *            The verses to add/increment
     * @param count
     *            The amount to increment by
     */
    open fun add(that: Key?, count: Int) {
        optimizeWrites()

        alterVerseBase(that, count)
        fireIntervalAdded(this, null, null)
    }

    /**
     * Remove/Decrement this verses in the rankings
     *
     * @param that
     *            The verses to remove/decrement
     */
    open fun unAdd(that: Key?) {
        optimizeWrites()

        alterVerseBase(that, -1)
        fireIntervalRemoved(this, null, null)
    }

    /**
     * Remove these verses from the rankings, ie, set their rank to zero.
     *
     * @param that
     *            The verses to remove/decrement
     */
    override fun remove(that: Key?) {
        optimizeWrites()

        for (aKey in that!!) {
            val verse = aKey as Verse
            kill(verse.ordinal)
        }

        fireIntervalRemoved(this, null, null)
    }

    override fun addAll(key: Key?) {
        optimizeWrites()

        if (key is PassageTally) {
            val tally: PassageTally = key

            val vib = versification.maximumOrdinal()
            for (i in 0..vib) {
                increment(i, tally.board[i])
            }

            incrementMax(tally.max)
        } else {
            for (aKey in key!!) {
                val verse = aKey as Verse
                increment(verse.ordinal, 1)
            }

            incrementMax(1)
        }

        fireIntervalAdded(this, null, null)
    }

    /**
     * Remove/Decrement these verses in the rankings
     *
     * @param that
     *            The verses to remove/decrement
     */
    open fun unAddAll(that: Passage?) {
        optimizeWrites()

        if (that is PassageTally) {
            val tally: PassageTally = that

            val vib = versification.maximumOrdinal()
            for (i in 0..vib) {
                increment(i, -tally.board[i])
            }
        } else {
            for (aKey in that!!) {
                val verse = aKey as Verse
                increment(verse.ordinal, -1)
            }
        }

        fireIntervalRemoved(this, null, null)

        // Just because we've decremented some doesn't
        // change the max. So we don't need to do:
        // incrementMax(-1);
    }

    override fun removeAll(key: Key?) {
        optimizeWrites()

        if (key is PassageTally) {
            val tally: PassageTally = key

            val vib = versification.maximumOrdinal()
            for (i in 0..vib) {
                if (tally.board[i] != 0) {
                    kill(i)
                }
            }
        } else {
            for (aKey in key!!) {
                val verse = aKey as Verse
                kill(verse.ordinal)
            }
        }

        fireIntervalRemoved(this, null, null)

        // Just because we've decremented some doesn't
        // change the max. So we don't need to do:
        // incrementMax(-1);
    }

    override fun clear() {
        optimizeWrites()

        for (i in 0 until board.size) {
            board[i] = 0
        }

        size = 0

        fireIntervalRemoved(this, null, null)
    }

    /**
     * Ensures that there are a maximum of <code>count</code> Verses in this
     * Passage. If there were more than <code>count</code> Verses then a new
     * Passage is created containing the Verses from <code>count</code> + 1
     * onwards. If there was not greater than <code>count</code> in the Passage,
     * then the passage remains unchanged, and null is returned.
     *
     * @param count
     *            The maximum number of Verses to allow in this collection
     * @return A new Passage containing the remaining verses or null
     * @see Verse
     */
    override fun trimVerses(count: Int): Passage? {
        optimizeWrites()

        var i = 0
        var overflow = false

        val remainder: Passage = this.clone()

        for (verse in this) {
            i++
            if (i > count) {
                remove(verse)
                overflow = true
            } else {
                remainder.remove(verse)
            }

        }

        if (overflow) {
            return remainder
        }
        return null
    }

    /**
     * Take the verses in the tally and give them all and equal rank of 1. After
     * this method has executed then both sorting methods for a.
     */
    open fun flatten() {
        optimizeWrites()

        for (i in 0 until board.size) {
            if (board[i] != 0) {
                board[i] = 1
            }
        }

        max = 1
    }

    override fun blur(verses: Int, restrict: RestrictionType?) {
        blur(verses, restrict, true, true)
    }

    override fun blur(verses: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        jswordAssert(verses >= 0) { "" }

        optimizeWrites()
        raiseEventSuppresion()
        raiseNormalizeProtection()

        if (!restrict!!.equals(RestrictionType.NONE)) {
            // This is a bit of a cheat, but there is no way I'm going
            // to do the math to speed up the restricted version
            val temp = this.clone()
            val it = temp.rangeIterator(RestrictionType.NONE)

            while (it.hasNext()) {
                val range = it.next()
                for (i in 0..verses) {
                    add(restrict.blur(versification, range, if (blurDown) verses else 0, if (blurUp) verses else 0))
                }
            }
        } else {
            val newBoard = IntArray(board.size)

            for (i in 0 until board.size) {
                if (board[i] != 0) {
                    // This could be re-written more simply:
                    // for (int j = -verses; j <= verses; j++) {
                    //     int k = i + j;
                    //     if (k >= 0 && k <= BibleInfo.maximumOrdinal()) {
                    //         new_board[k] += board[i] + verses - mod(j);
                    //     }
                    // }
                    // However splitting the loop in 2 will speed it up quite a bit.

                    var j = if (blurDown) -verses else 0
                    while (j < 0) {
                        val k = i + j
                        if (k >= 0) {
                            newBoard[k] += board[i] + verses + j
                        }
                        j++
                    }

                    newBoard[i] += board[i] + verses

                    j = 1
                    while (j <= (if (blurUp) verses else 0)) {
                        val k = i + j
                        if (k < board.size) {
                            newBoard[k] += board[i] + verses - j
                        }
                        j++
                    }
                }
            }

            board = newBoard
        }

        resetMax()

        lowerNormalizeProtection()
        if (lowerEventSuppressionAndTest()) {
            fireIntervalAdded(this, null, null)
        }
    }

    /**
     * Sometimes we end up not knowing what the max is - this makes sure we know
     * accurately. Same with size.
     */
    private fun resetMax() {
        optimizeWrites()

        max = 0
        size = 0
        for (i in 0 until board.size) {
            if (board[i] > 0) {
                size++
            }
            if (board[i] > max) {
                max = board[i]
            }
        }
    }

    /**
     * Increment/Decrement this verses in the rankings
     *
     * @param that
     *            The verses to add/increment
     * @param tally
     *            The amount to increment/decrement by
     */
    private fun alterVerseBase(that: Key?, tally: Int) {
        for (aKey in that!!) {
            val verse = aKey as Verse
            increment(verse.ordinal, tally)
        }

        if (tally > 0) {
            incrementMax(tally)
        }
    }

    /**
     * Increment a verse by an amount
     *
     * @param ord
     *            The verse to increment
     * @param tally
     *            The amount to increase by
     */
    private fun increment(ord: Int, tally: Int) {
        val exists = board[ord] > 0
        board[ord] += tally
        if (board[ord] > MAX_TALLY) {
            board[ord] = MAX_TALLY
        }
        if (board[ord] < 0) {
            board[ord] = 0
        }

        // Recompute the size
        if (exists && board[ord] == 0) {
            size--
        } else if (!exists && board[ord] > 0) {
            size++
        }
    }

    /**
     * Increment a verse by an amount
     *
     * @param tally
     *            The amount to increase by
     */
    private fun incrementMax(tally: Int) {
        max += tally
        if (max > MAX_TALLY) {
            max = MAX_TALLY
        }
        if (max < 0) {
            max = 0
        }
    }

    /**
     * Wipe the rank of the given verse to zero
     *
     * @param ord
     *            The verse to increment
     */
    private fun kill(ord: Int) {
        if (board[ord] > 0) {
            size--
        }

        board[ord] = 0
    }

    /**
     * Indicates how this PassageTally is to order it's Verses.
     */
    enum class Order {
        /**
         * Sort in Biblical order
         */
        BIBLICAL,

        /**
         * Sort in tally rank order
         */
        TALLY
    }

    /*
     * The number of verses in the tally.
     */
    private var size: Int = 0

    /**
     * The tally board itself
     */
    @JvmField
    protected var board: IntArray

    /**
     * The maximum tally possible
     */
    private var max: Int = 0

    /**
     * The maximum tally possible
     */
    private var order: Order = Order.BIBLICAL

    /**
     * Iterate over the Verses in normal verse order
     *
     * @author Joe Walker
     */
    private inner class VerseIterator : MutableIterator<Key> {
        /** What is the next Verse to be considered */
        private var next: Int = 0

        /**
         * Find the first unused verse
         */
        init {
            calculateNext()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return next <= board.size - 1
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Throws(NoSuchElementException::class)
        override fun next(): Key {
            if (next >= board.size) {
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
            throw UnsupportedOperationException()
        }

        /**
         * Find the next bit
         */
        private fun calculateNext() {
            do {
                next++
            } while (next < board.size && board[next] == 0)
        }
    }

    /**
     * Iterate over the Verses in order of their rank in the tally
     *
     * @author Joe Walker
     */
    private class OrderedVerseIterator
    /**
     * Find the first unused verse
     */
    constructor(v11n: Versification, board: IntArray) : MutableIterator<Key> {
        /**
         * The Versification is needed to decode board positions.
         */
        private var referenceSystem: Versification

        /**
         * So that we can get at the ranking of the given verse
         */
        private var last: TalliedVerse?

        /**
         * The Iterator we are converting
         */
        private var it: MutableIterator<TalliedVerse>

        init {
            referenceSystem = v11n
            val output = SortedArraySet<TalliedVerse>()

            val vib = board.size - 1
            for (i in 0..vib) {
                if (board[i] != 0) {
                    output.add(TalliedVerse(i, board[i]))
                }
            }

            it = output.iterator()
            last = null
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return it.hasNext()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Throws(NoSuchElementException::class)
        override fun next(): Key {
            last = it.next()
            return referenceSystem.decodeOrdinal(last!!.ord)
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Throws(UnsupportedOperationException::class)
        override fun remove() {
            throw UnsupportedOperationException()
        }

        /**
         * @return the next Verse in the iteration
         * @throws NoSuchElementException
         *             if hasNext() == false
         */
        @Throws(NoSuchElementException::class)
        fun lastRank(): Int {
            val last = last
            if (last != null) {
                return last.tally
            }
            throw NoSuchElementException(JSOtherMsg.lookupText("nextElement() has not been called yet."))
        }
    }

    /**
     * Hack to make this work with J2SE 1.1 as well as J2SE 1.2 This compared 2
     * Integers
     */
    private class TalliedVerse
    /**
     * Convenience ctor to set the public variables
     *
     * @param ord
     *            the verse id
     * @param tally
     *            the rank of the verse
     */
    constructor(ord: Int, tally: Int) : Comparable<TalliedVerse> {
        /**
         * The verse id
         */
        @JvmField
        var ord: Int = ord

        /**
         * The rank of the verse
         */
        @JvmField
        var tally: Int = tally

        override fun hashCode(): Int {
            val result = 31 + ord
            return 31 * result + tally
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null) {
                return false
            }
            if (this::class != other::class) {
                return false
            }
            val that = other as TalliedVerse
            if (tally != that.tally) {
                return false
            }
            if (ord != that.ord) {
                return false
            }
            return true
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        override fun compareTo(other: TalliedVerse): Int {
            if (other.tally == this.tally) {
                return this.ord - other.ord
            }

            return other.tally - this.tally
        }
    }

    /**
     * Iterate over the Ranges in order of their rank in the tally
     *
     * @author Joe Walker
     */
    private class OrderedVerseRangeIterator
    /**
     * Find the first unused verse
     *
     * @param v11n
     *            the versification to which this reference pertains
     * @param vit
     * @param board
     */
    constructor(v11n: Versification, vit: MutableIterator<Key>, board: IntArray) : MutableIterator<VerseRange> {
        /**
         * So that we can get at the ranking of the given verse
         */
        private var last: TalliedVerseRange?

        /**
         * The Iterator we are converting
         */
        private var it: MutableIterator<TalliedVerseRange>

        init {
            val output: MutableSet<TalliedVerseRange> = SortedArraySet<TalliedVerseRange>()

            val rit: MutableIterator<VerseRange> = AbstractPassage.VerseRangeIterator(v11n, vit, RestrictionType.NONE)
            while (rit.hasNext()) {
                val range = rit.next()

                // Calculate the maximum rank for a verse
                var rank = 0
                val iter = range.iterator()
                while (iter.hasNext()) {
                    val verse = iter.next() as Verse
                    val temp = board[verse.ordinal]
                    if (temp > rank) {
                        rank = temp
                    }
                }

                output.add(TalliedVerseRange(range, rank))
            }

            this.it = output.iterator()
            last = null
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return it.hasNext()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Throws(NoSuchElementException::class)
        override fun next(): VerseRange {
            last = it.next()
            return last!!.range
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
     * Hack to make this work with JDK1.1 as well as JDK1.2 This compared 2
     * Integers
     */
    private class TalliedVerseRange
    /**
     * Convenience ctor to set the public variables
     *
     * @param range
     *            The verse range
     * @param tally
     *            The rank of the verse
     */
    constructor(range: VerseRange, tally: Int) : Comparable<TalliedVerseRange> {
        /**
         * The verse range
         */
        @JvmField
        var range: VerseRange = range

        /**
         * The rank of the verse
         */
        @JvmField
        var tally: Int = tally

        override fun hashCode(): Int {
            val result = 31 + tally
            return 31 * result + range.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null) {
                return false
            }
            if (this::class != other::class) {
                return false
            }
            val that = other as TalliedVerseRange
            if (tally != that.tally) {
                return false
            }
            if (range != that.range) {
                return false
            }
            return true
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        override fun compareTo(other: TalliedVerseRange): Int {
            if (other.tally == this.tally) {
                return this.range.compareTo(other.range)
            }

            return other.tally - this.tally
        }
    }

    companion object {
        /**
         * The highest tally possible
         */
        const val MAX_TALLY: Int = 20000

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(PassageTally::class)
    }
}
