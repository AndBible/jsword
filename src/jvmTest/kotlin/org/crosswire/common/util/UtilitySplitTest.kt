package org.crosswire.common.util

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.URI

class UtilitySplitTest {
    @Test fun fileHolderRetainsLiteralSpelling() {
        val holder = Class.forName("org.crosswire.common.util.NetUtilJvm")
        val file = File("literal\\name/./child")
        assertEquals(file.toURI(), holder.getMethod("getURI", File::class.java).invoke(null, file))
        val uri = URI("file:/tmp/literal%5Cname")
        assertEquals(File(uri.path), holder.getMethod("getAsFile", URI::class.java).invoke(null, uri))
    }

    @Test fun holdersExposeStaticFileApis() {
        assertNotNull(Class.forName("org.crosswire.common.util.FileUtilJvm").getMethod("delete", File::class.java))
        assertNotNull(Class.forName("org.crosswire.common.util.IOUtilJvm").getMethod("unpackZip", File::class.java, File::class.java))
    }
}
