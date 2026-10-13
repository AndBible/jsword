package org.crosswire.common.util

import java.io.File
import java.net.URI
import org.junit.Assert.*
import org.junit.Test

class NetUtilPathTest {
    @Test fun projectionRetainsFileUriValidationAndDecodedFixtureNames() {
        val method = NetUtil::class.java.methods.singleOrNull { it.name == "toPath" && it.parameterTypes.contentEquals(arrayOf(URI::class.java)) }
        assertNotNull("NetUtil.toPath URI projection missing", method)
        val root = File(System.getProperty("ksword.fixturesDir"))
        for (file in listOf(File(root, "space name"), File(root, "ä"))) {
            val uri = file.toURI()
            assertEquals(File(uri).path, method!!.invoke(null, uri).toString())
        }
        for (uri in listOf(URI("relative"), URI("http:/file"), URI("file://host/file"), URI("file:/file?query"), URI("file:/file#fragment"), URI("file:opaque"))) {
            assertThrows(IllegalArgumentException::class.java) { File(uri) }
            val ex = assertThrows(java.lang.reflect.InvocationTargetException::class.java) { method!!.invoke(null, uri) }
            assertTrue(ex.cause is IllegalArgumentException)
        }
    }
}
