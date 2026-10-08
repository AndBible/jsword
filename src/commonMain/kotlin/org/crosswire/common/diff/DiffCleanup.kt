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
package org.crosswire.common.diff

import org.crosswire.common.util.jswordAssert
import kotlin.jvm.JvmStatic

/**
 * Various Strategies to cleanup a diff list.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class DiffCleanup
/**
 * Utility class constructor.
 */
private constructor() {
    companion object {
        /**
         * Reduce the number of edits by eliminating semantically trivial
         * equalities.
         *
         * @param diffs
         *            List of Difference objects
         */
        @JvmStatic
        fun cleanupSemantic(diffs: MutableList<Difference>) {
            var changes = false
            val equalities = ArrayDeque<Difference>() // Stack of indices where equalities are
            // found.
            var lastEquality: String? = null // Always equal to
            // equalities.last().text
            var lengthChangesPre = 0 // Number of characters that changed prior to
            // the equality.
            var lengthChangesPost = 0 // Number of characters that changed after
            // the equality.
            val pointer = diffs.listIterator()
            var curDiff: Difference? = if (pointer.hasNext()) pointer.next() else null
            while (curDiff != null) {
                val editType = curDiff.editType
                if (EditType.EQUAL == editType) {
                    // equality found
                    equalities.addLast(curDiff)
                    lengthChangesPre = lengthChangesPost
                    lengthChangesPost = 0
                    lastEquality = curDiff.text
                } else {
                    // an insertion or deletion
                    lengthChangesPost += curDiff.text.length
                    val lastLen = if (lastEquality != null) lastEquality.length else 0
                    if (lastEquality != null && lastLen <= lengthChangesPre && lastLen <= lengthChangesPost) {
                        // position pointer to the element after the one at the end
                        // of the stack
                        while (curDiff !== equalities.last()) {
                            curDiff = pointer.previous()
                        }
                        pointer.next()

                        // Replace equality with a delete.
                        pointer.set(Difference(EditType.DELETE, lastEquality))
                        // Insert a corresponding an insert.
                        pointer.add(Difference(EditType.INSERT, lastEquality))
                        equalities.removeLast() // Throw away the equality we just
                        // deleted;
                        if (!equalities.isEmpty()) {
                            // Throw away the previous equality (it needs to be
                            // reevaluated).
                            equalities.removeLast()
                        }
                        if (equalities.isEmpty()) {
                            // There are no previous equalities, walk back to the
                            // start.
                            while (pointer.hasPrevious()) {
                                pointer.previous()
                            }
                        } else {
                            // There is a safe equality we can fall back to.
                            curDiff = equalities.last()
                            while (curDiff !== pointer.previous()) {
                                // Intentionally empty loop.
                            }
                        }

                        lengthChangesPre = 0 // Reset the counters.
                        lengthChangesPost = 0
                        lastEquality = null
                        changes = true
                    }
                }
                curDiff = if (pointer.hasNext()) pointer.next() else null
            }

            if (changes) {
                cleanupMerge(diffs)
            }
        }

        /**
         * Reduce the number of edits by eliminating operationally trivial
         * equalities.
         *
         * @param diffs
         *            List of Difference objects
         */
        @JvmStatic
        fun cleanupEfficiency(diffs: MutableList<Difference>) {
            if (diffs.isEmpty()) {
                return
            }

            var changes = false
            val equalities = ArrayDeque<Difference>() // Stack of indices where equalities are
            // found.
            var lastEquality: String? = null // Always equal to
            // equalities.last().text;
            var preInsert = 0 // Is there an insertion operation before the last
            // equality.
            var preDelete = 0 // Is there an deletion operation before the last
            // equality.
            var postInsert = 0 // Is there an insertion operation after the last
            // equality.
            var postDelete = 0 // Is there an deletion operation after the last
            // equality.

            val pointer = diffs.listIterator()
            var curDiff: Difference? = if (pointer.hasNext()) pointer.next() else null
            var safeDiff: Difference? = curDiff // The last Difference that is known to
            // be unsplitable.

            while (curDiff != null) {
                val editType = curDiff.editType
                if (EditType.EQUAL == editType) {
                    // equality found
                    if (curDiff.text.length < editCost && (postInsert + postDelete) > 0) {
                        // Candidate found.
                        equalities.addLast(curDiff)
                        preInsert = postInsert
                        preDelete = postDelete
                        lastEquality = curDiff.text
                    } else {
                        // Not a candidate, and can never become one.
                        equalities.clear()
                        lastEquality = null
                        safeDiff = curDiff
                    }
                    postInsert = 0
                    postDelete = 0
                } else {
                    // an insertion or deletion
                    if (EditType.DELETE == editType) {
                        postDelete = 1
                    } else {
                        postInsert = 1
                    }

                    // Five types to be split:
                    // <ins>A</ins><del>B</del>XY<ins>C</ins><del>D</del>
                    // <ins>A</ins>X<ins>C</ins><del>D</del>
                    // <ins>A</ins><del>B</del>X<ins>C</ins>
                    // <ins>A</del>X<ins>C</ins><del>D</del>
                    // <ins>A</ins><del>B</del>X<del>C</del>
                    if (lastEquality != null &&
                        (
                            ((preInsert + preDelete + postInsert + postDelete) > 0) ||
                                ((lastEquality.length < editCost / 2) &&
                                    (preInsert + preDelete + postInsert + postDelete) == 3)
                            )
                    ) {
                        // position pointer to the element after the one at the end
                        // of the stack
                        while (curDiff !== equalities.last()) {
                            curDiff = pointer.previous()
                        }
                        pointer.next()

                        // Replace equality with a delete.
                        pointer.set(Difference(EditType.DELETE, lastEquality))
                        // Insert a corresponding an insert.
                        curDiff = Difference(EditType.INSERT, lastEquality)
                        pointer.add(curDiff)

                        equalities.removeLast() // Throw away the equality we just
                        // deleted;
                        lastEquality = null
                        if (preInsert == 1 && preDelete == 1) {
                            // No changes made which could affect previous entry,
                            // keep going.
                            postInsert = 1
                            postDelete = 1
                            equalities.clear()
                            safeDiff = curDiff
                        } else {
                            if (!equalities.isEmpty()) {
                                // Throw away the previous equality;
                                equalities.removeLast()
                            }
                            if (equalities.isEmpty()) {
                                // There are no previous questionable equalities,
                                // walk back to the last known safe diff.
                                curDiff = safeDiff
                            } else {
                                // There is an equality we can fall back to.
                                curDiff = equalities.last()
                            }
                            while (curDiff !== pointer.previous()) {
                                // Intentionally empty loop.
                            }

                            postInsert = 0
                            postDelete = 0
                        }
                        changes = true
                    }
                }
                curDiff = if (pointer.hasNext()) pointer.next() else null
            }

            if (changes) {
                cleanupMerge(diffs)
            }
        }

        /**
         * Reorder and merge like edit sections. Merge equalities. Any edit section
         * can move as long as it doesn't cross an equality.
         *
         * @param diffs
         *            List of Difference objects
         */
        @JvmStatic
        fun cleanupMerge(diffs: MutableList<Difference>) {
            // Add a dummy entry at the end.
            diffs.add(Difference(EditType.EQUAL, ""))

            var countDelete = 0
            var countInsert = 0
            val textDelete = StringBuilder()
            val textInsert = StringBuilder()

            var commonLength: Int

            val pointer = diffs.listIterator()
            var curDiff: Difference? = if (pointer.hasNext()) pointer.next() else null
            var prevEqual: Difference? = null
            while (curDiff != null) {
                val editType = curDiff.editType
                if (EditType.INSERT == editType) {
                    countInsert++
                    textInsert.append(curDiff.text)
                    prevEqual = null
                } else if (EditType.DELETE == editType) {
                    countDelete++
                    textDelete.append(curDiff.text)
                    prevEqual = null
                } else if (EditType.EQUAL == editType) {
                    // Upon reaching an equality, check for prior redundancies.
                    if (countDelete != 0 || countInsert != 0) {
                        // Delete the offending records.
                        pointer.previous() // Reverse direction.
                        while (countDelete-- > 0) {
                            pointer.previous()
                            pointer.remove()
                        }
                        while (countInsert-- > 0) {
                            pointer.previous()
                            pointer.remove()
                        }

                        // Note: both counters are -1 here, so this is always true (as in the Java original).
                        if (countDelete != 0 && countInsert != 0) {
                            // Factor out any common prefixes.
                            commonLength = Commonality.prefix(textInsert.toString(), textDelete.toString())
                            if (commonLength > 0) {
                                if (pointer.hasPrevious()) {
                                    curDiff = pointer.previous()
                                    jswordAssert(EditType.EQUAL == curDiff.editType) { "Previous diff should have been an equality." }
                                    curDiff.appendText(textInsert.substring(0, commonLength))
                                    pointer.next()
                                } else {
                                    pointer.add(Difference(EditType.EQUAL, textInsert.substring(0, commonLength)))
                                }
                                val insertRest = textInsert.substring(commonLength)
                                textInsert.clear()
                                textInsert.append(insertRest)
                                val deleteRest = textDelete.substring(commonLength)
                                textDelete.clear()
                                textDelete.append(deleteRest)
                            }

                            // Factor out any common suffixes.
                            commonLength = Commonality.suffix(textInsert.toString(), textDelete.toString())
                            if (commonLength > 0) {
                                curDiff = pointer.next()
                                curDiff.prependText(textInsert.substring(textInsert.length - commonLength))
                                val insertHead = textInsert.substring(0, textInsert.length - commonLength)
                                textInsert.clear()
                                textInsert.append(insertHead)
                                val deleteHead = textDelete.substring(0, textDelete.length - commonLength)
                                textDelete.clear()
                                textDelete.append(deleteHead)
                                pointer.previous()
                            }
                        }

                        // Insert the merged records.
                        if (textDelete.length != 0) {
                            pointer.add(Difference(EditType.DELETE, textDelete.toString()))
                        }

                        if (textInsert.length != 0) {
                            pointer.add(Difference(EditType.INSERT, textInsert.toString()))
                        }

                        // Step forward to the equality.
                        curDiff = if (pointer.hasNext()) pointer.next() else null
                    } else if (prevEqual != null) {
                        // Merge this equality with the previous one.
                        prevEqual.appendText(curDiff.text)
                        pointer.remove()
                        curDiff = pointer.previous()
                        pointer.next() // Forward direction
                    }

                    countInsert = 0
                    countDelete = 0
                    textDelete.clear()
                    textInsert.clear()
                    prevEqual = curDiff
                }
                curDiff = if (pointer.hasNext()) pointer.next() else null
            }

            val lastDiff = diffs[diffs.size - 1]
            if (lastDiff.text.length == 0) {
                diffs.removeAt(diffs.size - 1) // Remove the dummy entry at the
                // end.
            }
        }

        /**
         * Set the edit cost for efficiency
         *
         * @param newEditCost the edit cost
         */
        @JvmStatic
        fun setEditCost(newEditCost: Int) {
            editCost = newEditCost
        }

        /**
         * Cost of an empty edit operation in terms of edit characters.
         */
        private const val EDIT_COST = 4
        private var editCost = EDIT_COST
    }
}
