package org.crosswire.ksword.k5b

import org.crosswire.common.util.PropertyMap
import org.crosswire.jsword.book.install.InstallManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The entries of `InstallManager.installers` follow the `Map.Entry` equality contract (key and value equal, hash is
 * the xor of both), as the entries of the former `Collections.unmodifiableMap` did, so `entries.contains` works.
 */
class InstallManagerEntriesTest {
    @Before fun emptySiteMap() = InstallManager.installSiteMap(PropertyMap())

    @After fun restoreSiteMap() = InstallManager.installSiteMap(null)

    @Test fun equalEntriesCompareEqualAndEntriesContainsFindsThem() {
        val manager = InstallManager()
        val installer = manager.getInstallerFactory("sword-http")!!.createInstaller("test.invalid,/pub,/idx,,,")
        val other = manager.getInstallerFactory("sword-http")!!.createInstaller("other.invalid,/pub,/idx,,,")
        manager.addInstaller("A", installer)
        manager.addInstaller("B", other)

        val first = manager.installers.entries.first { it.key == "A" }
        val second = manager.installers.entries.first { it.key == "A" }
        val b = manager.installers.entries.first { it.key == "B" }
        assertNotSame(first, second)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertFalse(first == b)
        assertTrue(manager.installers.entries.contains(first))
        // entries of a plain map with the same key and value are equal too (the Map.Entry contract)
        val plain = mapOf("A" to installer).entries.first()
        assertEquals(first, plain)
        assertEquals(plain.hashCode(), first.hashCode())
        assertTrue(manager.installers.entries.contains(plain))
        assertFalse(manager.installers.entries.contains(mapOf("A" to other).entries.first()))
    }
}
