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
 * © CrossWire Bible Society, 2013 - 2016
 *
 */
package org.crosswire.jsword.book.sword.state

import org.crosswire.jsword.book.BookMetaData

/**
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
abstract class AbstractOpenFileState
/**
 * Create an AbstractOpenFileState tied to a BookMetaData.
 *
 * @param bmd the BookMetaData for this OpenFileState
 */
(bmd: BookMetaData) : OpenFileState {
    private var lastAccess: Long = System.currentTimeMillis()

    /**
     * The BookMetaData for this OpenFileState. Used to locate files.
     */
    private val bookMetaData: BookMetaData = bmd

    init {
        lastAccess = System.currentTimeMillis()
    }

    /**
     * Allows us to decide whether to release the resources or continue using them
     */
    override fun close() {
        OpenFileStateManager.instance().release(this)
    }

    /**
     * Get the BookMetaData for this OpenFileState.
     *
     * @return the BookMetaData
     */
    override fun getBookMetaData(): BookMetaData {
        return bookMetaData
    }

    /**
     * @return latest access before releasing back to the pool
     */
    override fun getLastAccess(): Long {
        return this.lastAccess
    }

    /**
     * @param lastAccess last time the file state was accessed
     */
    override fun setLastAccess(lastAccess: Long) {
        this.lastAccess = lastAccess
    }
}
