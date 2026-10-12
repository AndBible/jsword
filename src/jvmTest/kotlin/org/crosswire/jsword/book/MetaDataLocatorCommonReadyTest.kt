package org.crosswire.jsword.book

import org.crosswire.common.util.CWProject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MetaDataLocatorCommonReadyTest {
    @Test fun overrideLocationsMatchUriFileConversionAndCreateDirectories() {
        val dirs = CWProject.instance().projectResourceDirs
        val expectedWrite = dirs.firstOrNull()?.let { File(File(it), "jsword-mods.d") }
        val expectedRead = dirs.getOrNull(1)?.let { File(File(it), "jsword-mods.d") }
        assertEquals(expectedWrite, MetaDataLocator.JSWORD.writeLocation)
        assertEquals(expectedRead, MetaDataLocator.JSWORD.readLocation)
        expectedWrite?.let { assertTrue(it.isDirectory) }
        expectedRead?.let { assertTrue(it.isDirectory) }
        assertNull(MetaDataLocator.TRANSIENT.writeLocation)
        assertNull(MetaDataLocator.TRANSIENT.readLocation)
    }
}
