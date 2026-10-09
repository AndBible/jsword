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
            assertSameLines("parse(fi) $v", both("passage.parseAllFi", v, refs))
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
    fun driverScanSurvivesHeaderlessConf() {
        val root = java.nio.file.Files.createTempDirectory("ksword-scan").toFile()
        try {
            File(root, "modules/texts/rawtext/goodb16/").mkdirs()
            File(root, "modules/texts/rawtext/badb16/").mkdirs()
            val mods = File(root, "mods.d").also { it.mkdirs() }
            // A BOM-prefixed first line hides the [Initials] header: the metadata has no name at all.
            File(mods, "a-headerless.conf").writeText("\uFEFFDescription=No header\nDataPath=./modules/texts/rawtext/badb16/\nLang=en\n", Charsets.UTF_8)
            File(mods, "b-good.conf").writeText(
                "[GoodB16]\nDescription=Good\nDataPath=./modules/texts/rawtext/goodb16/\nModDrv=RawText\nLang=en\nEncoding=UTF-8\nVersification=KJV\n",
                Charsets.UTF_8)
            val (a, b) = both("corpus.driver.scan", root.path)
            assertEquals("found=[GoodB16]", a.trim(), "baseline must keep scanning past the header-less conf")
            assertSameLines("driver scan", a to b)
        } finally {
            root.deleteRecursively()
        }
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

    @Test
    fun rtfToOsis() {
        val snippets = listOf(
            "plain text", "", "a\\par b", "a\\par b\\par c", "a\\pard b", "x\\qc centered\\pard after",
            "\\qc one\\par two\\pard three", "\\i italic\\i0 plain", "\\b bold\\b0 plain", "\\i\\b both\\b0\\i0 done",
            "\\i nested \\b deep\\b0 back\\i0 out", "\\u233?cole", "\\u-1234?x", "\\u65?B", "\\u-1?", "\\u0?z", "\\u8364 euro",
            "a\\ b", "a\\\nb", "a\\{b\\}c", "trailing\\", "\\", "\\unknown word", "\\fs20 sized", "{\\b grouped} text",
            "{\\i {\\b nested} groups}", "\\i0 closes nothing", "\\b0\\b0", "\\par", "\\pard", "\\qc", "\\i", "\\b",
            "\\u", "\\ux", "\\u12", "a\\parb", "a\\pard", "\\pard\\pard", "\\qcx", "\\i0", "\\b word\\par next\\b0",
            "caf\\u233? \\i au lait\\i0\\par"
        )
        assertSameLines("rtf", both("corpus.osis.rtf", snippets.joinToString("\u0001")))
    }

    @Test
    fun gbfFilter() {
        val inputs = listOf(
            "plain", "", "  padded \t\n", "<FI>it<Fi>", "<FB>b<Fb>", "<FU>u<Fu>", "<FR>red<Fr>", "<FO>ot<Fo>", "<PP>poem<Pp>",
            "<JR>right", "<JL>left", "<TS>head<Ts>", "<TH>t<Th>", "<TT>book<Tt>", "<TB>ps<Tb>", "a<CM>b", "a<CL>b", "<CM>", "<CL>",
            "a<RF>note<Rf>b", "x<RF><Rf>y", "<RB>x", "<Rf>", "a<Rf>", "beginning<WH7225>", "w<WG3056><WG1234>", "a<WG1>b", "<WG3056>",
            "word<WTN-NSM>", "word<WG1><WTNSM>", "<WTNSM>", "x<WTNSM>", "a<WG1:2>b", "a<WH7225-1>b", "a<WT1>b", "a<WG1 2>b", "a<WG 1>b",
            "<RXGen.1.1>see<Rx>", "<RXzzzz qq>see<Rx>", "<RX>see<Rx>", "a<CG>b", "a<CT>b", "a<JC>b", "a<BI>b", "a<ZZ>b", "a<Zebra>b",
            "a<FI", "abc<FI", "a<b", "a<", "<", ">", "a>", "a>b", "a>b<FI>c", "<<hello>>", "<b>x</b>", "<>", "a<>b", "<F>", "<Fi>", "a<Fi>b",
            "a<Fi><Fi>b", "<FI>abc", "<FI><FB>x<Fb><Fi>y", "a<FB>b<FI>c<Fi>d<Fb>e", "?! x<FI>y<Fi>", ",.;: a<CM>b", " , x<CM>", "a ? b<CM>",
            "Tom &amp; Jerry", "&lt;", "say \"hi\"", "a & b", "\u00E9\u20AC \u0391\u03B3\u03AC\u03C0\u03B7", "<FI>\u00E9\u20AC<Fi>",
            "\u00E9<CM>\u20AC", "a<FI>b<RF>n<WG1>m<Rf>c<Fi>d", "<FI>a<RF>b", "a<WG1><WG2><WTX><WTY>", "\ud83d\ude00<FI>x", "a\nb<CM>\nc", "a<Fb><WG1>", ",. x<WG1>", "?! word<WTNSM>", "a, b<WG1>", " x<WG1>", ", <WG1>", ".,;:<WG1>", ": a b. c<WH1>d<WH2>"
        )
        assertSameLines("gbf", both("corpus.filter", "gbf", "KJV", inputs.joinToString("\u0001")))
    }

    @Test
    fun osisFilter() {
        val inputs = listOf(
            "plain", "", "<w lemma=\"strong:H7225\">In the beginning</w>", "<note type=\"study\">A note</note>",
            "<hi type=\"italic\">a <hi type=\"bold\">b</hi></hi>", "<title type=\"main\" canonical=\"true\">T</title>", "<a/>x",
            "a &amp; b", "1 &lt; 2", "a & b", "a &foo; b", "caf&eacute;", "&nbsp;&copy;&euro;", "&#233;&#x20AC;", "&#1;", "a&b;c",
            "<w>unclosed", "<w>a</x>", "<w>a & b", "</w>text", "<w lemma=\"x>tail", "a\u0001b", "<l>x</l>", "x</l>", "x</lg>", "<lg><l>a</l>",
            "<p>a</p></p>", "<b>x", "<br>x<br>", "<div>a</div><div>b", "<<>>", "<", ">", "<>", "&", "&;", "&&amp;", "<w a=\"1\" a=\"2\">x</w>",
            "\u00E9\u20AC \u0391\u03B3", "<w>\u00E9</w>", "<!-- c -->x", "<![CDATA[x<y]]>", "<?pi x?>y", "<w>a</w>\n<w>b</w>", "a\r\nb",
            "<note>unclosed <hi>deep", "x<w>y</w", "<w lemma='a'>q</w>", "<chapter eID=\"x\"/>", "<verse osisID=\"Gen.1.1\"/>text",
            "<a>1</a></a><b>", "</a>", "</a></b>", "<a></b>", "text</a>", "<a b=>", "<a b=c>x</a>"
        )
        assertSameLines("osis", both("corpus.filter", "osis", "KJV", inputs.joinToString("\u0001")))
    }

    @Test
    fun plainTextFilter() {
        val inputs = listOf(
            "line1\nline2", "", "a", "a\n", "\n", "\n\n", "a\n\nb", "a\r\nb\r\nc", "\r\n", "  a \n\tb  ", "a & b < c", "<b>x</b>",
            "\u00E9\u20AC\n\u0391\u03B3", "a\n\n\n", "\na", "a\rb", "\ud83d\ude00\nx", "x\u0000y"
        )
        assertSameLines("plaintext", both("corpus.filter", "plaintext", "KJV", inputs.joinToString("\u0001")))
    }

    @Test
    fun diffToOsis() {
        val pairs = listOf(
            "same" to "same", "the quick fox" to "the slow fox", "" to "added", "removed" to "", "abc" to "xyz",
            "In the beginning God created" to "In the beginning the LORD created", "a b c d" to "a x c y"
        )
        assertSameLines("diff", both("corpus.osis.diff", pairs.joinToString("\u0001") { it.first + "\u0002" + it.second }))
    }

    @Test
    fun multiBookData() {
        val pairs = listOf("KJV" to "ESV2011", "KJV" to "Geneva", "ESV2011" to "TurNTB", "KJV" to "NASB")
        val refs = listOf("Gen 1:1", "Joh 3:16", "Ps 23", "Rom 8:1-4", "Gen 1:1; Exo 3:2", "Jude 1:3")
        for ((a, b) in pairs) for (r in refs) for (cmp in listOf("true", "false")) {
            val (x, y) = both("corpus.bookdata.compare", a, b, r, cmp)
            assertEquals(x, y, "bookdata.compare $a $b $r compare=$cmp")
        }
    }

    @Test
    fun osisFrameworkAndVerse() {
        for (m in Oracle.call("corpus.modules").lines().filter { it.isNotBlank() }) assertSameLines("framework $m", both("corpus.osis.framework", m))
        for (v in listOf("KJV", "Synodal", "Catholic")) assertSameLines("verse $v", both("corpus.osis.verse", v))
    }

    @Test
    fun defaultBook() {
        assertSameLines("defaultbook", both("corpus.defaultbook"))
    }
}
