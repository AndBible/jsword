package org.crosswire.common.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MetadataUtilCharacterizationTest {
    @Test
    fun iniBomCommentsContinuationsAndMalformedTerminalContinuation() {
        val ini = IniSection()
        ini.load(" \uFEFF[Demo]\r\n\uFEFF# comment\rA = first \\\r\n ; skipped\n\uFEFFsecond\nA=third\nBad\n".toByteArray(), "UTF-8")
        assertEquals("Demo", ini.name)
        assertEquals(listOf("first\nsecond", "third"), ini.getValues("A")!!.toList())
        assertEquals("Skipping: Expected to see '=' in: Bad\n", ini.report())
        assertEquals("", ini.report())
        assertFailsWith<NullPointerException> { IniSection().load("A=end\\".toByteArray(), "UTF-8") }
    }

    @Test
    fun iniDecodingRetainsEncodingChoiceAndSingleBomRemoval() {
        val bytes = byteArrayOf(0x5b, 0x58, 0x5d, 10, 65, 61, 0xe9.toByte(), 10)
        val utf = IniSection().apply { load(bytes, "UTF-8") }
        val latin = IniSection().apply { load(bytes, "ISO-8859-1") }
        assertEquals("\ufffd", utf.get("A"))
        assertEquals("\u00e9", latin.get("A"))
        val ini = IniSection().apply { load("\uFEFF\uFEFFA=x\n\uFEFF A=y".toByteArray(), "UTF-8") }
        assertEquals("x", ini.get("\uFEFFA"))
        assertEquals("y", ini.get("A"))
    }

    @Test
    fun propertyLoadPreservesJdkEnumerationOrderAndDefaults() {
        val bytes = "z=last\na=first\nescaped\\ key=\\u00e9\nz=updated\n".toByteArray()
        val reference = Properties().apply { load(ByteArrayInputStream(bytes)) }
        val defaults = PropertyMap().apply { put("default", "fallback") }
        val actual = PropertyMap(defaults).apply { put("existing", "kept"); load(bytes) }
        assertEquals(listOf("existing") + reference.keys().toList(), actual.keys.toList())
        assertEquals("fallback", actual["default"])
        assertEquals("updated", actual["z"])
        assertEquals("\u00e9", actual["escaped key"])
        val output = ByteArrayOutputStream()
        PropertyMapJvm.store(actual, output, "Comment")
        val lines = output.toString("ISO-8859-1").lines()
        assertEquals("#Comment", lines[0])
        kotlin.test.assertTrue(lines[1].startsWith("#"))
        val reloaded = Properties().apply { load(ByteArrayInputStream(output.toByteArray())) }
        assertEquals(actual.toMap(), reloaded.entries.associate { it.key as String to it.value as String })
        kotlin.test.assertFalse(reloaded.containsKey("default"))
    }

    @Test
    fun languageFallbackUsesJdkDefaultDisplayLocale() {
        for (code in listOf("zz", "qaa", "iw", "in", "ji", "en-Latn-US", "ar")) {
            val language = Language(code)
            kotlin.test.assertTrue(language.name.isNotEmpty())
            assertEquals(language, Language(code))
            assertEquals(language.hashCode(), Language(code).hashCode())
        }
        assertEquals(false, Language("ar").isLeftToRight)
        assertEquals(true, Language("ar-Latn").isLeftToRight)
        assertEquals(OSType.MAC, OSType.fromString("Mac OS X"))
        assertEquals(OSType.WIN32, OSType.fromString("Windows 10"))
        assertEquals(OSType.DEFAULT, OSType.fromString("Linux"))
        assertEquals(java.io.File(System.getProperty("user.home")).toURI(), OSType.getUserHome())
    }
    @Test
    fun iniViewsRemainLiveAndRejectIteratorMutation() {
        val ini = IniSection().apply { add("A", "one") }
        val keys = ini.keys
        val values = ini.getValues("A")!!
        ini.add("B", "two")
        ini.add("A", "three")
        assertEquals(setOf("A", "B"), keys)
        assertEquals(listOf("one", "three"), values.toList())
        assertFailsWith<UnsupportedOperationException> { (values as MutableCollection<String>).remove("missing") }
        assertFailsWith<UnsupportedOperationException> { (keys as MutableCollection<String>).addAll(emptyList()) }
        val iterator = values.iterator()
        iterator.next()
        assertFailsWith<UnsupportedOperationException> {
            (iterator as java.util.Iterator<String>).remove()
        }
    }

    @Test
    fun metadataDetectsEncodingAfterInitialUtf8Parse() {
        val metadata = org.crosswire.jsword.book.sword.SwordBookMetaData::class.java
        val field = metadata.getDeclaredField("partialLoading").apply { isAccessible = true }
        val original = field.getBoolean(null)
        try {
            org.crosswire.jsword.book.sword.SwordBookMetaData.setPartialLoading(false)
            for ((encoding, expected) in listOf("UTF-8" to "\ufffd", "utf-8" to "\ufffd", "ISO-8859-1" to "\u00e9", "" to "\u00e9")) {
                val header = "\uFEFF[Demo]\nModDrv=zText\nLang=en\n" + if (encoding.isEmpty()) "" else "Encoding=$encoding\n"
                val bytes = header.toByteArray() + "Description=".toByteArray() + byteArrayOf(0xe9.toByte(), 10)
                val book = org.crosswire.jsword.book.sword.SwordBookMetaData(bytes, "Demo")
                assertEquals(expected, book.getProperty("Description"), encoding)
            }
        } finally {
            org.crosswire.jsword.book.sword.SwordBookMetaData.setPartialLoading(original)
        }
    }

    @Test
    fun projectHomeCopiesResourceArrayAndCreatesSubdirectory() {
        val home = java.nio.file.Files.createTempDirectory("k4-home").toFile()
        val projectCtor = CWProject::class.java.getDeclaredConstructor().apply { isAccessible = true }
        val oldProperty = System.getProperty("jsword.home")
        val oldCache = NetUtilJvm.getURICacheDir()
        try {
            System.setProperty("jsword.home", home.path)
            val project = projectCtor.newInstance()
            assertEquals(home.toURI(), project.writableProjectDir)
            val dirs = project.projectResourceDirs
            dirs[0] = null
            assertEquals(home.toURI(), project.projectResourceDirs[0])
            val child = project.getWritableProjectSubdir("child", true)!!
            kotlin.test.assertTrue(java.io.File(child).isDirectory)
        } finally {
            if (oldProperty == null) System.clearProperty("jsword.home") else System.setProperty("jsword.home", oldProperty)
            NetUtilJvm.setURICacheDir(oldCache)
            setResourceHome(CWProject.instance().projectResourceDirs)
            home.deleteRecursively()
        }
    }
    @Test
    fun writerUsesPlatformSeparatorAndFlushesWithoutClosing() {
        val old = iniLineSeparator
        try {
            iniLineSeparator = "\r\n"
            val output = java.io.StringWriter()
            IniSectionJvm.save(IniSection("Demo").apply { add("A", "one") }, output)
            assertEquals("[Demo]\r\nA = one\r\n", output.toString())
        } finally {
            iniLineSeparator = old
        }
    }

    @Test
    fun liveViewsRemoveIfAlwaysRejectsWithoutEvaluatingPredicate() {
        for (populated in listOf(false, true)) {
            val section = IniSection()
            if (populated) section.add("A", "one")
            val views = listOf(section.keys, section.getValues("A") ?: run {
                section.add("A", "one")
                val view = section.getValues("A")!!
                section.remove("A", "one")
                view
            })
            for (view in views) {
                var called = false
                assertFailsWith<UnsupportedOperationException> {
                    (view as java.util.Collection<String>).removeIf { called = true; false }
                }
                assertEquals(false, called)
            }
        }
    }

    @Test
    fun languageFallbackMatchesJdkDisplayLocaleRatherThanFixedEnglish() {
        val old = java.util.Locale.getDefault(java.util.Locale.Category.DISPLAY)
        try {
            for (locale in listOf(java.util.Locale.GERMAN, java.util.Locale.FRENCH)) {
                java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, locale)
                // JDK fallback is a platform boundary; unlike JSword bundles it uses DISPLAY.
                assertEquals("gsw", Languages.getName("gsw"))
                val expected = java.util.Locale("gsw").displayLanguage
                kotlin.test.assertNotEquals("gsw", expected)
                assertEquals(expected, Language("gsw").name)
            }
        } finally {
            java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, old)
        }
    }
}
