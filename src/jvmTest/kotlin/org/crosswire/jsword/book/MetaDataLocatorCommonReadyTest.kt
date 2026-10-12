package org.crosswire.jsword.book

import org.crosswire.common.util.CWProject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import okio.Path.Companion.toPath
import java.net.URI
import java.nio.file.Files

class MetaDataLocatorCommonReadyTest {
    /** Seed otherwise unreachable read-only branches without relying on host permissions (root in CI). */
    private fun withHomes(block: (CWProject, File) -> Unit) {
        val fields = listOf("instance", "homeProperty", "homeDirectory", "homeAltDirectory")
            .associateWith { CWProject::class.java.getDeclaredField(it).apply { isAccessible = true } }
        val saved = fields.mapValues { it.value.get(null) }
        val root = Files.createTempDirectory("locator space ä").toFile()
        try {
            CWProject.Companion::class.java.getDeclaredMethod("reset").apply { isAccessible = true }
                .invoke(CWProject.Companion)
            val project = CWProject.instance()
            // Establish cached locations directly: setHome establishes filesystem/cache globals as well.
            block(project, root)
        } finally {
            fields.forEach { (name, field) -> field.set(null, saved[name]) }
            root.deleteRecursively()
        }
    }

    private fun set(project: CWProject, name: String, value: Any?) {
        CWProject::class.java.getDeclaredField(name).apply { isAccessible = true }.set(project, value)
    }

    @Test fun createsBothJswordAndFrontendOverridesInIsolatedHomes() = withHomes { project, root ->
        val write = File(root, "write").apply { mkdirs() }
        val read = File(root, "read").apply { mkdirs() }
        val frontendWrite = File(write, "frontend").apply { mkdirs() }
        val frontendRead = File(read, "frontend").apply { mkdirs() }
        set(project, "writeHome", write.toURI())
        set(project, "readHome", read.toURI())
        set(project, "homes", arrayOf(write.toURI(), read.toURI()))
        set(project, "writableFrontEndHome", frontendWrite.toURI())
        set(project, "frontendReadHome", frontendRead.toURI())
        val cases = listOf(
            File(write, "jsword-mods.d") to { MetaDataLocator.JSWORD.writeLocation },
            File(read, "jsword-mods.d") to { MetaDataLocator.JSWORD.readLocation },
            File(frontendWrite, "jsword-mods.d") to { MetaDataLocator.FRONTEND.writeLocation },
            File(frontendRead, "jsword-mods.d") to { MetaDataLocator.FRONTEND.readLocation }
        )
        for ((expected, getter) in cases) {
            assertFalse("child must initially be absent: $expected", expected.exists())
            assertEquals(expected.path.toPath(), getter())
            assertTrue("getter must create directory: $expected", expected.isDirectory)
        }
    }

    @Test fun absentReadAndFrontendAndTransientLocationsRemainNull() = withHomes { project, root ->
        set(project, "writeHome", root.toURI())
        set(project, "homes", arrayOf(root.toURI()))
        assertNull(MetaDataLocator.JSWORD.readLocation)
        assertNull(MetaDataLocator.FRONTEND.readLocation)
        assertNull(MetaDataLocator.FRONTEND.writeLocation)
        assertNull(MetaDataLocator.TRANSIENT.readLocation)
        assertNull(MetaDataLocator.TRANSIENT.writeLocation)
        set(project, "homes", emptyArray<URI>())
        assertNull(MetaDataLocator.JSWORD.writeLocation)
    }

    @Test fun failedCreationStillReturnsLocationAndInvalidUriIsRejected() = withHomes { project, root ->
        val parentFile = File(root, "not-directory").apply { writeText("occupied") }
        set(project, "writeHome", parentFile.toURI())
        set(project, "homes", arrayOf(parentFile.toURI()))
        val expected = File(parentFile, "jsword-mods.d")
        assertEquals(expected.path.toPath(), MetaDataLocator.JSWORD.writeLocation)
        assertFalse(expected.exists())
        for (uri in listOf(URI("https://example.com/home"), URI("file://host/home"), URI("relative/home"), URI("file:/home?q=x"), URI("file:/home#fragment"))) {
            set(project, "homes", arrayOf(uri))
            assertThrows(IllegalArgumentException::class.java) { MetaDataLocator.JSWORD.writeLocation }
        }
    }
}
