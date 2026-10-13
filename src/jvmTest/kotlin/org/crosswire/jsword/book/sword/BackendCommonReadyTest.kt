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
        val backend = Probe(metadata("Feature=GreekDef\nStrongsPadding=true\n"))
        assertEquals("G00025", external(backend, "G25", "G00001"))
        assertEquals("H08674", external(backend, "H8674", "H00001"))
        assertEquals("G00025", external(backend, "G25!a", "G00001"))
        for (key in listOf("g25", "G-25", "G25A", "G25\n", "G", "i")) {
            assertEquals(key, external(backend, key, "G00001"))
        }
        assertEquals("00025", external(backend, "G25", "25"))
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
