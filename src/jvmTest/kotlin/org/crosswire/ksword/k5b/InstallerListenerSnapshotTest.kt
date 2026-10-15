package org.crosswire.ksword.k5b

import org.crosswire.common.util.PropertyMap
import org.crosswire.jsword.book.install.InstallManager
import org.crosswire.jsword.book.install.Installer
import org.crosswire.jsword.book.install.InstallerEvent
import org.crosswire.jsword.book.install.InstallerListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * Pins the snapshot semantics of the `CopyOnWriteArrayList` that holds the `InstallManager` listeners: a listener that
 * removes itself (or adds another listener) while an installer event fires neither breaks the other listeners nor
 * throws, and the change only takes effect for the next event. A plain `ArrayList` fails both tests: the removal
 * makes the loop skip the second listener (or throw ConcurrentModificationException), and the addition throws.
 */
class InstallerListenerSnapshotTest {
    @Before fun emptySiteMap() = InstallManager.installSiteMap(PropertyMap())

    @After fun restoreSiteMap() = InstallManager.installSiteMap(null)

    private fun installer(manager: InstallManager): Installer =
        manager.getInstallerFactory("sword-http")!!.createInstaller("test.invalid,/pub,/idx,,,")

    @Test fun listenerRemovingItselfDuringFireDoesNotBreakOthers() {
        val manager = InstallManager()
        val installer = installer(manager)
        val seen = mutableListOf<String>()
        lateinit var self: InstallerListener
        self = object : InstallerListener {
            override fun installerAdded(ev: InstallerEvent) { seen += "self-added"; manager.removeBooksListener(self) }
            override fun installerRemoved(ev: InstallerEvent) { seen += "self-removed" }
        }
        val other = object : InstallerListener {
            override fun installerAdded(ev: InstallerEvent) { seen += "other-added" }
            override fun installerRemoved(ev: InstallerEvent) { seen += "other-removed" }
        }
        manager.addInstallerListener(self)
        manager.addInstallerListener(other)

        manager.addInstaller("A", installer) // fires added: self removes itself, other still sees the event
        manager.removeInstaller("A") // self is gone for the next event
        manager.addInstaller("A", installer)

        assertEquals(listOf("self-added", "other-added", "other-removed", "other-added"), seen)
    }

    @Test fun listenerAddedDuringFireSeesOnlyLaterEvents() {
        val manager = InstallManager()
        val installer = installer(manager)
        val seen = mutableListOf<String>()
        val late = object : InstallerListener {
            override fun installerAdded(ev: InstallerEvent) { seen += "late-added" }
            override fun installerRemoved(ev: InstallerEvent) { seen += "late-removed" }
        }
        val adder = object : InstallerListener {
            override fun installerAdded(ev: InstallerEvent) { seen += "adder-added"; manager.addInstallerListener(late) }
            override fun installerRemoved(ev: InstallerEvent) { seen += "adder-removed" }
        }
        manager.addInstallerListener(adder)

        manager.addInstaller("A", installer) // late is added during this event and must not see it
        manager.removeInstaller("A")

        assertEquals(listOf("adder-added", "adder-removed", "late-removed"), seen)
    }

    @Test fun eventCarriesInstallerSourceAndDirection() {
        val manager = InstallManager()
        val installer = installer(manager)
        val events = mutableListOf<InstallerEvent>()
        manager.addInstallerListener(object : InstallerListener {
            override fun installerAdded(ev: InstallerEvent) { events += ev }
            override fun installerRemoved(ev: InstallerEvent) { events += ev }
        })

        manager.addInstaller("A", installer)
        manager.removeInstaller("A")

        assertEquals(listOf(true, false), events.map { it.isAddition })
        events.forEach {
            assertSame(installer, it.installer)
            assertSame(manager, it.getSource())
        }
    }
}
