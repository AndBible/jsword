package org.crosswire.common.util

import okio.Path.Companion.toPath
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CWProjectPathTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** Runs [block] against a fresh CWProject singleton homed in a temp dir, restoring the globals afterwards. */
    private fun withTempHome(frontend: String?, block: (CWProject) -> Unit) {
        val propKey = "jsword.home"
        val oldProp = System.getProperty(propKey)
        val reset = CWProject.Companion::class.java.getDeclaredMethod("reset").apply { isAccessible = true }
        val companion = CWProject.Companion
        try {
            System.setProperty(propKey, tmp.root.absolutePath)
            reset.invoke(companion)
            val project = CWProject.instance()
            project.setFrontendName(frontend) // must precede the home being established
            CWProject.setHome(propKey, ".jsword", "JSword")
            block(project)
        } finally {
            if (oldProp == null) System.clearProperty(propKey) else System.setProperty(propKey, oldProp)
            reset.invoke(companion)
            CWProject.setHome("jsword.home", ".jsword", "JSword")
        }
    }

    @Test
    fun writableProjectDirPathMatchesUri() = withTempHome(null) { p ->
        assertEquals(p.writableProjectDir!!.path.toPath(), p.writableProjectDirPath)
        assertEquals(tmp.root.absolutePath.toPath(), p.writableProjectDirPath)
    }

    @Test
    fun subdirPathCreatesAndMatchesUriVariant() = withTempHome(null) { p ->
        val path = assertNotNull(p.getWritableProjectSubdirPath("x", true))
        assertTrue(KFiles.isDirectory(path))
        assertEquals(p.getWritableProjectSubdir("x", false)!!.path.toPath(), path)
    }

    @Test
    fun subdirPathWithoutCreateDoesNotCreate() = withTempHome(null) { p ->
        val path = assertNotNull(p.getWritableProjectSubdirPath("lazy", false))
        assertTrue(!KFiles.exists(path))
    }

    @Test
    fun frontendPaths() = withTempHome("front") { p ->
        assertEquals(p.writableFrontendProjectDir!!.path.toPath(), p.writableFrontendProjectDirPath)
        assertTrue(KFiles.isDirectory(p.writableFrontendProjectDirPath!!))
        // writable home: there is no separate read-only frontend home
        assertNull(p.readableFrontendProjectDir)
        assertNull(p.readableFrontendProjectDirPath)
    }
}
