package org.crosswire.common.util

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

/** `file:` URI path to okio Path: the slash before a Windows drive letter is dropped on Windows only. */
class FilePathOfTest {
    private val win = "\\"
    private val posix = "/"

    @Test fun windowsDriveUriLosesLeadingSlash() {
        val uri = KUri("file:/C:/Users/J%C3%A4nis%20%C3%96/x")
        assertEquals("C:/Users/Jänis Ö/x".toPath(), NetUtil.filePathOf(uri.getPath()!!, win))
    }

    @Test fun posixModeKeepsDriveLikePathUnchanged() {
        val uri = KUri("file:/C:/Users/J%C3%A4nis%20%C3%96/x")
        assertEquals("/C:/Users/Jänis Ö/x".toPath(), NetUtil.filePathOf(uri.getPath()!!, posix))
    }

    @Test fun posixUriIsUnchangedInBothModes() {
        val uri = KUri("file:/home/u/x")
        assertEquals("/home/u/x".toPath(), NetUtil.filePathOf(uri.getPath()!!, win))
        assertEquals("/home/u/x".toPath(), NetUtil.filePathOf(uri.getPath()!!, posix))
    }

    @Test fun bareDriveAndNonDrivePathsInWindowsMode() {
        assertEquals("D:/".toPath(), NetUtil.filePathOf("/D:/", win))
        assertEquals("/ab:/x".toPath(), NetUtil.filePathOf("/ab:/x", win))
        assertEquals("/C:foo".toPath(), NetUtil.filePathOf("/C:foo", win))
    }

    /** The forward direction builds `/C:/...` paths (as `File.toURI` does); reading the URI back gives the same path. */
    @Test fun windowsPathRoundTripsThroughFileUri() {
        val original = "C:/Users/Jänis Ö/x"
        val uri = KUri("file", null, "/$original", null)
        assertEquals(original.toPath(), NetUtil.filePathOf(uri.getPath()!!, win))
    }
}
