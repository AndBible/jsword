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

import kotlin.jvm.JvmStatic
import kotlin.math.min

/**
 * Compute the distance between 2 strings. The larger the number the greater the
 * distance.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class Distance private constructor() {
    companion object {
        /**
         * Compute the LevenshteinDistance between two strings. See <a
         * href="http://www.merriampark.com/ldjava.htm"
         * >www.merriampark.com/ldjava.htm</a> for original implementation.
         *
         * @param source
         *            the baseline text
         * @param target
         *            the changed text
         * @return the distance
         */
        @JvmStatic
        fun getLevenshteinDistance(source: String?, target: String?): Int {
            if (source == null || target == null) {
                throw IllegalArgumentException("Strings must not be null")
            }

            /*
             * The difference between this impl. and the previous is that, rather
             * than creating and retaining a matrix of size s.length()+1 by
             * t.length()+1, we maintain two single-dimensional arrays of length
             * s.length()+1. The first, d, is the 'current working' distance array
             * that maintains the newest distance cost counts as we iterate through
             * the characters of String s. Each time we increment the index of
             * String t we are comparing, d is copied to p, the second int[]. Doing
             * so allows us to retain the previous cost counts as required by the
             * algorithm (taking the minimum of the cost count to the left, up one,
             * and diagonally up and to the left of the current cost count being
             * calculated). (Note that the arrays aren't really copied anymore, just
             * switched...this is clearly much better than cloning an array or doing
             * a System.arraycopy() each time through the outer loop.)
             *
             * Effectively, the difference between the two implementations is this
             * one does not cause an out of memory condition when calculating the LD
             * over two very large strings.
             */

            val sourceLength = source.length // length of source
            val targetLength = target.length // length of target

            if (sourceLength == 0) {
                return targetLength
            } else if (targetLength == 0) {
                return sourceLength
            }

            var prevDist = IntArray(sourceLength + 1) // 'previous' cost array,
            // horizontally
            var dist = IntArray(sourceLength + 1) // cost array, horizontally
            var swap: IntArray // placeholder to assist in swapping prevDist and dist

            // indexes into strings source and target
            var i: Int // iterates through source
            var j: Int // iterates through target

            var targetJ: Char // jth character of t

            var cost: Int

            i = 0
            while (i <= sourceLength) {
                prevDist[i] = i
                i++
            }

            j = 1
            while (j <= targetLength) {
                targetJ = target[j - 1]
                dist[0] = j

                i = 1
                while (i <= sourceLength) {
                    cost = if (source[i - 1] == targetJ) 0 else 1
                    // minimum of cell to the left + 1, to the top + 1, diagonally
                    // left and up +cost
                    dist[i] = min(min(dist[i - 1] + 1, prevDist[i] + 1), prevDist[i - 1] + cost)
                    i++
                }

                // copy current distance counts to 'previous row' distance counts
                swap = prevDist
                prevDist = dist
                dist = swap
                j++
            }

            // our last action in the above loop was to switch d and p, so p now
            // actually has the most recent cost counts
            return prevDist[sourceLength]
        }
    }
}
