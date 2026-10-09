package spike.xml

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import spike.xml.ksoup.KsoupArm

class SpikeExperimentsTest {
    private class Tally { var equal = 0; var different = 0; var armOnlyRejects = 0; var refOnlyRejects = 0; var bothReject = 0; var armThrows = 0
        val diffs = ArrayList<String>() }

    private fun clip(s: String?) = if (s == null) "<rejected>" else if (s.length > 500) s.take(500) + "...(${s.length} chars)" else s

    enum class Verdict { EQUAL, DIFFERENT, ARM_ONLY_REJECTS, REF_ONLY_REJECTS, BOTH_REJECT }

    /** The comparison used by the experiments (and by the anti-vacuity self-test below). */
    private fun classify(ref: String?, got: String?): Verdict = when {
        ref == null && got == null -> Verdict.BOTH_REJECT
        ref == null -> Verdict.REF_ONLY_REJECTS
        got == null -> Verdict.ARM_ONLY_REJECTS
        ref == got -> Verdict.EQUAL
        else -> Verdict.DIFFERENT
    }

    /** Malformation probes: (label, input). Cheap extra coverage beyond the 7 real rejects seen in the modules. */
    private val probes = listOf(
        "unclosed root" to "<r><a>x</a>",
        "stray &" to "<r>fish & chips</r>",
        "undefined entity" to "<r>&nbsp;</r>",
        "bad attribute quoting" to "<r a=1>x</r>",
        "unterminated attribute value" to "<r a=\"1>x</r>",
        "mis-nested tags" to "<r><a><b></a></b></r>",
        "end tag only" to "<r></r></r>",
        "text after root" to "<r/>junk",
        "two roots" to "<r/><s/>",
        "duplicate attribute" to "<r a=\"1\" a=\"2\"/>",
        "lt in text" to "<r>1 < 2</r>",
        "empty input" to "",
        "undeclared prefix" to "<p:r/>",
        "valid control" to "<r a=\"1\"><s>t</s>&amp;&lt;<![CDATA[x<y]]></r>")

    private fun firstDiff(a: String, b: String): String {
        val i = a.indices.firstOrNull { it >= b.length || a[it] != b[it] } ?: a.length
        fun ctx(s: String) = s.substring(maxOf(0, i - 40), minOf(s.length, i + 40)).replace("\n", "\\n").replace("\r", "\\r")
        return "at $i\n  jdom: ${ctx(a)}\n  kdom: ${ctx(b)}"
    }

    private fun median(l: List<Long>) = l.sorted()[l.size / 2]

    private fun runArm(arm: XmlArm) {
        val inputs = Reference.osisInputs() + Reference.thmlInputs()
        assertTrue(inputs.isNotEmpty(), "no inputs: ~/.sword has no OSIS/ThML modules")
        val tallies = LinkedHashMap<String, Tally>()
        for (inp in inputs) {
            val t = tallies.getOrPut(inp.group) { Tally() }
            val ref = Reference.referenceSerialize(inp.xml)
            var armThrew = false
            val got = try { arm.parseAndSerialize(inp.xml) } catch (e: Throwable) { armThrew = true; t.armThrows++; null }
            if (!armThrew) when (classify(ref, got)) {
                Verdict.BOTH_REJECT -> t.bothReject++
                Verdict.REF_ONLY_REJECTS -> t.refOnlyRejects++.also { if (t.diffs.size < 20) t.diffs.add("${inp.key} [reference rejects only]\n  arm: ${clip(got)}\n  input: ${clip(inp.xml)}") }
                Verdict.ARM_ONLY_REJECTS -> t.armOnlyRejects++.also { if (t.diffs.size < 20) t.diffs.add("${inp.key} [arm rejects only]\n  ref: ${clip(ref)}\n  input: ${clip(inp.xml)}") }
                Verdict.EQUAL -> t.equal++
                Verdict.DIFFERENT -> t.different++.also { if (t.diffs.size < 20) t.diffs.add("${inp.key} [different]\n  ref: ${clip(ref)}\n  arm: ${clip(got)}") }
            }
        }

        // timing: parse + canonical serialize over all inputs, 3 warm-up passes, 3 measured, median
        fun time(f: (String) -> Any?): Long {
            repeat(3) { inputs.forEach { f(it.xml) } }
            return median((1..3).map { val s = System.nanoTime(); inputs.forEach { f(it.xml) }; (System.nanoTime() - s) / 1_000_000 })
        }
        val refMs = time { Reference.referenceSerialize(it) }
        val armMs = time { try { arm.parseAndSerialize(it) } catch (e: Throwable) { null } }
        val chars = inputs.sumOf { it.xml.length.toLong() }

        // EPUB
        val docs = Reference.epubDocs()
        val refEpub = Reference.referenceEpub(docs)
        val armEpub = arm.epubQueries(docs) // an arm exception fails the test (no silent "different")
        assertEquals(refEpub.keys, armEpub.keys, "arm answered a different query set than the reference")
        var eqQ = 0; var diffQ = 0
        val epubRows = StringBuilder()
        for ((id, r) in refEpub) {
            val a = armEpub[id]
            val same = a == r
            if (same) eqQ++ else diffQ++
            epubRows.append("| $id | ${if (same) "equal" else "**different**"} | ${r.size} | ${a?.size ?: "missing"} |\n")
        }
        val epubDiffs = StringBuilder()
        for ((id, r) in refEpub) if (armEpub[id] != r) epubDiffs.append("- `$id`\n  - ref: `${clip(r.toString())}`\n  - arm: `${clip(armEpub[id]?.toString())}`\n")

        val probeRows = StringBuilder()
        for ((label, xml) in probes) {
            val ref = Reference.referenceSerialize(xml)
            val got = try { arm.parseAndSerialize(xml)?.let { if (it == ref) "accepts, equal" else "accepts, DIFFERENT: ${clip(it)}" } ?: "rejects" } catch (e: Throwable) { "THROWS ${e::class.simpleName}" }
            probeRows.append("| $label | ${if (ref == null) "rejects" else "accepts"} | $got |\n")
        }

        val md = StringBuilder()
        md.append("# XML spike report: ${arm.name}\n\n")
        md.append("Key cap: ${Reference.KEY_CAP} keys per module (evenly spaced sample). Inputs: ${inputs.size}, ${chars} chars.\n\n")
        md.append("Samples: ${Reference.sampleInfo}\n\n")
        md.append("## Parse + canonical serialize vs JDOM2/SAX reference\n\n")
        md.append("| group | equal | different | arm rejects only | reference rejects only | both reject | arm throws |\n|---|---|---|---|---|---|---|\n")
        for ((g, t) in tallies) md.append("| $g | ${t.equal} | ${t.different} | ${t.armOnlyRejects} | ${t.refOnlyRejects} | ${t.bothReject} | ${t.armThrows} |\n")
        md.append("\nOSIS total: ${sumOf(tallies, "osis:")}\n\nThML total (all stages): ${sumOf(tallies, "thml.")}\n\n")
        md.append("## Timing (median of 3 measured passes after 3 warm-ups, ms for all inputs)\n\n")
        md.append("| impl | ms | MB/s |\n|---|---|---|\n| reference jdom2/SAX | $refMs | ${"%.1f".format(chars / 1e6 / (refMs / 1000.0))} |\n| ${arm.name} | $armMs | ${"%.1f".format(chars / 1e6 / (armMs / 1000.0))} |\n\n")
        md.append("## Malformation probes (SAX reference vs arm)\n\n| probe | reference | ${arm.name} |\n|---|---|---|\n$probeRows\n")
        md.append("## EPUB queries ($eqQ equal, $diffQ different)\n\n| query | result | ref count | arm count |\n|---|---|---|---|\n$epubRows\n")
        md.append("### EPUB differences\n\n$epubDiffs\n")
        md.append("## Sample differences (max 20 per group)\n\n")
        for ((g, t) in tallies) if (t.diffs.isNotEmpty()) { md.append("### $g\n\n"); t.diffs.forEach { md.append("```\n$it\n```\n\n") } }

        val out = File("build/spike-report-${arm.name}.md")
        out.parentFile.mkdirs()
        out.writeText(md.toString())
        println(md.toString().lineSequence().take(60).joinToString("\n"))
    }

    private fun sumOf(t: Map<String, Tally>, prefix: String): String {
        val s = t.filterKeys { it.startsWith(prefix) }.values
        return "equal=${s.sumOf { it.equal }} different=${s.sumOf { it.different }} armOnly=${s.sumOf { it.armOnlyRejects }} refOnly=${s.sumOf { it.refOnlyRejects }} bothReject=${s.sumOf { it.bothReject }}"
    }

    @Test fun ksoup() = runArm(KsoupArm())
    @Test fun kdomXmlutil() = runArm(KdomArm(generic = true))
    @Test fun kdomXmlutilPlatformReader() = runArm(KdomArm(generic = false))

    /** Anti-vacuity: the comparison must be able to say "different" for a deliberately altered pair. */
    @Test fun comparisonDetectsDifferences() {
        val base = Reference.referenceSerialize("<r a=\"1\"><s>t</s></r>")
        assertEquals(Verdict.EQUAL, classify(base, Reference.referenceSerialize("<r a=\"1\"><s>t</s></r>")))
        assertEquals(Verdict.DIFFERENT, classify(base, Reference.referenceSerialize("<r a=\"2\"><s>t</s></r>")), "differing attribute")
        assertEquals(Verdict.DIFFERENT, classify(base, Reference.referenceSerialize("<r a=\"1\"><s>t</s><s/></r>")), "altered tree")
        assertEquals(Verdict.DIFFERENT, classify(base, Reference.referenceSerialize("<r a=\"1\"><s>T</s></r>")), "altered text")
        assertEquals(Verdict.ARM_ONLY_REJECTS, classify(base, null))
        assertEquals(Verdict.REF_ONLY_REJECTS, classify(null, base))
        // the arms themselves must see an altered input as different from the reference's original
        for (arm in listOf(KsoupArm(), KdomArm())) {
            assertEquals(Verdict.DIFFERENT, classify(base, arm.parseAndSerialize("<r a=\"1\"><s>t</s><s/></r>")), arm.name)
        }
        // and the EPUB comparison: a mutated document must change at least one query result
        val docs = Reference.epubDocs()
        val mutated = docs + ("ch1.xhtml" to docs.getValue("ch1.xhtml").replace("<a ", "<b ").replace("</a>", "</b>"))
        for (arm in listOf(KsoupArm(), KdomArm())) {
            assertTrue(arm.epubQueries(mutated) != arm.epubQueries(docs), "${arm.name}: EPUB queries blind to a mutation")
        }
    }

    /** Port correctness: kdom XMLOutputter vs org.jdom2 XMLOutputter on the same documents, all three formats. */
    @Test fun kdomOutputterMatchesJdom2() {
        val docs = Reference.epubDocs().values.toList() +
            listOf("<r xmlns=\"u\" xmlns:x=\"v\"><x:a x:k=\"1\">t <b>bold</b> <c/> tail</x:a>\n  <![CDATA[ c<d ]]> <s xml:space=\"preserve\"> a  <t/> </s></r>")
        var mismatches = StringBuilder()
        var n = 0
        for (xml in docs) {
            val j = org.jdom2.input.SAXBuilder().build(java.io.StringReader(xml))
            val k = spike.xml.kdom.XmlUtilBuilder.build(xml)
            val fmts = listOf("raw" to (org.jdom2.output.Format.getRawFormat() to spike.xml.kdom.Format.getRawFormat()),
                "pretty" to (org.jdom2.output.Format.getPrettyFormat() to spike.xml.kdom.Format.getPrettyFormat()),
                "compact" to (org.jdom2.output.Format.getCompactFormat() to spike.xml.kdom.Format.getCompactFormat()))
            for ((fn, f) in fmts) {
                n += 2
                val a = org.jdom2.output.XMLOutputter(f.first).outputString(j)
                val b = spike.xml.kdom.XMLOutputter(f.second).outputString(k)
                if (a != b) mismatches.append("DOC/$fn ${firstDiff(a, b)}\n")
                val a2 = org.jdom2.output.XMLOutputter(f.first).outputString(j.rootElement)
                val b2 = spike.xml.kdom.XMLOutputter(f.second).outputString(k.rootElement)
                if (a2 != b2) mismatches.append("ELEM/$fn ${firstDiff(a2, b2)}\n")
            }
        }
        File("build/kdom-outputter-check.txt").also { it.parentFile.mkdirs() }.writeText("compared $n outputs\n$mismatches")
        assertEquals("", mismatches.toString(), "kdom outputter differs from jdom2")
    }
}
