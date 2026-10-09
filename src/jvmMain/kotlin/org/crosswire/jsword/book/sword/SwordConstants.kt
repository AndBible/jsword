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
package org.crosswire.jsword.book.sword

/**
 * A Constants to help the SwordBookDriver to read Sword format data.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Mark Goodwin
 * @author Joe Walker
 * @author The SWORD Project (don't know who - no credits in original files (canon.h))
 */
class SwordConstants private constructor() {
    // Prevent instantiation

    companion object {
        /**
         * New testament data files
         */
        const val FILE_NT: String = "nt"

        /**
         * Old testament data files
         */
        const val FILE_OT: String = "ot"

        /**
         * Index file extensions
         */
        const val EXTENSION_VSS: String = ".vss"

        /**
         * Extension for index files
         */
        const val EXTENSION_INDEX: String = ".idx"

        /**
         * Extension for data files
         */
        const val EXTENSION_DATA: String = ".dat"

        /**
         * Extension for config files
         */
        const val EXTENSION_CONF: String = ".conf"

        /**
         * The data directory
         */
        const val DIR_DATA: String = "modules"

        /**
         * The configuration directory
         */
        const val DIR_CONF: String = "mods.d"

        /**
         * The configuration directory with a trailing /
         */
        const val PATH_CONF: String = "mods.d/"
    }
}
