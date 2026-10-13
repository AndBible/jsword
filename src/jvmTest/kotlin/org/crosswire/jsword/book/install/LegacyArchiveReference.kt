package org.crosswire.jsword.book.install

import org.apache.commons.compress.archivers.zip.ZipFile
import org.crosswire.common.util.IOUtil
import org.crosswire.common.util.NetUtil
import org.crosswire.jsword.JSMsg
import java.io.File
import java.net.MalformedURLException
import java.util.Locale

/** Frozen verbatim IOUtil.unpackZip body at cba5125; never delegates to ported code. */
internal object LegacyArchiveReference {
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
