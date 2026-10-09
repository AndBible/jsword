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

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Reader
import java.io.Writer
import java.util.ArrayList
import java.util.BitSet
import java.util.NoSuchElementException

import org.crosswire.common.util.StringUtil
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is a base class to help with some of the common implementation details
 * of being a Passage.
 * <p>
 * Importantly, this class takes care of Serialization in a general yet
 * optimized way. I think I am going to have a look at replacement here.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
abstract class AbstractPassage : Passage, Cloneable {
    /**
     * Setup that leaves original name being null
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     */
    protected constructor(v11n: Versification?) : this(v11n, null)

    /**
     * Setup the original name of this reference
     *
     * @param v11n
     *            The Versification to which this Passage belongs.
     * @param passageName
     *            The text originally used to create this Passage.
     */
    protected constructor(v11n: Versification?, passageName: String?) {
        this.v11n = v11n
        this.originalName = passageName
        this.listeners = ArrayList<PassageListener?>()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getVersification()
     */
    override val versification: Versification
        get() = v11n!!

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#reversify(org.crosswire.jsword.versification.Versification)
     */
    override fun reversify(newVersification: Versification?): Passage? {
        if (v11n!!.equals(newVersification)) {
            return this
        }
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#isWhole()
     */
    override val isWhole: Boolean
        get() = throw UnsupportedOperationException()

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#getWhole()
     */
    override val whole: Passage
        get() = throw UnsupportedOperationException()

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Key?): Int {
        val thatref = other as Passage
        if (thatref.countVerses() == 0) {
            if (countVerses() == 0) {
                return 0
            }
            // that is empty so he should come before me
            return -1
        }

        if (countVerses() == 0) {
            // we are empty be he isn't so we are first
            return 1
        }

        val thatfirst = thatref.getVerseAt(0)
        val thisfirst = getVerseAt(0)

        return versification.distance(thatfirst, thisfirst)
    }

    override fun clone(): AbstractPassage {
        // This gets us a shallow copy
        var copy: AbstractPassage? = null

        try {
            copy = super<Cloneable>.clone() as AbstractPassage
            copy.listeners = ArrayList<PassageListener?>()
            copy.listeners.addAll(listeners)

            copy.originalName = originalName
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }

        return copy!!
    }

    override fun equals(other: Any?): Boolean {
        // This is cheating because I am supposed to say:
        // <code>!obj.getClass().equals(this.getClass())</code>
        // However I think it is entirely valid for a RangedPassage
        // to equal a DistinctPassage since the point of the Factory
        // is that the user does not need to know the actual type of the
        // Object he is using.
        if (other !is Passage) {
            return false
        }
        val that: Passage = other
        // The real test
        // FIXME: this is not really true since the versification any longer.
        return that.getOsisRef() == getOsisRef()
    }

    override fun hashCode(): Int {
        return getOsisRef().hashCode()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName()
     */
    override fun getName(): String {
        val originalName = originalName
        if (PassageUtil.isPersistentNaming() && originalName != null) {
            return originalName
        }

        val retcode = StringBuilder()

        val it = rangeIterator(RestrictionType.NONE)
        var current: Verse? = null
        while (it.hasNext()) {
            val range = it.next()
            retcode.append(range.getName(current))

            // FIXME: Potential bug. According to iterator contract hasNext and
            // next must be paired.
            if (it.hasNext()) {
                retcode.append(REF_PREF_DELIM)
            }

            current = range.start
        }

        return retcode.toString()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName(org.crosswire.jsword.passage.Key)
     */
    override fun getName(base: Key?): String {
        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    override fun getRootName(): String {
        val it = rangeIterator(RestrictionType.NONE)
        while (it.hasNext()) {
            val range = it.next()
            return range.getRootName()
        }

        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    override fun getOsisRef(): String {
        val retcode = StringBuilder()

        val it = rangeIterator(RestrictionType.NONE)
        var hasNext = it.hasNext()
        while (hasNext) {
            val range: Key = it.next()
            retcode.append(range.getOsisRef())

            hasNext = it.hasNext()
            if (hasNext) {
                retcode.append(REF_OSIS_DELIM)
            }
        }

        return retcode.toString()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    override fun getOsisID(): String {
        val retcode = StringBuilder()

        val it = rangeIterator(RestrictionType.NONE)
        var hasNext = it.hasNext()
        while (hasNext) {
            val range: Key = it.next()
            retcode.append(range.getOsisID())

            hasNext = it.hasNext()
            if (hasNext) {
                retcode.append(REF_OSIS_DELIM)
            }
        }

        return retcode.toString()
    }

    override fun toString(): String {
        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getOverview()
     */
    override val overview: String
        get() {
            // TRANSLATOR: This provides an overview of the verses in one or more books. The placeholders here deserve extra comment.
            // {0,number,integer} is a placeholder for the count of verses. It will be displayed as an integer using the number system of the user's locale.
            // {0,choice,0#verses|1#verse|1<verses} uses the value of the number of verses to display the correct singular or plural form for the word "verse"
            //    Choices are separated by |. And each choice consists of a number, a comparison and the value to use when the comparison is met.
            //    Choices are ordered from smallest to largest. The numbers represent boundaries that determine when a choice is used.
            //    The comparison # means to match exactly.
            //    The comparison < means that the number on the left is less than the number being evaluated.
            //    Here, 0 is the first boundary specified by a #. So every number less than or equal to 0 get the first choice.
            //    In this situation, we are dealing with counting numbers, so we'll never have negative numbers.
            //    Next choice is 1 with a boundary specified by #. So all numbers greater than 0 (the first choice) but less than or equal to 1 get the second choice.
            //    In this situation, the only number that will match is 1.
            //    The final choice is 1<. This means that every number greater than 1 will get this choice.
            // Putting the first two placeholders together we get "0 verses", "1 verse" or "n verses" (where n is 2 or more)
            // The reason to go into this is that this pattern works for English. Other languages might have different ways of representing singular and plurals.
            // {1,number,integer} is a placeholder for the count of Bible books. It works the same way as the count of verses.
            // {1,choice,0#books|1#book|1<books} is the placeholder for the singular or plural of "book"
            return JSMsg.gettext("{0,number,integer} {0,choice,0#verses|1#verse|1<verses} in {1,number,integer} {1,choice,0#books|1#book|1<books}",
                    Integer.valueOf(countVerses()), Integer.valueOf(booksInPassage()
                    ))
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        // Is there any content?
        return !iterator().hasNext()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countVerses()
     */
    override fun countVerses(): Int {
        var count = 0

        val iter = iterator()
        while (iter.hasNext()) {
            count++
            iter.next()
        }

        return count
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#hasRanges(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun hasRanges(restrict: RestrictionType?): Boolean {
        var count = 0

        val it = rangeIterator(restrict)
        while (it.hasNext()) {
            it.next()
            count++
            if (count == 2) {
                return true
            }
        }

        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countRanges(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun countRanges(restrict: RestrictionType?): Int {
        var count = 0

        val it = rangeIterator(restrict)
        while (it.hasNext()) {
            it.next()
            count++
        }

        return count
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#booksInPassage()
     */
    override fun booksInPassage(): Int {
        // FIXME(DMS): a passage does not have to be ordered, for example PassageTally.
        var currentBook: BibleBook? = null
        var bookCount = 0

        for (aKey in this) {
            val verse = aKey as Verse
            if (currentBook !== verse.book) {
                currentBook = verse.book
                bookCount++
            }
        }

        return bookCount
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getVerseAt(int)
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getVerseAt(offset: Int): Verse {
        val it = iterator()
        var retcode: Any? = null

        for (i in 0..offset) {
            if (!it.hasNext()) {
                throw ArrayIndexOutOfBoundsException(JSOtherMsg.lookupText("Index out of range (Given {0,number,integer}, Max {1,number,integer}).", Integer.valueOf(offset), Integer.valueOf(countVerses())))
            }

            retcode = it.next()
        }

        return retcode as Verse
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getRangeAt(int, org.crosswire.jsword.passage.RestrictionType)
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getRangeAt(offset: Int, restrict: RestrictionType?): VerseRange {
        val it = rangeIterator(restrict)
        var retcode: Any? = null

        for (i in 0..offset) {
            if (!it.hasNext()) {
                throw ArrayIndexOutOfBoundsException(JSOtherMsg.lookupText("Index out of range (Given {0,number,integer}, Max {1,number,integer}).", Integer.valueOf(offset), Integer.valueOf(countVerses())))
            }

            retcode = it.next()
        }

        return retcode as VerseRange
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#rangeIterator(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange> {
        return VerseRangeIterator(versification, iterator(), restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#containsAll(org.crosswire.jsword.passage.Passage)
     */
    override fun containsAll(that: Passage?): Boolean {
        if (that is RangedPassage) {
            val iter = that.rangeIterator(RestrictionType.NONE)
            while (iter.hasNext()) {
                if (!contains(iter.next())) {
                    return false
                }
            }
        } else {
            val iter = that!!.iterator()
            while (iter.hasNext()) {
                if (!contains(iter.next())) {
                    return false
                }
            }

        }

        return true
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimVerses(int)
     */
    override fun trimVerses(count: Int): Passage? {
        optimizeWrites()
        raiseNormalizeProtection()

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

        lowerNormalizeProtection()
        // The event notification is done by the remove above

        if (overflow) {
            return remainder
        }
        return null
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimRanges(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun trimRanges(count: Int, restrict: RestrictionType?): Passage? {
        optimizeWrites()
        raiseNormalizeProtection()

        var i = 0
        var overflow = false

        val remainder: Passage = this.clone()

        val it = rangeIterator(restrict)
        while (it.hasNext()) {
            i++
            val range: Key = it.next()

            if (i > count) {
                remove(range)
                overflow = true
            } else {
                remainder.remove(range)
            }
        }

        lowerNormalizeProtection()
        // The event notification is done by the remove above

        if (overflow) {
            return remainder
        }
        return null
    }

    /* Now supports adding keys from different versifications.
     * (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#addAll(org.crosswire.jsword.passage.Key)
     */
    override fun addAll(key: Key?) {
        //check for key empty. This avoids the AIOBounds with that.getVerseAt, during event firing
        if (key!!.isEmpty()) {
            //nothing to add
            return
        }

        optimizeWrites()
        raiseEventSuppresion()
        raiseNormalizeProtection()


        if (key is RangedPassage) {
            val it = key.rangeIterator(RestrictionType.NONE)
            while (it.hasNext()) {
                // Avoid touching store to make thread safety easier.
                add(it.next())
            }
        } else {
            for (subkey in key) {
                add(subkey)
            }
        }

        lowerNormalizeProtection()
        if (lowerEventSuppressionAndTest()) {
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

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#removeAll(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        optimizeWrites()
        raiseEventSuppresion()
        raiseNormalizeProtection()

        if (key is RangedPassage) {
            val it = key.rangeIterator(RestrictionType.NONE)
            while (it.hasNext()) {
                // Avoid touching store to make thread safety easier.
                remove(it.next())
            }
        } else {
            val it = key!!.iterator()
            while (it.hasNext()) {
                // Avoid touching store to make thread safety easier.
                remove(it.next())
            }
        }

        lowerNormalizeProtection()
        if (lowerEventSuppressionAndTest()) {
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

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#retainAll(org.crosswire.jsword.passage.Key)
     */
    override fun retainAll(key: Key?) {
        optimizeWrites()
        raiseEventSuppresion()
        raiseNormalizeProtection()

        val temp: Passage = this.clone()
        for (verse in temp) {
            if (!key!!.contains(verse)) {
                remove(verse)
            }
        }

        lowerNormalizeProtection()
        if (lowerEventSuppressionAndTest()) {
            fireIntervalRemoved(this, null, null)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
        optimizeWrites()
        raiseNormalizeProtection()

        remove(versification.allVerses)

        if (lowerEventSuppressionAndTest()) {
            fireIntervalRemoved(this, null, null)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType, boolean, boolean)
     */
    override fun blur(verses: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        optimizeWrites()
        raiseEventSuppresion()
        raiseNormalizeProtection()

        val temp: Passage = this.clone()
        val it = temp.rangeIterator(RestrictionType.NONE)

        while (it.hasNext()) {
            val range = restrict!!.blur(versification, it.next(), if (blurDown) verses else 0, if (blurUp) verses else 0)
            add(range)
        }

        lowerNormalizeProtection()
        if (lowerEventSuppressionAndTest()) {
            fireIntervalAdded(this, null, null)
        }
    }


    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(verses: Int, restrict: RestrictionType?) {
        blur(verses, restrict, true, true)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#writeDescription(java.io.Writer)
     */
    @Throws(IOException::class)
    override fun writeDescription(out: Writer?) {
        val bout = BufferedWriter(out)
        bout.write(v11n!!.name)
        bout.newLine()

        val it = rangeIterator(RestrictionType.NONE)

        while (it.hasNext()) {
            val range: Key = it.next()
            bout.write(range.getName())
            bout.newLine()
        }

        bout.flush()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#readDescription(java.io.Reader)
     */
    @Throws(IOException::class, NoSuchVerseException::class)
    override fun readDescription(`in`: Reader?) {
        raiseEventSuppresion()
        raiseNormalizeProtection()

        var count = 0 // number of lines read
        // Quiet Android from complaining about using the default BufferReader buffer size.
        // The actual buffer size is undocumented. So this is a good idea any way.
        val bin = BufferedReader(`in`, 8192)

        val v11nName = bin.readLine()
        v11n = Versifications.instance().getVersification(v11nName)

        while (true) {
            val line = bin.readLine() ?: break

            count++
            addVerses(line, null)
        }

        // If the file was empty then there is nothing to do
        if (count == 0) {
            return
        }

        lowerNormalizeProtection()
        if (lowerEventSuppressionAndTest()) {
            fireIntervalAdded(this, getVerseAt(0), getVerseAt(countVerses() - 1))
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#optimizeReads()
     */
    override fun optimizeReads() {
    }

    /**
     * Simple method to instruct children to stop caching results
     */
    protected open fun optimizeWrites() {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#addPassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    override fun addPassageListener(li: PassageListener?) {
        synchronized(listeners) {
            listeners.add(li)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#removePassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    override fun removePassageListener(li: PassageListener?) {
        synchronized(listeners) {
            listeners.remove(li)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(that: Key?): Boolean {
        val ref = KeyUtil.getPassage(that)
        return containsAll(ref)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        return countVerses()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        var index = 0
        for (key in this) {
            if (key == that) {
                return index
            }

            index++
        }

        return -1
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#canHaveChildren()
     */
    override fun canHaveChildren(): Boolean {
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getChildCount()
     */
    override fun getChildCount(): Int {
        return 0
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        return getVerseAt(index)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        return parent
    }

    /**
     * Set a parent Key. This allows us to follow the Key interface more
     * closely, although the concept of a parent for a verse is fairly alien.
     *
     * @param parent
     *            The parent Key for this verse
     */
    open fun setParent(parent: Key?) {
        this.parent = parent
    }

    /**
     * AbstractPassage subclasses must call this method <b>after</b> one or more
     * elements of the list are added. The changed elements are specified by a
     * closed interval from start to end.
     *
     * @param source
     *            The thing that changed, typically "this".
     * @param start
     *            One end of the new interval.
     * @param end
     *            The other end of the new interval.
     * @see PassageListener
     */
    protected open fun fireIntervalAdded(source: Any?, start: Verse?, end: Verse?) {
        if (suppressEvents != 0) {
            return
        }

        // Create Event
        val ev = PassageEvent(source, PassageEvent.EventType.ADDED, start, end)

        // Copy listener vector so it won't change while firing
        val temp: MutableList<PassageListener?>
        synchronized(listeners) {
            temp = ArrayList<PassageListener?>()
            temp.addAll(listeners)
        }

        // And run through the list shouting
        for (i in 0 until temp.size) {
            val rl = temp[i]
            rl!!.versesAdded(ev)
        }
    }

    /**
     * AbstractPassage subclasses must call this method <b>before</b> one or
     * more elements of the list are added. The changed elements are specified
     * by a closed interval from start to end.
     *
     * @param source
     *            The thing that changed, typically "this".
     * @param start
     *            One end of the new interval.
     * @param end
     *            The other end of the new interval.
     * @see PassageListener
     */
    protected open fun fireIntervalRemoved(source: Any?, start: Verse?, end: Verse?) {
        if (suppressEvents != 0) {
            return
        }

        // Create Event
        val ev = PassageEvent(source, PassageEvent.EventType.REMOVED, start, end)

        // Copy listener vector so it won't change while firing
        val temp: MutableList<PassageListener?>
        synchronized(listeners) {
            temp = ArrayList<PassageListener?>()
            temp.addAll(listeners)
        }

        // And run through the list shouting
        for (i in 0 until temp.size) {
            val rl = temp[i]
            rl!!.versesRemoved(ev)
        }
    }

    /**
     * AbstractPassage subclasses must call this method <b>before</b> one or
     * more elements of the list are added. The changed elements are specified
     * by a closed interval from start to end.
     *
     * @param source
     *            The thing that changed, typically "this".
     * @param start
     *            One end of the new interval.
     * @param end
     *            The other end of the new interval.
     * @see PassageListener
     */
    protected open fun fireContentsChanged(source: Any?, start: Verse?, end: Verse?) {
        if (suppressEvents != 0) {
            return
        }

        // Create Event
        val ev = PassageEvent(source, PassageEvent.EventType.CHANGED, start, end)

        // Copy listener vector so it won't change while firing
        val temp: MutableList<PassageListener?>
        synchronized(listeners) {
            temp = ArrayList<PassageListener?>()
            temp.addAll(listeners)
        }

        // And run through the list shouting
        for (i in 0 until temp.size) {
            val rl = temp[i]
            rl!!.versesChanged(ev)
        }
    }

    /**
     * Create a Passage from a human readable string. The opposite of
     * <code>toString()</code>. Since this method is not public it leaves
     * control of <code>suppress_events</code> up to the people
     * that call it.
     *
     * @param refs
     *            A String containing the text of the RangedPassage
     * @param basis
     *            The basis for understanding refs
     * @throws NoSuchVerseException
     *             if the string is invalid
     */
    @Throws(NoSuchVerseException::class)
    protected open fun addVerses(refs: String?, basis: Key?) {
        optimizeWrites()

        val parts = StringUtil.split(refs, REF_ALLOWED_DELIMS)
        if (parts.size == 0) {
            return
        }

        var start = 0
        var vrBasis: VerseRange? = null
        if (basis is Verse) {
            vrBasis = VerseRange(v11n, basis)
        } else if (basis is VerseRange) {
            vrBasis = basis
        } else {
            // If we are not passed a useful basis,
            // then we treat the first as a special case because there is
            // nothing to sensibly base this reference on
            vrBasis = VerseRangeFactory.fromString(v11n, parts[0].trim { it <= ' ' })
            // We add it because it was part of the given input
            add(vrBasis)
            start = 1
        }

        // Loop for the other verses, interpreting each on the
        // basis of the one before.
        for (i in start until parts.size) {
            val next = VerseRangeFactory.fromString(v11n, parts[i].trim { it <= ' ' }, vrBasis)
            add(next)
            vrBasis = next
        }
    }

    /**
     * We sometimes need to sort ourselves out ... I don't think we need to be
     * synchronized since we are private and we could check that all public
     * calling of normalize() are synchronized, however this is safe, and I
     * don't think there is a cost associated with a double synchronize. (?)
     */
    /* protected */ // was package-private
    internal open fun normalize() {
        // before doing any normalization we should be checking that
        // skip_normalization == 0, and just returning if so.
    }

    /**
     * If things want to prevent normalization because they are doing a set of
     * changes that should be normalized in one go, this is what to call. Be
     * sure to call lowerNormalizeProtection() when you are done.
     */
    open fun raiseNormalizeProtection() {
        skipNormalization++

        if (skipNormalization > 10) {
            // This is a bit drastic and does not give us much
            // chance to fix the error
            // throw new LogicError();

            log.warn("skip_normalization={}", Integer.toString(skipNormalization))
        }
    }

    /**
     * If things want to prevent normalization because they are doing a set of
     * changes that should be normalized in one go, they should call
     * raiseNormalizeProtection() and when done call this. This also calls
     * normalize() if the count reaches zero.
     */
    open fun lowerNormalizeProtection() {
        skipNormalization--

        if (skipNormalization == 0) {
            normalize()
        }

        jswordAssert(skipNormalization >= 0) { "" }
    }

    /**
     * If things want to prevent event firing because they are doing a set of
     * changes that should be notified in one go, this is what to call. Be sure
     * to call lowerEventSuppression() when you are done.
     */
    open fun raiseEventSuppresion() {
        suppressEvents++

        if (suppressEvents > 10) {
            // This is a bit drastic and does not give us much
            // chance to fix the error
            // throw new LogicError();

            log.warn("suppress_events={}", Integer.toString(suppressEvents))
        }
    }

    /**
     * If things want to prevent event firing because they are doing a set of
     * changes that should be notified in one go, they should call
     * raiseEventSuppression() and when done call this.
     *
     * @return true if it is then safe to fire an event.
     */
    open fun lowerEventSuppressionAndTest(): Boolean {
        suppressEvents--
        jswordAssert(suppressEvents >= 0) { "" }

        return suppressEvents == 0
    }

    /**
     * Skip over verses that are part of a range
     */
    // was protected: VerseRange (same package, not a subclass) creates it, and Kotlin protected has no package access
    internal class VerseRangeIterator
    /**
     * iterate, amalgamating Verses into VerseRanges
     */
    internal constructor(v11n: Versification, it: MutableIterator<Key>, restrict: RestrictionType?) : MutableIterator<VerseRange> {
        /**
         * The Versification to which these verses belong.
         */
        private var v11n: Versification

        /**
         * The Iterator that we are proxying to
         */
        private var it: MutableIterator<Key>

        /**
         * What is the next VerseRange to be considered
         */
        private var nextRange: VerseRange? = null

        /**
         * What is the next Verse to be considered
         */
        private var nextVerse: Verse? = null

        /**
         * Do we restrict ranges to not crossing chapter boundaries
         */
        private var restrict: RestrictionType?

        init {
            this.v11n = v11n
            this.it = it
            this.restrict = restrict

            if (it.hasNext()) {
                nextVerse = it.next() as Verse
            }

            calculateNext()
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return nextRange != null
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Throws(NoSuchElementException::class)
        override fun next(): VerseRange {
            val retcode = nextRange

            if (retcode == null) {
                throw NoSuchElementException()
            }

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
         * Find the next VerseRange
         */
        private fun calculateNext() {
            if (nextVerse == null) {
                nextRange = null
                return
            }

            val start = nextVerse
            var end = nextVerse

            findnext@ while (true) {
                if (!it.hasNext()) {
                    nextVerse = null
                    break
                }

                nextVerse = it.next() as Verse

                // If the next verse adjacent
                if (!v11n.isAdjacentVerse(end, nextVerse)) {
                    break
                }

                // Even if the next verse is adjacent we might want to break
                // if we have moved into a new chapter/book
                if (!restrict!!.isSameScope(v11n, end, nextVerse)) {
                    break@findnext
                }

                end = nextVerse
            }

            nextRange = VerseRange(v11n, start, end)
        }
    }

    /**
     * Write out the object to the given ObjectOutputStream. There are 3 ways of
     * doing this - according to the 3 implementations of Passage.
     * <ul>
     * <li>Distinct: If we write out a list if verse ordinals then the space
     * used is 4 bytes per verse.
     * <li>Bitwise: If we write out a bitmap then the space used is something
     * like 31104/8 = 4k bytes.
     * <li>Ranged: The we write a list of start/end pairs then the space used is
     * 8 bytes per range.
     * </ul>
     * Since we can take our time about this section, we calculate the optimal
     * storage method before we do the saving. If some methods come out equal
     * first then bitwise is preferred, then distinct, then ranged, because I
     * imagine that for speed of deserialization this is the sensible order.
     * I've not tested it though.
     *
     * @param out
     *            The stream to write our state to
     * @throws IOException
     *             if the read fails
     */
    @Throws(IOException::class)
    protected open fun writeObjectSupport(out: ObjectOutputStream) {
        // Save off the versification by name
        out.writeUTF(v11n!!.name)

        // the size in bits of teach storage method
        val bitwiseSize = v11n!!.maximumOrdinal()
        val rangedSize = 8 * countRanges(RestrictionType.NONE)
        val distinctSize = 4 * countVerses()

        // if bitwise is equal smallest
        if (bitwiseSize <= rangedSize && bitwiseSize <= distinctSize) {
            out.writeInt(BITWISE)

            val store = BitSet(bitwiseSize)
            val iter = iterator()
            while (iter.hasNext()) {
                val verse = iter.next() as Verse
                store.set(verse.ordinal)
            }

            out.writeObject(store)
        } else if (distinctSize <= rangedSize) {
            // if distinct is not bigger than ranged
            // write the Passage type and the number of verses
            out.writeInt(DISTINCT)
            out.writeInt(countVerses())

            // write the verse ordinals in a loop
            for (aKey in this) {
                val verse = aKey as Verse
                out.writeInt(verse.ordinal)
            }
        } else {
            // otherwise use ranges
            // write the Passage type and the number of ranges
            out.writeInt(RANGED)
            out.writeInt(countRanges(RestrictionType.NONE))

            // write the verse ordinals in a loop
            val it = rangeIterator(RestrictionType.NONE)
            while (it.hasNext()) {
                val range = it.next()
                out.writeInt(range.start.ordinal)
                out.writeInt(range.getCardinality())
            }
        }

        // Ignore the original name. Is this wise?
        // I am expecting that people are not that fussed about it and
        // it could make everything far more verbose
    }

    /**
     * Serialization support.
     *
     * @param is
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`is`: ObjectInputStream) {
        listeners = ArrayList<PassageListener?>()
        originalName = null
        parent = null
        skipNormalization = 0
        suppressEvents = 0

        `is`.defaultReadObject()
    }

    /**
     * Write out the object to the given ObjectOutputStream
     *
     * @param is
     *            The stream to read our state from
     * @throws IOException
     *             if the read fails
     * @throws ClassNotFoundException
     *             If the read data is incorrect
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    protected open fun readObjectSupport(`is`: ObjectInputStream) {
        raiseEventSuppresion()
        raiseNormalizeProtection()

        // Read the versification by name
        val v11nName = `is`.readUTF()
        v11n = Versifications.instance().getVersification(v11nName)

        val type = `is`.readInt()
        when (type) {
            BITWISE -> {
                val store = `is`.readObject() as BitSet
                var i = 0
                while (i < v11n!!.maximumOrdinal()) {
                    if (store.get(i)) {
                        add(v11n!!.decodeOrdinal(i))
                    }
                    i++
                }
            }

            DISTINCT -> {
                val verses = `is`.readInt()
                for (i in 0 until verses) {
                    val ord = `is`.readInt()
                    add(v11n!!.decodeOrdinal(ord))
                }
            }

            RANGED -> {
                val ranges = `is`.readInt()
                for (i in 0 until ranges) {
                    val ord = `is`.readInt()
                    val count = `is`.readInt()
                    add(RestrictionType.NONE.toRange(versification, v11n!!.decodeOrdinal(ord), count))
                }
            }

            else -> throw ClassCastException(JSOtherMsg.lookupText("Can only use Verses and VerseRanges in this Collection"))
        }

        // We are ignoring the originalName. It was set to null in the
        // default ctor so I will ignore it here.

        // We don't bother to call fireContentsChanged(...) because
        // nothing can have registered at this point
        lowerEventSuppressionAndTest()
        lowerNormalizeProtection()
    }

    /**
     * The Versification to which this passage belongs.
     */
    @Transient
    private var v11n: Versification?

    /**
     * The parent key. See the key interface for more information. NOTE(joe):
     * These keys are not serialized, should we?
     *
     * @see Key
     */
    @Transient
    private var parent: Key? = null

    /**
     * Support for change notification
     */
    @JvmField
    @Transient
    protected var listeners: MutableList<PassageListener?>

    /**
     * The original string for picky users
     */
    @JvmField
    @Transient
    protected var originalName: String?

    /**
     * If we have several changes to make then we increment this and then
     * decrement it when done (and fire an event off). If the cost of
     * calculating the parameters to the fire is high then we can check that
     * this is 0 before doing the calculation.
     */
    @JvmField
    @Transient
    protected var suppressEvents: Int = 0

    /**
     * Do we skip normalization for now - if we want to skip then we increment
     * this, and the decrement it when done.
     */
    @JvmField
    @Transient
    protected var skipNormalization: Int = 0

    companion object {
        /**
         * Convert the Object to a VerseRange. If base is a Verse then return a
         * VerseRange of zero length.
         *
         * @param base
         *            The object to be cast
         * @return The VerseRange
         * @exception java.lang.ClassCastException
         *                If this is not a Verse or a VerseRange
         */
        @JvmStatic
        @Throws(ClassCastException::class)
        protected fun toVerseRange(v11n: Versification?, base: Any?): VerseRange {
            jswordAssert(base != null) { "" }

            if (base is VerseRange) {
                return base
            } else if (base is Verse) {
                return VerseRange(v11n, base)
            }

            throw ClassCastException(JSOtherMsg.lookupText("Can only use Verses and VerseRanges in this Collection"))
        }

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(AbstractPassage::class.java)

        /**
         * Serialization type constant for a BitWise layout
         */
        // was protected
        const val BITWISE: Int = 0

        /**
         * Serialization type constant for a Distinct layout
         */
        // was protected
        const val DISTINCT: Int = 1

        /**
         * Serialization type constant for a Ranged layout
         */
        // was protected
        const val RANGED: Int = 2

        /**
         * Count of serializations methods
         */
        // was protected
        const val METHOD_COUNT: Int = 3

        /**
         * What characters can we use to separate VerseRanges in a Passage
         */
        const val REF_ALLOWED_DELIMS: String = ",;\n\r\t"

        /**
         * What characters should we use to separate VerseRanges in a Passage
         */
        const val REF_PREF_DELIM: String = ", "

        /**
         * What characters should we use to separate VerseRanges in a Passage
         */
        const val REF_OSIS_DELIM: String = " "

        /**
         * Serialization ID
         */
        private const val serialVersionUID: Long = -5931560451407396276L
    }
}
