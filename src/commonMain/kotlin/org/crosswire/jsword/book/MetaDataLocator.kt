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
 * © CrossWire Bible Society, 2015 - 2016
 */
package org.crosswire.jsword.book

import okio.Path
import org.crosswire.common.util.KUri
import org.crosswire.jsword.book.sword.StorageLocation
import org.crosswire.jsword.book.sword.storageUriLocation
import org.crosswire.jsword.book.sword.storageMkdirs

import org.crosswire.common.util.CWProject

/**
 * A MetaDataLocator allows one to define where BookMetaData for a Book may be found.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
enum class MetaDataLocator {
    TRANSIENT {
        override val readLocationValue: StorageLocation?
            get() = null

        override val writeLocationValue: StorageLocation?
            get() = null
    },
    JSWORD {
        override val readLocationValue: StorageLocation?
            get() {
                val dirs = CWProject.instance().projectResourceDirs
                if (dirs.size > 1) {
                    return getLocation(dirs[1])
                }
                return null
            }

        override val writeLocationValue: StorageLocation?
            get() {
                val dirs = CWProject.instance().projectResourceDirs
                if (dirs.size > 0) {
                    return getLocation(dirs[0])
                }
                return null
            }
    },
    FRONTEND {
        override val readLocationValue: StorageLocation?
            get() = getLocation(CWProject.instance().readableFrontendProjectDir)

        override val writeLocationValue: StorageLocation?
            get() = getLocation(CWProject.instance().writableFrontendProjectDir)
    };

    val readLocation: Path? get() = readLocationValue?.path
    val writeLocation: Path? get() = writeLocationValue?.path
    internal abstract val readLocationValue: StorageLocation?
    internal abstract val writeLocationValue: StorageLocation?

    companion object {
        /**
         * Safely creates the file location, or null if the parent can't exist
         *
         * @param u the parent URI
         * @return the file representing the config
         */
        @JvmStatic
        protected fun getPath(u: KUri?): Path? = getLocation(u)?.path

        private fun getLocation(u: KUri?): StorageLocation? {
            if (u == null) {
                return null
            }

            val parent = storageUriLocation(u)
            val override = parent / DIR_CONF_OVERRIDE
            storageMkdirs(override)
            return override
        }

        /**
         * The configuration directory for overrides
         */
        private const val DIR_CONF_OVERRIDE = "jsword-mods.d"
    }
}
