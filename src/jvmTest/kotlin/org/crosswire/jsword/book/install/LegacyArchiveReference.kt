package org.crosswire.jsword.book.install

import org.apache.commons.compress.archivers.zip.ZipFile
import org.crosswire.common.util.IOUtil
import org.crosswire.common.util.NetUtil
import org.crosswire.jsword.JSMsg
import java.io.File
import java.net.MalformedURLException
import java.util.Locale

/** Frozen verbatim IOUtil.unpackZip body at cba5125; never delegates to ported code. */
import org.apache.commons.compress.archivers.*
import org.apache.commons.compress.archivers.tar.*
import org.apache.commons.compress.archivers.zip.*
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.crosswire.jsword.book.sword.SwordConstants
import org.crosswire.common.util.LoggerFactory
import java.io.InputStream
import java.net.URI

internal object LegacyArchiveReference {
    private val log = LoggerFactory.getLogger(LegacyArchiveReference::class)
    fun unpack(cacheDir: URI?, cache: URI?) {
        var fin: InputStream? = null
        var gin: GzipCompressorInputStream? = null
        var tin: ArchiveInputStream? = null
        var zout: ArchiveOutputStream? = null

        try {
            // The following is faster if it is passed a file.
            zout = ZipArchiveOutputStream(File(cacheDir!!.path, "mods.d.zip"))
            fin = NetUtil.getInputStream(cache!!)
            gin = GzipCompressorInputStream(fin)
            tin = TarArchiveInputStream(gin)
            while (true) {
                val entry: ArchiveEntry? = tin.nextEntry
                if (entry == null) {
                    break
                }

                // We'll create the mods.d directory elsewhere
                if (entry.isDirectory) {
                    continue
                }

                val path = entry.name

                if (!path.endsWith(SwordConstants.EXTENSION_CONF)) {
                    log.error("Not a SWORD config file: {}", path)
                    continue
                }

                val size = entry.size.toInt()
                // Every now and then an empty entry sneaks in
                if (size == 0) {
                    log.error("Empty entry: {}", path)
                    continue
                }

                val zipEntry: ArchiveEntry = ZipArchiveEntry(path)
                zout.putArchiveEntry(zipEntry)
                val buffer = ByteArray(size)
                val n = tin.read(buffer)
                if (n != size) {
                    log.error("Error: Could not read {} bytes for {} from {}", Integer.toString(size), path, cache.path)
                }
                zout.write(buffer, 0, n)
                zout.closeArchiveEntry()
            }
        } finally {
            IOUtil.close(tin)
            IOUtil.close(gin)
            IOUtil.close(fin)
            IOUtil.close(zout)
        }
    }


    fun unpackZip(file: File, destdir: File, include: Boolean = true, vararg includeExcludes: String) {
            // unpack the zip.
            val dbuf = ByteArray(4096)
            var zf: ZipFile? = null
            try {
                zf = ZipFile(file)
                val entries = zf.entries
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val entrypath = entry.name

                    //check filters
                    if (includeExcludes.size > 0) {
                        //if include, then we attempt to match ANY path
                        //if exclude, then we ensure that we match NO path
                        var skip = include
                        for (filter in includeExcludes) {
                            val matchesPath = entrypath.lowercase(Locale.getDefault()).startsWith(filter)
                            //for includes, ANY match counts, so we override the default of true to false to say 'not skip'
                            if (include && matchesPath) {
                                skip = false
                            }

                            //for excludes, the default of skip is false
                            if (!include && matchesPath) {
                                skip = true
                            }
                        }

                        if (skip) {
                            continue
                        }
                    }

                    val entryFile = File(destdir, entrypath)
                    val parentDir = entryFile.parentFile
                    // Is it already a directory ?
                    if (!parentDir.isDirectory) {
                        // Create the directory and make sure it worked.
                        if (!parentDir.mkdirs()) {
                            // TRANSLATOR: Error condition: A directory could not be created. {0} is a placeholder for the directory
                            throw MalformedURLException(JSMsg.gettext("The URL {0} could not be created as a directory.", parentDir.toString()))
                        }
                    }

                    // write entryFile from zip to filesystem but avoid writing dir entries out as files
                    if (!entry.isDirectory) {

                        val child = NetUtil.getURI(entryFile)

                        val dataOut = NetUtil.getOutputStream(child)
                        val dataIn = zf.getInputStream(entry)

                        while (true) {
                            val count = dataIn.read(dbuf)
                            if (count == -1) {
                                break
                            }
                            dataOut.write(dbuf, 0, count)
                        }

                        dataOut.close()
                    }
                }
            } finally {
                IOUtil.close(zf)
            }

    }
}
