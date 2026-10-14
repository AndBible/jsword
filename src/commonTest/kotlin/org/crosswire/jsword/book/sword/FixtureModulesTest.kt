package org.crosswire.jsword.book.sword

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.*
import kotlin.time.TimeSource
import okio.FileSystem
import org.crosswire.common.util.KCharset
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.pathUri
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.passage.DefaultLeafKeyList
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.VerseFactory
import org.crosswire.jsword.versification.system.Versifications
import org.crosswire.ksword.fixtures.fixtureEnvironment
import org.crosswire.ksword.fixtures.fixtureRoot
import org.crosswire.ksword.fixtures.isJvm

@OptIn(ExperimentalEncodingApi::class)
class FixtureModulesTest {
    private val moduleNames = setOf("K4RawText", "K4RawText4", "K4ZTextBook", "K4ZTextChapterLzss",
        "K4ZText4Verse", "K4Cipher", "K4Latin1", "K4Combining", "K4RawCom", "K4ZComChapter",
        "K4RawLD", "K4RawLD4", "K4ZLD", "K4Devotion", "K4GenBook")
    private val verseModules = moduleNames - setOf("K4RawLD", "K4RawLD4", "K4ZLD", "K4Devotion", "K4GenBook")
    private data class Entry(val name: String, val text: String)

    private fun manifest(): Map<String, List<Entry>> {
        val rows = FileSystem.SYSTEM.read(fixtureRoot() / "manifest.tsv") { readUtf8() }
            .lineSequence().filter { it.isNotBlank() }.map {
                val columns = it.split('\t')
                assertEquals(3, columns.size, "manifest row width")
                columns[0] to Entry(columns[1], Base64.decode(columns[2]).decodeToString())
            }.toList()
        assertEquals(41, rows.size, "frozen manifest data rows (43 facade-index keys include two GenBook parents)")
        assertEquals(moduleNames, rows.map { it.first }.toSet())
        return rows.groupBy({ it.first }, { it.second })
    }

    private fun metadata(initials: String): SwordBookMetaData {
        val root = fixtureRoot() / "space dir/ä"
        val conf = root / "mods.d/$initials.conf"
        return SwordBookMetaData(FileSystem.SYSTEM.read(conf) { readByteArray() }, "$initials.conf").apply {
            library = pathUri(root)
            // Buffer metadata is not installed; resolve its declared DataPath explicitly.
            location = NetUtil.lengthenURI(library!!, getProperty(SwordBookMetaData.KEY_DATA_PATH)!!)
        }
    }

    private fun backend(md: SwordBookMetaData): Backend<*> = if (md.initials == "K4RawText4") {
        assertNull(md.bookType, "RawText4 remains unregistered, just as in the oracle")
        RawBackend<org.crosswire.jsword.book.sword.state.RawBackendState>(md, 4)
    } else checkNotNull(md.bookType).getBackendForTest(md)

    private fun key(md: SwordBookMetaData, backend: Backend<*>, name: String): Key = when (md.initials) {
        in verseModules -> checkNotNull(VerseFactory.fromString(
            Versifications.instance().getVersification(md.getProperty(BookMetaData.KEY_VERSIFICATION)), name))
        "K4GenBook" -> backend.readIndex()!!.single { it.getName() == name }
        else -> DefaultLeafKeyList(name)
    }

    @Test fun everyManifestEntryReadsThroughItsRealBackend() {
        val expected = manifest()
        val confs = FileSystem.SYSTEM.list(fixtureRoot() / "space dir/ä/mods.d")
        assertEquals(moduleNames, confs.filter { it.name.endsWith(".conf") }.map { it.name.removeSuffix(".conf") }.toSet())
        var reads = 0
        for ((initials, entries) in expected) {
            val md = metadata(initials)
            val backend = backend(md)
            for (entry in entries) {
                val key = key(md, backend, entry.name)
                assertEquals(entry.text, backend.getRawText(key), "$initials ${entry.name}")
                val length = when (initials) {
                    "K4Latin1" -> KCharset.encode(entry.text, "windows-1252").size
                    "K4ZLD" -> entry.name.encodeToByteArray().size + 1 + 8
                    else -> entry.text.encodeToByteArray().size
                }
                assertEquals(length, backend.getRawTextLength(key), "$initials ${entry.name} raw length")
                reads++
            }
        }
        assertEquals(41, reads)
    }

    @Test fun verseHolesAndEndOfModuleReadExactlyEmpty() {
        var reads = 0
        for (initials in verseModules) {
            val md = metadata(initials)
            val backend = backend(md)
            for (reference in listOf("Gen.1.4", "John.1.4", "Rev.22.21")) {
                val key = key(md, backend, reference)
                val text = backend.getRawText(key)
                assertEquals("", text, "$initials $reference")
                assertContentEquals(byteArrayOf(), text.encodeToByteArray(), "$initials $reference bytes")
                assertEquals(0, backend.getRawTextLength(key), "$initials $reference length")
                reads++
            }
        }
        assertEquals(30, reads)
    }

    @Test fun genBookUsesRealHierarchicalIndexKeys() {
        val md = metadata("K4GenBook")
        val backend = backend(md)
        val keys = backend.readIndex()!!.toList()
        fun path(key: Key): String {
            val parent = key.getParent()
            return if (key.getName().isEmpty()) "" else (parent?.let { path(it) } ?: "") + "/" + key.getName()
        }
        assertEquals(listOf("", "/Part 1", "/Part 1/Chapter 1", "/Part 1/Chapter 2", "/Part 2"), keys.map { path(it) })
        assertEquals("", backend.getRawText(keys.single { path(it) == "/Part 1" }))
        assertEquals(0, backend.getRawTextLength(keys.single { path(it) == "/Part 1" }))
    }

    @Test fun optionalIosSyntheticTimingProbe() {
        if (isJvm || fixtureEnvironment("K4_TIMING") != "1") return
        for ((initials, entries) in manifest()) {
            val md = metadata(initials)
            val backend = backend(md)
            val keys = entries.map { key(md, backend, it.name) }
            val start = TimeSource.Monotonic.markNow()
            repeat(20) {
                entries.forEachIndexed { index, entry ->
                    assertEquals(entry.text, backend.getRawText(keys[index]), "$initials ${entry.name}")
                }
            }
            println("K4TIMING-IOS $initials ${start.elapsedNow().inWholeMilliseconds}")
        }
    }
}
