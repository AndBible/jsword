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
    @Test fun reflectiveFailuresRetainDistinctUnderlyingCausesIncludingErrors() {
        val method = CorpusProbe::class.java.getDeclaredMethod("fileMetadata", File::class.java, URI::class.java).apply { isAccessible = true }
        val root = Files.createTempDirectory("probe failures").toFile()
        try {
            val missing = File(root, "missing.conf")
            val malformed = File(root, "malformed.conf").apply { writeText("[Demo]\nModDrv=RawText\n") }
            fun cause(file: File, uri: URI?): Throwable = try {
                method.invoke(null, file, uri)
                error("expected failure")
            } catch (e: java.lang.reflect.InvocationTargetException) { e.targetException }
            val a = cause(missing, null)
            val b = cause(malformed, root.toURI())
            assertFalse(a is java.lang.reflect.InvocationTargetException)
            assertFalse(b is java.lang.reflect.InvocationTargetException)
            assertNotEquals(a.javaClass.name + ":" + a.message, b.javaClass.name + ":" + b.message)
            val throwTarget = CorpusProbe::class.java.getDeclaredMethod("throwTarget", Throwable::class.java).apply { isAccessible = true }
            val target = AssertionError("actual error")
            val wrapper = assertThrows(java.lang.reflect.InvocationTargetException::class.java) { throwTarget.invoke(null, target) }
            assertSame(target, wrapper.targetException)
        } finally { root.deleteRecursively() }
    }

    @Test fun unixBackslashFileAndDotSpellingMatchBaselineWithoutNormalizing() {
        if (File.separator != "/") return
        val root = Files.createTempDirectory("lexical sword").toFile()
        try {
            val file = File(root, "dir\\name.conf").apply { writeText("[Literal]\nDescription=Literal\nModDrv=RawText\n") }
            val oldType = Oracle.loader.loadClass("org.crosswire.jsword.book.sword.SwordBookMetaData")
            val old = oldType.getConstructor(File::class.java, URI::class.java).newInstance(file, null)
            val current = SwordBookMetaDataJvm.create(file, null)
            assertEquals(oldType.getMethod("getBookConf").invoke(old), current.bookConf)
            assertEquals(file, SwordBookMetaDataJvm.getConfigFile(current))
            assertEquals("Literal", current.name)
        } finally { root.deleteRecursively() }
    }

    @Test fun lexicalDownloadAndAugmentGettersMatchOldFileSpelling() {
        val oldType = Oracle.loader.loadClass("org.crosswire.jsword.book.sword.SwordBookPath")
        val oldOverride = oldType.getDeclaredField("overrideDownloadDir").apply { isAccessible = true }
        val oldAugment = oldType.getDeclaredField("augmentPath").apply { isAccessible = true }
        val savedOverride = oldOverride.get(null)
        val savedAugment = oldAugment.get(null)
        val currentType = org.crosswire.jsword.book.sword.SwordBookPath::class.java
        val currentOverride = currentType.getDeclaredField("overrideDownloadDir").apply { isAccessible = true }
        val savedCurrent = currentOverride.get(null)
        val currentAugment = currentType.getDeclaredField("augmentPath").apply { isAccessible = true }
        val savedCurrentAugment = currentAugment.get(null)
        try {
            val dot = File("./modules")
            val plain = File("modules")
            oldType.getMethod("setDownloadDir", File::class.java).invoke(null, dot)
            SwordBookPathJvm.setDownloadDir(dot)
            assertEquals(oldType.getMethod("getDownloadDir").invoke(null), SwordBookPathJvm.getDownloadDir())
            // Observe the old defensive-copy getter without invoking its unrelated registration side effect.
            oldAugment.set(null, arrayOf(dot, plain, File("dir\\name")))
            val legacy = oldType.getMethod("getAugmentPath").invoke(null) as Array<*>
            val locationType = Class.forName("org.crosswire.jsword.book.sword.StorageLocation")
            val locations = java.lang.reflect.Array.newInstance(locationType, legacy.size)
            legacy.forEachIndexed { index, value ->
                java.lang.reflect.Array.set(locations, index, locationType.getConstructor(String::class.java).newInstance((value as File).path))
            }
            currentAugment.set(null, locations)
            assertArrayEquals(legacy, SwordBookPathJvm.getAugmentPath())
            assertEquals(listOf("./modules", "modules", "dir\\name"), legacy.map { (it as File).path })
        } finally {
            oldOverride.set(null, savedOverride)
            oldAugment.set(null, savedAugment)
            currentOverride.set(null, savedCurrent)
            currentAugment.set(null, savedCurrentAugment)
        }
    }

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
