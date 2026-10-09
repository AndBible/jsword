package org.crosswire.common.util

import okio.Path.Companion.toPath
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** KFiles behaviours that need the real file system (symlinks, permissions). */
class KFilesJvmTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun deleteRemovesADanglingSymlinkLikeFileDelete() {
        val link = java.io.File(tmp.root, "dangling")
        try {
            Files.createSymbolicLink(link.toPath(), java.io.File(tmp.root, "no-such-target").toPath())
        } catch (e: Exception) {
            Assume.assumeNoException("symlinks not supported here", e)
        }
        assertFalse(link.exists()) // exists() follows the link
        assertTrue(Files.isSymbolicLink(link.toPath()))
        assertTrue(KFiles.delete(link.path.toPath()))
        assertFalse(Files.isSymbolicLink(link.toPath()))
    }

    @Test
    fun deleteOfMissingPathIsStillFalse() {
        assertFalse(KFiles.delete(java.io.File(tmp.root, "nope").path.toPath()))
    }

    @Test
    fun listOrNullIsNullForUnreadableAndMissingDirectories() {
        val dir = tmp.newFolder("locked")
        java.io.File(dir, "a").writeText("a")
        assertEquals(listOf("a"), KFiles.listOrNull(dir.path.toPath()))
        assertNull(KFiles.listOrNull(java.io.File(tmp.root, "nope").path.toPath()))
        assertTrue(dir.setReadable(false))
        try {
            Assume.assumeTrue("running as root: permissions are not enforced", dir.list() == null)
            assertNull(KFiles.listOrNull(dir.path.toPath()))
            assertEquals(emptyList(), KFiles.list(dir.path.toPath()))
        } finally {
            dir.setReadable(true)
        }
    }
}
