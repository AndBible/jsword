package org.crosswire.common.util

import org.apache.commons.compress.archivers.zip.ZipFile

internal fun ioZipEntry(entrySpec: String): ByteArray {
            // Get the buffer
            var buffer = ByteArray(0)
            val parts = StringUtil.split(entrySpec, '!')
            var zipFile: ZipFile? = null
            var zin: java.io.InputStream? = null
            try {
                zipFile = ZipFile(parts[0])
                val entry = zipFile.getEntry(parts[1])
                zin = zipFile.getInputStream(entry)
                val size = entry.size.toInt()
                buffer = ByteArray(size)
                // Repeatedly read until all has been read
                var offset = 0
                while (offset < size) {
                    offset += zin.read(buffer, offset, size - offset)
                }

                if (offset != size) {
                    LoggerFactory.getLogger(IOUtil::class).error("Error: Could not read {} bytes, instead {}, for {} from {}", Integer.toString(size), Integer.toString(offset), parts[1], parts[0])
                }
            } finally {
                IOUtil.close(zin)
                IOUtil.close(zipFile)
            }
            return buffer
}
