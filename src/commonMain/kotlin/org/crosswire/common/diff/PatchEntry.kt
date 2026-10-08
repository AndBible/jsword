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

import org.crosswire.common.util.splitLikeJava
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * A PatchEntry is a single "instruction" in a Patch, consisting of a interval
 * over which differences are applied and the differences that should be
 * applied.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class PatchEntry : Iterable<Difference> {
    // Constructor for a patch object.
    constructor()

    // Constructor for a patch object.
    constructor(patchText: String) : this() {
        fromText(patchText)
    }

    /**
     * The sourceStart
     */
    open var sourceStart: Int = 0

    /**
     * @param adjustment
     *            the adjustment to sourceStart
     */
    open fun adjustSourceStart(adjustment: Int) {
        this.sourceStart += adjustment
    }

    /**
     * The targetStart
     */
    open var targetStart: Int = 0

    /**
     * @param adjustment
     *            the adjustment to targetStart
     */
    open fun adjustTargetStart(adjustment: Int) {
        this.targetStart += adjustment
    }

    /**
     * The sourceLength
     */
    open var sourceLength: Int = 0

    /**
     * @param adjustment
     *            the adjustment to sourceLength
     */
    open fun adjustSourceLength(adjustment: Int) {
        this.sourceLength += adjustment
    }

    /**
     * The targetLength
     */
    open var targetLength: Int = 0

    /**
     * @param adjustment
     *            the adjustment to targetLength
     */
    open fun adjustTargetLength(adjustment: Int) {
        this.targetLength += adjustment
    }

    // Emulate GNU diff's format.
    // Header: @@ -382,8 +481,9 @@
    // Indices are printed as 1-based, not 0-based.
    override fun toString(): String {
        val txt = StringBuilder()
        txt.append("@@ -")
        txt.append(getCoordinates(sourceStart, sourceLength))
        txt.append(" +")
        txt.append(getCoordinates(targetStart, targetLength))
        txt.append(" @@\n")

        for (diff in diffs) {
            txt.append(diff.editType.symbol)
            txt.append(encode(diff.text))
            txt.append('\n')
        }
        // String result = txt.toString();
        // // Replicate the JavaScript encodeURI() function (not including %20)
        // result = result.replace("%3D", "=").replace("%3B",
        // ";").replace("%27", "'")
        // .replace("%2C", ",").replace("%2F", "/").replace("%7E", "~")
        // .replace("%21", "!").replace("%40", "@").replace("%23", "#")
        // .replace("%24", "$").replace("%26", "&").replace("%28", "(")
        // .replace("%29", ")").replace("%2B", "+").replace("%3A", ":")
        // .replace("%3F", "?");
        return txt.toString()
    }

    /**
     * Parse a textual representation of a patch entry and populate this patch
     * entry.
     *
     * @param input
     *            Text representation of this patch entry
     * @return this patch entry
     */
    open fun fromText(input: String): PatchEntry {
        diffs.clear()
        val text = input.splitLikeJava(newlinePattern)
        var sign: Char
        var line: String

        // Java: matcher.matches() was not checked; matcher.group(n) then threw IllegalStateException("No match found").
        val matcher = patchPattern.matchEntire(text[0]) ?: throw IllegalStateException("No match found")
        // m = text[0].match(/^@@ -(\d+),?(\d*) \+(\d+),?(\d*) @@$/);
        val groups = matcher.groupValues

        sourceStart = groups[1].toInt()

        if (groups[2].length == 0) {
            sourceStart--
            sourceLength = 1
        } else if (groups[2][0] == '0') {
            sourceLength = 0
        } else {
            sourceStart--
            sourceLength = groups[2].toInt()
        }

        targetStart = groups[3].toInt()
        if (groups[4].length == 0) {
            targetStart--
            targetLength = 1
        } else if (groups[4][0] == '0') {
            targetLength = 0
        } else {
            targetStart--
            targetLength = groups[4].toInt()
        }

        for (lineCount in 1 until text.size) {
            line = text[lineCount]
            if (line.length > 0) {
                sign = line[0]
                line = decode(line.substring(1))
                diffs.add(Difference(EditType.fromSymbol(sign)!!, line))
            }
        }
        return this
    }

    // Compute and return the source text (all equalities and deletions).
    open fun getSourceText(): String {
        val txt = StringBuilder()
        for (diff in diffs) {
            if (EditType.INSERT != diff.editType) {
                txt.append(diff.text)
            }
        }
        return txt.toString()
    }

    // Compute and return the destination text (all equalities and insertions).
    open fun getTargetText(): String {
        val txt = StringBuilder()
        for (diff in diffs) {
            if (EditType.DELETE != diff.editType) {
                txt.append(diff.text)
            }
        }
        return txt.toString()
    }

    open fun addContext(text: String) {
        val maxPatternLength = Match().maxPatternLength()
        var padding = 0
        var pattern = text.substring(targetStart, targetStart + sourceLength)
        val textLength = text.length

        // Increase the context until we're unique
        // (but don't let the pattern expand beyond the maximum length our
        // Locator can handle).
        val end = maxPatternLength - margin - margin
        // lastIndexOf with an explicit fromIndex: Java's lastIndexOf(String) starts at length, Kotlin's default at length - 1.
        while (text.indexOf(pattern) != text.lastIndexOf(pattern, textLength) && pattern.length < end) {
            padding += margin
            pattern = text.substring(max(0, targetStart - padding), min(textLength, targetStart + sourceLength + padding))
        }

        // Add one chunk for good luck.
        padding += margin

        // Add the prefix.
        val prefix = text.substring(max(0, targetStart - padding), targetStart)
        val prefixLength = prefix.length
        if (prefixLength > 0) {
            diffs.add(0, Difference(EditType.EQUAL, prefix))
        }

        // Add the suffix
        val suffix = text.substring(targetStart + sourceLength, min(textLength, targetStart + sourceLength + padding))
        val suffixLength = suffix.length
        if (suffixLength > 0) {
            diffs.add(Difference(EditType.EQUAL, suffix))
        }

        // Roll back the start points.
        sourceStart -= prefixLength
        targetStart -= prefixLength

        // Extend the lengths.
        sourceLength += prefixLength + suffixLength
        targetLength += prefixLength + suffixLength
    }

    open fun addDifference(diff: Difference) {
        diffs.add(diff)
    }

    open fun getDifferenceCount(): Int {
        return diffs.size
    }

    open fun hasDifferences(): Boolean {
        return !diffs.isEmpty()
    }

    override fun iterator(): Iterator<Difference> {
        return diffs.iterator()
    }

    open fun getFirstDifference(): Difference? {
        if (diffs.isEmpty()) {
            return null
        }
        return diffs[0]
    }

    open fun removeFirstDifference(): Difference? {
        if (diffs.isEmpty()) {
            return null
        }
        return diffs.removeAt(0)
    }

    open fun getLastDifference(): Difference? {
        if (diffs.isEmpty()) {
            return null
        }
        return diffs[diffs.size - 1]
    }

    protected open fun setDifferences(newDiffs: MutableList<Difference>) {
        diffs = newDiffs
    }

    private fun getCoordinates(start: Int, length: Int): String {
        val buf = StringBuilder()

        if (length == 0) {
            buf.append(start)
            buf.append(",0")
        } else if (length == 1) {
            buf.append(sourceStart + 1)
        } else {
            buf.append(start + 1)
            buf.append(',')
            buf.append(length)
        }

        return buf.toString()
    }

    /**
     * This algorithm allows for \n to be included in a difference. Thus it
     * needs to be escaped. We will use URL encoding of \n. But this makes % a
     * meta-character, thus it needs to be encoded too.
     *
     * @param str
     *            the unencoded string
     * @return the encoded string
     */
    private fun encode(str: String): String {
        val strlen = str.length
        val buf = StringBuilder(2 * strlen)
        for (i in 0 until strlen) {
            val c = str[i]
            when (c) {
                '%' -> buf.append("%25")
                '\n' -> buf.append("%0A")
                else -> buf.append(c)
            }
        }
        return buf.toString()
    }

    /**
     * Undo encoding
     *
     * @param str
     *            the encoded string
     * @return the unencoded string
     */
    private fun decode(str: String): String {
        val strlen = str.length
        val buf = StringBuilder(2 * strlen)
        var i = 0
        while (i < strlen) {
            val c = str[i]
            if (c == '%') {
                if ("%0A" == str.substring(i, i + 3)) {
                    buf.append('\n')
                } else { // if ("%25".equals(str.substring(i, i + 3))
                    buf.append('%')
                }
                i += 2
            } else {
                buf.append(c)
            }
            i++
        }
        return buf.toString()
    }

    private var diffs: MutableList<Difference> = ArrayList()

    companion object {
        /**
         * @param newMargin
         *            the margin to set
         */
        @JvmStatic
        fun setMargin(newMargin: Int) {
            margin = newMargin
        }

        /**
         * @return the margin
         */
        @JvmStatic
        fun getMargin(): Int {
            return margin
        }

        /**
         * Chunk size for context length.
         */
        private const val MARGIN = 4
        private var margin = MARGIN
        private val newlinePattern = Regex("\n")
        private val patchPattern = Regex("^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@$")
    }
}
