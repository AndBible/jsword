package org.crosswire.ksword.parity

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Oracle vs current over real modules, a passage corpus, all versifications, confs and serialization
 * (KSword K1 spec §4.2). Line-based outputs are compared line by line; the first 20 mismatches are
 * reported with both sides.
 */
class CorpusParityTest {
    /** Every n-th key per module is checked. 1 = all keys; raise only if parityTest exceeds 4 minutes (record the timing here).
     * Measured at stride 1: whole `:jsword:parityTest` about 50 s wall (everyKeyOfEveryModule ~40 s), so stride stays 1. */
    private val keyStride = 1

    private val versifications = listOf("KJV", "KJVA", "NRSV", "Synodal", "Leningrad", "German", "Vulg", "LXX", "Catholic", "Luther")

    private fun refs(): String = javaClass.getResource("/ksword/parity/refs.txt")!!.readText()
        .lines().filter { it.isNotEmpty() }.joinToString("\n") { if (it == "<empty>") "" else it }

    private fun both(op: String, vararg args: String): Pair<String, String> = Oracle.call(op, *args) to Current.call(op, *args)

    private fun assertSameLines(what: String, pair: Pair<String, String>, detail: ((String) -> String)? = null) {
        val (a, b) = pair
        if (a == b) return
        val al = a.lines()
        val bl = b.lines()
        val report = StringBuilder("$what differs (oracle ${al.size} lines, current ${bl.size} lines):\n")
        var shown = 0
        for (i in 0 until maxOf(al.size, bl.size)) {
            val x = al.getOrNull(i)
            val y = bl.getOrNull(i)
            if (x == y) continue
            report.append("  line $i\n    oracle : $x\n    current: $y\n")
            if (detail != null && x != null) report.append(detail(x))
            if (++shown == 20) break
        }
        fail(report.toString())
    }

    @Test
    fun sameModulesAndMetadata() {
        val (a, b) = both("corpus.modules")
        assertEquals(a, b)
        val modules = a.lines().filter { it.isNotBlank() }
        assertTrue(modules.size >= 9, "expected the test modules in ~/.sword, got $modules")
        for (m in modules) assertSameLines("meta $m", both("corpus.meta", m))
    }

    @Test
    fun everyKeyOfEveryModule() {
        for (m in Oracle.call("corpus.modules").lines().filter { it.isNotBlank() }) {
            assertSameLines("keys $m", both("corpus.keys", m, keyStride.toString())) { line ->
                val index = line.substringBefore('\t')
                if (index.toIntOrNull() == null) "" else
                    "    oracle full : ${Oracle.call("corpus.keyAt", m, index).take(2000)}\n" +
                        "    current full: ${Current.call("corpus.keyAt", m, index).take(2000)}\n"
            }
            assertSameLines("chapters $m", both("corpus.chapters", m))
        }
    }

    @Test
    fun passageParsing() {
        val refs = refs()
        assertTrue(refs.lines().size >= 2000, "refs.txt must have at least 2000 lines")
        for (v in versifications) {
            assertSameLines("parse $v", both("passage.parseAll", v, refs))
            assertSameLines("tally $v", both("passage.tally", v, refs))
            assertSameLines("mapKjv $v", both("v11n.mapKjv", v, refs))
        }
    }

    @Test
    fun versificationTables() {
        val (a, b) = both("v11n.names")
        assertEquals(a, b)
        for (v in a.lines().filter { it.isNotBlank() }) assertSameLines("v11n $v", both("v11n.dump", v))
    }

    @Test
    fun confParsing() {
        val projectDir = System.getProperty("ksword.projectDir") ?: error("ksword.projectDir is not set")
        val confs = (File(System.getProperty("user.home"), ".sword/mods.d").listFiles().orEmpty().toList() +
            File(projectDir, "src/jvmTest").walkTopDown().filter { it.isFile }.toList())
            .filter { it.name.endsWith(".conf") }.sortedBy { it.path }
        assertTrue(confs.size >= 9, "expected the test module confs, got $confs")
        for (c in confs) assertSameLines("conf ${c.path}", both("conf.parse", c.path))
    }

    @Test
    fun serializationBothDirections() {
        val kinds = listOf("Rocket", "Bitwise", "Distinct", "Ranged", "Tally", "Verse", "VerseRange")
        val refs = listOf("Gen 1:1-5; Exo 2", "Mat 5-7, 10:1; Rev 22", "Ps 119", "Jude 3")
        for (k in kinds) for (r in refs) {
            val fromOracle = Oracle.call("ser.write", k, "KJV", r)
            val fromCurrent = Current.call("ser.write", k, "KJV", r)
            assertEquals(Oracle.call("ser.read", fromOracle), Current.call("ser.read", fromOracle), "$k $r oracle→current")
            assertEquals(Current.call("ser.read", fromCurrent), Oracle.call("ser.read", fromCurrent), "$k $r current→oracle")
        }
    }
}
