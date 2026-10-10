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

import org.crosswire.common.util.StringUtil
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.versification.Versification

/**
 * A factory that creates VerseRanges from user input.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class VerseRangeFactory
/**
 * prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Construct a VerseRange from a human readable string. For example
         * "Gen 1:1-3" in case the user does not want to have their typing 'fixed'
         * by a meddling patronizing computer.
         *
         * @param v11n
         *            The versification for this VerseRange
         * @param orginal
         *            The textual representation
         * @return the verse range for the string
         * @exception NoSuchVerseException
         *                If the text can not be understood
         */
        @JvmStatic
        @Throws(NoSuchVerseException::class)
        fun fromString(v11n: Versification?, orginal: String?): VerseRange {
            return fromString(v11n, orginal, null)
        }

        /**
         * Construct a VerseRange from a String and a VerseRange. For example given
         * "2:2" and a basis of Gen 1:1-2 the result would be range of 1 verse
         * starting at Gen 2:2. Also given "2:2-5" and a basis of Gen 1:1-2 the
         * result would be a range of 5 verses starting at Gen 1:1.
         * <p>
         * This constructor is different from the (String, Verse) constructor in
         * that if the basis is a range that exactly covers a chapter and the string
         * is a single number, then we assume that the number refers to a chapter
         * and not to a verse. This allows us to have a Passage like "Gen 1,2" and
         * have the 2 understood as chapter 2 and not verse 2 of Gen 1, which would
         * have occurred otherwise.
         *
         * @param v11n
         *            The versification for this VerseRange
         * @param original
         *            The string describing the verse e.g "2:2"
         * @param basis
         *            The verse that forms the basis by which to understand the
         *            original.
         * @return the verse range
         * @exception NoSuchVerseException
         *                If the reference is illegal
         */
        @JvmStatic
        @Throws(NoSuchVerseException::class)
        fun fromString(v11n: Versification?, original: String?, basis: VerseRange?): VerseRange {
            val parts = StringUtil.splitAll(original, VerseRange.RANGE_OSIS_DELIM)

            when (parts.size) {
                1 -> return fromText(v11n, original, parts[0], parts[0], basis)

                2 -> return fromText(v11n, original, parts[0], parts[1], basis)

                else ->
                    // TRANSLATOR: The user specified a verse range with too many separators. {0} is a placeholder for the allowable separators.
                    throw NoSuchVerseException(JSMsg.gettext("A verse range cannot have more than 2 parts. (Parts are separated by {0}) Given {1}", VerseRange.RANGE_OSIS_DELIM, original))
            }
        }

        /**
         * The internal mechanism by which we construct a VerseRange
         *
         * @param v11n
         *            The versification for this VerseRange
         * @param original
         *            The string describing the verse e.g "2:2"
         * @param startVerseDesc
         *            The part of the range before the range separator
         * @param endVerseDesc
         *            The part of the range after the range separator
         * @param basis
         *            The verse that forms the basis by which to understand the
         *            original.
         * @exception NoSuchVerseException
         *                If the reference is illegal
         */
        @Throws(NoSuchVerseException::class)
        private fun fromText(v11n: Versification?, original: String?, startVerseDesc: String, endVerseDesc: String, basis: VerseRange?): VerseRange {
            val startParts = AccuracyType.tokenize(startVerseDesc)
            val accuracyStart = AccuracyType.fromText(v11n, original, startParts, basis)
            val start = accuracyStart.createStartVerse(v11n, basis, startParts)
            v11n!!.validate(start.book, start.chapter, start.verse)

            val endParts: Array<String>
            if (startVerseDesc == endVerseDesc) {
                endParts = startParts
            } else {
                endParts = AccuracyType.tokenize(endVerseDesc)
            }

            val accuracyEnd = AccuracyType.fromText(v11n, original, endParts, accuracyStart, basis)
            val end = accuracyEnd.createEndVerse(v11n, start, endParts)
            v11n.validate(end.book, end.chapter, end.verse)

            return VerseRange(v11n, start, end)
        }
    }
}
