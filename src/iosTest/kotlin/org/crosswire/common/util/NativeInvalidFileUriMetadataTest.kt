@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*

class NativeInvalidFileUriMetadataTest {
    @Test fun decodedNulCannotReadExistingPrefixMetadata() = memScoped {
        val template = (platform.Foundation.NSTemporaryDirectory() + "ksword-nul-XXXXXX").cstr.getPointer(this)
        val fd = mkstemp(template)
        assertTrue(fd >= 0); close(fd)
        val path = template.toKString()
        try {
            NativeLexicalFiles.sink(path).use { sink ->
                val bytes = okio.Buffer().writeUtf8("nonempty")
                sink.write(bytes, bytes.size)
            }
            val valid = KUri("file", null, null, -1, path, null, null)
            assertEquals(8, netUrlSize(valid))
            assertTrue(netUrlLastModified(valid) > 0)
            val invalid = KUri(valid.toString() + "%00ignored")
            assertEquals(0, netUrlSize(invalid))
            assertEquals(0L, netUrlLastModified(invalid))
        } finally { unlink(path) }
    }
}
