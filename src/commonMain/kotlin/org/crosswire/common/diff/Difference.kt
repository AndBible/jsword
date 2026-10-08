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
 *
 * Represents a single difference, consisting of an EditType and associated
 * text.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Difference(edit: EditType, text: String) {
    /**
     * The edit to perform; the EditType.
     */
    open var editType: EditType = edit

    /**
     * The text.
     */
    open var text: String = text

    /**
     * The index.
     */
    open var index: Int = 0

    /**
     * @param addText
     *            the text to set
     */
    open fun appendText(addText: String) {
        text += addText
    }

    /**
     * @param addText
     *            the text to set
     */
    open fun appendText(addText: Char) {
        text += addText
    }

    /**
     * @param addText
     *            the text to set
     */
    open fun prependText(addText: String) {
        text = addText + text
    }

    /**
     * @param addText
     *            the text to set
     */
    open fun prependText(addText: Char) {
        text = addText + text
    }

    override fun toString(): String = editType.toString() + ':' + text

    override fun hashCode(): Int = 31 * editType.hashCode() + text.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || this::class != other::class) {
            return false
        }

        other as Difference

        return editType == other.editType && text == other.text
    }
}
