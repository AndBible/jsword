package spike.xml

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import spike.xml.ksoup.KsoupArm

class SpikeExperimentsTest {
    private class Tally { var equal = 0; var different = 0; var armOnlyRejects = 0; var refOnlyRejects = 0; var bothReject = 0; var armThrows = 0
        val diffs = ArrayList<String>() }

    private fun clip(s: String?) = if (s == null) "<rejected>" else if (s.length > 500) s.take(500) + "...(${s.length} chars)" else s

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
            when {
                armThrew -> {}
                ref == null && got == null -> t.bothReject++
                ref == null -> t.refOnlyRejects++.also { if (t.diffs.size < 20) t.diffs.add("${inp.key} [reference rejects only]\n  arm: ${clip(got)}\n  input: ${clip(inp.xml)}") }
                got == null -> t.armOnlyRejects++.also { if (t.diffs.size < 20) t.diffs.add("${inp.key} [arm rejects only]\n  ref: ${clip(ref)}\n  input: ${clip(inp.xml)}") }
                ref == got -> t.equal++
                else -> t.different++.also { if (t.diffs.size < 20) t.diffs.add("${inp.key} [different]\n  ref: ${clip(ref)}\n  arm: ${clip(got)}") }
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
        val armEpub = try { arm.epubQueries(docs) } catch (e: Throwable) { println("arm epub failed: $e"); emptyMap() }
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
}
