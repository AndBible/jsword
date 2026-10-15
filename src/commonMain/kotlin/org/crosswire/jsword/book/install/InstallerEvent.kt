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
package org.crosswire.jsword.book.install

/**
 * An InstallerEvent is fired whenever an Installer is added or removed from the
 * system.
 * 
 * Plain class instead of `java.util.EventObject` (K5b rule 26); `getSource()` is kept.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class InstallerEvent
/**
 * Basic constructor
 * 
 * @param source the originator of the event
 * @param installer
 *            The installer, or null if there is more than one change.
 * @param added
 *            True if the changed installer is an addition.
 */
(source: Any?, installer: Installer?, private val added: Boolean) {
    private val source: Any = requireNotNull(source) { "null source" }

    /**
     * The object on which the event initially occurred (was `EventObject.getSource`, K5b rule 26).
     */
    open fun getSource(): Any = source

    /**
     * The changed installer
     */
    private val changedInstaller: Installer? = installer

    /**
     * Get the changed installer
     * 
     * @return The installer
     */
    open val installer: Installer?
        get() = changedInstaller

    /**
     * Is this an addition event?
     * @return whether this is an addition event
     */
    open val isAddition: Boolean
        get() = added
}
