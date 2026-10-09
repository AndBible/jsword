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

import org.crosswire.common.icu.NumberShaper
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.versification.Versification
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Locale
import java.util.NoSuchElementException

/**
 * A VerseRange is one step between a Verse and a Passage - it is a Verse plus a
 * verseCount. Every VerseRange has a start, a verseCount and an end. A
 * VerseRange is designed to be immutable. This is a necessary from a
 * collections point of view. A VerseRange should always be valid, although some
 * versions may not return any text for verses that they consider to be
 * miss-translated in some way.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class VerseRange : VerseKey<VerseRange>, Cloneable {
    /**
     * The default VerseRange is a single verse - Genesis 1:1. I didn't want to
     * provide this constructor however, you are supposed to provide a default
     * ctor for all beans. For this reason I suggest you don't use it.
     *
     * @param v11n
     *            The versification for the range
     */
    constructor(v11n: Versification?) : this(v11n, Verse.DEFAULT, Verse.DEFAULT)

    /**
     * Construct a VerseRange from a Verse. The resultant VerseRange will be 1
     * verse in verseCount.
     *
     * @param v11n
     *            The versification for the range
     * @param start
     *            The verse to start from
     */
    constructor(v11n: Versification?, start: Verse?) : this(v11n, start, start)

    constructor(v11n: Versification?, start: Verse?, end: Verse?) {
        jswordAssert(v11n != null) { "" }
        jswordAssert(start != null) { "" }
        jswordAssert(end != null) { "" }

        this.v11n = v11n!!
        shaper = NumberShaper()

        val distance = this.v11n.distance(start, end)

        if (distance < 0) {
            this.start = end!!
            this.end = start!!
            this.verseCount = calcVerseCount()
        } else if (distance == 0) {
            this.start = start!!
            this.end = start
            this.verseCount = 1
        } else {
            this.start = start!!
            this.end = end!!
            this.verseCount = calcVerseCount()
        }
    }

    /**
     * Merge 2 VerseRanges together. The resulting range will encompass
     * Everything in-between the extremities of the 2 ranges.
     *
     * @param a
     *            The first verse range to be merged
     * @param b
     *            The second verse range to be merged
     */
    constructor(a: VerseRange?, b: VerseRange?) {
        v11n = a!!.v11n
        shaper = NumberShaper()
        start = v11n.min(a.start, b!!.start)!!
        end = v11n.max(a.end, b.end)!!
        verseCount = calcVerseCount()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#getVersification()
     */
    override val versification: Versification
        get() = v11n

    override fun isValidIn(targetVersification: Versification?): Boolean {
        val verseIterator = iterator()
        while (verseIterator.hasNext()) {
            if (!(verseIterator.next() as Verse).isValidIn(targetVersification)) {
                return false
            }
        }
        return true
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#reversify(org.crosswire.jsword.versification.Versification)
     */
    override fun reversify(newVersification: Versification?): VerseRange? {
        if (v11n.equals(newVersification)) {
            return this
        }
        val newStart = start.reversify(newVersification)
        if (newStart == null) {
            return null
        }
        val newEnd = end.reversify(newVersification)
        if (newEnd == null) {
            return null
        }
        return VerseRange(newVersification, newStart, newEnd)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#isWhole()
     */
    override val isWhole: Boolean
        get() = start.isWhole && end.isWhole

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#getWhole()
     */
    override val whole: VerseRange
        get() {
            if (isWhole) {
                return this
            }
            return VerseRange(v11n, start.whole, end.whole)
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName()
     */
    override fun getName(): String {
        return getName(null)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName(org.crosswire.jsword.passage.Key)
     */
    override fun getName(base: Key?): String {
        if (PassageUtil.isPersistentNaming() && originalName != null) {
            return originalName!!
        }

        val rangeName = doGetName(base, null)
        // Only shape it if it can be unshaped.
        if (shaper.canUnshape()) {
            return shaper.shape(rangeName)!!
        }

        return rangeName
    }

    /**
     * A Human readable version of the Key in specified locale. For Biblical passages this uses
     * short books names, and the shortest sensible rendering, for example
     * "Mat 3:1-4" and "Mar 1:1, 3, 5" and "3Jo, Jude"
     *
     * @param locale the desired locale of name
     * @return a String containing a description of the Key
     */
    fun getNameInLocale(base: Key?, locale: Locale?): String {
        if (locale == null) return getName(base)
        if (PassageUtil.isPersistentNaming() && originalName != null) {
            return originalName!!
        }

        val rangeName = doGetName(base, locale)
        // Only shape it if it can be unshaped.
        if (shaper.canUnshape()) {
            return shaper.shape(rangeName)!!
        }

        return rangeName
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    override fun getRootName(): String {
        return start.getRootName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    override fun getOsisRef(): String {
        val startBook = start.book
        val endBook = end.book
        val startChapter = start.chapter
        val endChapter = end.chapter

        // If this is in 2 separate books
        if (startBook !== endBook) {
            val buf = StringBuilder()
            if (v11n.isStartOfBook(start)) {
                buf.append(startBook.osis)
            } else if (v11n.isStartOfChapter(start)) {
                buf.append(startBook.osis)
                buf.append(Verse.VERSE_OSIS_DELIM)
                buf.append(startChapter)
            } else {
                buf.append(start.getOsisRef())
            }

            buf.append(RANGE_PREF_DELIM)

            if (v11n.isEndOfBook(end)) {
                buf.append(endBook.osis)
            } else if (v11n.isEndOfChapter(end)) {
                buf.append(endBook.osis)
                buf.append(Verse.VERSE_OSIS_DELIM)
                buf.append(endChapter)
            } else {
                buf.append(end.getOsisRef())
            }

            return buf.toString()
        }

        // This range is exactly a whole book
        if (isWholeBook) {
            // Just report the name of the book, we don't need to worry
            // about the
            // base since we start at the start of a book, and should have
            // been
            // recently normalized()
            return startBook.osis
        }

        // If this is 2 separate chapters in the same book
        if (startChapter != endChapter) {
            val buf = StringBuilder()
            if (v11n.isStartOfChapter(start)) {
                buf.append(startBook.osis)
                buf.append(Verse.VERSE_OSIS_DELIM)
                buf.append(startChapter)
            } else {
                buf.append(start.getOsisRef())
            }

            buf.append(RANGE_PREF_DELIM)

            if (v11n.isEndOfChapter(end)) {
                buf.append(endBook.osis)
                buf.append(Verse.VERSE_OSIS_DELIM)
                buf.append(endChapter)
            } else {
                buf.append(end.getOsisRef())
            }

            return buf.toString()
        }

        // If this range is exactly a whole chapter
        if (isWholeChapter) {
            // Just report the name of the book and the chapter
            val buf = StringBuilder()
            buf.append(startBook.osis)
            buf.append(Verse.VERSE_OSIS_DELIM)
            buf.append(startChapter)
            return buf.toString()
        }

        // If this is 2 separate verses
        if (start.verse != end.verse) {
            val buf = StringBuilder()
            buf.append(start.getOsisRef())
            buf.append(RANGE_PREF_DELIM)
            buf.append(end.getOsisRef())
            return buf.toString()
        }

        // The range is a single verse
        return start.getOsisRef()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    override fun getOsisID(): String {

        // This range is exactly a whole book
        if (isWholeBook) {
            // Just report the name of the book, we don't need to worry
            // about the base since we start at the start of a book, and
            // should have been recently normalized()
            return start.book.osis
        }

        // If this range is exactly a whole chapter
        if (isWholeChapter) {
            // Just report the name of the book and the chapter
            return start.book.osis + Verse.VERSE_OSIS_DELIM + start.chapter
        }

        val startOrdinal = start.ordinal
        val endOrdinal = end.ordinal

        // to see if it is wholly contained in the range and output it if it is.

        // Estimate the size of the buffer: book.dd.dd (where book is 3-5, 3 typical)
        val buf = StringBuilder((endOrdinal - startOrdinal + 1) * 10)
        buf.append(start.getOsisID())
        for (i in startOrdinal + 1 until endOrdinal) {
            buf.append(AbstractPassage.REF_OSIS_DELIM)
            buf.append(v11n.decodeOrdinal(i).getOsisID())
        }

        // It just might be a single verse range!
        if (startOrdinal != endOrdinal) {
            buf.append(AbstractPassage.REF_OSIS_DELIM)
            buf.append(end.getOsisID())
        }

        return buf.toString()
    }

    override fun toString(): String {
        return getName()
    }

    /**
     * Fetch the first verse in this range.
     *
     * @return The first verse in the range
     */
    var start: Verse
        private set

    /**
     * Fetch the last verse in this range.
     *
     * @return The last verse in the range
     */
    @Transient
    var end: Verse
        private set

    override fun clone(): VerseRange {
        // This gets us a shallow copy
        var copy: VerseRange? = null
        try {
            copy = super<Cloneable>.clone() as VerseRange
            copy.start = start
            copy.end = end
            copy.verseCount = verseCount
            copy.originalName = originalName
            copy.shaper = NumberShaper()
            copy.v11n = v11n
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }

        return copy!!
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VerseRange) {
            return false
        }
        val vr: VerseRange = other
        return verseCount == vr.verseCount && start == vr.start && v11n.equals(vr.v11n)
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + verseCount
        val v11nOrNull: Versification? = v11n
        return 31 * result + (v11nOrNull?.hashCode() ?: 0)
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Key?): Int {
        val that = other as VerseRange

        val result = start.compareTo(that.start)
        return if (result == 0) this.verseCount - that.verseCount else result
    }

    /**
     * Are the 2 VerseRanges in question contiguous. that is - could they be
     * represented by a single VerseRange. Note that one range could be entirely
     * contained within the other and they would be considered adjacentTo() For
     * example Gen 1:1-2 is adjacent to Gen 1:1-5 and Gen 1:3-4 but not to Gen
     * 1:4-10. Also Gen 1:29-30 is adjacent to Gen 2:1-10
     *
     * @param that
     *            The VerseRange to compare to
     * @return true if the ranges are adjacent
     */
    fun adjacentTo(that: VerseRange?): Boolean {
        val thatStart = that!!.start.ordinal
        val thatEnd = that.end.ordinal
        val thisStart = start.ordinal
        val thisEnd = end.ordinal

        // if that starts inside or is next to this we are adjacent.
        if (thatStart >= thisStart - 1 && thatStart <= thisEnd + 1) {
            return true
        }

        // if this starts inside or is next to that we are adjacent.
        if (thisStart >= thatStart - 1 && thisStart <= thatEnd + 1) {
            return true
        }

        // otherwise we're not adjacent
        return false
    }

    /**
     * Do the 2 VerseRanges in question actually overlap. This is slightly more
     * restrictive than the adjacentTo() test which could be satisfied by ranges
     * like Gen 1:1-2 and Gen 1:3-4. overlaps() however would return false given
     * these ranges. For example Gen 1:1-2 is adjacent to Gen 1:1-5 but not to
     * Gen 1:3-4 not to Gen 1:4-10. Also Gen 1:29-30 does not overlap Gen 2:1-10
     *
     * @param that
     *            The VerseRange to compare to
     * @return true if the ranges are adjacent
     */
    fun overlaps(that: VerseRange?): Boolean {
        val thatStart = that!!.start.ordinal
        val thatEnd = that.end.ordinal
        val thisStart = start.ordinal
        val thisEnd = end.ordinal

        // if that starts inside this we are adjacent.
        if (thatStart >= thisStart && thatStart <= thisEnd) {
            return true
        }

        // if this starts inside that we are adjacent.
        if (thisStart >= thatStart && thisStart <= thatEnd) {
            return true
        }

        // otherwise we're not adjacent
        return false
    }

    /**
     * Is the given verse entirely within our range. For example if this =
     * "Gen 1:1-31" then: <tt>contains(Verse("Gen 1:3")) == true</tt>
     * <tt>contains(Verse("Gen 2:1")) == false</tt>
     *
     * @param that
     *            The Verse to compare to
     * @return true if we contain it.
     */
    fun contains(that: Verse?): Boolean {
        return v11n.distance(start, that) >= 0 && v11n.distance(that, end) >= 0
    }

    /**
     * Is the given range within our range. For example if this = "Gen 1:1-31"
     * then: <tt>this.contains(Verse("Gen 1:3-10")) == true</tt>
     * <tt>this.contains(Verse("Gen 2:1-1")) == false</tt>
     *
     * @param that
     *            The Verse to compare to
     * @return true if we contain it.
     */
    fun contains(that: VerseRange?): Boolean {
        return v11n.distance(start, that!!.start) >= 0 && v11n.distance(that.end, end) >= 0
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        if (key is VerseRange) {
            return contains(key as VerseRange?)
        }
        if (key is Verse) {
            return contains(key as Verse?)
        }
        return false
    }

    /**
     * Does this range represent exactly one chapter, no more or less.
     *
     * @return true if we are exactly one chapter.
     */
    val isWholeChapter: Boolean
        get() = v11n.isSameChapter(start, end) && isWholeChapters

    /**
     * Does this range represent a number of whole chapters
     *
     * @return true if we are a whole number of chapters.
     */
    val isWholeChapters: Boolean
        get() = v11n.isStartOfChapter(start) && v11n.isEndOfChapter(end)

    /**
     * Does this range represent exactly one book, no more or less.
     *
     * @return true if we are exactly one book.
     */
    val isWholeBook: Boolean
        get() = v11n.isSameBook(start, end) && isWholeBooks

    /**
     * Does this range represent a whole number of books.
     *
     * @return true if we are a whole number of books.
     */
    val isWholeBooks: Boolean
        get() = v11n.isStartOfBook(start) && v11n.isEndOfBook(end)

    /**
     * Does this range occupy more than one book;
     *
     * @return true if we occupy 2 or more books
     */
    val isMultipleBooks: Boolean
        get() = start.book !== end.book

    /**
     * Create an array of Verses
     *
     * @return The array of verses that this makes up
     */
    fun toVerseArray(): Array<Verse> {
        val ord = start.ordinal
        return Array(verseCount) { i -> v11n.decodeOrdinal(ord + i) }
    }

    /**
     * Enumerate the subranges in this range
     *
     * @param restrict
     * @return a range iterator
     */
    fun rangeIterator(restrict: RestrictionType?): MutableIterator<VerseRange> {
        return AbstractPassage.VerseRangeIterator(v11n, iterator(), restrict)
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
    fun setParent(parent: Key?) {
        this.parent = parent
    }

    private fun doGetName(base: Key?, locale: Locale?): String {
        // Cache these we're going to be using them a lot.
        val startBook = start.book
        val startChapter = start.chapter
        val startVerse = start.verse
        val endBook = end.book
        val endChapter = end.chapter
        val endVerse = end.verse

        // If this is in 2 separate books
        if (startBook !== endBook) {
            // This range is exactly a whole book
            if (isWholeBooks) {
                // Just report the name of the book, we don't need to worry
                // about the base since we start at the start of a book,
                // and should have been recently normalized()
                return v11n.getPreferredNameInLocale(startBook, locale) + RANGE_PREF_DELIM + v11n.getPreferredNameInLocale(endBook, locale)
            }

            // If this range is exactly a whole chapter
            if (isWholeChapters) {
                // Just report book and chapter names
                return v11n.getPreferredNameInLocale(startBook, locale) + Verse.VERSE_PREF_DELIM1 + startChapter + RANGE_PREF_DELIM +
                        v11n.getPreferredNameInLocale(endBook, locale) + Verse.VERSE_PREF_DELIM1 + endChapter
            }

            if (v11n.isChapterIntro(start)) {
                return v11n.getPreferredNameInLocale(startBook, locale) + Verse.VERSE_PREF_DELIM1 + startChapter + RANGE_PREF_DELIM + end.getNameInLocale(base, locale)
            }
            if (v11n.isBookIntro(start)) {
                return v11n.getPreferredNameInLocale(startBook, locale) + RANGE_PREF_DELIM + end.getNameInLocale(base, locale)
            }
            return start.getNameInLocale(base, locale) + RANGE_PREF_DELIM + end.getNameInLocale(base, locale)
        }

        // This range is exactly a whole book
        if (isWholeBook) {
            // Just report the name of the book, we don't need to worry about
            // the
            // base since we start at the start of a book, and should have been
            // recently normalized()
            return v11n.getPreferredNameInLocale(startBook, locale)!!
        }

        // If this is 2 separate chapters
        if (startChapter != endChapter) {
            // If this range is a whole number of chapters
            if (isWholeChapters) {
                // Just report the name of the book and the chapters
                return v11n.getPreferredNameInLocale(startBook, locale) + Verse.VERSE_PREF_DELIM1 + startChapter + RANGE_PREF_DELIM + endChapter
            }

            return start.getNameInLocale(base, locale) + RANGE_PREF_DELIM + endChapter + Verse.VERSE_PREF_DELIM2 + endVerse
        }

        // If this range is exactly a whole chapter
        if (isWholeChapter) {
            // Just report the name of the book and the chapter
            return v11n.getPreferredNameInLocale(startBook, locale) + Verse.VERSE_PREF_DELIM1 + startChapter
        }

        // If this is 2 separate verses
        if (startVerse != endVerse) {
            return start.getNameInLocale(base, locale) + RANGE_PREF_DELIM + endVerse
        }

        // The range is a single verse
        return start.getNameInLocale(base, locale)
    }

    /**
     * Calculate the last verse in this range.
     *
     * @return The last verse in the range
     */
    private fun calcEnd(): Verse {
        if (verseCount == 1) {
            return start
        }
        return v11n.add(start, verseCount - 1)!!
    }

    /**
     * Calculate how many verses in this range
     *
     * @return The number of verses. Always&gt;= 1.
     */
    private fun calcVerseCount(): Int {
        return v11n.distance(start, end) + 1
    }

    /**
     * Check to see that everything is ok with the Data
     */
    private fun verifyData() {
        // Kotlin evaluates the condition eagerly (jswordAssert), Java `assert` only with -ea.
        if (VerseRange::class.java.desiredAssertionStatus()) {
            jswordAssert(verseCount == calcVerseCount()) { "start=" + start + ", end=" + end + ", verseCount=" + verseCount }
        }
    }

    /**
     * Write out the object to the given ObjectOutputStream
     *
     * @param out
     *            The stream to write our state to
     * @throws IOException
     *             If the write fails
     * @serialData Write the ordinal number of this verse
     */
    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()

        // Ignore the original name. Is this wise?
        // I am expecting that people are not that fussed about it and
        // it could make everything far more verbose
    }

    /**
     * Write out the object to the given ObjectOutputStream
     *
     * @param in
     *            The stream to read our state from
     * @throws IOException
     *             If the write fails
     * @throws ClassNotFoundException
     *             If the read data is incorrect
     * @serialData Write the ordinal number of this verse
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()

        end = calcEnd()
        shaper = NumberShaper()

        verifyData()

        // We are ignoring the originalName and parent.
    }

    /**
     * Iterate over the Verses in the VerseRange
     */
    private class VerseIterator(range: VerseRange) : MutableIterator<Key> {
        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return nextVerse != null
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Throws(NoSuchElementException::class)
        override fun next(): Key {
            if (nextVerse == null) {
                throw NoSuchElementException()
            }
            val currentVerse = nextVerse
            nextVerse = if (++count < total) v11n.next(nextVerse) else null
            return currentVerse!!
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Throws(UnsupportedOperationException::class)
        override fun remove() {
            throw UnsupportedOperationException()
        }

        private var v11n: Versification = range.versification
        private var nextVerse: Verse? = range.start
        private var count: Int = 0
        private var total: Int = range.getCardinality()
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
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        return verseCount
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        return verseCount == 0
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return VerseIterator(this)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#addAll(org.crosswire.jsword.passage.Key)
     */
    override fun addAll(key: Key?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#removeAll(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#retainAll(org.crosswire.jsword.passage.Key)
     */
    override fun retainAll(key: Key?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        return null
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        return -1
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(by: Int, restrict: RestrictionType?) {
        blur(by, restrict, true, true)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        val newRange = restrict!!.blur(v11n, this, if (blurDown) by else 0, if (blurUp) by else 0)
        start = newRange.start
        end = newRange.end
        verseCount = newRange.verseCount
    }

    /**
     * The Versification with which this range is defined. It is transient and, as in Java, never restored
     * by deserialization, so it is null there although the type says otherwise (assigned in every
     * constructor, hence neither lateinit nor a null check on access: a NullPointerException must be
     * raised by the JVM on the field, as in Java).
     */
    @Transient
    private var v11n: Versification

    /**
     * The number of verses in the range
     */
    private var verseCount: Int = 0

    /**
     * Allow the conversion to and from other number representations.
     */
    @Transient
    private var shaper: NumberShaper

    /**
     * The parent key.
     */
    @Transient
    private var parent: Key? = null

    /**
     * The original string for picky users
     */
    @Transient
    private var originalName: String? = null

    companion object {
        /**
         * Create a VerseRange that is the stuff left of VerseRange a when you
         * remove the stuff in VerseRange b.
         *
         * @param a
         *            Verses at the start or end of b
         * @param b
         *            All the verses
         * @return A list of the Verses outstanding
         */
        @JvmStatic
        fun remainder(a: VerseRange?, b: VerseRange?): Array<VerseRange> {
            var rstart: VerseRange? = null
            var rend: VerseRange? = null

            val v11n = a!!.versification

            // If a starts before b get the Range of the prequel
            if (v11n.distance(a.start, b!!.start) > 0) {
                rstart = VerseRange(v11n, a.start, v11n.subtract(b.end, 1))
            }

            // If a ends after b get the Range of the sequel
            if (v11n.distance(a.end, b.end) < 0) {
                rend = VerseRange(v11n, v11n.add(b.end, 1), a.end)
            }

            if (rstart == null) {
                if (rend == null) {
                    return arrayOf()
                }
                return arrayOf(rend)
            }

            if (rend == null) {
                return arrayOf(rstart)
            }
            return arrayOf(rstart, rend)
        }

        /**
         * Create a VerseRange that is the stuff in VerseRange a that is also
         * in VerseRange b.
         *
         * @param a
         *            The verses that you might want
         * @param b
         *            The verses that you definitely don't
         * @return A list of the Verses outstanding
         */
        @JvmStatic
        fun intersection(a: VerseRange?, b: VerseRange?): VerseRange? {
            val v11n = a!!.versification
            val newStart = v11n.max(a.start, b!!.start)
            val newEnd = v11n.min(a.end, b.end)

            if (v11n.distance(newStart, newEnd) >= 0) {
                return VerseRange(a.versification, newStart, newEnd)
            }

            return null
        }

        /**
         * What characters can we use to separate the 2 parts to a VerseRanges
         */
        const val RANGE_OSIS_DELIM: Char = '-'

        /**
         * What characters should we use to separate VerseRange parts on output
         */
        const val RANGE_PREF_DELIM: Char = RANGE_OSIS_DELIM

        /**
         * Serialization ID
         */
        private const val serialVersionUID = 8307795549869653580L
    }
}
