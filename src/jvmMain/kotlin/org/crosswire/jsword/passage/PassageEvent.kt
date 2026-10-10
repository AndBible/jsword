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


/**
 * Defines an event that encapsulates changes to a Passage. For many operations
 * on a Passage, calculating the extent of the changes is hard. In these cases
 * we default the range to the whole Bible.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class PassageEvent
/**
 * Constructs a PassageEvent object.
 *
 * @param source
 *            the source Object (typically `this`)
 * @param versesChanged
 *            an int specifying VERSES_CHANGED, VERSES_ADDED, VERSES_REMOVED
 * @param lower
 *            an int specifying the bottom of a range
 * @param upper
 *            an int specifying the top of a range
 */
(private val source: Any, versesChanged: EventType?, lower: Verse?, upper: Verse?) {

    open fun getSource(): Any = source

    /**
     * Indicates what kind of change happened to a Passage.
     */
    enum class EventType {
        /**
         * Identifies one or more changes in the lists contents.
         */
        CHANGED,

        /**
         * Identifies the addition of one or more contiguous items to the list
         */
        ADDED,

        /**
         * Identifies the removal of one or more contiguous items from the list
         */
        REMOVED,
    }

    /**
     * The type of change
     */
    private var type: EventType? = versesChanged

    /**
     * The lowest numbered element to have changed
     */
    private var lower: Verse? = lower

    /**
     * The highest numbered element to have changed
     */
    private var upper: Verse? = upper

    /**
     * Returns the event type. The possible values are:
     *
     *  * VERSES_CHANGED
     *  * VERSES_ADDED
     *  * VERSES_REMOVED
     *
     * @return an int representing the type value
     */
    open fun getType(): EventType? {
        return type
    }

    /**
     * Returns the lower index of the range. For a single element, this value is
     * the same as that returned by [getUpperIndex].
     *
     * @return an int representing the lower index value
     */
    open fun getLowerIndex(): Verse? {
        return lower
    }

    /**
     * Returns the upper index of the range. For a single element, this value is
     * the same as that returned by [getLowerIndex].
     *
     * @return an int representing the upper index value
     */
    open fun getUpperIndex(): Verse? {
        return upper
    }

}
