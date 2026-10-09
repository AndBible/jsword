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
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.versification.Versification

/**
 * This is a simple proxy to a real Passage object that denies all attempts to
 * write to it.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
internal class ReadOnlyPassage : Passage, Cloneable {
    /**
     * Construct a ReadOnlyPassage from a real Passage to which we proxy.
     *
     * @param ref
     *            The real Passage
     * @param ignore
     *            Do we throw up if someone tries to change us
     */
    constructor(ref: Passage?, ignore: Boolean) {
        this.ref = ref!!
        this.ignore = ignore
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
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only passage"))
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#remove(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only passage"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#retainAll(org.crosswire.jsword.passage.Key)
     */
    override fun retainAll(key: Key?) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only passage"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(that: Key?): Boolean {
        return ref.contains(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#canHaveChildren()
     */
    override fun canHaveChildren(): Boolean {
        return ref.canHaveChildren()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getChildCount()
     */
    override fun getChildCount(): Int {
        return ref.getChildCount()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        return ref.getCardinality()
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return ref.iterator()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        return ref.get(index)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        return ref.indexOf(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        return ref.getParent()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName()
     */
    override fun getName(): String {
        return ref.getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName(org.crosswire.jsword.passage.Key)
     */
    override fun getName(base: Key?): String {
        return ref.getName(base)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    override fun getRootName(): String {
        return ref.getRootName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    override fun getOsisRef(): String {
        return ref.getOsisRef()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    override fun getOsisID(): String {
        return ref.getOsisID()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getOverview()
     */
    override val overview: String
        get() = ref.overview

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        return ref.isEmpty()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countVerses()
     */
    override fun countVerses(): Int {
        return ref.countVerses()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#hasRanges(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun hasRanges(restrict: RestrictionType?): Boolean {
        return ref.hasRanges(restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countRanges(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun countRanges(restrict: RestrictionType?): Int {
        return ref.countRanges(restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimVerses(int)
     */
    override fun trimVerses(count: Int): Passage? {
        return ref.trimVerses(count)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimRanges(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun trimRanges(count: Int, restrict: RestrictionType?): Passage? {
        return ref.trimRanges(count, restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#booksInPassage()
     */
    override fun booksInPassage(): Int {
        return ref.booksInPassage()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getVerseAt(int)
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getVerseAt(offset: Int): Verse {
        return ref.getVerseAt(offset)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getRangeAt(int, org.crosswire.jsword.passage.RestrictionType)
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun getRangeAt(offset: Int, restrict: RestrictionType?): VerseRange {
        return ref.getRangeAt(offset, restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#rangeIterator(org.crosswire.jsword.passage.RestrictionType)
     */
    override fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange> {
        return ref.rangeIterator(restrict)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#add(org.crosswire.jsword.passage.Key)
     */
    override fun add(that: Key?) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only passage"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#remove(org.crosswire.jsword.passage.Key)
     */
    override fun remove(that: Key?) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only passage"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#containsAll(org.crosswire.jsword.passage.Passage)
     */
    override fun containsAll(that: Passage?): Boolean {
        return ref.containsAll(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only passage"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(by: Int, restrict: RestrictionType?) {
        blur(by, restrict, true, true)
    }


    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType, boolean, boolean)
     */
    @Synchronized
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only passage"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#readDescription(java.io.Reader)
     */
    override fun readDescription(`in`: Reader?) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only passage"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#writeDescription(java.io.Writer)
     */
    @Throws(IOException::class)
    override fun writeDescription(out: Writer?) {
        ref.writeDescription(out)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#optimizeReads()
     */
    override fun optimizeReads() {
        ref.optimizeReads()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#addPassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    override fun addPassageListener(li: PassageListener?) {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#removePassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    override fun removePassageListener(li: PassageListener?) {
    }

    override fun equals(other: Any?): Boolean {
        return ref.equals(other)
    }

    override fun hashCode(): Int {
        return ref.hashCode()
    }

    override fun toString(): String {
        return ref.toString()
    }

    override fun clone(): ReadOnlyPassage {
        var clone: ReadOnlyPassage? = null
        try {
            clone = super<Cloneable>.clone() as ReadOnlyPassage
            clone.ref = this.ref
            clone.ignore = this.ignore
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }

        return clone!!
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Key?): Int {
        return ref.compareTo(other)
    }

    /**
     * The object we are proxying to
     */
    private var ref: Passage

    /**
     * Do we just silently ignore change attempts or throw up
     */
    private var ignore: Boolean

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID: Long = 3257853173036102193L
    }
}
