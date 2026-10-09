package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** [IndexFiles.parse] must equal the stream-based [NetUtil.listByIndexFile] for the same text. */
class IndexFilesDifferentialTest {
    private fun viaNetUtil(text: String, filter: URIFilter? = null): List<String> {
        val f = File.createTempFile("indexfiles", ".txt")
        try {
            f.writeBytes(text.encodeToByteArray())
            val r = if (filter == null) NetUtil.listByIndexFile(f.toURI()) else NetUtil.listByIndexFile(f.toURI(), filter)
            return r.toList()
        } finally {
            f.delete()
        }
    }

    @Test fun embeddedIndexFiles() {
        for (name in listOf("readings.txt", "rtol.txt")) {
            val text = KswordResources.text(name)!!
            assertEquals(name, viaNetUtil(text), IndexFiles.parse(text))
        }
    }

    @Test fun commentsBlanksAndWhitespace() {
        val text = "# header\n\n  a  \r\nb # trailing\r\n\t\n#only\nindex.txt\n c d \u0001x\n\u0001\nlast-no-newline"
        assertEquals(viaNetUtil(text), IndexFiles.parse(text))
        assertEquals(listOf("a", "b", "c d \u0001x", "last-no-newline"), IndexFiles.parse(text))
    }

    @Test fun filterIsApplied() {
        val text = "a\nbb\nccc\n"
        val accept = { s: String -> s.length > 1 }
        val viaFilter = viaNetUtil(text, object : URIFilter { override fun accept(name: String?) = accept(name!!) })
        assertEquals(viaFilter, IndexFiles.parse(text, accept))
        assertEquals(listOf("bb", "ccc"), viaFilter)
    }
}
