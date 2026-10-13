package org.crosswire.jsword.book.sword

import java.nio.charset.Charset
import java.util.Locale
import org.crosswire.common.crypt.Sapphire
import org.crosswire.jsword.book.sword.state.RawLDBackendState
import org.junit.Assert.*
import org.junit.Test

/** Fixed expectations and frozen JDK operations characterize the pre-port backend. */
class BackendCommonReadyTest {
    private fun metadata(extra: String = "") = SwordBookMetaData(
        ("[Probe]\nModDrv=RawLD\nLang=en\n" + extra).toByteArray(), "probe.conf"
    )

    private open class Probe(md: SwordBookMetaData) : RawLDBackend<RawLDBackendState>(md, 2) {
        fun raw(entry: DataEntry) = getRawText(entry)
    }

    private fun external(backend: Probe, key: String, pattern: String): String =
        RawLDBackend::class.java.getDeclaredMethod("external2internal", String::class.java, String::class.java)
            .apply { isAccessible = true }.invoke(backend, key, pattern) as String

    @Test fun cipherUsesUtf8AndNamedCharsetWithUnknownFallback() {
        val key = "k4-Ä-key"
        val utf8 = byteArrayOf(107, 52, 45, -61, -124, 45, 107, 101, 121)
        assertArrayEquals(utf8, key.toByteArray(Charset.defaultCharset()))
        val input = ByteArray(32) { it.toByte() }
        val reference = input.copyOf()
        val engine = Sapphire(utf8)
        for (i in reference.indices) reference[i] = engine.cipher(reference[i])
        engine.burn()
        val backend = Probe(metadata().apply { setProperty(SwordBookMetaData.KEY_CIPHER_KEY, key) })
        backend.decipher(input)
        assertArrayEquals(reference, input)
        for ((charset, expected) in listOf(
            "UTF-8" to utf8,
            "ISO-8859-1" to byteArrayOf(107, 52, 45, -60, 45, 107, 101, 121),
            "not-a-charset" to utf8
        )) {
            var received: ByteArray? = null
            val entry = object : DataEntry("probe", byteArrayOf(), "UTF-8") {
                override fun getRawText(cipherKey: ByteArray?): String {
                    received = cipherKey
                    return "captured"
                }
            }
            val md = metadata().apply { setProperty(SwordBookMetaData.KEY_CIPHER_KEY, key) }
            val named = object : Probe(md) {
                override val bookMetaData: org.crosswire.jsword.book.BookMetaData =
                    object : org.crosswire.jsword.book.BookMetaData by md {
                        override val bookCharset: String = charset
                    }
            }
            assertEquals("captured", named.raw(entry))
            assertArrayEquals(expected, received)
        }
    }

    @Test fun strongsPaddingAndRegexPreserveExistingBranches() {
        val original = Locale.getDefault(Locale.Category.FORMAT)
        try {
        Locale.setDefault(Locale.Category.FORMAT, Locale.US)
        val backend = Probe(metadata("Feature=GreekDef\nStrongsPadding=true\n"))
        assertEquals("G00025", external(backend, "G25", "G00001"))
        assertEquals("H08674", external(backend, "H8674", "H00001"))
        assertEquals("G00025", external(backend, "G25!a", "G00001"))
        for (key in listOf("g25", "G-25", "G25A", "G25\n", "G", "i")) {
            assertEquals(key, external(backend, key, "G00001"))
        }
        assertEquals("00025", external(backend, "G25", "25"))
        } finally { Locale.setDefault(Locale.Category.FORMAT, original) }
    }

    @Test fun paddingPreservesJdkDefaultLocaleDigitsAndGreedyPattern() {
        val original = Locale.getDefault(Locale.Category.FORMAT)
        try {
            for (locale in listOf("en-US", "ar", "fa", "bn", "th-TH-u-nu-thai")) {
                Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag(locale))
                val backend = Probe(metadata("Feature=GreekDef\nStrongsPadding=true\n"))
                for (number in listOf(0, 1, 25, 8674, Int.MAX_VALUE)) {
                    val key = "G$number"
                    val matcher = java.util.regex.Pattern.compile("^([GH])(\\d+)((!)?([a-z])?)$").matcher("G00001")
                    assertTrue(matcher.matches())
                    val reference = java.text.DecimalFormat(if (matcher.group(2).length == 4) "0000" else "00000")
                        .format(number.toLong())
                    assertEquals(locale, "G$reference", external(backend, key, "G00001"))
                    assertEquals(locale, java.text.DecimalFormat("00000").format(number.toLong()),
                        external(backend, key, "25"))
                }
                // The frozen regex captures all four digits and selects four-place padding.
                assertEquals("H" + java.text.DecimalFormat("0000").format(8674L),
                    external(backend, "H8674", "H0001"))
                for (key in listOf("G-1", "G25\n", "G25A", "G٢٥")) {
                    assertEquals(key, external(backend, key, "G00001"))
                }
                try {
                    external(backend, "G2147483648", "G00001")
                    fail("overflow must retain NumberFormatException")
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    assertTrue(e.cause is NumberFormatException)
                }
            }
        } finally { Locale.setDefault(Locale.Category.FORMAT, original) }
    }

    @Test fun devotionInternalPatternRetainsFullMatchAndOutOfRangeAcceptance() {
        val backend = Probe(metadata("Category=Daily Devotional\n"))
        assertEquals(org.crosswire.jsword.book.BookCategory.DAILY_DEVOTIONS, backend.bookMetaData.bookCategory)
        val frozen = java.util.regex.Pattern.compile("^\\d\\d\\.\\d\\d$")
        for (key in listOf("01.01", "99.99", "00.00", "1.01", "01.01\n", "x01.01", "٠١.٠١")) {
            if (frozen.matcher(key).matches()) {
                assertEquals(key, external(backend, key, ""))
            } else {
                // Execute the old date-parser branch independently, including its error.
                val reference = runCatching {
                    val calendar = java.util.GregorianCalendar()
                    val formatter = org.crosswire.common.icu.DateFormatter.getDateInstance()
                    formatter.setLenient(true)
                    calendar.time = formatter.parse(key)
                    org.crosswire.common.util.MessageFormatLite.format("{0,number,00}.{1,number,00}",
                        org.crosswire.common.util.KswordLocale.default(), arrayOf<Any>(
                            1 + calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DATE)))
                }
                val actual = runCatching { external(backend, key, "") }
                if (reference.isSuccess) assertEquals(key, reference.getOrThrow(), actual.getOrThrow())
                else assertEquals(key, reference.exceptionOrNull()!!::class,
                    (actual.exceptionOrNull() as java.lang.reflect.InvocationTargetException).targetException::class)
            }
        }
    }

    @Test fun bothFeaturesNaslexSuffixAndDisabledPaddingRetainOldBranches() {
        val original = Locale.getDefault(Locale.Category.FORMAT)
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ar"))
            val both = Probe(metadata("Feature=GreekDef\nFeature=HebrewDef\nStrongsPadding=true\n"))
            assertEquals("G" + java.text.DecimalFormat("0000").format(25L), external(both, "G25!a", "G00001"))
            val nasMd = SwordBookMetaData("[naslex]\nModDrv=RawLD\nFeature=GreekDef\nStrongsPadding=true\n".toByteArray(), "nas.conf")
            val nas = object : Probe(nasMd) {
                override val bookMetaData: org.crosswire.jsword.book.BookMetaData =
                    object : org.crosswire.jsword.book.BookMetaData by nasMd {
                        override val initials = "naslex"
                    }
            }
            assertEquals("G" + java.text.DecimalFormat("00000").format(25L) + "a", external(nas, "G25!a", "G00001"))
            val unpadded = Probe(metadata("Feature=GreekDef\n").apply {
                setProperty(SwordBookMetaData.KEY_STRONGS_PADDING, "false")
            })
            // The frozen implementation returns the suffix-stripped key, not its unpadded buffer.
            for (key in listOf("G00025", "G00025a", "G00025!a", "H00001")) {
                val reference = key.removeSuffix("a").removeSuffix("!")
                assertEquals(reference, external(unpadded, key, "G00001"))
            }
        } finally { Locale.setDefault(Locale.Category.FORMAT, original) }
    }

    @Test fun debugOutputPreservesHeaderAndEmptyImp() {
        val backend = object : Probe(metadata("DataPath=./missing\n").apply {
            library = java.io.File(System.getProperty("java.io.tmpdir")).toURI()
        }) {
            override fun initState() = RawLDBackendState(bookMetaData)
            override fun getCardinality() = 0
        }
        val output = StringBuilder()
        backend.dumpIdxRaf(output)
        backend.toIMP(output)
        assertEquals("index\toffset\tsize\tkey\tvalue\n", output.toString())
    }

    @Test fun clonePreservesUnknownRuntimeSubclassAndShallowFields() {
        val reference = mutableListOf("shared")
        val backend = object : Probe(metadata()) { val extra = reference }
        val clone = backend.clone()
        assertNotSame(backend, clone)
        assertEquals(backend.javaClass, clone.javaClass)
        assertSame(backend.bookMetaData, clone.bookMetaData)
        val field = backend.javaClass.getDeclaredField("extra").apply { isAccessible = true }
        assertSame(reference, field.get(clone))
        assertTrue(clone is java.lang.Cloneable)
    }

    @Test fun knownPortableBackendCopiesPreserveBaseAndSizingFields() {
        for (backend in listOf(RawLDBackend<RawLDBackendState>(metadata(), 2), ZLDBackend(metadata()))) {
            backend.setIgnoreReadErrors(false)
            val method = backend.javaClass.getDeclaredMethod("portableBackendClone").apply { isAccessible = true }
            val copy = method.invoke(backend) as AbstractKeyBackend<*>
            assertEquals(backend.javaClass, copy.javaClass)
            assertSame(backend.bookMetaData, copy.bookMetaData)
            for (name in listOf("datasize", "entrysize")) {
                val field = RawLDBackend::class.java.getDeclaredField(name).apply { isAccessible = true }
                assertEquals(field.get(backend), field.get(copy))
            }
            val ignore = AbstractBackend::class.java.getDeclaredField("ignoreReadErrors").apply { isAccessible = true }
            assertEquals(false, ignore.get(copy))
        }
    }

    @Test fun searchUppercaseIgnoresTurkishDefaultLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val backend = Probe(metadata())
            val method = RawLDBackend::class.java.getDeclaredMethod("normalizeForSearch", String::class.java)
                .apply { isAccessible = true }
            assertEquals("I", method.invoke(backend, "i"))
            assertEquals("STRASSE", method.invoke(backend, "straße"))
        } finally { Locale.setDefault(original) }
    }
}
