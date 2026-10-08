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

/**
 * LineMap is a heuristic algorithm that allows the differencing of a
 * representation of lines. A Diff of the source and target maps can be
 * reconstituted with restore.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class LineMap(source: String, target: String) {
    /**
     * Each character in sourceMap provides an integer representation of the
     * line in the original.
     */
    private val sourceMap: String

    /**
     * Each character in sourceMap provides an integer representation of the
     * line in the original.
     */
    private val targetMap: String

    /**
     * The lines from the original. Useful for reconstitution.
     */
    private val lines: MutableList<String>

    /**
     * Split two texts into a list of strings. Reduce the texts to a string of
     * hashes where each Unicode character represents one line. The result is
     * that text1 is encoded into
     *
     * @param source
     *            Baseline string
     * @param target
     *            Changed string
     */
    init {
        // e.g. linearray[4] == "Hello\n"
        // e.g. linehash.get("Hello\n") == 4

        // "\x00" is a valid character, but various debuggers don't like it.
        // So we'll insert a junk entry to avoid generating a null character.
        lines = ArrayList()
        lines.add("")

        val linehash = HashMap<String, Int>()
        sourceMap = linesToCharsMunge(source, lines, linehash)
        targetMap = linesToCharsMunge(target, lines, linehash)
    }

    /**
     * Rehydrate the text in a diff from a string of line hashes to real lines
     * of text.
     *
     * @param diffs
     *            List of Difference objects
     */
    open fun restore(diffs: List<*>) {
        val text = StringBuilder()
        for (x in 0 until diffs.size) {
            val diff = diffs[x] as Difference
            val chars = diff.text

            text.clear()
            for (y in 0 until chars.length) {
                text.append(lines[chars[y].code])
            }

            diff.text = text.toString()
        }
    }

    /**
     * @return the sourceMap
     */
    open fun getSourceMap(): String {
        return sourceMap
    }

    /**
     * @return the targetMap
     */
    open fun getTargetMap(): String {
        return targetMap
    }

    /**
     * @return the lines
     */
    open fun getLines(): List<String> {
        return lines
    }

    /**
     * Split a text into a list of strings. Reduce the texts to a string of
     * hashes where each Unicode character represents one line.
     *
     * @param text
     *            String to encode
     * @param linearray
     *            List of unique strings
     * @param linehash
     *            Map of strings to indices
     * @return Encoded string
     */
    private fun linesToCharsMunge(text: String, linearray: MutableList<String>, linehash: MutableMap<String, Int>): String {
        val buf = StringBuilder()
        var work = text
        // text.split('\n') would work fine, but would temporarily double our
        // memory footprint for minimal speed improvement.
        while (work.length != 0) {
            var i = work.indexOf('\n')
            if (i == -1) {
                i = work.length - 1
            }
            val line = work.substring(0, i + 1)
            work = work.substring(i + 1)
            if (linehash.containsKey(line)) {
                val charInt = linehash[line]!!
                buf.append(charInt.toChar())
            } else {
                linearray.add(line)
                linehash[line] = linearray.size - 1
                buf.append((linearray.size - 1).toChar())
            }
        }
        return buf.toString()
    }
}
