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

import okio.IOException
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.CopyOnWriteList
import org.crosswire.common.util.FileUtil
import org.crosswire.common.util.KswordResources
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.PluginEntry
import org.crosswire.common.util.PluginRegistry
import org.crosswire.common.util.PropertiesCharset
import org.crosswire.common.util.PropertiesParser
import org.crosswire.common.util.PropertyMap
import org.crosswire.common.util.Reporter
import kotlin.jvm.JvmStatic

/**
 * A manager to abstract out the non-view specific book installation tasks.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class InstallManager {
    /**
     * The map of installer factories, in registry order
     */
    private var factories: Map<String, PluginEntry<InstallerFactory>> = LinkedHashMap()

    /**
     * The list of discovered installers
     */
    private var installersMap: MutableMap<String, Installer> = LinkedHashMap<String, Installer>()

    /**
     * The list of listeners (a snapshot-iterating copy-on-write list, rule 20)
     */
    private var listeners: MutableList<InstallerListener> = CopyOnWriteList<InstallerListener>()

    /**
     * Simple ctor
     */
    init {
        try {
            val sitemap: PropertyMap = preloadedSitemap ?: loadSitemap()
            factories = PluginRegistry.entries(InstallerFactory::class).associateTo(LinkedHashMap()) { it.name to it }
            var i = 0
            var def = sitemap.get(PREFIX + ++i)
            while (def != null) {
                val parts = def.split(",", limit = 3)
                val type = parts[0]
                val name = parts[1]
                val rest = parts[2]

                val entry = factories[type]
                if (entry == null) {
                    log.warn("Unable to get class for {}", type)
                } else {
                    val installer = entry.create().createInstaller(rest)

                    internalAdd(name, installer)
                }
                def = sitemap.get(PREFIX + ++i)
            }
        } catch (ex: IOException) {
            Reporter.informUser(this, ex)
        }
    }

    /**
     * The user's saved site list (`<FQN>.plugin`, written by [save] into a JSword home directory) overrides
     * the embedded default, as it did when the default was a classpath resource. The homes are the readable
     * project directories of [CWProject] (the same list the JVM class loader was given).
     */
    private fun loadSitemap(): PropertyMap {
        val saved = findSavedSitemap()
        if (saved != null) {
            return NetUtil.loadProperties(saved)
        }
        // Intentionally dropped: the old lookup also honoured a short-name override `<home>/InstallManager.plugin`.
        // Only `<home>/<FQN>.plugin`, which save() writes, is honoured now.
        val embedded = KswordResources.bytes("InstallManager.plugin") ?: return PropertyMap()
        return PropertyMap().apply { putAll(PropertiesParser.parse(embedded, PropertiesCharset.LATIN1)) }
    }

    /** The first readable `<home>/<FQN>.plugin`, or null. */
    private fun findSavedSitemap(): org.crosswire.common.util.KUri? {
        for (home in CWProject.instance().projectResourceDirs) {
            val candidate = NetUtil.lengthenURI(home!!, SITEMAP_NAME + FileUtil.EXTENSION_PLUGIN)
            if (NetUtil.canRead(candidate!!)) {
                return candidate
            }
        }
        return null
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
            buf.clear()
            buf.append(installer.getType())
            buf.append(',')
            buf.append(name)
            buf.append(',')
            buf.append(installer.getInstallerDefinition())
            props.put(PREFIX + i++, buf.toString())
        }
        val outputURI = CWProject.instance().getWritableURI(SITEMAP_NAME, FileUtil.EXTENSION_PLUGIN)
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
        get() = UnmodifiableNameView(factories.keys)

    /**
     * Find the registered name of the InstallerFactory that created the given
     * installer. There isn't a nice way to do this right now so we just do a
     * trawl through all the known factories looking!
     * 
     * @param installer the installer
     * @return the name of the factory for the installer
     */
    fun getFactoryNameForInstaller(installer: Installer): String? {
        val match = installer::class
        for ((name, entry) in factories) {
            if (entry.create().createInstaller()::class == match) {
                return name
            }
        }

        log.warn("Failed to find factory name for {} among the {} factories.", installer, factories.size.toString())
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

        log.warn("Failed to find installer name for {} among the {} installers.", installer, installersMap.size.toString())
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
     * @return The found InstallerFactory; an unknown or null name throws a NullPointerException (the old
     *            reflective construction of a null class did the same, despite the old documentation)
     */
    fun getInstallerFactory(name: String?): InstallerFactory? {
        val entry = if (name == null) null else factories[name]
        return entry!!.create()
    }

    /**
     * Accessor for the known installers
     * @return a map of named installers
     */
    val installers: Map<String, Installer>
        get() = UnmodifiableInstallerView(installersMap)

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
     * Add an InstallerListener to our list of listeners
     * 
     * @param li
     *            The new listener
     */
    fun addInstallerListener(li: InstallerListener) {
        listeners.add(li)
    }

    /**
     * Remove an InstallerListener from our list of listeners
     * 
     * @param li
     *            The old listener
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

    /**
     * Live read-only view of the factory names, the replacement of `Collections.unmodifiableSet` (rule 26): it
     * reflects later changes, and neither the set nor its iterator can remove.
     */
    private class UnmodifiableNameView(private val names: Set<String>) : AbstractSet<String>() {
        override val size: Int get() = names.size

        override fun contains(element: String): Boolean = names.contains(element)

        override fun iterator(): Iterator<String> {
            val source = names.iterator()
            return object : Iterator<String> {
                override fun hasNext(): Boolean = source.hasNext()
                override fun next(): String = source.next()
            }
        }
    }

    /**
     * Live read-only view of the installers by name, the replacement of `Collections.unmodifiableMap` (rule 26):
     * mutators of the map, its entry set and its iterators throw `UnsupportedOperationException`.
     */
    private class UnmodifiableInstallerView(private val map: Map<String, Installer>) : AbstractMap<String, Installer>() {
        override val entries: Set<Map.Entry<String, Installer>>
            get() = object : AbstractSet<Map.Entry<String, Installer>>() {
                override val size: Int get() = map.size

                override fun iterator(): Iterator<Map.Entry<String, Installer>> {
                    val source = map.entries.iterator()
                    return object : Iterator<Map.Entry<String, Installer>> {
                        override fun hasNext(): Boolean = source.hasNext()
                        override fun next(): Map.Entry<String, Installer> = source.next().let { ReadOnlyEntry(it.key, it.value) }
                    }
                }
            }

        override val size: Int get() = map.size

        override fun containsKey(key: String): Boolean = map.containsKey(key)

        override fun get(key: String): Installer? = map[key]
    }

    /**
     * A read-only map entry with the `Map.Entry` equality contract (key and value equal, hash is the xor of both),
     * so that `installers.entries.contains(entry)` and comparisons with entries of other maps work.
     */
    private class ReadOnlyEntry(override val key: String, override val value: Installer) : Map.Entry<String, Installer> {
        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
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
         * The fully qualified class name of this class: the base name of the saved site list
         * (`<name>.plugin` in a JSword home). Pinned by the saved files of existing users.
         */
        private const val SITEMAP_NAME = "org.crosswire.jsword.book.install.InstallManager"

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(InstallManager::class)
    }
}
