package org.crosswire.common.util

internal actual fun ioZipEntry(entrySpec: String): ByteArray {
    val parts = StringUtil.split(entrySpec, '!')
    // The archive opens before the missing second token is dereferenced; split's one-token
    // branch intentionally retains trailing separators in the archive's literal spelling.
    val zip = NativeZipArchive(parts[0])
    return archiveSingleEntry(zip, parts[1])
}
