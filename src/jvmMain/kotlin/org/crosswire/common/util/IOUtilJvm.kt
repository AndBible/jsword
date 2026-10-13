package org.crosswire.common.util

import java.io.File
import java.io.IOException

object IOUtilJvm {
        /**
         * Unpack a zip file to a given directory. Honor the paths as given in the
         * zip file.
         *
         * @param file
         *            The zip file to download
         * @param destdir
         *            The directory to unpack up
         * @throws IOException
         *             If there is an file error
         */
        @JvmStatic
        @Throws(IOException::class)
        fun unpackZip(file: File, destdir: File) {
            unpackZip(file, destdir, true)
        }

        /**
         * Unpack a zip file to a given directory. Honor the paths as given in the
         * zip file.
         *
         * @param file
         *            The zip file to download
         * @param destdir
         *            The directory to unpack up
         * @param include
         *            true to indicate the next arguments will be a filter that only includes what is specified.
         * @param includeExcludes
         *            a list of case insensitive patterns that will act as an inclusion or exclusion prefix
         * @throws IOException
         *            If there is an file error
         */
        @JvmStatic
        @Throws(IOException::class)
        fun unpackZip(file: File, destdir: File, include: Boolean, vararg includeExcludes: String) {
            org.crosswire.jsword.book.install.SwordArchive.unpackZipSpelling(file.path, destdir.path, include, *includeExcludes)
        }

}
