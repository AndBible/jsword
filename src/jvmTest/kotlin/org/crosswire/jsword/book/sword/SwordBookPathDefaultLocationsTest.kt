package org.crosswire.jsword.book.sword

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SwordBookPath.scanDefaultLocations] off (the desktop host owns its module dir): the standard SWORD locations of the
 * user's home are neither searched nor migrated. The flag-on path is not exercised here: its `~/.jsword` migration
 * would act on the project dir JSword cached from the real home.
 */
class SwordBookPathDefaultLocationsTest {
    @Test fun withDefaultLocationsOffNothingInTheHomeIsSearchedOrMoved() {
        val home = Files.createTempDirectory("fake-home").toFile()
        val previousHome = System.getProperty("user.home")
        val previousFlag = SwordBookPath.scanDefaultLocations
        try {
            File(home, ".sword/mods.d").mkdirs()
            File(home, ".sword/mods.d/homebook.conf").writeText("[HomeBook]\nModDrv=RawText\nDataPath=./modules/texts/rawtext/homebook/\n")
            File(home, ".jsword/mods.d").mkdirs()
            File(home, ".jsword/mods.d/old.conf").writeText("[Old]\n")
            File(home, ".jsword/modules").mkdirs()
            System.setProperty("user.home", home.absolutePath)
            SwordBookPath.scanDefaultLocations = false

            assertEquals(0, SwordBookPath.getDefaultPaths().size)
            assertTrue("~/.jsword must not be migrated", File(home, ".jsword/mods.d/old.conf").isFile)
            assertFalse(File(home, ".sword/mods.d/old.conf").exists())
        } finally {
            SwordBookPath.scanDefaultLocations = previousFlag
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }

    @Test fun defaultIsOn() {
        assertTrue(SwordBookPath.scanDefaultLocations)
    }
}
