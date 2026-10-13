@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.cinterop.*
import okio.*
import org.crosswire.common.compress.LegacyRawZipSource
import org.crosswire.common.compress.NativeRawInflater
import org.crosswire.jsword.book.install.NativeUnsupportedZipFeatureException
import org.crosswire.jsword.book.install.archiveZipName
import platform.posix.*

/** One compressed archive snapshot; metadata only, with exact first central duplicate lookup. */
internal class NativeZipArchive(path: String) {
    private val zip = CentralZipArchive(NativeLexicalFiles.read(path), ::archiveZipName)
    fun entry(name: String): CentralZipArchive.Entry? = zip.entry(name)
    fun jarEntry(name: String): CentralZipArchive.Entry? = zip.jarEntry(name)
    fun source(entry: CentralZipArchive.Entry): Source {
        if (entry.flags and 1 != 0 || entry.method !in listOf(0, 8))
            throw NativeUnsupportedZipFeatureException(if (entry.flags and 1 != 0) "encryption" else "method", entry)
        val payload = zip.payload(entry)
        return if (entry.method == 0) Buffer().write(payload)
        // Commons ZipFile's bounded deflate stream supplies one dummy byte at compressed EOF.
        else LegacyRawZipSource(payload + byteArrayOf(0), 0, ::NativeRawInflater, "Unexpected end of ZLIB input stream")
    }
    fun declaredBody(entry: CentralZipArchive.Entry): ByteArray = source(entry).use { input ->
        val body = ByteArray(entry.size.toInt())
        val scratch = Buffer()
        var offset = 0
        while (offset < body.size) {
            if (offset < 0) throw ArrayIndexOutOfBoundsException(null)
            val count = input.read(scratch, (body.size - offset).toLong())
            if (count == -1L) offset-- else offset += scratch.read(body, offset, count.toInt())
        }
        body
    }
}

internal fun archiveSingleEntry(path: String, entryName: String): ByteArray =
    archiveSingleEntry(NativeZipArchive(path), entryName)

internal fun archiveSingleEntry(zip: NativeZipArchive, entryName: String): ByteArray =
    zip.declaredBody(zip.entry(entryName)!!)

/** DOS timestamps use the local default time zone, not the archive filesystem modification time. */
internal fun archiveEntryTime(entry: CentralZipArchive.Entry): Long = memScoped {
    val date = alloc<tm>().apply {
        tm_year = ((entry.dosDate ushr 9) and 127) + 80
        tm_mon = ((entry.dosDate ushr 5) and 15) - 1
        tm_mday = entry.dosDate and 31
        tm_hour = (entry.dosTime ushr 11) and 31
        tm_min = (entry.dosTime ushr 5) and 63
        tm_sec = (entry.dosTime and 31) * 2
        tm_isdst = -1
    }
    mktime(date.ptr) * 1000L
}
