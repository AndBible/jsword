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
package org.crosswire.common.diff

import org.crosswire.common.util.jswordAssert
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * A Commonality is shared text at the beginning, middle or end of two strings.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class Commonality
/**
 * This is a utility class, therefore the constructor is private.
 */
private constructor() {
    companion object {
        /**
         * Find the length of a common prefix.
         *
         * @param source
         *            First string
         * @param target
         *            Second string
         * @return The number of characters common to the start of each string.
         */
        @JvmStatic
        fun prefix(source: String, target: String): Int {
            var pointermin = 0
            var pointermax = min(source.length, target.length)
            var pointermid = pointermax
            while (pointermin < pointermid) {
                if (source.regionMatches(0, target, 0, pointermid)) {
                    pointermin = pointermid
                } else {
                    pointermax = pointermid
                }
                pointermid = (pointermax - pointermin) / 2 + pointermin
            }

            return pointermid
        }

        /**
         * Find the length of a common suffix.
         *
         * @param source
         *            First string
         * @param target
         *            Second string
         * @return The number of characters common to the end of each string.
         */
        @JvmStatic
        fun suffix(source: String, target: String): Int {
            var pointermin = 0
            var pointermax = min(source.length, target.length)
            var pointermid = pointermax
            while (pointermin < pointermid) {
                if (source.regionMatches(source.length - pointermid, target, target.length - pointermid, pointermid)) {
                    pointermin = pointermid
                } else {
                    pointermax = pointermid
                }
                pointermid = (pointermax - pointermin) / 2 + pointermin
            }

            return pointermid
        }

        /**
         * Do the two texts share a substring which is at least half the length of
         * the longer text?
         *
         * @param source
         *            Baseline string
         * @param target
         *            Changed string
         * @return a CommonMiddle Or null if there was no match.
         */
        @JvmStatic
        fun halfMatch(source: String, target: String): CommonMiddle? {
            val sourceLength = source.length
            val targetLength = target.length
            val longText = if (sourceLength > targetLength) source else target
            val shortText = if (sourceLength > targetLength) target else source
            val longTextLength = max(sourceLength, targetLength)
            if (longTextLength < 10 || shortText.length < 1) {
                return null // Pointless.
            }

            // First check if the second quarter is the seed for a half-match.
            val hm1 = halfMatch(longText, shortText, ceil(longTextLength, 4))
            // Check again based on the third quarter.
            val hm2 = halfMatch(longText, shortText, ceil(longTextLength, 2))
            val hm: CommonMiddle?
            if (hm1 == null && hm2 == null) {
                return null
            } else if (hm2 == null) {
                hm = hm1
            } else if (hm1 == null) {
                hm = hm2
            } else {
                // Both matched. Select the longest.
                hm = if (hm1.getCommonality()!!.length > hm2.getCommonality()!!.length) hm1 else hm2
            }

            // A half-match was found, sort out the return data.
            if (sourceLength > targetLength) {
                return hm
            }

            // While hm cannot be null our QA checker can't figure that out.
            if (hm != null) {
                return CommonMiddle(hm.getTargetPrefix(), hm.getTargetSuffix(), hm.getSourcePrefix(), hm.getSourceSuffix(), hm.getCommonality())
            }
            return null
        }

        /**
         * Does a substring of shortText exist within longText such that the
         * substring is at least half the length of longText?
         *
         * @param longText
         *            Longer string
         * @param shortText
         *            Shorter string
         * @param startIndex
         *            Start index of quarter length substring within longText
         * @return Five element String array, containing the prefix of longText, the
         *         suffix of longText, the prefix of shortText, the suffix of
         *         shortText and the common middle. Or null if there was no match.
         */
        private fun halfMatch(longText: String, shortText: String, startIndex: Int): CommonMiddle? {
            // Start with a 1/4 length substring at position i as a seed.
            val seed = longText.substring(startIndex, startIndex + (longText.length / 4))
            var j = -1
            var common = ""
            var longTextPrefix = ""
            var longTextSuffix = ""
            var shortTextPrefix = ""
            var shortTextSuffix = ""
            while (shortText.indexOf(seed, j + 1).also { j = it } != -1) {
                val prefixLength = prefix(longText.substring(startIndex), shortText.substring(j))
                val suffixLength = suffix(longText.substring(0, startIndex), shortText.substring(0, j))
                if (common.length < (prefixLength + suffixLength)) {
                    common = shortText.substring(j - suffixLength, j) + shortText.substring(j, j + prefixLength)
                    longTextPrefix = longText.substring(0, startIndex - suffixLength)
                    longTextSuffix = longText.substring(startIndex + prefixLength)
                    shortTextPrefix = shortText.substring(0, j - suffixLength)
                    shortTextSuffix = shortText.substring(j + prefixLength)
                }
            }

            if (common.length >= longText.length / 2) {
                return CommonMiddle(longTextPrefix, longTextSuffix, shortTextPrefix, shortTextSuffix, common)
            }

            return null
        }

        private fun ceil(number: Int, divisor: Int): Int {
            jswordAssert(divisor > 0) { "" }
            val result = number / divisor + (if ((number % divisor) > 0) 1 else 0)
            // assert result == (int) Math.ceil(((double) number) / ((double) divisor));
            return result
        }
    }
}
