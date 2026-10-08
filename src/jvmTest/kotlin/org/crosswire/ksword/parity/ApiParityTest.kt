package org.crosswire.ksword.parity

import java.io.File
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.fail

/**
 * Compares the Java-visible API of every baseline class with the current code (KSword K1 spec §4.1).
 * Removed or changed lines fail unless allowlisted with "-"; added lines fail unless they are a known
 * Kotlin artefact or allowlisted with "+". Stale allowlist entries fail too.
 */
class ApiParityTest {
    private data class Entry(val cls: String, val sign: Char, val line: String)

    private fun allowlist(): Set<Entry> {
        val projectDir = System.getProperty("ksword.projectDir") ?: error("ksword.projectDir is not set")
        val file = File(projectDir, "oracle/api-allowlist.txt")
        return file.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.map { raw ->
            val hash = raw.indexOf(" # ")
            require(hash > 0 && raw.substring(hash + 3).isNotBlank()) { "allowlist entry without a reason: $raw" }
            val body = raw.substring(0, hash).trim()
            val parts = body.split(" ", limit = 3)
            require(parts.size == 3 && parts[1].length == 1 && parts[1][0] in "+-") { "bad allowlist entry: $raw" }
            Entry(parts[0], parts[1][0], parts[2])
        }.toSet()
    }

    private fun baselineClassNames(): List<String> {
        val jar = Oracle.classpath.single { it.name == "jsword-baseline.jar" }
        return JarFile(jar).use { jf ->
            jf.entries().asSequence().map { it.name }
                .filter { it.endsWith(".class") && (it.startsWith("org/crosswire/") || it.startsWith("gnu/")) }
                .filter { !it.startsWith("org/crosswire/ksword/") }
                .map { it.removeSuffix(".class").replace('/', '.') }
                .sorted().toList()
        }
    }

    @Test
    fun apiMatchesBaseline() {
        val allow = allowlist()
        val used = mutableSetOf<Entry>()
        val problems = mutableListOf<String>()
        val generic = mutableListOf<String>()
        val currentLoader = ApiParityTest::class.java.classLoader
        var compared = 0
        for (name in baselineClassNames()) {
            val old = Oracle.loader.loadClass(name)
            if (!ApiShape.isCompared(old)) continue
            compared++
            val cur = try {
                Class.forName(name, false, currentLoader)
            } catch (e: ClassNotFoundException) {
                val entry = Entry(name, '-', "<class>")
                if (entry in allow) used += entry else problems += "$name - <class missing>"
                continue
            }
            val a = ApiShape.of(old)
            val b = ApiShape.of(cur)
            for (line in a - b) {
                val e = Entry(name, '-', line)
                if (e in allow) used += e else problems += "$name - $line"
            }
            for (line in b - a) {
                if (ApiShape.isKotlinArtefact(cur, line)) continue
                val e = Entry(name, '+', line)
                if (e in allow) used += e else problems += "$name + $line"
            }
            val ga = ApiShape.genericOf(old)
            val gb = ApiShape.genericOf(cur)
            (ga - gb).forEach { generic += "$name - $it" }
            (gb - ga).forEach { generic += "$name + $it" }
        }
        val stale = allow - used
        stale.forEach { problems += "STALE allowlist entry: ${it.cls} ${it.sign} ${it.line}" }

        val reports = File(System.getProperty("ksword.reportsDir") ?: error("ksword.reportsDir is not set")).apply { mkdirs() }
        File(reports, "api-diff.txt").writeText("compared classes: $compared\n" + problems.joinToString("\n") + "\n")
        File(reports, "api-generic.txt").writeText("generic signature differences: ${generic.size}\n" + generic.joinToString("\n") + "\n")
        println("ApiParityTest: $compared classes compared, ${problems.size} problems, ${generic.size} generic differences")
        if (compared < 300) fail("only $compared classes compared; the baseline jar listing is broken")
        if (problems.isNotEmpty()) {
            fail("${problems.size} API differences (full list: ${reports}/api-diff.txt):\n" + problems.take(200).joinToString("\n"))
        }
    }
}
