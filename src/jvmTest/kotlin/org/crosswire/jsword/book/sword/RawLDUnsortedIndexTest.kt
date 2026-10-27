package org.crosswire.jsword.book.sword

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import org.crosswire.jsword.book.sword.state.RawLDBackendState
import org.crosswire.jsword.passage.DefaultLeafKeyList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #3148: user-made TEI lexica (e.g. BauerAland) have an index that is not sorted the way
 * the binary search expects: Strong's-number aliases (`00004` -> `@LINK άβαρής`) come first,
 * lower-case accented Greek lemmata sort differently once upper-cased, and some keys are
 * duplicated as a self link (`A` -> `@LINK A`). Every listed key must still be readable.
 */
class RawLDUnsortedIndexTest {
    private val root: File = Files.createTempDirectory("rawld-unsorted").toFile()
    private companion object {
        var modules = 0
    }

    @After fun cleanUp() {
        root.deleteRecursively()
    }

    /** Writes a RawLD4 module with the entries in the given (unsorted) order. */
    private fun module(vararg entries: Pair<String, String>): RawLDBackend<RawLDBackendState> {
        // A fresh directory per module: open file states are pooled per book.
        val name = "probe${modules++}"
        val dir = File(root, "modules/lexdict/rawld/$name").apply { mkdirs() }
        val idx = ByteArrayOutputStream()
        val dat = ByteArrayOutputStream()
        for ((key, body) in entries) {
            val bytes = "$key\r\n$body".toByteArray(Charsets.UTF_8)
            for (value in listOf(dat.size(), bytes.size)) {
                idx.write(byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte()))
            }
            dat.write(bytes)
        }
        File(dir, "dict.idx").writeBytes(idx.toByteArray())
        File(dir, "dict.dat").writeBytes(dat.toByteArray())
        val md = SwordBookMetaData(
            ("[$name]\nDataPath=./modules/lexdict/rawld/$name/dict\nModDrv=RawLD4\nEncoding=UTF-8\n" +
                "SourceType=TEI\nFeature=GreekDef\n").toByteArray(),
            "probe.conf",
        ).apply { library = root.toURI() }
        return RawLDBackend(md, 4)
    }

    private val entries = arrayOf(
        "00004" to "@LINK άβαρής",
        "00009" to "@LINK Άβιληνη",
        "A" to "@LINK A",
        "02229" to "@LINK ή",
        "Iaomai" to "@LINK ίάομαι",
        "A" to "<entryFree n=\"A|06000|A\">A</entryFree>",
        "άβαρής" to "<entryFree n=\"άβαρής|00004|abares\">abares</entryFree>",
        "Άβιληνη" to "<entryFree n=\"Άβιληνη|00009|Abilene\">Abilene</entryFree>",
        "ή" to "<entryFree n=\"ή|02229|e\">e</entryFree>",
        "ίάομαι" to "<entryFree n=\"ίάομαι|02390|iaomai\">iaomai</entryFree>",
        "Ισχυρός" to "<entryFree n=\"Ισχυρός|02478|ischyros\">ischyros</entryFree>",
    )

    @Test fun everyListedKeyOfAnUnsortedIndexIsReadable() {
        val backend = module(*entries)
        assertEquals(entries.size, backend.getCardinality())
        for (i in 0 until backend.getCardinality()) {
            val key = backend.get(i)
            val text = backend.getRawText(key)
            assertTrue("${key.getName()} -> $text", text.contains("<entryFree"))
        }
    }

    @Test fun aliasResolvesToItsLemmaEntry() {
        val backend = module(*entries)
        assertTrue(backend.getRawText(DefaultLeafKeyList("00004")).contains("abares"))
        assertTrue(backend.getRawText(DefaultLeafKeyList("02229")).contains("|02229|"))
        assertTrue(backend.getRawText(DefaultLeafKeyList("A")).contains("|06000|"))
    }

    @Test fun linkCycleEndsInsteadOfOverflowingTheStack() {
        val backend = module("A" to "@LINK B", "B" to "@LINK A")
        val error = runCatching { backend.getRawText(DefaultLeafKeyList("A")) }.exceptionOrNull()
        assertTrue("$error", error !is StackOverflowError)
    }
}
