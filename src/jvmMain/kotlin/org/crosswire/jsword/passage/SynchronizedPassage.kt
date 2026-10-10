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

import org.crosswire.common.util.ArrayIndexOutOfBoundsException

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

import okio.IOException

import org.crosswire.jsword.versification.Versification

/**
 * This is a simple proxy to a real Passage object that makes all accesses
 * synchronized. It is final to give the VM as much hope as possible at being
 * able to inline stuff.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
internal class SynchronizedPassage : Passage {
    private val lock = SynchronizedObject()
    /** Copies the fields and preserves the deep-copy steps of the original clone. */
    protected constructor(copyFrom: SynchronizedPassage) {
        ref = synchronized(copyFrom.lock) { copyFrom.ref.clone() as Passage }
    }

    /**
     * Construct a SynchronizedPassage from a real Passage to which we proxy.
     *
     * @param ref
     *            The real Passage
     */
    constructor(ref: Passage?) {
        this.ref = ref!!
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getVersification()
     */
    override val versification: Versification
        get() = ref.versification

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#reversify(org.crosswire.jsword.versification.Versification)
     */
    override fun reversify(newVersification: Versification?): Passage? {
        return ref.reversify(newVersification)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#isWhole()
     */
    override val isWhole: Boolean
        get() = ref.isWhole

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#getWhole()
     */
    override val whole: Passage
        get() = ref.whole

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#addAll(org.crosswire.jsword.passage.Key)
     */
    override fun addAll(key: Key?) {
        synchronized(lock) {
            ref.addAll(key)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#removeAll(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        synchronized(lock) {
            ref.removeAll(key)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#retainAll(org.crosswire.jsword.passage.Key)
     */
    override fun retainAll(key: Key?) {
        synchronized(lock) {
            ref.retainAll(key)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(that: Key?): Boolean {
        synchronized(lock) {
            return ref.contains(that)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getChildCount()
     */
    override fun getChildCount(): Int {
        synchronized(lock) {
            return ref.getChildCount()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        synchronized(lock) {
            return ref.getCardinality()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#canHaveChildren()
     */
    override fun canHaveChildren(): Boolean {
        synchronized(lock) {
            return ref.canHaveChildren()
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        synchronized(lock) {
            return ref.iterator()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        synchronized(lock) {
            return ref.get(index)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        synchronized(lock) {
            return ref.indexOf(that)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        synchronized(lock) {
            return ref.getParent()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName()
     */
    override fun getName(): String {
        synchronized(lock) {
            return ref.getName()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName(org.crosswire.jsword.passage.Key)
     */
    override fun getName(base: Key?): String {
        synchronized(lock) {
            return ref.getName(base)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    override fun getRootName(): String {
        synchronized(lock) {
            return ref.getRootName()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    override fun getOsisRef(): String {
        synchronized(lock) {
            return ref.getOsisRef()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    override fun getOsisID(): String {
        synchronized(lock) {
            return ref.getOsisID()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getOverview()
     */
    override val overview: String
        get() = synchronized(lock) { ref.overview }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        synchronized(lock) {
            return ref.isEmpty()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countVerses()
     */
    override fun countVerses(): Int {
        synchronized(lock) {
            return ref.countVerses()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#hasRanges(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun hasRanges(restrict: RestrictionType?): Boolean {
        synchronized(lock) {
            return ref.hasRanges(restrict)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countRanges(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun countRanges(restrict: RestrictionType?): Int {
        synchronized(lock) {
            return ref.countRanges(restrict)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimVerses(int)
     */
    override fun trimVerses(count: Int): Passage? {
        synchronized(lock) {
            return ref.trimVerses(count)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimRanges(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun trimRanges(count: Int, restrict: RestrictionType?): Passage? {
        synchronized(lock) {
            return ref.trimRanges(count, restrict)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#booksInPassage()
     */
    override fun booksInPassage(): Int {
        synchronized(lock) {
            return ref.booksInPassage()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getVerseAt(int)
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getVerseAt(offset: Int): Verse {
        synchronized(lock) {
            return ref.getVerseAt(offset)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getRangeAt(int, org.crosswire.jsword.passage.RestrictionType)
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getRangeAt(offset: Int, restrict: RestrictionType?): VerseRange {
        synchronized(lock) {
            return ref.getRangeAt(offset, restrict)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#rangeIterator(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange> {
        synchronized(lock) {
            return ref.rangeIterator(restrict)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#add(org.crosswire.jsword.passage.Key)
     */
    override fun add(that: Key?) {
        synchronized(lock) {
            ref.add(that)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#remove(org.crosswire.jsword.passage.Key)
     */
    override fun remove(that: Key?) {
        synchronized(lock) {
            ref.remove(that)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#containsAll(org.crosswire.jsword.passage.Passage)
     */
    override fun containsAll(that: Passage?): Boolean {
        synchronized(lock) {
            return ref.containsAll(that)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
        synchronized(lock) {
            ref.clear()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(by: Int, restrict: RestrictionType?) {
        synchronized(lock) {
            blur(by, restrict, true, true)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType, boolean, boolean)
     */
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        synchronized(lock) {
            ref.blur(by, restrict, blurDown, blurUp)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#readDescription(String)
     */
    @Throws(IOException::class, NoSuchVerseException::class)
    override fun readDescription(text: String) {
        synchronized(lock) {
            ref.readDescription(text)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#writeDescription(Appendable)
     */
    @Throws(IOException::class)
    override fun writeDescription(out: Appendable) {
        synchronized(lock) {
            ref.writeDescription(out)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#optimizeReads()
     */
    override fun optimizeReads() {
        synchronized(lock) {
            ref.optimizeReads()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#addPassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    override fun addPassageListener(li: PassageListener?) {
        synchronized(lock) {
            ref.addPassageListener(li)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#removePassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    override fun removePassageListener(li: PassageListener?) {
        synchronized(lock) {
            ref.removePassageListener(li)
        }
    }

    override fun clone(): SynchronizedPassage {
        synchronized(lock) {
            return SynchronizedPassage(this)
        }
    }

    override fun hashCode(): Int {
        synchronized(lock) {
            return ref.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        synchronized(lock) {
            return ref.equals(other)
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Key?): Int {
        synchronized(lock) {
            return ref.compareTo(other)
        }
    }

    /**
     * The object we are proxying to
     */
    private var ref: Passage

}
