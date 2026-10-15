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

import kotlin.jvm.JvmStatic

import okio.Path
import okio.Path.Companion.toPath
import okio.IOException
import org.crosswire.common.util.systemProperty
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
    companion object {
        @JvmStatic @Throws(BookException::class)
        fun setAugmentPath(dirs: Array<Path>?) = setAugmentLocations(dirs?.map { StorageLocation(it.toString()) }?.toTypedArray())
        @JvmStatic fun getAugmentPath(): Array<Path> = getAugmentLocations().map { it.path }.toTypedArray()
        @JvmStatic fun getSwordPath(): Array<Path> = getSwordLocations().map { it.path }.toTypedArray()
        @JvmStatic fun getBookList(dir: Path): Array<String>? = getBookNames(StorageLocation(dir.toString()))
        @JvmStatic fun getSwordDownloadDir(): Path = getSwordDownloadLocation().path
        @JvmStatic fun getDownloadDir(): Path? = getDownloadLocation()?.path
        @JvmStatic fun setDownloadDir(dir: Path) = setDownloadLocation(StorageLocation(dir.toString()))

        /**
         * Establish additional locations that Sword may hold books.
         *
         * @param theNewDirs
         *            The new Sword directories
         * @throws BookException
         */
        @JvmStatic
        @Throws(BookException::class)
        internal fun setAugmentLocations(theNewDirs: Array<StorageLocation>?) {
            val newDirs = theNewDirs
            if (newDirs == null) {
                return
            }

            augmentPath = newDirs.copyOf()

            // Now we need to (re)register ourselves
            Books.installed().registerDriver(SwordBookDriver.instance())
        }

        /**
         * Retrieve the additional locations that Sword may hold Books.
         *
         * @return The new Sword directory
         */
        @JvmStatic
        internal fun getAugmentLocations(): Array<StorageLocation> {
            return augmentPath.copyOf()
        }

        /**
         * Obtain a prioritized path of Book locations. This contains the download
         * dir as the first location, the user's augment path and finally all the
         * discovered standard locations.
         *
         * @return the array of Book locations.
         */
        @JvmStatic
        internal fun getSwordLocations(): Array<StorageLocation> {
            val swordPath = mutableListOf<StorageLocation>()

            // The first place to look for Books
            swordPath.add(getSwordDownloadLocation())

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
        internal fun getBookNames(bookDir: StorageLocation): Array<String>? {
            return storageListNames(bookDir)?.filter { !it.startsWith(PREFIX_GLOBALS) && it.endsWith(SwordConstants.EXTENSION_CONF) }?.toTypedArray()
        }

        /**
         * Search all of the "standard" Sword locations for Books. Remember all the
         * locations.
         */
        private fun getDefaultPaths(): Array<StorageLocation> {
            // If possible migrate the old location to the new one
            migrateBookDir()

            val bookDirs: MutableList<StorageLocation> = mutableListOf()

            val home = systemProperty(PROPERTY_USER_HOME)

            // Is sword.conf in the current directory?
            readSwordConf(bookDirs, ".")

            // mods.d in the current directory?
            testDefaultPath(bookDirs, ".")

            // how about in the library, just next door?
            testDefaultPath(bookDirs, ".." + storageSeparator() + DIR_SWORD_LIBRARY)

            // if there is a property set for the sword home directory
            // The Sword project defines SWORD_HOME, but JSword expects this to be
            // transformed into sword.home.
            val swordhome = systemProperty(PROPERTY_SWORD_HOME)
            if (swordhome != null) {
                testDefaultPath(bookDirs, swordhome)

                // how about in the library, just next door?
                testDefaultPath(bookDirs, swordhome + storageSeparator() + ".." + storageSeparator() + DIR_SWORD_LIBRARY)
            }

            if (systemProperty("os.name")!!.startsWith("Windows")) {
                testDefaultPath(bookDirs, DIR_WINDOWS_DEFAULT)
                // how about in the library, just next door?
                testDefaultPath(bookDirs, DIR_WINDOWS_DEFAULT + storageSeparator() + ".." + storageSeparator() + DIR_SWORD_LIBRARY)
            }

            // .sword in the users home directory?
            readSwordConf(bookDirs, home + storageSeparator() + DIR_SWORD_CONF)

            // Check for sword.conf in the usual places
            val sysconfigPaths = StringUtil.split(DIR_SWORD_GLOBAL_CONF, ':')
            for (i in sysconfigPaths.indices) {
                readSwordConf(bookDirs, sysconfigPaths[i])
            }

            val userDataArea = OSType.getOSType().getUserAreaFolder(DIR_SWORD_CONF, DIR_SWORD_CONF_ALT)

            // Check look for mods.d in the sword user data area
            testDefaultPath(bookDirs, userDataArea!!.getPath()!!)

            // JSword used to hold books in ~/.jsword (or its equivalent) but has
            // code that will
            // migrate it to ~/.sword (or its equivalent)
            // If the migration did not work then use the old area
            testDefaultPath(bookDirs, CWProject.instance().writableProjectDir!!.getPath()!!)

            return bookDirs.toTypedArray()
        }

        private fun readSwordConf(bookDirs: MutableList<StorageLocation>, swordConfDir: StorageLocation) {
            val sysconfig = swordConfDir / SWORD_GLOBAL_CONF
            if (storageCanRead(sysconfig)) {
                try {
                    val prop = PropertyMap()
                    prop.load(storageRead(sysconfig))
                    testDefaultPath(bookDirs, prop.get(DATA_PATH))
                    testDefaultPath(bookDirs, prop.get(AUGMENT_PATH))
                } catch (ex: IOException) {
                    log.warn("Failed to read system config file", ex)
                }
            }
        }

        private fun readSwordConf(bookDirs: MutableList<StorageLocation>, swordConfDir: String) {
            readSwordConf(bookDirs, StorageLocation(swordConfDir))
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
        private fun testDefaultPath(bookDirs: MutableList<StorageLocation>, path: StorageLocation?) {
            if (path == null) {
                return
            }

            val mods = path / SwordConstants.DIR_CONF
            if (storageDirectory(mods) && storageCanRead(mods)) {
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
        private fun testDefaultPath(bookDirs: MutableList<StorageLocation>, path: String?) {
            if (path == null) {
                return
            }

            testDefaultPath(bookDirs, StorageLocation(path))
        }

        private fun getDefaultDownloadPath(): StorageLocation {
            var path: StorageLocation? = null
            val possiblePaths = getDefaultPaths()

            for (i in possiblePaths.indices) {
                val mods = possiblePaths[i] / SwordConstants.DIR_CONF
                if (storageCanWrite(mods)) {
                    path = possiblePaths[i]
                    break
                }
            }

            // If it is not found on the path then it doesn't exist yet and needs to
            // be established
            if (path == null) {
                val userDataArea = OSType.getOSType().getUserAreaFolder(DIR_SWORD_CONF, DIR_SWORD_CONF_ALT)
                path = StorageLocation(userDataArea!!.getPath()!!)
            }

            return path
        }

        private fun migrateBookDir() {
            // Books should be on this path
            val userDataArea = OSType.getOSType().getUserAreaFolder(DIR_SWORD_CONF, DIR_SWORD_CONF_ALT)

            val swordBookPath = StorageLocation(userDataArea!!.getPath()!!)

            // The "old" Book location might be in one of two locations
            // It might be ~/.jsword or the new project dir
            var oldPath = StorageLocation(CWProject.instance().deprecatedWritableProjectDir!!.getPath()!!)

            if (storageDirectory(oldPath)) {
                migrateBookDir(oldPath, swordBookPath)
                return
            }

            // now trying the new project dir
            oldPath = StorageLocation(CWProject.instance().writableProjectDir!!.getPath()!!)

            if (storageDirectory(oldPath)) {
                migrateBookDir(oldPath, swordBookPath)
                return
            }

            // Finally, it might be ~/.sword
            oldPath = StorageLocation(OSType.DEFAULT.getUserAreaFolder(DIR_SWORD_CONF, DIR_SWORD_CONF_ALT)!!.getPath()!!)
            if (storageDirectory(oldPath)) {
                migrateBookDir(oldPath, swordBookPath)
            }
        }

        private fun migrateBookDir(oldPath: StorageLocation, newPath: StorageLocation) {
            // move the modules and confs
            val oldDataDir = oldPath / SwordConstants.DIR_DATA
            val newDataDir = newPath / SwordConstants.DIR_DATA
            val oldConfDir = oldPath / SwordConstants.DIR_CONF
            val newConfDir = newPath / SwordConstants.DIR_CONF

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

        private fun migrate(oldPath: StorageLocation, newPath: StorageLocation): Boolean {
            if (oldPath == newPath || !storageExists(oldPath)) {
                return true
            }

            // make sure the parent exists
            val parent = newPath.parent!!
            if (!storageExists(parent) && !storageMkdirs(parent)) {
                return false
            }

            return storageMove(oldPath, newPath)
        }

        /**
         * Get the download directory, which is either the one that the user chose
         * or that JSword picked for the user.
         *
         * @return Returns the download directory.
         */
        @JvmStatic
        internal fun getSwordDownloadLocation(): StorageLocation {
            if (overrideDownloadDir != null) {
                return overrideDownloadDir!!
            }
            return defaultDownloadDir
        }

        /**
         * @return Returns the download directory that the user chose.
         */
        @JvmStatic
        internal fun getDownloadLocation(): StorageLocation? {
            return overrideDownloadDir
        }

        /**
         * @param dlDir
         *            The download directory that the user specifies.
         */
        @JvmStatic
        internal fun setDownloadLocation(dlDir: StorageLocation) {
            if ("" != dlDir.toString()) {
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
         * Path prefix for config file
         */
        private const val PREFIX_GLOBALS = "globals."

        /**
         * The directory URL
         */
        private var augmentPath: Array<StorageLocation> = arrayOf()

        /**
         * The directory URL
         */
        private var defaultDownloadDir: StorageLocation = getDefaultDownloadPath()

        /**
         * The directory URL
         */
        private var overrideDownloadDir: StorageLocation? = null

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(SwordBookPath::class)
    }
}

/** Authoritative File spelling; Path is only a convenience projection for Path API consumers. */
internal class StorageLocation(raw: String) {
    val spelling: String = storageFileSpelling(raw)
    val path: Path get() = spelling.toPath()
    val name: String get() = storageName(this)
    val parent: StorageLocation? get() = storageParent(this)
    operator fun div(child: String): StorageLocation = storageChild(this, child)
    override fun equals(other: Any?): Boolean = other is StorageLocation && storageFileEquals(this, other)
    override fun hashCode(): Int = storageFileHash(this)
    override fun toString(): String = spelling
}
