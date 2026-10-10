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
 * © CrossWire Bible Society, 2007 - 2016
 *
 */
package org.crosswire.jsword.versification

import org.crosswire.common.util.KLocale

import org.crosswire.common.util.lowercase
import org.crosswire.common.util.uppercase
import org.crosswire.common.util.StringUtil
import org.crosswire.jsword.book.CaseType

import kotlin.math.min

/**
 * BookName represents the different ways a book of the bible is named.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class BookName
/**
 * Create a BookName for a Book of the Bible in a given language.
 * 
 * @param locale
 *            the language of this BookName
 * @param book
 *            the Book's canonical number
 * @param longName
 *            the Book's long name
 * @param shortName
 *            the Book's short name, if any
 * @param alternateNames
 *            optional comma separated list of alternates for the Book
 */
(
    locale: KLocale?,
    /**
     * Get the BibleBook to which this set of names is tied.
     * 
     * @return The book
     */
    val book: BibleBook?,
    longName: String?,
    shortName: String?,
    alternateNames: String?
) {
    private val longNameRaw: String
    private val shortNameRaw: String

    /**
     * @return the normalizedLongName
     */
    val normalizedLongName: String

    /**
     * @return the normalizedShortName
     */
    val normalizedShortName: String
    private var alternateNames: Array<String>? = null

    /** The locale for the Book Name  */
    private val locale: KLocale

    init {
        this.locale = locale!!
        this.longNameRaw = longName!!
        this.normalizedLongName = normalize(longName, locale)
        this.shortNameRaw = shortName!!
        this.normalizedShortName = normalize(shortName, locale)

        if (alternateNames != null) {
            this.alternateNames = StringUtil.split(normalize(alternateNames, locale), ',')
        }
    }

    /**
     * Get the preferred name of a book. Altered by the case setting (see
     * setBookCase() and isFullBookName())
     * 
     * @return The preferred name of the book
     */
    val preferredName: String
        get() {
            if (isTruncateShortName()!!) {
                return truncatedShortName
            } else if (isFullBookName()) {
                return longName
            }
            return shortName
        }

    /**
     * Get the full name of a book (e.g. "Genesis"). Altered by the case setting
     * (see setCase())
     * 
     * @return The full name of the book
     */
    val longName: String
        get() {
            val caseType = getDefaultCase()

            if (caseType == CaseType.LOWER) {
                return longNameRaw.lowercase(locale)
            }

            if (caseType == CaseType.UPPER) {
                return longNameRaw.uppercase(locale)
            }

            return longNameRaw
        }

    /**
     * Get the short name of a book (e.g. "Gen"). Altered by the case setting
     * (see setCase())
     * 
     * @return The short name of the book
     */
    val shortName: String
        get() {
            val caseType = getDefaultCase()

            if (caseType == CaseType.LOWER) {
                return shortNameRaw.lowercase(locale)
            }

            if (caseType == CaseType.UPPER) {
                return shortNameRaw.uppercase(locale)
            }

            return shortNameRaw
        }

    val truncatedShortName: String
        get() = shortName.substring(0, min(truncateShortName!!, shortName.length))

    /**
     * Match the normalized name as closely as possible. It will match if:
     * <ol>
     * <li>it is a prefix of a normalized alternate name</li>
     * <li>a normalized alternate name is a prefix of it</li>
     * <li>it is a prefix of a normalized long name</li>
     * <li>it is a prefix of a normalized short name</li>
     * <li>a normalized short name is a prefix of it</li>
     * </ol>
     * 
     * @param normalizedName
     *            the already normalized name to match against.
     * @return true of false
     */
    fun match(normalizedName: String?): Boolean {
        // Does it match one of the alternative versions
        val alternateNames = this.alternateNames!!
        for (j in alternateNames.indices) {
            val targetBookName = alternateNames[j]
            if (targetBookName.startsWith(normalizedName!!) || normalizedName.startsWith(targetBookName)) {
                return true
            }
        }

        // Does it match a long version of the book
        if (normalizedLongName.startsWith(normalizedName!!)) {
            return true
        }

        // or a short version
        if (normalizedShortName.startsWith(normalizedName) || (normalizedShortName.length > 0 && normalizedName.startsWith(normalizedShortName))) {
            return true
        }

        return false
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    override fun hashCode(): Int {
        return book.hashCode()
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
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

        val otherName = other as BookName
        return book === otherName.book
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    override fun toString(): String {
        return preferredName
    }

    companion object {
        @JvmStatic
        fun isTruncateShortName(): Boolean? {
            return truncateShortName!! > 0
        }

        @JvmStatic
        fun getTruncateShortName(): Int? {
            return truncateShortName
        }

        @JvmStatic
        fun setTruncateShortName(truncateShortName: Int?) {
            BookName.truncateShortName = truncateShortName
        }

        /**
         * Normalize by stripping punctuation and whitespace and lowercasing.
         * 
         * @param str
         *            the string to normalize
         * @param locale the locale that should be used for normalization
         * @return the normalized string
         */
        @JvmStatic
        fun normalize(str: String?, locale: KLocale?): String {
            return normPattern.replace(str!!, "").lowercase(locale!!)
        }

        /**
         * This is only used by config.
         * 
         * @param bookCase
         *            The new case to use for reporting book names
         * @exception IllegalArgumentException
         *                If the case is not between 0 and 2
         * @see .getCase
         */
        @JvmStatic
        fun setCase(bookCase: Int) {
            BookName.bookCase = CaseType.fromInteger(bookCase)
        }

        /**
         * How do we report the names of the books?. These are static. This is on
         * the assumption that we will not want to have different sections of the
         * app using a different format. I expect this to be a good assumption, and
         * it saves passing a Book class around everywhere. CaseType.MIXED is not
         * allowed
         * 
         * @param newBookCase
         *            The new case to use for reporting book names
         * @exception IllegalArgumentException
         *                If the case is not between 0 and 2
         * @see .getCase
         */
        @JvmStatic
        fun setCase(newBookCase: CaseType?) {
            bookCase = newBookCase
        }

        /**
         * This is only used by config
         * 
         * @return The current case setting
         * @see .setCase
         */
        @JvmStatic
        fun getCase(): Int {
            return bookCase!!.toInteger()
        }

        /**
         * This is only used by config
         * 
         * @return Whether the name is long or short. Default is Full (true).
         * @see .setFullBookName
         */
        @JvmStatic
        fun isFullBookName(): Boolean {
            return fullBookName
        }

        /**
         * Set whether the name should be full or abbreviated, long or short.
         * 
         * @param fullName
         *            The new case to use for reporting book names
         * @see .isFullBookName
         */
        @JvmStatic
        fun setFullBookName(fullName: Boolean) {
            fullBookName = fullName
        }

        /**
         * How do we report the names of the books?.
         * 
         * @return The current case setting
         * @see .setCase
         */
        @JvmStatic
        fun getDefaultCase(): CaseType? {
            return bookCase
        }

        /** remove spaces and some punctuation in Book Name (make sure , is allowed)  */
        private val normPattern: Regex = Regex("[. ]")

        /** How the book names are reported.  */
        private var bookCase: CaseType? = CaseType.SENTENCE

        /** Whether long or short, full or abbreviated names are used.  */
        private var fullBookName = true

        // Even shorter version than abbreviated (max N letters). 0 to disable.
        private var truncateShortName: Int? = 0
    }
}
