package org.crosswire.ksword.parity

import java.io.File
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FixtureParityTest {
    private val fixtures = File(System.getProperty("ksword.fixturesDir") ?: "src/commonTest/resources/k4-modules")
    private val root = File(fixtures, "space dir/ä")

    private fun manifest(): Map<String, List<Pair<String, String>>> =
        File(fixtures, "manifest.tsv").readLines().filter { it.isNotBlank() }
            .map { it.split('\t') }.groupBy({ it[0] }, {
                it[1] to String(Base64.getDecoder().decode(it[2]), Charsets.UTF_8)
            })

    private fun compare(initials: String, expected: List<Pair<String, String>>, oracle: String, current: String) {
        assertEquals(oracle, current, "oracle vs current: $initials")
        val empties = oracle.lineSequence().filter { it.startsWith("empty\t") }.map { it.split('\t') }.toList()
        empties.forEach { assertEquals("", it[2], "$initials missing ${it[1]}"); assertEquals("0", it[3]) }
        val lines = oracle.lineSequence().filter { '\t' in it && it.substringBefore('\t').toIntOrNull() != null }.map { it.split('\t') }.toList()
        assertEquals("count=${lines.size}", oracle.lineSequence().single { it.startsWith("count=") })
        lines.forEachIndexed { index, row ->
            assertEquals(index.toString(), row[0])
            assertEquals(4, row.size, "raw length must be probed")
            assertTrue(row[3].toInt() >= 0)
        }
        val manifestLines = if (initials == "K4Devotion") {
            val dates = listOf(1 to 1, 2 to 29, 12 to 31)
            lines.forEachIndexed { i, row ->
                val calendar = java.util.GregorianCalendar().apply {
                    set(java.util.Calendar.MONTH, dates[i].first - 1)
                    set(java.util.Calendar.DAY_OF_MONTH, dates[i].second)
                }
                assertEquals(java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, java.util.Locale.US).format(calendar.time), row[1])
                if (i == 1 && calendar.get(java.util.Calendar.MONTH) == 2) {
                    assertTrue(row[2].startsWith("!"), "non-leap current-year facade lookup fails")
                } else {
                    assertEquals(expected[i].second, String(Base64.getDecoder().decode(row[2]), Charsets.UTF_8))
                }
            }
            oracle.lineSequence().filter { it.startsWith("internal\t") }.map { it.split('\t') }.toList()
        } else if (initials == "K4GenBook") {
            assertEquals(listOf("", "/Part 1", "/Part 1/Chapter 1", "/Part 1/Chapter 2", "/Part 2"),
                oracle.lineSequence().filter { it.startsWith("tree\t") }.map { it.substringAfter('\t') }.toList())
            val root = lines.single { it[1].isEmpty() }
            assertTrue(root[2].startsWith("!org.crosswire.jsword.book.BookException: No entry for ''"))
            lines.filter { it[1].isNotEmpty() }
        } else lines
        manifestLines.forEach { assertTrue(!it[2].startsWith("!"), "$initials ${it[1]}: ${it[2]}") }
        val dumped = manifestLines.map { it[1] to String(Base64.getDecoder().decode(it[2]), Charsets.UTF_8) }
            .filter { it.second.isNotEmpty() }
        assertEquals(expected, dumped, "oracle vs manifest: $initials")
        manifestLines.filter { it[2].isNotEmpty() }.forEach { row ->
            val raw = String(Base64.getDecoder().decode(row[2]), Charsets.UTF_8)
            val expectedLength = when (initials) {
                "K4Latin1" -> raw.toByteArray(java.nio.charset.Charset.forName("windows-1252")).size
                "K4ZLD" -> row[1].toByteArray().size + 1 + 8 // index payload is the block locator, not decompressed text
                else -> raw.toByteArray().size
            }
            assertEquals(expectedLength, row[3].toInt(), "$initials ${row[1]} raw length")
        }
    }

    @Test fun oracleCurrentAndManifestAgreeOnEveryFixture() {
        assertTrue(File(fixtures, "manifest.tsv").isFile, "generated fixture manifest is required")
        val expected = manifest()
        val names = setOf("K4RawText", "K4RawText4", "K4ZTextBook", "K4ZTextChapterLzss", "K4ZText4Verse",
            "K4Cipher", "K4Latin1", "K4Combining", "K4RawCom", "K4ZComChapter", "K4RawLD", "K4RawLD4",
            "K4ZLD", "K4Devotion", "K4GenBook")
        assertEquals(names, expected.keys)
        val confs = File(root, "mods.d").listFiles()!!.filter { it.extension == "conf" }.sortedBy { it.name }
        assertEquals(names, confs.map { it.nameWithoutExtension }.toSet())
        var keys = 0
        for (conf in confs) {
            val oracle = Oracle.call("fixture.dump", conf.path)
            compare(conf.nameWithoutExtension, expected.getValue(conf.nameWithoutExtension), oracle,
                Current.call("fixture.dump", conf.path))
            keys += oracle.lineSequence().count { it.substringBefore('\t').toIntOrNull() != null }
        }
        println("K4FIXTURES modules=${confs.size} keys=$keys")
    }

    @Test fun aPlantedDifferenceIsDetected() {
        val conf = File(root, "mods.d/K4RawText.conf")
        val oracle = Oracle.call("fixture.dump", conf.path)
        val row = oracle.lines().first { it.split('\t').getOrNull(2)?.isNotEmpty() == true && '\t' in it }
        val columns = row.split('\t').toMutableList()
        columns[2] = Base64.getEncoder().encodeToString("planted difference".toByteArray())
        val altered = oracle.replace(row, columns.joinToString("\t"))
        val differential = assertFailsWith<AssertionError> {
            compare("K4RawText", manifest().getValue("K4RawText"), oracle, altered)
        }
        val independent = assertFailsWith<AssertionError> {
            compare("K4RawText", manifest().getValue("K4RawText"), altered, altered)
        }
        println("K4MUTATION differential=${differential.message!!.substringBefore(':')} manifest=${independent.message!!.substringBefore(':')}")
    }
}
