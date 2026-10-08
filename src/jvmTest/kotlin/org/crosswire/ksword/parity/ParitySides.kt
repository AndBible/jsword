package org.crosswire.ksword.parity

import java.io.File
import java.net.URLClassLoader
import java.util.function.BiFunction

/**
 * The frozen baseline JSword (K0 spec §6.1) in its own class loader. The parent is the platform class
 * loader, so neither the current JSword classes nor their resources are visible to it.
 */
object Oracle {
    val classpath: List<File> by lazy {
        val raw = System.getProperty("ksword.oracle.classpath")
            ?: error("ksword.oracle.classpath is not set; run the parityTest task")
        raw.split(File.pathSeparator).filter { it.isNotBlank() }.map(::File)
    }

    val loader: URLClassLoader by lazy {
        URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getPlatformClassLoader())
    }

    @Suppress("UNCHECKED_CAST")
    private val probe: BiFunction<String, List<String>, String> by lazy {
        loader.loadClass("org.crosswire.ksword.parity.ParityProbe").getDeclaredConstructor().newInstance()
            as BiFunction<String, List<String>, String>
    }

    fun call(op: String, vararg args: String): String {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = loader
        try {
            return probe.apply(op, args.toList())
        } finally {
            thread.contextClassLoader = previous
        }
    }
}

/** The same probe compiled against the current code. */
object Current {
    private val probe = ParityProbe()

    fun call(op: String, vararg args: String): String = probe.apply(op, args.toList())
}

/** The baseline commit as recorded in oracle/build-oracle.sh (the authoritative record). */
fun recordedBaselineCommit(): String {
    val projectDir = System.getProperty("ksword.projectDir") ?: error("ksword.projectDir is not set")
    val line = File(projectDir, "oracle/build-oracle.sh").readLines().single { it.startsWith("BASELINE_COMMIT=") }
    return line.removePrefix("BASELINE_COMMIT=").trim()
}
