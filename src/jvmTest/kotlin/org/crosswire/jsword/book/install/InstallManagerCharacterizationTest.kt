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
 * \u00A9 CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.book.install

import org.crosswire.common.util.CWClassLoader
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.NetUtilJvm
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.util.TreeSet
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Characterization of [InstallManager]: factory lookup, adding and
 * removing installers with listener events, and the save/reload round trip.
 * Runs against a temporary CWProject home that is restored afterwards. No
 * network access is made.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class InstallManagerCharacterizationTest {
    private var savedInstance: Any? = null
    private var savedHomeProperty: String? = null
    private lateinit var tempHome: File

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        val instanceField: Field = CWProject::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        savedInstance = instanceField.get(null)
        savedHomeProperty = System.getProperty("jsword.home")

        tempHome = Files.createTempDirectory("installmanager-test").toFile()
        System.setProperty("jsword.home", tempHome.absolutePath)

        // Fresh, not yet established CWProject so that the temporary home is used
        val reset = CWProject::class.java.getDeclaredMethod("reset")
        reset.isAccessible = true
        reset.invoke(null)
        CWProject.instance().setFrontendName("jsword-test")
    }

    @AfterTest
    @Throws(Exception::class)
    fun tearDown() {
        val instanceField: Field = CWProject::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, savedInstance)
        if (savedHomeProperty == null) {
            System.clearProperty("jsword.home")
        } else {
            System.setProperty("jsword.home", savedHomeProperty!!)
        }

        // Re-point the statics that establishing the temporary home changed
        val restored = savedInstance as CWProject
        CWClassLoader.setHome(restored.projectResourceDirs)
        val writable = restored.writableProjectDir
        NetUtilJvm.setURICacheDir(File(File(writable!!), "netcache"))

        deleteRecursively(tempHome)
    }

    private class Recorder : InstallerListener {
        val events: MutableList<String> = ArrayList()
        val installers: MutableList<Installer> = ArrayList()

        override fun installerAdded(ev: InstallerEvent) {
            assertTrue(ev.isAddition)
            events.add("added")
            installers.add(ev.installer!!)
        }

        override fun installerRemoved(ev: InstallerEvent) {
            assertFalse(ev.isAddition)
            events.add("removed")
            installers.add(ev.installer!!)
        }
    }

    @Test
    fun testFactoryNames() {
        val manager = InstallManager()
        // InstallerFactory.plugin defines sword-http and sword-https (sword-ftp is commented out)
        val expected: Set<String> = TreeSet(listOf("sword-http", "sword-https"))
        assertEquals(expected, TreeSet(manager.installerFactoryNames))
    }

    @Test
    fun testFactoryNamesUnmodifiable() {
        assertFailsWith<UnsupportedOperationException> {
            // Seen through the java.util interface, as the Java caller of the old test did
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
            (InstallManager().installerFactoryNames as java.util.Set<String>).add("x")
        }
    }

    @Test
    fun testGetInstallerFactory() {
        val manager = InstallManager()
        assertNotNull(manager.getInstallerFactory("sword-http"))
        assertNotNull(manager.getInstallerFactory("sword-https"))
    }

    /**
     * Quirk: the factory class for an unknown name is null and is passed on to
     * ReflectionUtil.construct, so the lookup throws instead of returning null
     * (the Javadoc promises null).
     */
    @Test
    fun testGetInstallerFactoryUnknownNameThrows() {
        assertFailsWith<NullPointerException> {
            InstallManager().getInstallerFactory("nope")
        }
    }

    @Test
    fun testDefaultSiteMapInstallers() {
        // InstallManager.plugin defines seven sword-https installers, named by the second field
        val manager = InstallManager()
        assertEquals(7, manager.installers.size)
        assertTrue(manager.installers.containsKey("CrossWire"))
        assertEquals("sword-https", manager.getInstaller("CrossWire")!!.getType())
        assertNull(manager.getInstaller("Test"))
    }

    @Test
    fun testAddInstallerLookupAndNames() {
        val manager = InstallManager()
        val installer = createTestInstaller(manager, DEFINITION)
        assertEquals("sword-http", installer.getType())
        assertEquals(DEFINITION, installer.getInstallerDefinition())

        val recorder = Recorder()
        manager.addInstallerListener(recorder)
        manager.addInstaller("Test", installer)

        assertSame(installer, manager.getInstaller("Test"))
        assertSame(installer, manager.installers["Test"])
        assertEquals("Test", manager.getInstallerNameForInstaller(installer))
        assertEquals("sword-http", manager.getFactoryNameForInstaller(installer))
        assertEquals(listOf("added"), recorder.events)
        assertSame(installer, recorder.installers[0])
    }

    @Test
    fun testRemoveInstaller() {
        val manager = InstallManager()
        val installer = createTestInstaller(manager, DEFINITION)
        manager.addInstaller("Test", installer)

        val recorder = Recorder()
        manager.addInstallerListener(recorder)
        manager.removeInstaller("Test")

        assertNull(manager.getInstaller("Test"))
        assertEquals(listOf("removed"), recorder.events)
        assertSame(installer, recorder.installers[0])

        // Removing an unknown name is silent
        manager.removeInstaller("Test")
        assertEquals(1, recorder.events.size)

        // After removing the listener no events arrive
        manager.removeBooksListener(recorder)
        manager.addInstaller("Test", installer)
        assertEquals(1, recorder.events.size)
    }

    @Test
    fun testReplaceSameNameFiresRemovedThenAdded() {
        val manager = InstallManager()
        val first = createTestInstaller(manager, DEFINITION)
        val second = createTestInstaller(manager, "other.invalid,/pub,/idx,,,")
        manager.addInstaller("Test", first)

        val recorder = Recorder()
        manager.addInstallerListener(recorder)
        manager.addInstaller("Test", second)

        assertSame(second, manager.getInstaller("Test"))
        assertEquals(listOf("removed", "added"), recorder.events)
        assertSame(first, recorder.installers[0])
        assertSame(second, recorder.installers[1])
    }

    @Test
    fun testEqualInstallerUnderNewNameDropsOldName() {
        val manager = InstallManager()
        val first = createTestInstaller(manager, DEFINITION)
        val equal = createTestInstaller(manager, DEFINITION)
        assertEquals(first, equal)
        manager.addInstaller("One", first)

        val recorder = Recorder()
        manager.addInstallerListener(recorder)
        manager.addInstaller("Two", equal)

        // names and installers are both unique: the old name goes away
        assertNull(manager.getInstaller("One"))
        assertSame(equal, manager.getInstaller("Two"))
        assertEquals(listOf("removed", "added"), recorder.events)
        assertEquals("Two", manager.getInstallerNameForInstaller(first))
    }

    @Test
    fun testUnknownInstallerHasNoName() {
        val manager = InstallManager()
        val stranger = createTestInstaller(manager, "stranger.invalid,/a,/b,,,")
        assertNull(manager.getInstallerNameForInstaller(stranger))
    }

    @Test
    fun testInstallersMapUnmodifiable() {
        assertFailsWith<UnsupportedOperationException> {
            // Seen through the java.util interface, as the Java caller of the old test did
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
            (InstallManager().installers as java.util.Map<String, Installer>).remove("CrossWire")
        }
    }

    @Test
    fun testSaveThenReload() {
        val manager = InstallManager()
        manager.addInstaller("Test", createTestInstaller(manager, DEFINITION))
        manager.save()

        // save() wrote <home>/<fqn>.plugin
        val saved = File(tempHome, InstallManager::class.java.name + ".plugin")
        assertTrue(saved.isFile)

        val reloaded = InstallManager()
        val installer = reloaded.getInstaller("Test")
        assertNotNull(installer)
        assertEquals("sword-http", installer.getType())
        assertEquals(DEFINITION, installer.getInstallerDefinition())
        assertEquals(HashSet(manager.installers.keys), HashSet(reloaded.installers.keys))
    }

    @Test
    fun testNullNames() {
        val manager = InstallManager()
        // Map lookups with a null key: no such installer, nothing removed, no event
        assertNull(manager.getInstaller(null))
        val recorder = Recorder()
        manager.addInstallerListener(recorder)
        val before = manager.installers.size
        manager.removeInstaller(null)
        assertEquals(before, manager.installers.size)
        assertTrue(recorder.events.isEmpty())
    }

    /** Null name: the HashMap lookup gives a null class, which ReflectionUtil.construct rejects with an NPE. */
    @Test
    fun testGetInstallerFactoryNullNameThrows() {
        assertFailsWith<NullPointerException> {
            InstallManager().getInstallerFactory(null)
        }
    }

    companion object {
        /** Installer definition: host,packageDir,catalogDir,indexDir,proxyHost,proxyPort. */
        private const val DEFINITION = "test.invalid,/pub,/idx,,,"

        private fun deleteRecursively(file: File) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursively(child)
                }
            }
            file.delete()
        }

        private fun createTestInstaller(manager: InstallManager, definition: String): Installer {
            return manager.getInstallerFactory("sword-http")!!.createInstaller(definition)
        }
    }
}
