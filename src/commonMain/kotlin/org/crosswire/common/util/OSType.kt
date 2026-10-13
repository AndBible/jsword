/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * <p>
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
package org.crosswire.common.util

import okio.Path.Companion.toPath

/**
 * Types of Operating Systems for which specialized behavior is needed.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
enum class OSType
/**
 * Simple ctor
 *
 * @param name the name of the OS
 */
(private val osName: String) {
    MAC("Mac") {
        override val userArea: KUri?
            get() = NetUtil.lengthenURI(getUserHome(), MAC_USER_DATA_AREA)

        override fun getUserAreaFolder(hiddenFolderName: String?, visibleFolderName: String?): KUri? {
            return NetUtil.lengthenURI(userArea!!, visibleFolderName!!)
        }
    },

    WIN32("Win") {
        override val userArea: KUri?
            get() {
                if ("Windows 10".equals(currentOsName, ignoreCase = true)) {
                    return NetUtil.lengthenURI(getUserHome(), WIN32_V10_USER_DATA_AREA)
                }
                return NetUtil.lengthenURI(getUserHome(), WIN32_USER_DATA_AREA)
            }

        override fun getUserAreaFolder(hiddenFolderName: String?, visibleFolderName: String?): KUri? {
            return NetUtil.lengthenURI(userArea!!, visibleFolderName!!)
        }
    },

    DEFAULT("*nix") {
        override val userArea: KUri?
            get() = getUserHome()

        override fun getUserAreaFolder(hiddenFolderName: String?, visibleFolderName: String?): KUri? {
            return NetUtil.lengthenURI(userArea!!, hiddenFolderName!!)
        }
    };

    /**
     * Get the user area for this OSType.
     *
     * @return the user area
     */
    abstract val userArea: KUri?

    /**
     * A folder in the user area. This osType will determine which to use in
     * constructing the URI to the folder.
     *
     * @param hiddenFolderName
     *            is typically a "unix" hidden folder name such as .jsword.
     * @param visibleFolderName
     *            is an visible folder name, such as JSword.
     *
     * @return the user area folder
     */
    abstract fun getUserAreaFolder(hiddenFolderName: String?, visibleFolderName: String?): KUri?

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    override fun toString(): String {
        return osName
    }

    companion object {
        /**
         * @return the URI of the user home
         */
        @JvmStatic
        fun getUserHome(): KUri {
            return pathUri(systemProperty("user.home")!!.toPath())
        }

        /**
         * Get the machine's OSType.
         *
         * @return the machine's OSType
         */
        @JvmStatic
        fun getOSType(): OSType {
            return osType
        }

        /**
         * Lookup method to convert from a String to an OSType
         *
         * @param name the name of an OSType
         * @return the OSType or DEFAULT
         */
        @JvmStatic
        fun fromString(name: String): OSType {
            for (v in values()) {
                if (name.startsWith(v.osName)) {
                    return v
                }
            }

            return DEFAULT
        }

        /**
         * The Windows user settings parent directory
         */
        private const val WIN32_USER_DATA_AREA = "Application Data"
        private const val WIN32_V10_USER_DATA_AREA = "AppData/Roaming"

        /**
         * The Mac user settings parent directory
         */
        private const val MAC_USER_DATA_AREA = "Library/Application Support"

        /**
         * The machine's osType
         */
        internal var currentOsName: String? = platformOsName()

        private var osType: OSType = fromString(currentOsName!!)
    }
}
