package org.crosswire.jsword.book

import java.io.File

/** File boundary for JVM consumers; the locator owns the Path implementation. */
object MetaDataLocatorJvm {
    @JvmStatic fun getReadLocation(locator: MetaDataLocator): File? = locator.readLocation?.toFile()
    @JvmStatic fun getWriteLocation(locator: MetaDataLocator): File? = locator.writeLocation?.toFile()
}
