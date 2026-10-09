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

import java.io.IOException
import java.io.Reader
import java.io.Writer

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.versification.Versification

/**
 * This is a simple proxy to a real Passage object that makes all accesses
 * synchronized. It is final to give the VM as much hope as possible at being
 * able to inline stuff.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
internal class SynchronizedPassage : Passage, Cloneable {
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
    @Synchronized
    override fun addAll(key: Key?) {
        ref.addAll(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#removeAll(org.crosswire.jsword.passage.Key)
     */
    @Synchronized
    override fun removeAll(key: Key?) {
        ref.removeAll(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#retainAll(org.crosswire.jsword.passage.Key)
     */
    @Synchronized
    override fun retainAll(key: Key?) {
        ref.retainAll(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#contains(org.crosswire.jsword.passage.Key)
     */
    @Synchronized
    override fun contains(that: Key?): Boolean {
        return ref.contains(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getChildCount()
     */
    @Synchronized
    override fun getChildCount(): Int {
        return ref.getChildCount()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    @Synchronized
    override fun getCardinality(): Int {
        return ref.getCardinality()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#canHaveChildren()
     */
    @Synchronized
    override fun canHaveChildren(): Boolean {
        return ref.canHaveChildren()
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Synchronized
    override fun iterator(): MutableIterator<Key> {
        return ref.iterator()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    @Synchronized
    override fun get(index: Int): Key? {
        return ref.get(index)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    @Synchronized
    override fun indexOf(that: Key?): Int {
        return ref.indexOf(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    @Synchronized
    override fun getParent(): Key? {
        return ref.getParent()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName()
     */
    @Synchronized
    override fun getName(): String {
        return ref.getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName(org.crosswire.jsword.passage.Key)
     */
    @Synchronized
    override fun getName(base: Key?): String {
        return ref.getName(base)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    @Synchronized
    override fun getRootName(): String {
        return ref.getRootName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    @Synchronized
    override fun getOsisRef(): String {
        return ref.getOsisRef()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    @Synchronized
    override fun getOsisID(): String {
        return ref.getOsisID()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getOverview()
     */
    override val overview: String
        @Synchronized get() = ref.overview

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    @Synchronized
    override fun isEmpty(): Boolean {
        return ref.isEmpty()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countVerses()
     */
    @Synchronized
    override fun countVerses(): Int {
        return ref.countVerses()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#hasRanges(org.crosswire.jsword.passage.RestrictionType)
     */
    @Synchronized
    override fun hasRanges(restrict: RestrictionType?): Boolean {
        return ref.hasRanges(restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countRanges(org.crosswire.jsword.passage.RestrictionType)
     */
    @Synchronized
    override fun countRanges(restrict: RestrictionType?): Int {
        return ref.countRanges(restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimVerses(int)
     */
    @Synchronized
    override fun trimVerses(count: Int): Passage? {
        return ref.trimVerses(count)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimRanges(int, org.crosswire.jsword.passage.RestrictionType)
     */
    @Synchronized
    override fun trimRanges(count: Int, restrict: RestrictionType?): Passage? {
        return ref.trimRanges(count, restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#booksInPassage()
     */
    @Synchronized
    override fun booksInPassage(): Int {
        return ref.booksInPassage()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getVerseAt(int)
     */
    @Synchronized
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getVerseAt(offset: Int): Verse {
        return ref.getVerseAt(offset)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getRangeAt(int, org.crosswire.jsword.passage.RestrictionType)
     */
    @Synchronized
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getRangeAt(offset: Int, restrict: RestrictionType?): VerseRange {
        return ref.getRangeAt(offset, restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#rangeIterator(org.crosswire.jsword.passage.RestrictionType)
     */
    @Synchronized
    override fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange> {
        return ref.rangeIterator(restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#add(org.crosswire.jsword.passage.Key)
     */
    @Synchronized
    override fun add(that: Key?) {
        ref.add(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#remove(org.crosswire.jsword.passage.Key)
     */
    @Synchronized
    override fun remove(that: Key?) {
        ref.remove(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#containsAll(org.crosswire.jsword.passage.Passage)
     */
    @Synchronized
    override fun containsAll(that: Passage?): Boolean {
        return ref.containsAll(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    @Synchronized
    override fun clear() {
        ref.clear()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    @Synchronized
    override fun blur(by: Int, restrict: RestrictionType?) {
        blur(by, restrict, true, true)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType, boolean, boolean)
     */
    @Synchronized
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        ref.blur(by, restrict, blurDown, blurUp)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#readDescription(java.io.Reader)
     */
    @Synchronized
    @Throws(IOException::class, NoSuchVerseException::class)
    override fun readDescription(`in`: Reader?) {
        ref.readDescription(`in`)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#writeDescription(java.io.Writer)
     */
    @Synchronized
    @Throws(IOException::class)
    override fun writeDescription(out: Writer?) {
        ref.writeDescription(out)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#optimizeReads()
     */
    @Synchronized
    override fun optimizeReads() {
        ref.optimizeReads()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#addPassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    @Synchronized
    override fun addPassageListener(li: PassageListener?) {
        ref.addPassageListener(li)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#removePassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    @Synchronized
    override fun removePassageListener(li: PassageListener?) {
        ref.removePassageListener(li)
    }

    @Synchronized
    override fun clone(): SynchronizedPassage {
        var clone: SynchronizedPassage? = null
        try {
            val copy = super<Cloneable>.clone() as SynchronizedPassage
            clone = copy
            synchronized(copy) {
                copy.ref = ref.clone() as Passage
            }
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }
        return clone!!
    }

    @Synchronized
    override fun hashCode(): Int {
        return ref.hashCode()
    }

    @Synchronized
    override fun equals(other: Any?): Boolean {
        return ref.equals(other)
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Synchronized
    override fun compareTo(other: Key?): Int {
        return ref.compareTo(other)
    }

    /**
     * The object we are proxying to
     */
    private var ref: Passage

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID: Long = 3833181441264531251L
    }
}
