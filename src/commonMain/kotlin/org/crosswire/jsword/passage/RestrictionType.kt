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

import kotlin.jvm.JvmStatic

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.versification.Versification
import kotlin.math.max
import kotlin.math.min

/**
 * Types of Passage Blurring Restrictions.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
enum class RestrictionType {
    /**
     * There is no restriction on blurring.
     */
    NONE {
        override fun isSameScope(v11n: Versification?, start: Verse?, end: Verse?): Boolean {
            return true
        }

        override fun blur(v11n: Versification?, range: VerseRange?, blurDown: Int, blurUp: Int): VerseRange {
            val start = v11n!!.subtract(range!!.start, blurDown)
            val end = v11n.add(range.end, blurUp)
            return VerseRange(v11n, start, end)
        }

        override fun blur(v11n: Versification?, verse: Verse?, blurDown: Int, blurUp: Int): VerseRange {
            val start = v11n!!.subtract(verse, blurDown)
            val end = v11n.add(verse, blurUp)
            return VerseRange(v11n, start, end)
        }

        override fun toRange(v11n: Versification?, verse: Verse?, count: Int): VerseRange {
            var end = verse
            if (count > 1) {
                end = v11n!!.add(verse, count - 1)
            }
            return VerseRange(v11n, verse, end)
        }
    },

    /**
     * Blurring is restricted to the chapter
     */
    CHAPTER {
        override fun isSameScope(v11n: Versification?, start: Verse?, end: Verse?): Boolean {
            return v11n!!.isSameChapter(start, end)
        }

        override fun blur(v11n: Versification?, range: VerseRange?, blurDown: Int, blurUp: Int): VerseRange {
            val start = range!!.start
            val startBook = start.book
            val startChapter = start.chapter
            var startVerse = start.verse - blurDown

            val end = range.end
            val endBook = end.book
            val endChapter = end.chapter
            var endVerse = end.verse + blurUp

            startVerse = max(startVerse, 0)
            endVerse = min(endVerse, v11n!!.getLastVerse(endBook, endChapter))

            val newStart = Verse(v11n, startBook, startChapter, startVerse)
            val newEnd = Verse(v11n, endBook, endChapter, endVerse)
            return VerseRange(v11n, newStart, newEnd)
        }

        override fun blur(v11n: Versification?, verse: Verse?, blurDown: Int, blurUp: Int): VerseRange {
            val book = verse!!.book
            val chapter = verse.chapter
            var startVerse = verse.verse - blurDown
            var endVerse = verse.verse + blurUp

            startVerse = max(startVerse, 0)
            endVerse = min(endVerse, v11n!!.getLastVerse(book, chapter))

            val start = Verse(v11n, book, chapter, startVerse)
            val end = Verse(v11n, book, chapter, endVerse)
            return VerseRange(v11n, start, end)
        }

        override fun toRange(v11n: Versification?, verse: Verse?, count: Int): VerseRange {
            val end = v11n!!.add(verse, count - 1)
            return VerseRange(v11n, verse, end)
        }
    };

    /**
     * Are the two verses in the same scope.
     *
     * @param v11n
     *            the versification to which this reference pertains
     * @param start
     *            the first verse
     * @param end
     *            the second verse
     * @return true if the two are in the same scope.
     */
    abstract fun isSameScope(v11n: Versification?, start: Verse?, end: Verse?): Boolean

    /**
     * Blur a verse range the specified amount. Since verse ranges are
     * immutable, it creates a new one.
     *
     * @param v11n
     *            the versification to which this reference pertains
     * @param range
     * @param blurDown
     * @param blurUp
     * @return a verse range after blurring.
     */
    abstract fun blur(v11n: Versification?, range: VerseRange?, blurDown: Int, blurUp: Int): VerseRange

    /**
     * Blur a verse the specified amount. Since verse are immutable and refer to
     * a single verse, it creates a verse range.
     *
     * @param v11n
     *            the versification to which this reference pertains
     * @param verse
     * @param blurDown
     * @param blurUp
     * @return a verse range after blurring
     */
    abstract fun blur(v11n: Versification?, verse: Verse?, blurDown: Int, blurUp: Int): VerseRange

    /**
     * Create a range from the verse having the specified number of verses.
     *
     * @param v11n
     *            the versification to which this reference pertains
     * @param verse
     * @param count
     * @return a verse range created by extending a verse forward.
     */
    abstract fun toRange(v11n: Versification?, verse: Verse?, count: Int): VerseRange

    /**
     * Get an integer representation for this RestrictionType
     *
     * @return the integer representation
     */
    fun toInteger(): Int {
        return ordinal
    }

    companion object {
        /**
         * Lookup method to convert from a String
         * @param name
         * @return the matching restriction type
         */
        @JvmStatic
        fun fromString(name: String?): RestrictionType? {
            for (v in entries) {
                if (v.name.equals(name, ignoreCase = true)) {
                    return v
                }
            }

            // cannot get here
            jswordAssert(false) { "" }
            return null
        }

        /**
         * Lookup method to convert from an integer
         *
         * @param i
         * @return the restriction type from its ordinal value
         */
        @JvmStatic
        fun fromInteger(i: Int): RestrictionType? {
            for (v in entries) {
                if (v.ordinal == i) {
                    return v
                }
            }

            // cannot get here
            jswordAssert(false) { "" }
            return null
        }

        /**
         * The default Blur settings. This is used by config to set a default.
         *
         * @param value
         *            The new default blur setting
         */
        @JvmStatic
        fun setBlurRestriction(value: Int) {
            defaultBlurRestriction = fromInteger(value)
        }

        /**
         * The default Blur settings. This is used by config to manage a default
         * setting.
         *
         * @return The current default blurRestriction setting
         */
        @JvmStatic
        fun getBlurRestriction(): Int {
            return getDefaultBlurRestriction()!!.toInteger()
        }

        /**
         * The default Blur settings. This is used by BlurCommandWord
         *
         * @return The current default blurRestriction setting
         */
        @JvmStatic
        fun getDefaultBlurRestriction(): RestrictionType? {
            return defaultBlurRestriction
        }

        /**
         * A default restriction type for blurring.
         */
        private var defaultBlurRestriction: RestrictionType? = NONE
    }
}
