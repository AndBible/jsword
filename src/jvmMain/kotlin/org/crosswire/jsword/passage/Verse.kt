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
import org.crosswire.common.util.ItemIterator
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.BibleNames
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Locale

/**
 * A Verse is a pointer to a single verse. Externally its unique identifier is
 * a String of the form "Gen 1:1" Internally we use
 * `( v11n, book, chapter, verse )`
 *
 * <p>
 * A Verse is designed to be immutable. This is a necessary from a collections
 * point of view. A Verse should always be valid, although some versions may not
 * return any text for verses that they consider to be untranslated in some
 * way.
 * </p>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class Verse : VerseKey<Verse>, Cloneable {
    /**
     * Create a Verse from book, chapter and verse numbers, throwing up if the
     * specified Verse does not exist.
     *
     * @param v11n
     *            The versification to which this verse belongs
     * @param book
     *            The book number (Genesis = 1)
     * @param chapter
     *            The chapter number
     * @param verse
     *            The verse number
     */
    constructor(v11n: Versification?, book: BibleBook?, chapter: Int, verse: Int) : this(v11n, book, chapter, verse, null)

    /**
     * Create a Verse from book, chapter and verse numbers, throwing up if the
     * specified Verse does not exist.
     *
     * @param v11n
     *            The versification to which this verse belongs
     * @param book
     *            The book number (Genesis = 1)
     * @param chapter
     *            The chapter number
     * @param verse
     *            The verse number
     * @param subIdentifier
     *            The optional sub identifier
     */
    constructor(v11n: Versification?, book: BibleBook?, chapter: Int, verse: Int, subIdentifier: String?) {
        this.v11n = v11n!!
        this.book = book!!
        this.chapter = chapter
        this.verse = verse
        this.subIdentifier = subIdentifier
        this.ordinal = v11n.getOrdinal(this)
    }

    /**
     * Create a Verse from book, chapter and verse numbers, patching up if the
     * specified verse does not exist.
     * <p>
     * The actual value of the boolean is ignored. However for future proofing
     * you should only use 'true'. Do not use patch_up=false, use
     * `Verse(int, int, int)` This so that we can declare this
     * constructor to not throw an exception. Is there a better way of doing
     * this?
     *
     * @param v11n
     *            The versification to which this verse belongs
     * @param book
     *            The book number (Genesis = 1)
     * @param chapter
     *            The chapter number
     * @param verse
     *            The verse number
     * @param patchUp
     *            True to trigger reference fixing
     */
    constructor(v11n: Versification?, book: BibleBook?, chapter: Int, verse: Int, patchUp: Boolean) {
        if (!patchUp) {
            throw IllegalArgumentException(JSOtherMsg.lookupText("Use patchUp=true."))
        }

        this.v11n = v11n!!
        val patched = this.v11n.patch(book, chapter, verse)
        this.book = patched.book
        this.chapter = patched.chapter
        this.verse = patched.verse
        this.ordinal = patched.ordinal
    }

    /**
     * Set a Verse using a verse ordinal number - WARNING Do not use this method
     * unless you really know the dangers of doing so. Ordinals are not always
     * going to be the same. So you should use Versification, Book, Chapter and Verse
     * in preference to an int ordinal whenever possible. Ordinal numbers are 1
     * based and not 0 based.
     *
     * @param v11n
     *            The versification to which this verse belongs
     * @param ordinal
     *            The verse id
     */
    constructor(v11n: Versification?, ordinal: Int) {
        val decoded = v11n!!.decodeOrdinal(ordinal)
        this.v11n = v11n
        this.book = decoded.book
        this.chapter = decoded.chapter
        this.verse = decoded.verse
        this.ordinal = decoded.ordinal
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#isWhole()
     */
    override val isWhole: Boolean
        get() {
            val sub = subIdentifier
            return sub == null || sub.length == 0
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#getWhole()
     */
    override val whole: Verse
        get() {
            if (isWhole) {
                return this
            }
            return Verse(v11n, book, chapter, verse)
        }

    override fun toString(): String {
        return getName()
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
        if (base != null && base !is Verse) {
            return getName()
        }

        val verseName = doGetName(base as Verse?, null)
        // Only shape it if it can be unshaped.
        if (shaper.canUnshape()) {
            return shaper.shape(verseName)!!
        }

        return verseName
    }

    /**
     * A Human readable version of the Key in specified locale. For Biblical passages this uses
     * short books names, and the shortest sensible rendering, for example
     * "Mat 3:1" and "Mar 1:1, 3, 5" and "3Jo, Jude"
     *
     * @param locale the desired locale of name
     * @return a String containing a description of the Key
     */
    fun getNameInLocale(base: Key?, locale: Locale?): String {
        if (locale == null) return getName(base)
        if (base != null && base !is Verse) {
            return getName()
        }

        val verseName = doGetName(base as Verse?, locale)
        // Only shape it if it can be unshaped.
        if (shaper.canUnshape()) {
            return shaper.shape(verseName)!!
        }

        return verseName
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    override fun getRootName(): String {
        return BibleNames.instance().getShortName(book)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    override fun getOsisRef(): String {
        return getOsisID()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    override fun getOsisID(): String {
        val buf = getVerseIdentifier()
        val sub = subIdentifier
        if (sub != null && sub.length > 0) {
            buf.append(VERSE_OSIS_SUB_PREFIX)
            buf.append(sub)
        }
        return buf.toString()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    val osisIDNoSubIdentifier: String
        get() = getVerseIdentifier().toString()

    /**
     * Gets the common name of the verse, excluding any !abc sub-identifier
     * @return the verse OSIS-ID, excluding the sub-identifier
     */
    private fun getVerseIdentifier(): StringBuilder {
        val buf = StringBuilder()
        buf.append(book.osis)
        buf.append(VERSE_OSIS_DELIM)
        buf.append(chapter)
        buf.append(VERSE_OSIS_DELIM)
        buf.append(verse)
        return buf
    }

    override fun clone(): Verse {
        var copy: Verse? = null
        try {
            copy = super<Cloneable>.clone() as Verse
            copy.v11n = this.v11n
            copy.book = this.book
            copy.chapter = this.chapter
            copy.verse = this.verse
            copy.ordinal = this.ordinal
            copy.subIdentifier = this.subIdentifier
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }

        return copy!!
    }

    override fun equals(other: Any?): Boolean {
        // Since this can not be null
        if (other !is Verse) {
            return false
        }

        val that: Verse = other

        // The real tests
        return this.ordinal == that.ordinal
                && this.v11n.equals(that.v11n)
                && bothNullOrEqual(this.subIdentifier, that.subIdentifier)
    }

    override fun hashCode(): Int {
        var result = 31 + ordinal
        val v11nOrNull: Versification? = v11n
        result = 31 * result + (v11nOrNull?.hashCode() ?: 0)
        return 31 * result + (subIdentifier?.hashCode() ?: 0)
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Key?): Int {
        return this.ordinal - (other as Verse).ordinal
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.VerseKey#getVersification()
     */
    override val versification: Versification
        get() = v11n

    override fun isValidIn(targetVersification: Versification?): Boolean {
        try {
            // Pass true for silent so it won't throw an exception.
            return targetVersification!!.validate(book, chapter, verse, true)
        } catch (e: NoSuchVerseException) {
            return false
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#reversify(org.crosswire.jsword.versification.Versification)
     */
    override fun reversify(newVersification: Versification?): Verse? {
        if (v11n.equals(newVersification)) {
            return this
        }

        try {
            //check the v11n supports this key, otherwise this leads to all sorts of issues
            if (newVersification!!.validate(book, chapter, verse, true)) {
                return Verse(newVersification, book, chapter, verse)
            }
        } catch (ex: NoSuchVerseException) {
            // will never happen
            log.error("Contract for validate was changed to thrown an exception when silent mode is true", ex)
        }
        return null
    }

    /**
     * Return the book that we refer to
     *
     * @return The book of the Bible
     */
    @Transient
    var book: BibleBook
        private set

    /**
     * Return the chapter that we refer to
     *
     * @return The chapter number
     */
    @Transient
    var chapter: Int = 0
        private set

    /**
     * Return the verse that we refer to
     *
     * @return The verse number
     */
    @Transient
    var verse: Int = 0
        private set

    /**
     * Return the sub identifier if any
     * @return The optional OSIS sub identifier
     *
     * The OSIS Sub-identifier if present.
     * This should be a string that allows for the likes of:
     * a.xy.asdf.qr
     */
    var subIdentifier: String? = null
        private set

    /**
     * Return the ordinal value of the verse in its versification.
     *
     * @return The verse number
     *
     * The ordinal value for this verse within its versification.
     */
    var ordinal: Int = 0
        private set

    /**
     * Create an array of Verses
     *
     * @return The array of verses that this makes up
     */
    fun toVerseArray(): Array<Verse> {
        return arrayOf(this)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        return null
    }

    /**
     * Compute the verse representation given the context.
     *
     * @param verseBase
     *            the context or null if there is none
     * @return the verse representation
     */
    private fun doGetName(verseBase: Verse?, locale: Locale?): String {
        val buf = StringBuilder()
        // To cope with thing like Jude 2...
        if (book.isShortBook) {
            if (verseBase == null || verseBase.book !== book) {
                buf.append(BibleNames.instance().getPreferredNameInLocale(book, locale))
                buf.append(VERSE_PREF_DELIM1)
                buf.append(verse)
                return buf.toString()
            }

            return Integer.toString(verse)
        }

        if (verseBase == null || verseBase.book !== book) {
            buf.append(BibleNames.instance().getPreferredNameInLocale(book, locale))
            buf.append(VERSE_PREF_DELIM1)
            buf.append(chapter)
            buf.append(VERSE_PREF_DELIM2)
            buf.append(verse)
            return buf.toString()
        }

        if (verseBase.chapter != chapter) {
            buf.append(chapter)
            buf.append(VERSE_PREF_DELIM2)
            buf.append(verse)
            return buf.toString()
        }

        return Integer.toString(verse)
    }

    /**
     * Write out the object to the given ObjectOutputStream
     *
     * @param out
     *            The stream to write our state to
     * @throws IOException
     *             if the read fails
     * @serialData Write the ordinal number of this verse
     */
    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
        out.writeUTF(v11n.name)
    }

    /**
     * Write out the object to the given ObjectOutputStream
     *
     * @param in
     *            The stream to read our state from
     * @throws IOException
     *             if the read fails
     * @throws ClassNotFoundException
     *             If the read data is incorrect
     * @serialData Write the ordinal number of this verse
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        val v11nName = `in`.readUTF()
        v11n = Versifications.instance().getVersification(v11nName)!!
        val decoded = v11n.decodeOrdinal(ordinal)

        this.book = decoded.book
        this.chapter = decoded.chapter
        this.verse = decoded.verse
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
        return 1
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return this == key
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return ItemIterator<Key>(this)
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
        // do nothing
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        if (index == 0) {
            return this
        }
        return null
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        if (this == that) {
            return 0
        }
        return -1
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
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        throw UnsupportedOperationException()
    }

    /**
     * The versification for this verse.
     */
    @Transient
    private var v11n: Versification

    companion object {
        /**
         * Determine whether two objects are equal, allowing nulls
         * @param x
         * @param y
         * @return true if both are null or the two are equal
         */
        @JvmStatic
        fun bothNullOrEqual(x: Any?, y: Any?): Boolean {
            return x === y || (x != null && x == y)
        }

        /**
         * This is simply a convenience function to wrap Integer.parseInt() and give
         * us a reasonable exception on failure. It is called by VerseRange hence
         * protected, however I would prefer private
         *
         * @param text
         *            The string to be parsed
         * @return The correctly parsed chapter or verse
         */
        @JvmStatic
        @Throws(NoSuchVerseException::class)
        protected fun parseInt(text: String?): Int {
            try {
                return Integer.parseInt(shaper.unshape(text!!))
            } catch (ex: NumberFormatException) {
                // TRANSLATOR: The chapter or verse number is actually not a number, but something else.
                // {0} is a placeholder for what the user supplied.
                throw NoSuchVerseException(JSMsg.gettext("Cannot understand {0} as a chapter or verse.", text))
            }
        }

        /**
         * What characters should we use to separate parts of an OSIS verse
         * reference
         */
        const val VERSE_OSIS_DELIM: Char = '.'

        /**
         * What characters should we use to start an OSIS sub identifier
         */
        const val VERSE_OSIS_SUB_PREFIX: Char = '!'

        /**
         * What characters should we use to separate the book from the chapter
         */
        const val VERSE_PREF_DELIM1: Char = ' '

        /**
         * What characters should we use to separate the chapter from the verse
         */
        const val VERSE_PREF_DELIM2: Char = ':'

        /**
         * The default verse
         */
        @JvmField
        val DEFAULT: Verse = Verse(Versifications.instance().getVersification("KJV"), BibleBook.GEN, 1, 1)

        /**
         * Allow the conversion to and from other number representations.
         */
        private var shaper = NumberShaper()

        private val log = LoggerFactory.getLogger(Verse::class.java)

        /**
         * To make serialization work across new versions
         */
        private const val serialVersionUID = -4033921076023185171L
    }
}
