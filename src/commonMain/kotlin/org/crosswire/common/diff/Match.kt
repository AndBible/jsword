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
 * Computes the difference between two texts to create a patch. Applies the
 * patch onto another text, allowing for errors.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Match : Locator {
    constructor() : this("", "", 0)

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
    constructor(text: String, pattern: String, loc: Int) {
        this.text = text
        this.pattern = pattern
        this.loc = loc
        this.locator = Bitap(text, pattern, loc)
    }

    override fun maxPatternLength(): Int {
        return locator.maxPatternLength()
    }

    /**
     * Locate the best instance of 'pattern' in 'text' near 'loc'.
     *
     * @return Best match index or -1, if no match found
     */
    override fun locate(): Int {
        if (text == pattern) {
            // Shortcut (potentially not guaranteed by the algorithm)
            return 0
        }

        if (text.length == 0) {
            // Nothing to match.
            return -1
        }

        loc = max(0, min(loc, text.length - pattern.length))
        if (text.substring(loc, loc + pattern.length) == pattern) {
            // Perfect match at the perfect spot! (Includes case of null
            // pattern)
            return loc
        }

        return locator.locate()
    }

    /**
     * The text to search.
     */
    private val text: String

    /**
     * The pattern to find in the text.
     */
    private val pattern: String

    /**
     * The location in text to focus the search.
     */
    private var loc: Int

    /**
     * The strategy for locating a best match.
     */
    private val locator: Locator
}
