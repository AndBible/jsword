package org.crosswire.ksword.parity

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.regex.Pattern
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.SwordBookMetaDataJvm
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.common.util.Filter
import org.junit.Assert.*
import org.junit.Test

/** Differential characterization of the actual old File implementation, not a reconstructed model. */
class StorageBoundaryParityTest {
    @Test fun fileMetadataParsingAndFilteredEncodingMatchBaseline() {
        val root = Files.createTempDirectory("storage oracle space \u00e4").toFile()
        try {
            val oldType = Oracle.loader.loadClass("org.crosswire.jsword.book.sword.SwordBookMetaData")
            val oldFilter = Oracle.loader.loadClass("org.crosswire.common.util.Filter")
            val file = File(root, "demo.conf")
            for (encoding in listOf("UTF-8", "uTf-8", "Latin-1", "absent")) {
                val text = "[Demo]\nDescription=Caf\u00e9\nModDrv=RawText\n" +
                    (if (encoding == "absent") "" else "Encoding=$encoding\n")
                file.writeBytes(text.toByteArray(if (encoding == "Latin-1") charset("windows-1252") else Charsets.UTF_8))
                val old = oldType.getConstructor(File::class.java, URI::class.java).newInstance(file, null)
                val current = SwordBookMetaDataJvm.create(file, null)
                assertEquals(oldType.getMethod("getName").invoke(old), current.name)
                assertEquals(oldType.getMethod("getBookConf").invoke(old), current.bookConf)
                assertEquals(oldType.getMethod("getConfigFile").invoke(old), SwordBookMetaDataJvm.getConfigFile(current))
                val filter = java.lang.reflect.Proxy.newProxyInstance(Oracle.loader, arrayOf(oldFilter)) { _, method, args ->
                    if (method.name == "test") args!![0] != "Encoding" else null
                }
                oldType.getMethod("reload", oldFilter).invoke(old, filter)
                current.reload(Filter { it != "Encoding" })
                assertEquals(oldType.getMethod("getName").invoke(old), current.name)
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun regexFindAndListingMatchActualBaseline() {
        val oldType = Oracle.loader.loadClass("org.crosswire.jsword.book.sword.SwordBookMetaData")
        for (name in listOf("RTF_PATTERN", "HTML_PATTERN")) {
            val old = oldType.getDeclaredField(name).apply { isAccessible = true }.get(null) as Pattern
            val current = SwordBookMetaData::class.java.getDeclaredField(name).apply { isAccessible = true }.get(null) as Regex
            for (value in listOf("", "plain", "\\pard", "x\\pae", "\\par", "\\qc", "\\b", "\\i", "\\u-1234", "\\u1234567", "\\u123", "\\u\u0661\u0662\u0663\u0664", "<b>", "A>", "<\u00e4>", "line\n<b>")) {
                assertEquals("$name: $value", old.matcher(value).find(), current.containsMatchIn(value))
            }
        }
        val root = Files.createTempDirectory("listing oracle").toFile()
        try {
            for (name in listOf("z.conf", "a.conf", "globals.x.conf", "B.CONF")) File(root, name).writeText("")
            File(root, "dir.conf").mkdir()
            val oldPath = Oracle.loader.loadClass("org.crosswire.jsword.book.sword.SwordBookPath")
            for (dir in listOf(root, File(root, "missing"), File(root, "a.conf"), File(""))) {
                val expected = oldPath.getMethod("getBookList", File::class.java).invoke(null, dir) as Array<*>?
                assertArrayEquals(expected, SwordBookPathJvm.getBookList(dir))
            }
        } finally { root.deleteRecursively() }
    }
}
