package org.crosswire.common.util

import java.io.File
import java.net.URI
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class InvalidFileUriMetadataTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun decodedNulCannotReturnExistingPrefixMetadata() {
        val file = tmp.newFile("existing").apply { writeText("nonempty"); setLastModified(1_500_000_000_000L) }
        assertTrue(file.length() > 0); assertTrue(file.lastModified() > 0)
        val uri = URI(file.toURI().toString() + "%00ignored")
        val invalid = File(uri.path)
        assertEquals(0L, invalid.lastModified())
        assertEquals(0L, netUrlLastModified(uri))
        assertEquals(uri.toURL().openConnection().contentLength, netUrlSize(uri))
        assertEquals(0, netUrlSize(uri))
    }
}
