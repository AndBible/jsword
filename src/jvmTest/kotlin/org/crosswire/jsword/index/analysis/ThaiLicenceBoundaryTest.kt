package org.crosswire.jsword.index.analysis

import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Licence constraint of K6 Task 7: the OpenJDK Thai data (GPLv2+CPE) is embedded in iOS binaries only. The JVM
 * artifact must contain neither the generated `IosKswordResources` nor the raw data files.
 */
class ThaiLicenceBoundaryTest {
    @Test fun iosKswordResourcesIsNotOnTheJvmClasspath() {
        assertFailsWith<ClassNotFoundException> { Class.forName("org.crosswire.common.util.IosKswordResources") }
    }

    @Test fun commonKswordResourcesHoldNoOpenJdkThaiData() {
        // Base64-embedded data leaves no file named thai_dict anywhere, so the file-name checks below cannot see it.
        val leaked = org.crosswire.common.util.KswordResources.paths.filter {
            "openjdk17" in it || it.endsWith("thai_dict") || it.endsWith("WordBreakIteratorData_th")
        }
        assertEquals(emptyList<String>(), leaked)
    }

    @Test fun jvmMainOutputsHoldNoOpenJdkThaiData() {
        val dirs = listOf(File(System.getProperty("ksword.mainResourcesDir") ?: error("ksword.mainResourcesDir not set"))) +
            (System.getProperty("ksword.mainClassesDirs") ?: error("ksword.mainClassesDirs not set")).split(File.pathSeparator).map(::File)
        for (dir in dirs) {
            assertTrue(dir.isDirectory, "$dir (compile the jvm main output first)")
            val bad = dir.walkTopDown().filter { it.isFile }.map { it.name }.filter {
                it == "WordBreakIteratorData_th" || it == "thai_dict" || it.startsWith("IosKswordResources")
            }.toList()
            assertEquals(emptyList<String>(), bad, "OpenJDK Thai data or IosKswordResources in $dir")
        }
    }

    @Test fun jvmJarHoldsNoOpenJdkThaiData() {
        val jar = System.getProperty("ksword.jvmJar")?.let(::File) ?: error("ksword.jvmJar not set")
        assertTrue(jar.isFile, "$jar (the jvmJar task must have run)")
        ZipFile(jar).use { zip ->
            val bad = zip.entries().asSequence().map { it.name }.filter {
                it.endsWith("WordBreakIteratorData_th") || it.endsWith("thai_dict") || it.contains("IosKswordResources")
            }.toList()
            assertEquals(emptyList<String>(), bad)
        }
    }
}
