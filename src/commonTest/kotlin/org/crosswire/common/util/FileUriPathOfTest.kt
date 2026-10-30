package org.crosswire.common.util

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

/** Filesystem path to `file:` URI path (the reverse of [NetUtil.filePathOf]). */
class FileUriPathOfTest {
    private val win = "\\"
    private val posix = "/"

    @Test fun windowsDrivePathGetsLeadingSlashAndForwardSlashes() {
        assertEquals("/C:/Users/x/sword", NetUtil.fileUriPathOf("C:\\Users\\x\\sword".toPath(), separator = win))
    }

    @Test fun windowsDirectoryGetsTrailingSlash() {
        assertEquals("/C:/Users/x/sword/", NetUtil.fileUriPathOf("C:\\Users\\x\\sword".toPath(), directory = true, separator = win))
        assertEquals("/D:/", NetUtil.fileUriPathOf("D:\\".toPath(), directory = true, separator = win))
    }

    @Test fun windowsKUriPercentEncodesSpaceAndUmlaut() {
        val uri = NetUtil.fileKUri("C:\\Users\\Jänis Ö\\sword".toPath(), directory = true, separator = win)
        assertEquals("/C:/Users/Jänis Ö/sword/", uri.getPath())
        assertEquals("file:/C:/Users/Jänis%20Ö/sword/", uri.toString())
    }

    @Test fun windowsRoundTripsThroughFilePathOf() {
        val p = "C:\\Users\\Jänis Ö\\sword".toPath()
        val uri = NetUtil.fileKUri(p, separator = win)
        assertEquals("C:/Users/Jänis Ö/sword".toPath(), NetUtil.filePathOf(uri.getPath()!!, win))
    }

    @Test fun posixIsByteIdenticalToPathString() {
        assertEquals("/home/u/x", NetUtil.fileUriPathOf("/home/u/x".toPath(), separator = posix))
        assertEquals("/home/u/x/", NetUtil.fileUriPathOf("/home/u/x".toPath(), directory = true, separator = posix))
        assertEquals("/", NetUtil.fileUriPathOf("/".toPath(), directory = true, separator = posix))
    }

    @Test fun posixDriveLikePathIsNotRewritten() {
        assertEquals("/C:/x", NetUtil.fileUriPathOf("/C:/x".toPath(), separator = posix))
    }
}
