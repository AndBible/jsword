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

import org.crosswire.common.util.parseReferenceInt

import org.crosswire.common.util.StringUtil
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.Versification

/**
 * The Osis ID parser simply assumes 1-3 parts divided by '.'.
 * Any deviation from the dot-delimited formatted yields nulls.
 *
 * OSIS Refs should be separated by a '-' if there are 2 refs signifying a range e.g. Gen.1-Gen.3.
 *
 * The current implementation doesn't support an OSIS ID or OSIS ref with a missing chapter,
 * as are currently returned by the getOsisRef() calls occasionally.
 *
 * Algorithm:
 * If ony 1 ID passed in then create ending id from it to enable a single flow through algorithm.
 * Missing chapter or verse of starting id will be set to 0/1.
 * Missing chapter or verse of ending id will be set to last chapter/verse.
 *
 * @author Chris Burrell, mjdenham
 */
class OsisParser {
    /**
     * String OSIS Ref parser, assumes a - separating two osis IDs
     * @param v11n the v11n
     * @param osisRef the ref
     * @return the equivalent verse range
     */
    fun parseOsisRef(v11n: Versification?, osisRef: String?): VerseRange? {
        val osisIDs = StringUtil.splitAll(osisRef, VerseRange.RANGE_OSIS_DELIM)
        // Too many or few parts?
        if (osisIDs.size < 1 || osisIDs.size > 2) {
            return null
        }

        // Parts must have content.
        if (osisIDs[0].length == 0 || (osisIDs.size == 2 && osisIDs[1].length == 0)) {
            return null
        }

        // Ensure ending OSIS id exists to simplify future logic - yes it is okay to use the start id as the end id here
        val startOsisID = osisIDs[0]
        val endOsisID: String
        if (osisIDs.size == 1) {
            endOsisID = startOsisID
        } else {
            endOsisID = osisIDs[1]
        }

        // ensure no empty parts in osis id1 and not too many parts
        val startOsisIDParts = splitOsisId(startOsisID)
        if (isAnEmptyPart(startOsisIDParts) || startOsisIDParts.size > 3) {
            return null
        }

        // manipulate first osis id to 3 parts, padding with first chapter or verse
        while (startOsisIDParts.size < 3) {
            startOsisIDParts.add(START_CHAPTER_OR_VERSE)
        }
        // now we have a full 3 part start OSIS id

        // Now let us manufacture a 3 part end OSIS id

        // First check no empty parts were passed in for osis id 2
        val endOsisIDParts = splitOsisId(endOsisID)
        if (isAnEmptyPart(endOsisIDParts)) {
            return null
        }

        // Add end chapter/verse if missing
        val endOsisIDPartCount = endOsisIDParts.size
        if (endOsisIDPartCount < 3) {
            // need to calculate chapter and verse for osis id 2

            // there will always be a book
            val bookName = endOsisIDParts[0]
            val book = BibleBook.fromExactOSIS(bookName)

            // can asssume last chapter if unspecified because this is the trailing osis Id
            val chapter: Int
            if (endOsisIDPartCount == 1) {
                chapter = v11n!!.getLastChapter(book)
                endOsisIDParts.add(chapter.toString())
            } else {
                chapter = parseReferenceInt(endOsisIDParts[1])
            }

            // can asssume last verse if unspecified because this is the trailing osis Id
            val verse: Int
            if (endOsisIDPartCount < 3) {
                verse = v11n!!.getLastVerse(book, chapter)
                endOsisIDParts.add(verse.toString())
            }
        }

        // Now there is exactly 1 beginning and 1 ending 3-part verse only beyond this point

        val start = parseOsisID(v11n, startOsisIDParts)
        if (start == null) {
            return null
        }

        val end = parseOsisID(v11n, endOsisIDParts)
        if (end == null) {
            return null
        }

        return VerseRange(v11n, start, end)
    }

    /**
     * Strict OSIS ID parsers, case-sensitive
     * @param v11n the versification to use when constructing the verse
     * @param osisID the ID we want to parse
     * @return the verse that matches the OSIS ID
     */
    fun parseOsisID(v11n: Versification?, osisID: String?): Verse? {
        if (osisID == null) {
            return null
        }

        val osisIDParts = splitOsisId(osisID)

        if (osisIDParts.size != 3 || isAnEmptyPart(osisIDParts)) {
            return null
        }

        return parseOsisID(v11n, osisIDParts)
    }

    private fun parseOsisID(v11n: Versification?, osisIDParts: List<String>): Verse? {

        val b = BibleBook.fromExactOSIS(osisIDParts[0])
        if (b == null) {
            return null
        }

        // Allow a Verse to have a sub identifier on the last part.
        // We should use it, but throwing it away for now.
        val endParts = StringUtil.splitAll(osisIDParts[2], Verse.VERSE_OSIS_SUB_PREFIX)
        var subIdentifier: String? = null
        if (endParts.size == 2 && endParts[1].length > 0) {
            subIdentifier = endParts[1]
        }
        return Verse(v11n, b, parseReferenceInt(osisIDParts[1]), parseReferenceInt(endParts[0]), subIdentifier)
    }

    /**
     * Split string like 'Gen.1.1' into a 3 element list
     */
    private fun splitOsisId(osisID1: String): MutableList<String> {
        val partArray = StringUtil.splitAll(osisID1, Verse.VERSE_OSIS_DELIM)

        // add to an appropriately sized editable list
        val list = ArrayList<String>(3)
        list.addAll(partArray)
        return list
    }

    private companion object {
        // This could be 1 or 0 but for now I have used 1
        private const val START_CHAPTER_OR_VERSE = "1"

        /**
         * Check no part of the Osis ref is empty
         */
        private fun isAnEmptyPart(parts: List<String>): Boolean {
            for (part in parts) {
                if (part.length == 0) {
                    return true
                }
            }
            return false
        }
    }
}
