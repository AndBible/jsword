package org.crosswire.jsword.versification

import java.io.File
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/** Each localized Full entry must define Short/Alt instead of silently inheriting English. */
class BibleNamesBundleFilesTest {
    @Test
    fun testEveryBundleDefinesItsOwnShortAndAltNames() {
        val problems = mutableListOf<String>()
        for (bundle in listBibleNamesBundles()) {
            val props = Properties()
            // Keys are ASCII; Properties' ISO-8859-1 decoding is harmless here.
            bundle.inputStream().use { props.load(it) }
            for (key in props.stringPropertyNames()) {
                if (!key.endsWith(".Full")) continue
                val book = key.substring(0, key.length - ".Full".length)
                for (suffix in arrayOf(".Short", ".Alt")) {
                    if (!props.containsKey(book + suffix)) {
                        problems.add(bundle.name + " defines " + key + " but not " + book + suffix)
                    }
                }
            }
        }
        if (problems.isNotEmpty()) {
            fail("Incomplete BibleNames bundles (missing keys inherit English values):\n" + problems.joinToString("\n"))
        }
    }

    private fun listBibleNamesBundles(): List<File> {
        val base = assertNotNull(BibleNames::class.java.classLoader.getResource("BibleNames.properties"),
            "BibleNames.properties must be on the classpath")
        assertEquals("file", base.protocol, "BibleNames.properties must be an unpacked file")
        val dir = File(base.toURI()).parentFile
        val files = assertNotNull(dir.listFiles { _, name -> name.startsWith("BibleNames") && name.endsWith(".properties") },
            "Could not list $dir")
        val bundles = files.toList()
        assertTrue(bundles.size > 10, "Expected to find several BibleNames bundles, found ${bundles.size}")
        return bundles
    }
}
