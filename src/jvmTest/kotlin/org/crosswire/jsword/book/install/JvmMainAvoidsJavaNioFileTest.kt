package org.crosswire.jsword.book.install

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Android minSdk is 23 and core library desugaring is off, so any `java.nio.file` reference in jsword's
 * JVM main classes breaks module install on API 23-25. Robolectric runs on a JDK where the package exists,
 * so the only portable proof is a scan of the compiled classes' constant pools.
 */
class JvmMainAvoidsJavaNioFileTest {
    @Test fun mainClassesReferenceNoJavaNioFile() {
        val location = SwordArchive::class.java.protectionDomain.codeSource.location.toURI()
        val root = File(location)
        assertTrue(root.isDirectory, "Expected jsword main classes as a directory, got $location")
        val needle = "java/nio/file/".toByteArray(Charsets.ISO_8859_1)
        var scanned = 0
        val offenders = mutableListOf<String>()
        root.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { file ->
            scanned++
            if (indexOf(file.readBytes(), needle) >= 0) offenders += file.relativeTo(root).path
        }
        assertTrue(scanned >= 500, "Only $scanned class files scanned under $root; the guard would be vacuous")
        assertTrue(
            offenders.isEmpty(),
            "Classes referencing java.nio.file (API 26+: Android minSdk is 23 and desugaring is off): $offenders",
        )
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }
}
