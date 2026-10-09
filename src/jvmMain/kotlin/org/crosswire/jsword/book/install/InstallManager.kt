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

import org.crosswire.common.util.CWClassLoader
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.FileUtil
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.PluginRegistry
import org.crosswire.common.util.PropertiesCharset
import org.crosswire.common.util.PropertiesParser
import org.crosswire.common.util.KswordResources
import org.crosswire.common.util.PropertyMap
import org.crosswire.common.util.ReflectionUtil
import org.crosswire.common.util.Reporter
import org.crosswire.common.util.jswordAssert
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

import java.io.IOException
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A manager to abstract out the non-view specific book installation tasks.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class InstallManager {
    /**
     * The map of installer factories
     */
    private var factories: MutableMap<String, Class<InstallerFactory>>? = null

    /**
     * The list of discovered installers
     */
    private var installersMap: MutableMap<String, Installer> = LinkedHashMap<String, Installer>()

    /**
     * The list of listeners
     */
    private var listeners: MutableList<InstallerListener> = CopyOnWriteArrayList<InstallerListener>()

    /**
     * Simple ctor
     */
    init {
        try {
            val sitemap: PropertyMap = preloadedSitemap ?: loadSitemap()
            @Suppress("UNCHECKED_CAST")
            factories = PluginRegistry.entries(InstallerFactory::class).associateTo(LinkedHashMap()) { it.name to (it.implClass.java as Class<InstallerFactory>) }
            var i = 0
            var def = sitemap.get(PREFIX + ++i)
            while (def != null) {
                try {
                    val parts = def.split(",", limit = 3)
                    val type = parts[0]
                    val name = parts[1]
                    val rest = parts[2]

                    val clazz = factories!![type]
                    if (clazz == null) {
                        log.warn("Unable to get class for {}", type)
                    } else {
                        val ifactory = ReflectionUtil.construct<InstallerFactory>(clazz)
                        val installer = ifactory.createInstaller(rest)

                        internalAdd(name, installer)
                    }
                } catch (e: ReflectiveOperationException) {
                    Reporter.informUser(this, e)
                }
                def = sitemap.get(PREFIX + ++i)
            }
        } catch (ex: IOException) {
            Reporter.informUser(this, ex)
        }
    }

    /**
     * The user's saved site list (`<FQN>.plugin`, written by [save] into a JSword home directory) overrides
     * the embedded default, as it did when the default was a classpath resource.
     */
    private fun loadSitemap(): PropertyMap {
        val saved = CWClassLoader.findHomeResource(javaClass.name + FileUtil.EXTENSION_PLUGIN)
        if (saved != null) {
            return NetUtil.loadProperties(saved)
        }
        return PropertyMap().apply {
            putAll(PropertiesParser.parse(KswordResources.bytes("InstallManager.plugin")!!, PropertiesCharset.LATIN1))
        }
    }

    /**
     * Save all properties to the user's local area. Uses the same property name
     * so as to override it.
     */
    fun save() {
        val props = PropertyMap()
        val buf = StringBuilder()
        var i = 1
        for (name in installersMap.keys) {
            val installer = installersMap[name]!!
            // Clear the buffer
            buf.delete(0, buf.length)
            buf.append(installer.getType())
            buf.append(',')
            buf.append(name)
            buf.append(',')
            buf.append(installer.getInstallerDefinition())
            props.put(PREFIX + i++, buf.toString())
        }
        val outputURI = CWProject.instance().getWritableURI(javaClass.name, FileUtil.EXTENSION_PLUGIN)
        try {
            NetUtil.storeProperties(props, outputURI!!, "Saved Installer Sites")
        } catch (e: IOException) {
            log.error("Failed to save installers", e)
        }
    }

    /**
     * The names of all the known InstallerFactories
     * 
     * @return the set of all installer factory names
     */
    val installerFactoryNames: Set<String>
        get() = Collections.unmodifiableSet(factories!!.keys)

    /**
     * Find the registered name of the InstallerFactory that created the given
     * installer. There isn't a nice way to do this right now so we just do a
     * trawl through all the known factories looking!
     * 
     * @param installer the installer
     * @return the name of the factory for the installer
     */
    fun getFactoryNameForInstaller(installer: Installer): String? {
        val match: Class<out Installer> = installer.javaClass
        for (name in factories!!.keys) {
            val factclazz = factories!![name]
            try {
                val ifactory = ReflectionUtil.construct<InstallerFactory>(factclazz)
                val clazz: Class<out Installer> = ifactory.createInstaller().javaClass
                if (clazz == match) {
                    return name
                }
            } catch (e: ReflectiveOperationException) {
                log.warn("Failed to instantiate installer factory: {}={}", name, factclazz!!.name, e)
            }
        }

        log.warn("Failed to find factory name for {} among the {} factories.", installer, Integer.toString(factories!!.size))
        return null
    }

    /**
     * Find the registered name of the Installer. There isn't a nice way to do
     * this right now so we just do a trawl through all the known factories
     * looking!
     * 
     * @param installer the installer
     * @return the name of the installer
     */
    fun getInstallerNameForInstaller(installer: Installer): String? {
        for (name in installersMap.keys) {
            val test = installersMap[name]
            if (installer == test) {
                return name
            }
        }

        log.warn("Failed to find installer name for {} among the {} installers.", installer, Integer.toString(installersMap.size))
        for (name in installersMap.keys) {
            val test = installersMap[name]
            log.warn("  it isn't equal to {}", test!!.getInstallerDefinition())
        }
        return null
    }

    /**
     * Find the InstallerFactory associated with the given name.
     * 
     * @param name
     *            The InstallerFactory name to look-up
     * @return The found InstallerFactory or null if the name was not found
     */
    fun getInstallerFactory(name: String?): InstallerFactory? {
        try {
            return ReflectionUtil.construct<InstallerFactory>(if (name == null) null else factories!![name])
        } catch (e: ReflectiveOperationException) {
            jswordAssert(false) { e }
        }
        return null
    }

    /**
     * Accessor for the known installers
     * @return a map of named installers
     */
    val installers: Map<String, Installer>
        get() = Collections.unmodifiableMap(installersMap)

    /**
     * Find an installer by name
     * 
     * @param name
     *            The name of the installer to find
     * @return The installer or null if none was found by the given name
     */
    fun getInstaller(name: String?): Installer? {
        return if (name == null) null else installersMap[name]
    }

    /**
     * Add an installer to our list of installers
     * 
     * @param name
     *            The name by which we reference the installer
     * @param installer
     *            The installer to add
     */
    fun addInstaller(name: String, installer: Installer) {
        removeInstaller(name)

        internalAdd(name, installer)
        fireInstallersChanged(this, installer, true)
    }

    /**
     * InstallManager is a Map, however we demand that both names and installers
     * are unique (so we can lookup a name from an installer)
     * 
     * @param name
     *            The name of the new installer
     * @param installer
     *            The installer to associate with the given name
     */
    private fun internalAdd(name: String, installer: Installer) {
        val it = installersMap.keys.iterator()
        while (it.hasNext()) {
            val tname = it.next()
            val tinstaller = installersMap[tname]!!

            if (tinstaller == installer) {
                // We have a dupe - remove the old name
                log.warn("duplicate installers: {}={}. removing {}", name, tname, tname)

                // Can't call removeInstaller while iterating.
                it.remove()
                fireInstallersChanged(this, tinstaller, false)
            }
        }

        installersMap[name] = installer
    }

    /**
     * Remove an installer from our list
     * 
     * @param name
     *            The name by which this installer is referenced
     */
    fun removeInstaller(name: String?) {
        if (name != null && installersMap.containsKey(name)) {
            val old = installersMap.remove(name)
            fireInstallersChanged(this, old, false)
        }
    }

    /**
     * Remove a BibleListener from our list of listeners
     * 
     * @param li
     *            The old listener
     */
    fun addInstallerListener(li: InstallerListener) {
        listeners.add(li)
    }

    /**
     * Add a BibleListener to our list of listeners
     * 
     * @param li
     *            The new listener
     */
    fun removeBooksListener(li: InstallerListener) {
        listeners.remove(li)
    }

    /**
     * Kick of an event sequence
     * 
     * @param source
     *            The event source
     * @param installer
     *            The meta-data of the changed Bible
     * @param added
     *            Is it added?
     */
    protected fun fireInstallersChanged(source: Any?, installer: Installer?, added: Boolean) {
        val ev = InstallerEvent(source, installer, added)
        for (listener in listeners) {
            if (added) {
                listener.installerAdded(ev)
            } else {
                listener.installerRemoved(ev)
            }
        }
    }

    companion object {
        // was package-private
        internal var preloadedSitemap: PropertyMap? = null

        @JvmStatic
        fun installSiteMap(siteMap: PropertyMap?) {
            preloadedSitemap = siteMap
        }

        /**
         * The prefix for the keys in the installer property file.
         */
        private const val PREFIX = "Installer."

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(InstallManager::class)
    }
}
