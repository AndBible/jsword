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
/*
 * Package org.crosswire.common.diff (was package-info.java):
 *
 * An implementation of Diff that works within the line.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006
 * http://neil.fraser.name/software/diff_match_patch/
 */
package org.crosswire.common.diff

import kotlin.jvm.JvmSuppressWildcards

/**
 * Computes the difference between two texts to create a list of differences.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Diff
/**
 * Construct an object that can find the differences between two texts.
 *
 * @param source
 *            Old string to be diffed
 * @param target
 *            New string to be diffed
 * @param checkLines
 *            Speedup flag. If false, then don't run a line-level diff first
 *            to identify the changed areas. If true, then run a faster
 *            slightly less optimal diff
 */
(
    /**
     * The baseline text.
     */
    private var source: String,
    /**
     * The changed text.
     */
    private var target: String,
    /**
     * Whether a slightly faster less optimal diff should be run.
     */
    private var checkLines: Boolean,
) {
    /**
     * Construct an object that can find the differences between two texts. Run
     * a faster slightly less optimal diff. This constructor allows the
     * 'checkLines' to be optional. Most of the time checkLines is wanted, so
     * default to true.
     *
     * @param source
     *            Old string to be diffed
     * @param target
     *            New string to be diffed
     */
    constructor(source: String, target: String) : this(source, target, true)

    /**
     * Find the differences between two texts. Simplifies the problem by
     * stripping any common prefix or suffix off the texts before diffing.
     *
     * @return List of Difference objects
     */
    open fun compare(): MutableList<Difference> {
        // Check for equality (speedup)
        var diffs: MutableList<Difference>
        if (source == target) {
            diffs = ArrayList()
            diffs.add(Difference(EditType.EQUAL, source))
            return diffs
        }

        // Trim off common prefix (speedup)
        var commonLength = Commonality.prefix(source, target)
        val commonPrefix = source.substring(0, commonLength)
        source = source.substring(commonLength)
        target = target.substring(commonLength)

        // Trim off common suffix (speedup)
        commonLength = Commonality.suffix(source, target)
        val commonSuffix = source.substring(source.length - commonLength)
        source = source.substring(0, source.length - commonLength)
        target = target.substring(0, target.length - commonLength)

        // Compute the diff on the middle block
        diffs = compute()

        // Restore the prefix and suffix
        if ("" != commonPrefix) {
            diffs.add(0, Difference(EditType.EQUAL, commonPrefix))
        }

        if ("" != commonSuffix) {
            diffs.add(Difference(EditType.EQUAL, commonSuffix))
        }

        DiffCleanup.cleanupMerge(diffs)

        return diffs
    }

    /**
     * Find the differences between two texts.
     *
     * @return List of Difference objects
     */
    private fun compute(): MutableList<Difference> {
        var diffs: MutableList<Difference> = ArrayList()

        if ("" == source) {
            // Just add some text (speedup)
            diffs.add(Difference(EditType.INSERT, target))
            return diffs
        }

        if ("" == target) {
            // Just delete some text (speedup)
            diffs.add(Difference(EditType.DELETE, source))
            return diffs
        }

        val longText = if (source.length > target.length) source else target
        val shortText = if (source.length > target.length) target else source
        val i = longText.indexOf(shortText)
        if (i != -1) {
            // Shorter text is inside the longer text (speedup)
            val editType = if (source.length > target.length) EditType.DELETE else EditType.INSERT
            diffs.add(Difference(editType, longText.substring(0, i)))
            diffs.add(Difference(EditType.EQUAL, shortText))
            diffs.add(Difference(editType, longText.substring(i + shortText.length)))
            return diffs
        }

        // Check to see if the problem can be split in two.
        val middleMatch = Commonality.halfMatch(source, target)
        if (middleMatch != null) {
            // A half-match was found, sort out the return data.
            // Send both pairs off for separate processing.
            val startDiff = Diff(middleMatch.getSourcePrefix()!!, middleMatch.getTargetPrefix()!!, checkLines)
            val endDiff = Diff(middleMatch.getSourceSuffix()!!, middleMatch.getTargetSuffix()!!, checkLines)
            // Merge the results.
            diffs = startDiff.compare()
            diffs.add(Difference(EditType.EQUAL, middleMatch.getCommonality()!!))
            diffs.addAll(endDiff.compare())
            return diffs
        }

        // Perform a real diff.
        if (checkLines && source.length + target.length < 250) {
            checkLines = false // Too trivial for the overhead.
        }

        var lineMap: LineMap? = null
        if (checkLines) {
            // Scan the text on a line-by-line basis first.
            lineMap = LineMap(source, target)
            source = lineMap.getSourceMap()
            target = lineMap.getTargetMap()
        }

        val generated = DifferenceEngine(source, target).generate()

        if (generated == null) {
            // No acceptable result.
            diffs = ArrayList()
            diffs.add(Difference(EditType.DELETE, source))
            diffs.add(Difference(EditType.INSERT, target))
        } else {
            diffs = generated
        }

        if (checkLines && lineMap != null) {
            // Convert the diff back to original text.
            lineMap.restore(diffs)
            // Eliminate freak matches (e.g. blank lines)
            DiffCleanup.cleanupSemantic(diffs)

            // Rediff any replacement blocks, this time character-by-character.
            // Add a dummy entry at the end.
            diffs.add(Difference(EditType.EQUAL, ""))
            var countDeletes = 0
            var countInserts = 0
            val textDelete = StringBuilder()
            val textInsert = StringBuilder()
            val pointer = diffs.listIterator()
            var curDiff: Difference? = pointer.next()
            while (curDiff != null) {
                val editType = curDiff.editType
                if (EditType.INSERT == editType) {
                    countInserts++
                    textInsert.append(curDiff.text)
                } else if (EditType.DELETE == editType) {
                    countDeletes++
                    textDelete.append(curDiff.text)
                } else {
                    // Upon reaching an equality, check for prior redundancies.
                    if (countDeletes >= 1 && countInserts >= 1) {
                        // Delete the offending records and add the merged ones.
                        pointer.previous()
                        for (j in 0 until countDeletes + countInserts) {
                            pointer.previous()
                            pointer.remove()
                        }
                        val newDiff = Diff(textDelete.toString(), textInsert.toString(), false)
                        for (diff in newDiff.compare()) {
                            pointer.add(diff)
                        }
                    }
                    countInserts = 0
                    countDeletes = 0
                    textDelete.clear()
                    textInsert.clear()
                }
                curDiff = if (pointer.hasNext()) pointer.next() else null
            }
            diffs.removeAt(diffs.size - 1) // Remove the dummy entry at the
            // end.
        }
        return diffs
    }

    /**
     * loc is a location in source, compute and return the equivalent location
     * in target. e.g. "The cat" vs "The big cat", 1-&gt;1, 5-&gt;8
     *
     * @param diffs
     *            List of Difference objects
     * @param loc
     *            Location within source
     * @return Location within target
     */
    @JvmSuppressWildcards
    open fun xIndex(diffs: List<Difference>, loc: Int): Int {
        var chars1 = 0
        var chars2 = 0
        var lastChars1 = 0
        var lastChars2 = 0
        var lastDiff: Difference? = null
        for (diff in diffs) {
            val editType = diff.editType

            // Equality or deletion?
            if (EditType.INSERT != editType) {
                chars1 += diff.text.length
            }

            // Equality or insertion?
            if (EditType.DELETE != editType) {
                chars2 += diff.text.length
            }

            // Overshot the location?
            if (chars1 > loc) {
                lastDiff = diff
                break
            }
            lastChars1 = chars1
            lastChars2 = chars2
        }

        // Was the location was deleted?
        if (lastDiff != null && EditType.DELETE == lastDiff.editType) {
            return lastChars2
        }

        // Add the remaining character length.
        return lastChars2 + (loc - lastChars1)
    }

    /**
     * Convert a Difference list into a pretty HTML report.
     *
     * @param diffs
     *            List of Difference objects
     * @return HTML representation
     */
    @JvmSuppressWildcards
    open fun prettyHtml(diffs: List<Difference>): String {
        val buf = StringBuilder()
        for (x in 0 until diffs.size) {
            val diff = diffs[x]
            val editType = diff.editType // Mode (delete, equal,
            // insert)
            val text = diff.text // Text of change.
            // TODO(DMS): Do replacements
            // text = text.replace(/&/g, "&amp;");
            // text = text.replace(/</g, "&lt;");
            // text = text.replace(/>/g, "&gt;");
            // text = text.replace(/\n/g, "&para;<BR>");
            if (EditType.DELETE == editType) {
                buf.append("<del style=\"background:#FFE6E6;\">")
                buf.append(text)
                buf.append("</del>")
            } else if (EditType.INSERT == editType) {
                buf.append("<ins style=\"background:#E6FFE6;\">")
                buf.append(text)
                buf.append("</ins>")
            } else {
                buf.append("<span>")
                buf.append(text)
                buf.append("</span>")
            }
        }
        return buf.toString()
    }
}
