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

import kotlin.math.max
import kotlin.math.min

/**
 * Marshals a patch to a list of Differences, Differences to a patch and applies
 * a list of differences to text to patch it.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Patch {
    /**
     * Create an empty patch.
     */
    constructor() {
        patches = ArrayList()
        margin = PatchEntry.getMargin()
    }

    /**
     * Create a Patch from a textual representation,
     *
     * @param input
     *            Text representation of patches
     */
    constructor(input: String) : this() {
        fromText(input)
    }

    /**
     * Create a patch that can turn text1 into text2.
     *
     * @param source
     *            Old text
     * @param target
     *            New text
     */
    constructor(source: String, target: String) : this(source, target, null)

    /**
     * Create a patch that can turn text1 into text2. Use the diffs provided, if
     * not null. Compute diffs otherwise.
     *
     * @param source
     *            Old text
     * @param target
     *            New text
     * @param diffs
     *            Optional array of diff tuples for text1 to text2.
     */
    constructor(source: String, target: String, diffs: MutableList<Difference>?) : this() {
        make(source, target, diffs)
    }

    /**
     * Compute a list of patches to turn text1 into text2. Use the diffs
     * provided.
     *
     * @param source
     *            Old text
     * @param target
     *            New text
     * @param diffList
     *            Optional array of diff tuples for text1 to text2.
     * @return this patch
     */
    open fun make(source: String, target: String, diffList: MutableList<Difference>?): Patch {
        var diffs = diffList
        if (diffs == null) {
            val diff = Diff(source, target)
            diffs = diff.compare()
            if (diffs.size > 2) {
                DiffCleanup.cleanupSemantic(diffs)
                DiffCleanup.cleanupEfficiency(diffs)
            }
        }

        patches.clear()

        if (diffs.isEmpty()) {
            return this // Get rid of the null case.
        }

        var patch = PatchEntry()
        var charCount1 = 0 // Number of characters into the text1 string.
        var charCount2 = 0 // Number of characters into the text2 string.
        // Recreate the patches to determine context info.
        var prePatchText = source
        var postPatchText = source
        var x = 0
        for (diff in diffs) {
            val editType = diff.editType
            val diffText = diff.text
            val len = diffText.length

            if (!patch.hasDifferences() && EditType.EQUAL != editType) {
                // A new patch starts here.
                patch.sourceStart = charCount1
                patch.targetStart = charCount2
            }

            if (EditType.INSERT == editType) {
                // Insertion
                patch.addDifference(diff)
                patch.adjustTargetLength(len)
                postPatchText = postPatchText.substring(0, charCount2) + diffText + postPatchText.substring(charCount2)
            } else if (EditType.DELETE == editType) {
                // Deletion.
                patch.adjustSourceLength(len)
                patch.addDifference(diff)
                postPatchText = postPatchText.substring(0, charCount2) + postPatchText.substring(charCount2 + len)
            } else if (EditType.EQUAL == editType && len <= 2 * margin && patch.hasDifferences() && diffs.size != x + 1) {
                // Small equality inside a patch.
                patch.addDifference(diff)
                patch.adjustSourceLength(len)
                patch.adjustTargetLength(len)
            }

            // Time for a new patch.
            if (EditType.EQUAL == editType && len >= 2 * margin && patch.hasDifferences()) {
                patch.addContext(prePatchText)
                patches.add(patch)
                patch = PatchEntry()
                prePatchText = postPatchText
            }

            // Update the current character count.
            if (EditType.INSERT != editType) {
                charCount1 += len
            }

            if (EditType.DELETE != editType) {
                charCount2 += len
            }

            x++
        }

        // Pick up the leftover patch if not empty.
        if (patch.hasDifferences()) {
            patch.addContext(prePatchText)
            patches.add(patch)
        }

        return this
    }

    /**
     * Merge this patch onto the text. Return a patched text, as well as an
     * array of true/false values indicating which patches were applied.
     *
     * @param text
     *            Old text
     * @return the patch result
     */
    open fun apply(text: String): PatchResults {
        splitMax()
        val results = BooleanArray(patches.size)
        var resultText = text
        var delta = 0
        var expectedLoc = 0
        var startLoc = -1
        var text1 = ""
        var text2 = ""
        var diffs: List<Difference>
        var index1 = 0
        var index2 = 0
        var x = 0
        for (aPatch in patches) {
            expectedLoc = aPatch.targetStart + delta
            text1 = aPatch.getSourceText()
            val match = Match(resultText, text1, expectedLoc)
            startLoc = match.locate()
            if (startLoc == -1) {
                // No match found. :(
                results[x] = false
            } else {
                // Found a match. :)
                results[x] = true
                delta = startLoc - expectedLoc
                text2 = resultText.substring(startLoc, startLoc + text1.length)
                if (text1 == text2) {
                    // Perfect match, just shove the replacement text in.
                    resultText = resultText.substring(0, startLoc) + aPatch.getTargetText() + resultText.substring(startLoc + text1.length)
                } else {
                    // Imperfect match. Run a diff to get a framework of
                    // equivalent indicies.
                    val diff = Diff(text1, text2, false)
                    diffs = diff.compare()
                    index1 = 0
                    for (aDiff in aPatch) {
                        val editType = aDiff.editType
                        if (EditType.EQUAL != editType) {
                            index2 = diff.xIndex(diffs, index1)
                        }

                        if (EditType.INSERT == editType) {
                            // Insertion
                            resultText = resultText.substring(0, startLoc + index2) + aDiff.text + resultText.substring(startLoc + index2)
                        } else if (EditType.DELETE == editType) {
                            // Deletion
                            resultText = resultText.substring(0, startLoc + index2) +
                                resultText.substring(startLoc + diff.xIndex(diffs, index1 + aDiff.text.length))
                        }

                        if (EditType.DELETE != editType) {
                            index1 += aDiff.text.length
                        }
                    }
                }
            }
            x++
        }
        return PatchResults(resultText, results)
    }

    /**
     * Look through the patches and break up any which are longer than the
     * maximum limit of the match algorithm.
     */
    open fun splitMax() {
        val maxPatternLength = Match().maxPatternLength()
        val pointer = patches.listIterator()
        var bigPatch: PatchEntry? = if (pointer.hasNext()) pointer.next() else null
        while (bigPatch != null) {
            if (bigPatch.sourceLength <= maxPatternLength) {
                if (!pointer.hasNext()) {
                    break
                }

                bigPatch = pointer.next()
            }

            // Remove the big old patch.
            pointer.remove()
            val patchSize = maxPatternLength
            var start1 = bigPatch.sourceStart
            var start2 = bigPatch.targetStart
            var preContext = ""
            while (bigPatch.hasDifferences()) {
                // Create one of several smaller patches.
                val patch = PatchEntry()
                var empty = true

                var len = preContext.length
                patch.sourceStart = start1 - len
                patch.targetStart = start2 - len
                if (len > 0) {
                    patch.sourceLength = len
                    patch.targetLength = len
                    patch.addDifference(Difference(EditType.EQUAL, preContext))
                }

                while (bigPatch.hasDifferences() && patch.sourceLength < patchSize - margin) {
                    val bigDiff = bigPatch.getFirstDifference()!!
                    val editType = bigDiff.editType
                    var diffText = bigDiff.text
                    if (EditType.INSERT == editType) {
                        // Insertions are harmless.
                        len = diffText.length
                        patch.adjustTargetLength(len)
                        start2 += len
                        patch.addDifference(bigPatch.removeFirstDifference()!!)
                        empty = false
                    } else {
                        // Deletion or equality. Only take as much as we can
                        // stomach.
                        diffText = diffText.substring(0, min(diffText.length, patchSize - patch.sourceLength - margin))
                        len = diffText.length
                        patch.adjustSourceLength(len)
                        start1 += len
                        if (EditType.EQUAL == editType) {
                            patch.adjustTargetLength(len)
                            start2 += len
                        } else {
                            empty = false
                        }

                        patch.addDifference(Difference(editType, diffText))

                        if (diffText == bigDiff.text) {
                            bigPatch.removeFirstDifference()
                        } else {
                            bigDiff.text = bigDiff.text.substring(len)
                        }
                    }
                }

                // Compute the head context for the next patch.
                preContext = patch.getTargetText()
                preContext = preContext.substring(max(0, preContext.length - margin))

                // Append the end context for this patch.
                val postcontext: String
                if (bigPatch.getSourceText().length > margin) {
                    postcontext = bigPatch.getSourceText().substring(0, margin)
                } else {
                    postcontext = bigPatch.getSourceText()
                }
                if (postcontext.length > 0) {
                    patch.adjustSourceLength(postcontext.length)
                    patch.adjustTargetLength(postcontext.length)
                    if (patch.getDifferenceCount() > 0 && EditType.EQUAL == patch.getLastDifference()!!.editType) {
                        val diff = patch.getLastDifference()!!
                        diff.appendText(postcontext)
                    } else {
                        patch.addDifference(Difference(EditType.EQUAL, postcontext))
                    }
                }

                if (!empty) {
                    pointer.add(patch)
                }
            }

            bigPatch = if (pointer.hasNext()) pointer.next() else null
        }
    }

    /**
     * Take a list of patches and return a textual representation.
     *
     * @return Text representation of patches.
     */
    open fun toText(): String {
        val text = StringBuilder()
        for (entry in patches) {
            text.append(entry)
        }
        return text.toString()
    }

    /**
     * Parse a textual representation of patches and return a List of Patch
     * objects.
     *
     * @param input
     *            Text representation of patches
     * @return List of Patch objects
     */
    open fun fromText(input: String): Patch {
        patches.clear()

        // Add segments before each match found
        var index = 0
        for (m in patchBoundaryPattern.findAll(input)) {
            val start = m.range.first
            val match = input.substring(index, start)
            patches.add(PatchEntry(match))
            index = start + 1
        }

        if (index == 0) {
            // No match was found, the patch consists of the entire string
            patches.add(PatchEntry(input))
        } else {
            // Add remaining segment
            patches.add(PatchEntry(input.substring(index)))
        }

        return this
    }

    /**
     * A holder of the results of a patch, with a results indicating which patch
     * entries were able to be applied.
     *
     * @param text the text
     * @param results the results
     */
    open class PatchResults(text: String, results: BooleanArray) {
        /**
         * @return the results (a copy)
         */
        open val results: BooleanArray = results.copyOf()
            get() = field.copyOf()

        /**
         * @return the text
         */
        open val text: String = text
    }

    private val patches: MutableList<PatchEntry>
    private val margin: Int

    private companion object {
        // Ideally we'd like to have the @@ be merely a look-ahead, but it doesn't
        // work that way with split.
        private val patchBoundaryPattern = Regex("\n@@")
    }
}
