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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Builds a map of a baseline/source text and a changed/target text, navigating
 * it to determine differences.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * The timeout deadline is measured on the monotonic clock (`TimeSource.Monotonic`) instead of the wall clock
 * (`System.currentTimeMillis()` in the Java original); the timeout length is the same.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class DifferenceEngine
/**
 * Find the differences between two texts. Simplifies the problem by
 * stripping any common prefix or suffix off the texts before diffing.
 *
 * @param source
 *            Old string to be diffed
 * @param target
 *            New string to be diffed
 */
(
    /**
     * Made final because we now rely on caching the string lengths (they could be cached
     * at method level if require non-final in future)
     * The baseline text.
     */
    private val source: String,
    /**
     * The changed text.
     *
     */
    private val target: String,
) {
    // cached versions of source and target length,
    // as for roughly 1 call to generate(), was causing 117'000 calls to length()
    private val targetLength: Int = target.length
    private val sourceLength: Int = source.length

    /**
     * Empty Difference Engine, which won't find anything.
     */
    constructor() : this("", "")

    /**
     * Explore the intersection points between the two texts.
     *
     * @return List of Difference objects or null if no diff available
     */
    open fun generate(): MutableList<Difference>? {
        val deadline = TimeSource.Monotonic.markNow() + (timeout * 1000).toLong().milliseconds
        val maxD = (this.sourceLength + this.targetLength) / 2
        var vMap1: MutableList<MutableSet<String>> = ArrayList()
        var vMap2: MutableList<MutableSet<String>> = ArrayList()
        val v1 = HashMap<Int, Int>()
        val v2 = HashMap<Int, Int>()
        v1[1] = 0
        v2[1] = 0
        var x: Int
        var y: Int
        var footstep: String // Used to track overlapping paths.
        val footsteps = HashMap<String, Int>()
        var done = false
        // If the total number of characters is odd, then the front path will
        // collide with the reverse path.
        val front = (this.sourceLength + this.targetLength) % 2 != 0
        for (d in 0 until maxD) {
            // Bail out if timeout reached.
            if (timeout > 0 && deadline.hasPassedNow()) {
                return null
            }

            // Walk the front path one step.
            vMap1.add(HashSet()) // Adds at index 'd'.
            for (k in -d..d step 2) {
                val kPlus1Value = v1[k + 1]
                val kMinus1Value = v1[k - 1]
                if (k == -d || k != d && kMinus1Value!! < kPlus1Value!!) {
                    x = kPlus1Value!!
                } else {
                    x = kMinus1Value!! + 1
                }
                y = x - k
                footstep = "$x,$y"
                if (front && (footsteps.containsKey(footstep))) {
                    done = true
                }
                if (!front) {
                    footsteps[footstep] = d
                }
                while (!done && x < this.sourceLength && y < this.targetLength && source[x] == target[y]) {
                    x++
                    y++
                    footstep = "$x,$y"
                    if (front && footsteps.containsKey(footstep)) {
                        done = true
                    }
                    if (!front) {
                        footsteps[footstep] = d
                    }
                }
                v1[k] = x
                val s = vMap1[d]
                s.add("$x,$y")
                if (done) {
                    // Front path ran over reverse path.
                    val footstepValue = footsteps[footstep]!!
                    vMap2 = vMap2.subList(0, footstepValue + 1)
                    val a = path1(vMap1, source.substring(0, x), target.substring(0, y))
                    a.addAll(path2(vMap2, source.substring(x), target.substring(y)))
                    return a
                }
            }

            // Walk the reverse path one step.
            vMap2.add(HashSet()) // Adds at index 'd'.
            for (k in -d..d step 2) {
                val kPlus1Value = v2[k + 1]
                val kMinus1Value = v2[k - 1]
                if (k == -d || k != d && kMinus1Value!! < kPlus1Value!!) {
                    x = kPlus1Value!!
                } else {
                    x = kMinus1Value!! + 1
                }
                y = x - k
                footstep = "${this.sourceLength - x},${this.targetLength - y}"
                if (!front && (footsteps.containsKey(footstep))) {
                    done = true
                }
                if (front) {
                    footsteps[footstep] = d
                }
                while (!done && x < this.sourceLength && y < this.targetLength && source[this.sourceLength - x - 1] == target[this.targetLength - y - 1]) {
                    x++
                    y++
                    footstep = "${this.sourceLength - x},${this.targetLength - y}"
                    if (!front && (footsteps.containsKey(footstep))) {
                        done = true
                    }
                    if (front) {
                        footsteps[footstep] = d
                    }
                }

                v2[k] = x
                val s = vMap2[d]
                s.add("$x,$y")
                if (done) {
                    // Reverse path ran over front path.
                    val footstepValue = footsteps[footstep]!!
                    vMap1 = vMap1.subList(0, footstepValue + 1)
                    val a = path1(vMap1, source.substring(0, this.sourceLength - x), target.substring(0, this.targetLength - y))
                    a.addAll(path2(vMap2, source.substring(this.sourceLength - x), target.substring(this.targetLength - y)))
                    return a
                }
            }
        }

        // Number of diffs equals number of characters, no commonality at all.
        return null
    }

    /**
     * Work from the middle back to the start to determine the path.
     *
     * @param vMap
     *            List of path sets.
     * @param newSource
     *            Old string fragment to be diffed
     * @param newTarget
     *            New string fragment to be diffed
     * @return List of Difference objects
     */
    protected open fun path1(vMap: MutableList<MutableSet<String>>, newSource: String, newTarget: String): MutableList<Difference> {
        val path: MutableList<Difference> = ArrayList()
        var x = newSource.length
        var y = newTarget.length
        var lastEditType: EditType? = null
        for (d in vMap.size - 2 downTo 0) {
            while (true) {
                val set = vMap[d]
                if (set.contains("${x - 1},$y")) {
                    x--
                    if (EditType.DELETE == lastEditType) {
                        val firstDiff = path[0]
                        firstDiff.prependText(newSource[x])
                    } else {
                        path.add(0, Difference(EditType.DELETE, newSource.substring(x, x + 1)))
                    }
                    lastEditType = EditType.DELETE
                    break
                } else if (set.contains("$x,${y - 1}")) {
                    y--
                    if (EditType.INSERT == lastEditType) {
                        val firstDiff = path[0]
                        firstDiff.prependText(newTarget[y])
                    } else {
                        path.add(0, Difference(EditType.INSERT, newTarget.substring(y, y + 1)))
                    }
                    lastEditType = EditType.INSERT
                    break
                } else {
                    x--
                    y--
                    jswordAssert(newSource[x] == newTarget[y]) { "No diagonal.  Can't happen. (path1)" }
                    if (EditType.EQUAL == lastEditType) {
                        val firstDiff = path[0]
                        firstDiff.prependText(newSource[x])
                    } else {
                        path.add(0, Difference(EditType.EQUAL, newSource.substring(x, x + 1)))
                    }
                    lastEditType = EditType.EQUAL
                }
            }
        }
        return path
    }

    /**
     * Work from the middle back to the end to determine the path.
     *
     * @param vMap
     *            List of path sets.
     * @param newSource
     *            Old string fragment to be diffed
     * @param newTarget
     *            New string fragment to be diffed
     * @return List of Difference objects
     */
    protected open fun path2(vMap: MutableList<MutableSet<String>>, newSource: String, newTarget: String): MutableList<Difference> {
        val path: MutableList<Difference> = ArrayList()

        // cached versions of length from immutable strings
        val cachedNewSourceLength = newSource.length
        val cachedNewTargetLength = newTarget.length

        var x = cachedNewSourceLength
        var y = cachedNewTargetLength
        var lastEditType: EditType? = null
        for (d in vMap.size - 2 downTo 0) {
            while (true) {
                val set = vMap[d]
                if (set.contains("${x - 1},$y")) {
                    x--
                    if (EditType.DELETE == lastEditType) {
                        val lastDiff = path[path.size - 1]
                        lastDiff.appendText(newSource[cachedNewSourceLength - x - 1])
                    } else {
                        path.add(Difference(EditType.DELETE, newSource.substring(cachedNewSourceLength - x - 1, cachedNewSourceLength - x)))
                    }
                    lastEditType = EditType.DELETE
                    break
                } else if (set.contains("$x,${y - 1}")) {
                    y--
                    if (EditType.INSERT == lastEditType) {
                        val lastDiff = path[path.size - 1]
                        lastDiff.appendText(newTarget[cachedNewTargetLength - y - 1])
                    } else {
                        path.add(Difference(EditType.INSERT, newTarget.substring(cachedNewTargetLength - y - 1, cachedNewTargetLength - y)))
                    }
                    lastEditType = EditType.INSERT
                    break
                } else {
                    x--
                    y--
                    jswordAssert(newSource[cachedNewSourceLength - x - 1] == newTarget[cachedNewTargetLength - y - 1]) { "No diagonal.  Can't happen. (path2)" }

                    if (EditType.EQUAL == lastEditType) {
                        val lastDiff = path[path.size - 1]
                        lastDiff.appendText(newSource[cachedNewSourceLength - x - 1])
                    } else {
                        path.add(Difference(EditType.EQUAL, newSource.substring(cachedNewSourceLength - x - 1, cachedNewSourceLength - x)))
                    }
                    lastEditType = EditType.EQUAL
                }
            }
        }
        return path
    }

    companion object {
        /**
         * Set the timeout for the diff operation. The default is 1 second. Use 0
         * for infinity.
         *
         * @param newTimeout the new timeout
         */
        @JvmStatic
        fun setTimeout(newTimeout: Float) {
            timeout = newTimeout
        }

        /**
         * Number of seconds to map a diff before giving up. Use 0 for infinity.
         */
        private const val TIMEOUT = 1.0f
        private var timeout = TIMEOUT
    }
}
