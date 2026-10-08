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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * An implementation of the Bitap algorithm for finding a "fuzzy" location of a
 * match.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Bitap
/**
 * Locate the best instance of 'pattern' in 'text' near 'loc'.
 *
 * @param text
 *            The text to search
 * @param pattern
 *            The pattern to search for
 * @param loc
 *            The location to search around
 */
(
    /**
     * The text to search.
     */
    private val text: String,
    /**
     * The pattern to find in the text.
     */
    private val pattern: String,
    /**
     * The location in text to focus the search.
     */
    private val loc: Int,
) : Locator {
    /**
     * The length of the string constrained between MINLENGHT and MAXLENGTH
     */
    private var scoreTextLength: Int = 0

    /**
     * Alphabet is the compiled representation of the pattern.
     */
    private val alphabet: MutableMap<Char, Int> = HashMap()

    override fun maxPatternLength(): Int {
        return MAXBITS
    }

    override fun locate(): Int {
        // Initialize the alphabet.
        alphabet()

        // Coerce the text length between reasonable maximums and minimums.
        scoreTextLength = max(text.length, minLength)
        scoreTextLength = min(scoreTextLength, maxLength)

        // Highest score beyond which we give up.
        var scoreThreshold: Double = threshold.toDouble()

        // Is there a nearby exact match? (speedup)
        var bestLoc = text.indexOf(pattern, loc)
        if (bestLoc != -1) {
            scoreThreshold = min(bitapScore(0, bestLoc), scoreThreshold)
        }

        // What about in the other direction? (speedup)
        bestLoc = text.lastIndexOf(pattern, loc + pattern.length)
        if (bestLoc != -1) {
            scoreThreshold = min(bitapScore(0, bestLoc), scoreThreshold)
        }

        // Initialize the bit arrays.
        val matchmask = 2.0.pow(pattern.length - 1).toInt()

        bestLoc = -1

        var binMin: Int
        var binMid: Int
        var binMax = max(loc + loc, text.length)
        var lastrd = IntArray(0)
        for (d in 0 until pattern.length) {
            // Scan for the best match; each iteration allows for one more
            // error.
            val rd = IntArray(text.length)

            // Run a binary search to determine how far from 'loc' we can stray
            // at this error level.
            binMin = loc
            binMid = binMax
            while (binMin < binMid) {
                if (bitapScore(d, binMid) < scoreThreshold) {
                    binMin = binMid
                } else {
                    binMax = binMid
                }
                binMid = (binMax - binMin) / 2 + binMin
            }

            binMax = binMid // Use the result from this iteration as the
            // maximum for the next.
            var start = max(0, loc - (binMid - loc) - 1)
            val finish = min(text.length - 1, pattern.length + binMid)

            if (text[finish] == pattern[pattern.length - 1]) {
                rd[finish] = 2.0.pow(d + 1).toInt() - 1
            } else {
                rd[finish] = 2.0.pow(d).toInt() - 1
            }

            // A while loop, not a downTo range: start is moved inside the loop and Java re-tested it each pass.
            var j = finish - 1
            while (j >= start) {
                val curChar = text[j]
                val mask = alphabet[curChar] ?: 0
                if (d == 0) { // First pass: exact match.
                    rd[j] = ((rd[j + 1] shl 1) or 1) and mask
                } else { // Subsequent passes: fuzzy match.
                    rd[j] = ((rd[j + 1] shl 1) or 1) and mask or ((lastrd[j + 1] shl 1) or 1) or ((lastrd[j] shl 1) or 1) or lastrd[j + 1]
                }

                if ((rd[j] and matchmask) != 0) {
                    val score = bitapScore(d, j)
                    // This match will almost certainly be better than any
                    // existing match. But check anyway.
                    if (score <= scoreThreshold) {
                        // Told you so.
                        scoreThreshold = score
                        bestLoc = j
                        if (j > loc) {
                            // When passing loc, don't exceed our current
                            // distance from loc.
                            start = max(0, loc - (j - loc))
                        } else {
                            // Already passed loc, downhill from here on in.
                            break
                        }
                    }
                }
                j--
            }

            if (bitapScore(d + 1, loc) > scoreThreshold) {
                // No hope for a (better) match at greater error levels.
                break
            }

            lastrd = rd
        }

        return bestLoc
    }

    protected open fun getAlphabet(): MutableMap<Char, Int> {
        return alphabet
    }

    /**
     * Compute and return the score for a match with e errors and x location.
     *
     * @param e
     *            Number of errors in match
     * @param x
     *            Location of match
     * @return Overall score for match
     */
    private fun bitapScore(e: Int, x: Int): Double {
        // Compute and return the score for a match with e errors and x
        // location.
        val d = abs(loc - x)
        return (e / pattern.length.toFloat() / balance) + (d / scoreTextLength.toFloat() / (1.0 - balance))
    }

    // Initialize the alphabet for the Bitap algorithm.
    protected open fun alphabet() {
        val len = pattern.length

        jswordAssert(len <= MAXBITS) { "Pattern too long for this application." }

        for (i in 0 until len) {
            val c = pattern[i]
            val value = alphabet[c]
            var mask = value ?: 0
            mask = mask or 2.0.pow(len - i - 1).toInt()
            alphabet[c] = mask
        }
    }

    companion object {
        @JvmStatic
        fun setBalance(newBalance: Float) {
            balance = newBalance
        }

        @JvmStatic
        fun setThreshold(newThreshold: Float) {
            threshold = newThreshold
        }

        @JvmStatic
        fun setMinLength(newMinLength: Int) {
            minLength = newMinLength
        }

        @JvmStatic
        fun setMaxLength(newMaxLength: Int) {
            maxLength = newMaxLength
        }

        /**
         * The maximum number of bits in an int. Change this to 64 if long is used
         * by alphabet.
         */
        private const val MAXBITS = 32

        /**
         * Tweak the relative importance (0.0 = accuracy, 1.0 = proximity)
         */
        private const val BALANCE = 0.5f
        private var balance = BALANCE

        /**
         * At what point is no match declared (0.0 = perfection, 1.0 = very loose)
         */
        private const val THRESHOLD = 0.5f
        private var threshold = THRESHOLD

        /**
         * The min and max cutoffs used when computing text lengths.
         */
        private const val MINLENGTH = 100
        private var minLength = MINLENGTH
        private const val MAXLENGTH = 1000
        private var maxLength = MAXLENGTH
    }
}
