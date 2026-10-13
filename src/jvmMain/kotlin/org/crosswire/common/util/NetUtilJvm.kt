/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.common.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.net.URISyntaxException
import java.net.MalformedURLException

object NetUtilJvm {
    private const val TEMP_SUFFIX = "tmp"
    private var cachedir: File? = null
    /**
     * Quick implementation of FilenameFilter that uses a URIFilter
     */
    open class URIFilterFilenameFilter
    /**
     * Simple ctor
     *
     * @param filter the filter
     */
    (filter: URIFilter?) : FilenameFilter {
        /* (non-Javadoc)
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        override fun accept(arg0: File?, name: String?): Boolean {
            return filter!!.accept(name)
        }

        private var filter: URIFilter? = filter
    }



        /**
         * Return a File from the URI either by extracting from a file: URI or by
         * downloading to a temp dir first
         *
         * @param uri
         *            The original URI to the file.
         * @return The URI as a file
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getAsFile(uri: URI): File {
            // if the URI is already a file URI, return it
            if (uri.scheme.equals(NetUtil.PROTOCOL_FILE)) {
                return File(uri.path)
            }
            val hashString = (uri.toString().hashCode().toString() + "").replace('-', 'm')

            // get the location of the tempWorkingDir
            val workingDir = getURICacheDir()
            var workingFile: File? = null

            if (workingDir != null && workingDir.isDirectory) {
                workingFile = File(workingDir, hashString)
            } else {
                // If there's no working dir, we just use temp...
                workingFile = File.createTempFile(hashString, TEMP_SUFFIX)
            }
            workingFile.deleteOnExit()

            // copy the contents of the URI to the file
            var output: OutputStream? = null
            var input: InputStream? = null
            try {
                output = FileOutputStream(workingFile)
                input = uri.toURL().openStream()
                val data = ByteArray(512)
                var read = 0
                while (read != -1) {
                    output.write(data, 0, read)
                    read = input.read(data)
                }
            } finally {
                try {
                    if (input != null) {
                        input.close()
                    }
                } finally {
                    if (output != null) {
                        output.close()
                    }
                }
            }

            // return the new file in URI form
            return workingFile
        }

        /**
         * Attempt to obtain an InputStream from a URI. If the URI is a file scheme
         * then just open it directly. Otherwise, call uri.toURL().openStream().
         *
         * @param uri
         *            The URI to attempt to read from
         * @return An InputStream connection
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getInputStream(uri: URI): InputStream {
            // We favor the FileOutputStream
            if (uri.scheme.equals(NetUtil.PROTOCOL_FILE)) {
                return FileInputStream(uri.path)
            }
            return uri.toURL().openStream()
        }

        /**
         * Attempt to obtain an OutputStream from a URI. The simple case will open
         * it if it is local. Otherwise, it will call
         * uri.toURL().openConnection().getOutputStream(), however in some JVMs (MS
         * at least this fails where new FileOutputStream(url) works.
         *
         * @param uri
         *            The URI to attempt to write to
         * @return An OutputStream connection
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getOutputStream(uri: URI): OutputStream {
            return getOutputStream(uri, false)
        }

        /**
         * Attempt to obtain an OutputStream from a URI. The simple case will open
         * it if it is local. Otherwise, it will call
         * uri.toURL().openConnection().getOutputStream(), however in some JVMs (MS
         * at least this fails where new FileOutputStream(url) works.
         *
         * @param uri
         *            The URI to attempt to write to
         * @param append
         *            Do we write to the end of the file instead of the beginning
         * @return An OutputStream connection
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getOutputStream(uri: URI, append: Boolean): OutputStream {
            // We favor the FileOutputStream method here because append
            // is not well defined for the openConnection method
            if (uri.scheme.equals(NetUtil.PROTOCOL_FILE)) {
                return FileOutputStream(uri.path, append)
            }
            val cnx = uri.toURL().openConnection()
            cnx.doOutput = true
            return cnx.getOutputStream()
        }

        /**
         * Returns the cache directory.
         *
         * @return File
         */
        @JvmStatic
        fun getURICacheDir(): File? {
            return cachedir
        }

        /**
         * Sets the cache directory.
         *
         * @param cachedir
         *            The cache directory to set
         */
        @JvmStatic
        fun setURICacheDir(cachedir: File?) {
            NetUtilJvm.cachedir = cachedir
        }

        /**
         * Get a URI version of the given file.
         *
         * @param file
         *            The File to turn into a URI
         * @return a URI for the given file
         */
        @JvmStatic
        fun getURI(file: File): URI {
            return file.toURI()
        }

        /**
         * Convert an URL to an URI.
         *
         * @param url
         *            to convert
         * @return the URI representation of the URL
         */
        @JvmStatic
        fun toURI(url: URL): URI? {
            try {
                return URI(url.toExternalForm())
            } catch (e: URISyntaxException) {
                return null
            }
        }

        /**
         * Convert an URI to an URL.
         *
         * @param uri
         *            to convert
         * @return the URL representation of the URI
         */
        @JvmStatic
        fun toURL(uri: URI): URL? {
            try {
                return uri.toURL()
            } catch (e: MalformedURLException) {
                return null
            }
        }
}
