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

import java.io.File
import java.io.FileInputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList

import org.crosswire.common.util.CWProject
import org.crosswire.common.util.OSType
import org.crosswire.common.util.PropertyMap
import org.crosswire.common.util.StringUtil
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.Books
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * This represents all of the Sword Books (aka modules).
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class SwordBookPath
/**
 * Some basic name initialization
 */
private constructor() {
    /**
     * Check that the directories in the version directory really represent
     * versions.
     */
    internal class CustomFilenameFilter : FilenameFilter {
        /*
         * (non-Javadoc)
         *
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        override fun accept(parent: File?, name: String?): Boolean {
            return !name!!.startsWith(PREFIX_GLOBALS) && name.endsWith(SwordConstants.EXTENSION_CONF)
        }
    }

    companion object {
        /**
         * Establish additional locations that Sword may hold books.
         *
         * @param theNewDirs
         *            The new Sword directories
         * @throws BookException
         */
        @JvmStatic
        @Throws(BookException::class)
        fun setAugmentPath(theNewDirs: Array<File>?) {
            val newDirs = theNewDirs
            if (newDirs == null) {
                return
            }

            augmentPath = newDirs.clone()

            // Now we need to (re)register ourselves
            Books.installed().registerDriver(SwordBookDriver.instance())
        }

        /**
         * Retrieve the additional locations that Sword may hold Books.
         *
         * @return The new Sword directory
         */
        @JvmStatic
        fun getAugmentPath(): Array<File> {
            return augmentPath.clone()
        }

        /**
         * Obtain a prioritized path of Book locations. This contains the download
         * dir as the first location, the user's augment path and finally all the
         * discovered standard locations.
         *
         * @return the array of Book locations.
         */
        @JvmStatic
        fun getSwordPath(): Array<File> {
            val swordPath = ArrayList<File>()

            // The first place to look for Books
            swordPath.add(getSwordDownloadDir())

            // Then all the user's augments
            for (i in augmentPath.indices) {
                val path = augmentPath[i]
                if (!swordPath.contains(path)) {
                    swordPath.add(path)
                }
            }

            val defaultPath = getDefaultPaths()
            // Then all the user's bookDirs
            for (i in defaultPath.indices) {
                val path = defaultPath[i]
                if (!swordPath.contains(path)) {
                    swordPath.add(path)
                }
            }

            return swordPath.toTypedArray()
        }

        /**
         * Get a list of books in a given location.
         *
         * @param bookDir
         *            the directory in which to look
         * @return the list of books in that location
         */
        @JvmStatic
        fun getBookList(bookDir: File): Array<String>? {
            return bookDir.list(CustomFilenameFilter())
        }

        /**
         * Search all of the "standard" Sword locations for Books. Remember all the
         * locations.
         */
        private fun getDefaultPaths(): Array<File> {
            // If possible migrate the old location to the new one
            migrateBookDir()

            val bookDirs: MutableList<File> = ArrayList()

            val home = System.getProperty(PROPERTY_USER_HOME)

            // Is sword.conf in the current directory?
            readSwordConf(bookDirs, ".")

            // mods.d in the current directory?
            testDefaultPath(bookDirs, ".")

            // how about in the library, just next door?
            testDefaultPath(bookDirs, ".." + File.separator + DIR_SWORD_LIBRARY)

            // if there is a property set for the sword home directory
            // The Sword project defines SWORD_HOME, but JSword expects this to be
            // transformed into sword.home.
            val swordhome = System.getProperty(PROPERTY_SWORD_HOME)
            if (swordhome != null) {
                testDefaultPath(bookDirs, swordhome)

                // how about in the library, just next door?
                testDefaultPath(bookDirs, swordhome + File.separator + ".." + File.separator + DIR_SWORD_LIBRARY)
            }

            if (System.getProperty("os.name").startsWith("Windows")) {
                testDefaultPath(bookDirs, DIR_WINDOWS_DEFAULT)
                // how about in the library, just next door?
                testDefaultPath(bookDirs, DIR_WINDOWS_DEFAULT + File.separator + ".." + File.separator + DIR_SWORD_LIBRARY)
            }

            // .sword in the users home directory?
            readSwordConf(bookDirs, home + File.separator + DIR_SWORD_CONF)

            // Check for sword.conf in the usual places
            val sysconfigPaths = StringUtil.split(DIR_SWORD_GLOBAL_CONF, ':')
            for (i in sysconfigPaths.indices) {
                readSwordConf(bookDirs, sysconfigPaths[i])
            }

            val userDataArea = OSType.getOSType().getUserAreaFolder(DIR_SWORD_CONF, DIR_SWORD_CONF_ALT)

            // Check look for mods.d in the sword user data area
            testDefaultPath(bookDirs, File(userDataArea!!.path))

            // JSword used to hold books in ~/.jsword (or its equivalent) but has
            // code that will
            // migrate it to ~/.sword (or its equivalent)
            // If the migration did not work then use the old area
            testDefaultPath(bookDirs, File(CWProject.instance().writableProjectDir!!.path))

            return bookDirs.toTypedArray()
        }

        private fun readSwordConf(bookDirs: MutableList<File>, swordConfDir: File) {
            val sysconfig = File(swordConfDir, SWORD_GLOBAL_CONF)
            if (sysconfig.canRead()) {
                var `is`: InputStream? = null
                try {
                    val prop = PropertyMap()
                    `is` = FileInputStream(sysconfig)
                    prop.load(`is`)
                    var datapath = prop.get(DATA_PATH)
                    testDefaultPath(bookDirs, datapath)
                    datapath = prop.get(AUGMENT_PATH)
                    testDefaultPath(bookDirs, datapath)
                } catch (ex: IOException) {
                    log.warn("Failed to read system config file", ex)
                } finally {
                    if (`is` != null) {
                        try {
                            `is`.close()
                        } catch (e: IOException) {
                            log.warn("Failed to close system config file", e)
                        }
                    }
                }
            }
        }

        private fun readSwordConf(bookDirs: MutableList<File>, swordConfDir: String) {
            readSwordConf(bookDirs, File(swordConfDir))
        }

        /**
         * Check to see if the given directory is a Sword mods.d directory and then
         * add it to the list if it is.
         *
         * @param bookDirs
         *            The list to add good paths
         * @param path
         *            the path to check
         */
        private fun testDefaultPath(bookDirs: MutableList<File>, path: File?) {
            if (path == null) {
                return
            }

            val mods = File(path, SwordConstants.DIR_CONF)
            if (mods.isDirectory && mods.canRead()) {
                bookDirs.add(path)
            }
        }

        /**
         * Check to see if the given directory is a Sword mods.d directory and then
         * add it to the list if it is.
         *
         * @param bookDirs
         *            The list to add good paths
         * @param path
         *            the path to check
         */
        private fun testDefaultPath(bookDirs: MutableList<File>, path: String?) {
            if (path == null) {
                return
            }

            testDefaultPath(bookDirs, File(path))
        }

        private fun getDefaultDownloadPath(): File {
            var path: File? = null
            val possiblePaths = getDefaultPaths()

            for (i in possiblePaths.indices) {
                val mods = File(possiblePaths[i], SwordConstants.DIR_CONF)
                if (mods.canWrite()) {
                    path = possiblePaths[i]
                    break
                }
            }

            // If it is not found on the path then it doesn't exist yet and needs to
            // be established
            if (path == null) {
                val userDataArea = OSType.getOSType().getUserAreaFolder(DIR_SWORD_CONF, DIR_SWORD_CONF_ALT)
                path = File(userDataArea!!.path)
            }

            return path
        }

        private fun migrateBookDir() {
            // Books should be on this path
            val userDataArea = OSType.getOSType().getUserAreaFolder(DIR_SWORD_CONF, DIR_SWORD_CONF_ALT)

            val swordBookPath = File(userDataArea!!.path)

            // The "old" Book location might be in one of two locations
            // It might be ~/.jsword or the new project dir
            var oldPath = File(CWProject.instance().deprecatedWritableProjectDir!!.path)

            if (oldPath.isDirectory) {
                migrateBookDir(oldPath, swordBookPath)
                return
            }

            // now trying the new project dir
            oldPath = File(CWProject.instance().writableProjectDir!!.path)

            if (oldPath.isDirectory) {
                migrateBookDir(oldPath, swordBookPath)
                return
            }

            // Finally, it might be ~/.sword
            oldPath = File(OSType.DEFAULT.getUserAreaFolder(DIR_SWORD_CONF, DIR_SWORD_CONF_ALT)!!.path)
            if (oldPath.isDirectory) {
                migrateBookDir(oldPath, swordBookPath)
            }
        }

        private fun migrateBookDir(oldPath: File, newPath: File) {
            // move the modules and confs
            val oldDataDir = File(oldPath, SwordConstants.DIR_DATA)
            val newDataDir = File(newPath, SwordConstants.DIR_DATA)
            val oldConfDir = File(oldPath, SwordConstants.DIR_CONF)
            val newConfDir = File(newPath, SwordConstants.DIR_CONF)

            // move the modules
            if (!migrate(oldDataDir, newDataDir)) {
                return
            }

            // move the confs
            if (!migrate(oldConfDir, newConfDir)) {
                // oops, restore the modules
                migrate(newDataDir, oldDataDir)
            }
        }

        private fun migrate(oldPath: File, newPath: File): Boolean {
            if (oldPath == newPath || !oldPath.exists()) {
                return true
            }

            // make sure the parent exists
            val parent = newPath.parentFile
            if (!parent.exists() && !parent.mkdirs()) {
                return false
            }

            return oldPath.renameTo(newPath)
        }

        /**
         * Get the download directory, which is either the one that the user chose
         * or that JSword picked for the user.
         *
         * @return Returns the download directory.
         */
        @JvmStatic
        fun getSwordDownloadDir(): File {
            if (overrideDownloadDir != null) {
                return overrideDownloadDir!!
            }
            return defaultDownloadDir
        }

        /**
         * @return Returns the download directory that the user chose.
         */
        @JvmStatic
        fun getDownloadDir(): File? {
            return overrideDownloadDir
        }

        /**
         * @param dlDir
         *            The download directory that the user specifies.
         */
        @JvmStatic
        fun setDownloadDir(dlDir: File) {
            if ("" != dlDir.path) {
                overrideDownloadDir = dlDir
                log.debug("Setting sword download directory to: {}", dlDir)
            }
        }

        /**
         * Default windows installation directory
         */
        private const val DIR_WINDOWS_DEFAULT = "C:\\Program Files\\CrossWire\\The SWORD Project"

        /**
         * Library may be a sibling of DIR_WINDOWS_DEFAULT or SWORD_HOME or CWD
         */
        private const val DIR_SWORD_LIBRARY = "library"

        /**
         * Users config directory for Sword in Unix
         */
        private const val DIR_SWORD_CONF = ".sword"

        /**
         * Users config directory for Sword in Unix
         */
        private const val DIR_SWORD_CONF_ALT = "Sword"

        /**
         * Sword global config file
         */
        private const val SWORD_GLOBAL_CONF = "sword.conf"

        /**
         * Sword global config file locations
         */
        private const val DIR_SWORD_GLOBAL_CONF = "/etc:/usr/local/etc"

        /**
         * Sword global config file's path to where mods can be found
         */
        private const val DATA_PATH = "DataPath"

        /**
         * Sword global config file's path to where mods can be found
         */
        private const val AUGMENT_PATH = "AugmentPath"

        /**
         * System property for sword home directory
         */
        private const val PROPERTY_SWORD_HOME = "sword.home"

        /**
         * Java system property for users home directory
         */
        private const val PROPERTY_USER_HOME = "user.home"

        /**
         * File prefix for config file
         */
        private const val PREFIX_GLOBALS = "globals."

        /**
         * The directory URL
         */
        private var augmentPath: Array<File> = arrayOf()

        /**
         * The directory URL
         */
        private var defaultDownloadDir: File = getDefaultDownloadPath()

        /**
         * The directory URL
         */
        private var overrideDownloadDir: File? = null

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(SwordBookPath::class)
    }
}
