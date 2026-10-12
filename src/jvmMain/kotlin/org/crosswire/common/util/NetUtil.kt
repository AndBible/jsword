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

import okio.Path.Companion.toPath
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.JarURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.Arrays
import java.util.Date

/**
 * The NetUtil class looks after general utility stuff around the java.net
 * package.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author Mark Goodwin
 * @author DM Smith
 */
class NetUtil
/**
 * Basic constructor - ensure that we can't be instantiated
 */
private constructor() {
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
     * Check that the directories in the version directory really represent
     * versions.
     */
    open class IsDirectoryURIFilter
    /**
     * Simple ctor
     *
     * @param parent the parent directory
     */
    (parent: URI?) : URIFilter {
        /* (non-Javadoc)
         * @see org.crosswire.common.util.URIFilter#accept(java.lang.String)
         */
        override fun accept(name: String?): Boolean {
            return isDirectory(lengthenURI(parent!!, name!!)!!)
        }

        private var parent: URI? = parent
    }

    companion object {
        /**
         * Constant for the file: protocol or scheme
         */
        const val PROTOCOL_FILE: String = "file"

        /**
         * Constant for the http: protocol or scheme
         */
        const val PROTOCOL_HTTP: String = "http"
        const val PROTOCOL_HTTPS: String = "https"

        /**
         * Constant for the ftp: protocol or scheme
         */
        const val PROTOCOL_FTP: String = "ftp"

        /**
         * Constant for the jar: protocol or scheme
         */
        const val PROTOCOL_JAR: String = "jar"

        /**
         * For directory listings
         */
        const val INDEX_FILE: String = "index.txt"

        /**
         * URL/URI separator
         */
        const val SEPARATOR: String = "/"

        /**
         * Separating the username from the rest of the URL/URI
         */
        const val AUTH_SEPERATOR_USERNAME: String = "@"

        /**
         * Separating the password from the username
         */
        const val AUTH_SEPERATOR_PASSWORD: String = ":"

        /**
         * The temporary suffix, used when a temporary file is needed in the
         * system's temporary directory.
         */
        private const val TEMP_SUFFIX = "tmp"

        @JvmStatic
        fun copy(uri: URI): URI? {
            try {
                return URI(uri.toString())
            } catch (e: URISyntaxException) {
                jswordAssert(false) { e }
                return null
            }
        }

        /**
         * If the directory does not exist, create it. Note this currently only
         * works with file: type URIs
         *
         * @param orig
         *            The directory URI to create
         * @throws MalformedURLException
         *                If the URI is not valid
         */
        @JvmStatic
        @Throws(MalformedURLException::class)
        fun makeDirectory(orig: URI) {
            checkFileURI(orig)

            val path = orig.path.toPath()

            // If it is a file, except
            if (KFiles.isFile(path)) {
                // TRANSLATOR: Error condition: A directory was expected, but a file was found. {0} is a placeholder for the file.
                throw MalformedURLException(JSMsg.gettext("The URL {0} is a file.", orig))
            }

            // Is it already a directory ?
            if (!KFiles.isDirectory(path)) {
                // Create the directory and make sure it worked.
                val created = try {
                    KFiles.createDirectories(path)
                    true
                } catch (e: IOException) {
                    false
                }
                if (!created) {
                    // TRANSLATOR: Error condition: A directory could not be created. {0} is a placeholder for the directory
                    throw MalformedURLException(JSMsg.gettext("The URL {0} could not be created as a directory.", orig))
                }
            }
        }

        /**
         * If the file does not exist, create it. Note this currently only works
         * with file: type URIs
         *
         * @param orig
         *            The file URI to create
         * @throws MalformedURLException
         *                If the URI is not valid
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(MalformedURLException::class, IOException::class)
        fun makeFile(orig: URI) {
            checkFileURI(orig)

            val path = orig.path.toPath()

            // If it is a file, except
            if (KFiles.isDirectory(path)) {
                // TRANSLATOR: Error condition: A file was expected, but a directory was found. {0} is a placeholder for the directory.
                throw MalformedURLException(JSMsg.gettext("The URL {0} is a directory.", orig))
            }

            // Is it already a directory ?
            if (!KFiles.isFile(path)) {
                KFiles.createEmptyFile(path)

                // Did that work?
                if (!KFiles.isFile(path)) {
                    // TRANSLATOR: Error condition: A file could not be created. {0} is a placeholder for the file
                    throw MalformedURLException(JSMsg.gettext("The URL {0} could not be created as a file.", orig))
                }
            }
        }

        /**
         * If there is a file at the other end of this URI return true.
         *
         * @param uri
         *            The URI to check
         * @return true if the URI points at a file
         */
        @JvmStatic
        fun isFile(uri: URI): Boolean {
            if (uri.scheme.equals(PROTOCOL_FILE)) {
                return KFiles.isFile(uri.path.toPath())
            }

            try {
                // This will throw if the resource does not exist
                uri.toURL().openStream().close()
                return true
            } catch (ex: IOException) {
                // the resource does not exist!
                return false
            }
        }

        /**
         * If there is a directory at the other end of this URI return true. Note
         * non file: type URI will always return false
         *
         * @param orig
         *            The URI to check
         * @return true if the URI points at a file: directory
         */
        @JvmStatic
        fun isDirectory(orig: URI): Boolean {
            if (!orig.scheme.equals(PROTOCOL_FILE)) {
                return false
            }

            return KFiles.isDirectory(orig.path.toPath())
        }

        /**
         * If there is a writable directory or file at the other end of this URI
         * return true. Note non file: type URIs will always return false
         *
         * @param orig
         *            The URI to check
         * @return true if the URI points at a writable file or directory
         */
        @JvmStatic
        fun canWrite(orig: URI): Boolean {
            if (!orig.scheme.equals(PROTOCOL_FILE)) {
                return false
            }

            return File(orig.path).canWrite()
        }

        /**
         * If there is a readable directory or file at the other end of this URI
         * return true. Note non file: type URIs will always return false
         *
         * @param orig
         *            The URI to check
         * @return true if the URI points at a readable file or directory
         */
        @JvmStatic
        fun canRead(orig: URI): Boolean {
            if (!orig.scheme.equals(PROTOCOL_FILE)) {
                return false
            }

            return File(orig.path).canRead()
        }

        /**
         * Move a URI from one place to another. Currently this only works for file:
         * URIs, however the interface should not need to change to handle more
         * complex URIs
         *
         * @param oldUri
         *            The URI to move
         * @param newUri
         *            The destination URI
         * @return whether the move happened
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun move(oldUri: URI, newUri: URI): Boolean {
            checkFileURI(oldUri)
            checkFileURI(newUri)

            return KFiles.move(oldUri.path.toPath(), newUri.path.toPath())
        }

        /**
         * Delete a URI. Currently this only works for file: URIs, however the
         * interface should not need to change to handle more complex URIs
         *
         * @param orig
         *            The URI to delete
         * @return whether the deleted happened
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun delete(orig: URI): Boolean {
            checkFileURI(orig)

            return KFiles.delete(orig.path.toPath())
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
            if (uri.scheme.equals(PROTOCOL_FILE)) {
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
         * Utility to strip a string from the end of a URI.
         *
         * @param orig
         *            The URI to strip
         * @param strip
         *            The text to strip from the end of the URI
         * @return The stripped URI
         * @exception MalformedURLException
         *                If the URI does not end in the given text
         */
        @JvmStatic
        @Throws(MalformedURLException::class)
        fun shortenURI(orig: URI, strip: String): URI {
            var file = orig.path
            val lastChar = file[file.length - 1]
            if (isSeparator(lastChar)) {
                file = file.substring(0, file.length - 1)
            }

            val test = file.substring(file.length - strip.length)
            if (test != strip) {
                throw MalformedURLException(JSOtherMsg.lookupText("The URL {0} does not end in {1}.", orig, strip))
            }

            val newFile = file.substring(0, file.length - strip.length)

            try {
                return URI(orig.scheme, orig.userInfo, orig.host, orig.port, newFile, "",
                    "")
            } catch (e: URISyntaxException) {
                throw MalformedURLException(JSOtherMsg.lookupText("The URL {0} does not end in {1}.", orig, strip))
            }
        }

        /**
         * Utility to add a string to the end of a URI.
         *
         * @param orig
         *            The URI to lengthen
         * @param anExtra
         *            The text to add to the end of the URI
         * @return The lengthened URI
         */
        @JvmStatic
        fun lengthenURI(orig: URI, anExtra: String): URI? {
            val extra = anExtra
            try {
                val path = StringBuilder(orig.path)
                val lastChar = path[path.length - 1]
                val firstChar = extra[0]
                if (isSeparator(firstChar)) {
                    if (isSeparator(lastChar)) {
                        path.append(extra.substring(1))
                    } else {
                        path.append(extra)
                    }
                } else {
                    if (!isSeparator(lastChar)) {
                        path.append(SEPARATOR)
                    }
                    path.append(extra)
                }

                return URI(orig.scheme, orig.userInfo, orig.host, orig.port, path.toString(), orig.query, orig.fragment)
            } catch (ex: URISyntaxException) {
                return null
            }
        }

        private fun isSeparator(c: Char): Boolean {
            return c == '/' || c == '\\'
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
            if (uri.scheme.equals(PROTOCOL_FILE)) {
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
            if (uri.scheme.equals(PROTOCOL_FILE)) {
                return FileOutputStream(uri.path, append)
            }
            val cnx = uri.toURL().openConnection()
            cnx.doOutput = true
            return cnx.getOutputStream()
        }

        /**
         * List the items available assuming that this URI points to a directory.
         * <p>
         * There are 2 methods of calculating the answer - if the URI is a file: URI
         * then we can just use File.list(), otherwise we ask for a file inside the
         * directory called index.txt and assume the directories contents to be
         * listed one per line.
         * <p>
         * If the URI is a file: URI then we execute both methods and warn if there
         * is a difference, but returning the values from the index.txt method.
         *
         * @param uri the URI to list
         * @param filter the filter for the listing
         * @return the filtered list
         * @throws MalformedURLException
         *                If the URI is not valid
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(MalformedURLException::class, IOException::class)
        fun list(uri: URI, filter: URIFilter?): Array<String>? {
            // We should probably cache this in some way? This is going
            // to get very slow calling this often across a network
            var reply = arrayOf<String>()
            try {
                val search = lengthenURI(uri, INDEX_FILE)
                reply = listByIndexFile(search!!, filter)
            } catch (ex: FileNotFoundException) {
                // So the index file was not found - this isn't going to work over
                // JNLP or other systems that can't use file: URIs. But it is silly
                // to get to picky so if there is a solution using file: then just
                // print a warning and carry on.
                LOGGER.warn("index file for {} was not found.", uri.toString())
                if (uri.scheme.equals(PROTOCOL_FILE)) {
                    return listByFile(uri, filter)
                }
            }

            // if we can - check that the index file is up to date.
            if (uri.scheme.equals(PROTOCOL_FILE)) {
                val files = listByFile(uri, filter)

                // Check that the answers are the same
                if (files!!.size != reply.size) {
                    LOGGER.warn("index file for {} has incorrect number of entries.", uri.toString())
                } else {
                    val list = Arrays.asList(*files)
                    for (i in files.indices) {
                        if (!list.contains(files[i])) {
                            LOGGER.warn("file: based index found {} but this was not found using index file.", files[i])
                        }
                    }
                }
            }

            return reply
        }

        /**
         * List all the files specified by the index file passed in.
         *
         * @param uri the URI to list
         * @param filter the filter for the listing
         * @return the filtered list
         * @throws MalformedURLException
         *                If the URI is not valid
         */
        @JvmStatic
        @Throws(MalformedURLException::class)
        fun listByFile(uri: URI, filter: URIFilter?): Array<String>? {
            val dir = uri.path.toPath()
            if (!KFiles.isDirectory(dir)) {
                // TRANSLATOR: Error condition: A directory was expected, but a file was found. {0} is a placeholder for the file.
                throw MalformedURLException(JSMsg.gettext("The URL {0} is not a directory", uri.toString()))
            }

            // null for a directory that cannot be listed, like File.list()
            return KFiles.listOrNull(dir)?.filter { filter!!.accept(it) }?.toTypedArray()
        }

        /**
         * List all the strings specified by the index file passed in. To be
         * acceptable it must be a non-0 length string, not commented with #, and
         * not the index file itself.
         *
         * @param index the URI to list
         * @return the list.
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun listByIndexFile(index: URI): Array<String> {
            return listByIndexFile(index, DefaultURIFilter())
        }

        /**
         * List all the files specified by the index file passed in.
         * <p>Each line is pre-processed:</p>
         * <ul>
         * <li>Ignore comments (# to end of line)</li>
         * <li>Trim spaces from line.</li>
         * <li>Ignore blank lines.</li>
         * </ul>
         *
         * To be acceptable it:
         * <ul>
         * <li> cannot be the index file itself</li>
         * <li> and must acceptable by the filter.</li>
         * </ul>
         *
         * @param index the URI to list
         * @param filter the filter for the listing
         * @return the list.
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun listByIndexFile(index: URI, filter: URIFilter?): Array<String> {
            var `in`: InputStream? = null
            try {
                `in` = getInputStream(index)
                val text = `in`.readBytes().decodeToString()
                return IndexFiles.parse(text) { filter!!.accept(it) }.toTypedArray()
            } finally {
                IOUtil.close(`in`)
            }
        }

        /**
         * Load up properties given by a URI.
         *
         * @param uri
         *            the location of the properties
         * @return the properties given by the URI
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun loadProperties(uri: URI): PropertyMap {
            var ins: InputStream? = null
            try {
                ins = getInputStream(uri)
                val prop = PropertyMap()
                PropertyMapJvm.load(prop, ins!!)
                ins.close()
                return prop
            } finally {
                IOUtil.close(ins)
            }
        }

        /**
         * Store the properties at the location given by the uri using the supplied
         * title.
         *
         * @param properties
         *            the properties to store
         * @param uri
         *            the location of the store
         * @param title
         *            the label held in the properties file
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun storeProperties(properties: PropertyMap?, uri: URI, title: String?) {
            var out: OutputStream? = null

            try {
                out = getOutputStream(uri)
                val temp = PropertyMap()
                temp.putAll(properties!!)
                PropertyMapJvm.store(temp, out!!, title)
            } finally {
                IOUtil.close(out)
            }
        }

        /**
         * @param uri
         *            the resource whose size is wanted
         * @return the size of that resource
         */
        @JvmStatic
        fun getSize(uri: URI): Int {
            return getSize(uri, null, null)
        }

        @JvmStatic
        fun getSize(uri: URI, proxyHost: String?): Int {
            return getSize(uri, proxyHost, null)
        }

        /** The `Int` size of a file, or -1 when it does not fit (what `URLConnection.getContentLength` returned). */
        internal fun fileSizeToInt(size: Long): Int = if (size > Int.MAX_VALUE) -1 else size.toInt()

        @JvmStatic
        fun getSize(uri: URI, proxyHost: String?, proxyPort: Int?): Int {
            if (uri.scheme.equals(PROTOCOL_FILE)) {
                val path = uri.path.toPath()
                // Plain files only; anything else (missing, directory) keeps the URLConnection behaviour.
                if (KFiles.isFile(path)) {
                    return fileSizeToInt(KFiles.size(path))
                }
            }

            try {
                if (uri.scheme.equals(PROTOCOL_HTTP)) {
                    val resource = WebResource(uri, proxyHost, proxyPort)
                    val size = resource.size
                    resource.shutdown()
                    return size
                }

                return uri.toURL().openConnection().contentLength
            } catch (e: IOException) {
                return 0
            }
        }

        /**
         * When was the given URI last modified. If no modification time is
         * available then this method return the current time.
         *
         * @param uri the URI to examine
         * @return the last modified date
         */
        @JvmStatic
        fun getLastModified(uri: URI): Long {
            return getLastModified(uri, null, null)
        }

        /**
         * When was the given URI last modified. If no modification time is
         * available then this method return the current time.
         *
         * @param uri the URI to examine
         * @param proxyHost the proxy host
         * @return the last modified date
         */
        @JvmStatic
        fun getLastModified(uri: URI, proxyHost: String?): Long {
            return getLastModified(uri, proxyHost, null)
        }

        /**
         * When was the given URI last modified. If no modification time is
         * available then this method return the current time.
         *
         * @param uri the URI to examine
         * @param proxyHost the proxy host
         * @param proxyPort the proxy port
         * @return the last modified date
         */
        @JvmStatic
        fun getLastModified(uri: URI, proxyHost: String?, proxyPort: Int?): Long {
            if (uri.scheme.equals(PROTOCOL_FILE)) {
                val path = uri.path.toPath()
                // Plain files only; anything else (missing, directory) keeps the URLConnection behaviour.
                if (KFiles.isFile(path)) {
                    return KFiles.lastModifiedMillis(path)
                }
            }

            try {
                if (uri.scheme.equals(PROTOCOL_HTTP)) {
                    val resource = WebResource(uri, proxyHost, proxyPort)
                    val time = resource.lastModified
                    resource.shutdown()
                    return time
                }

                val urlConnection = uri.toURL().openConnection()
                var time = urlConnection.lastModified

                // If it were a jar then time contains the last modified date of the jar.
                if (urlConnection is JarURLConnection) {
                    // form is jar:file:.../xxx.jar!.../filename.ext
                    val jarConnection: JarURLConnection = urlConnection
                    val jarEntry = jarConnection.jarEntry
                    time = jarEntry.time
                }

                return time
            } catch (ex: IOException) {
                LOGGER.warn("Failed to get modified time", ex)
                return Date().time
            }
        }

        /**
         * Returns whether the left is newer than the right by comparing their last
         * modified dates.
         *
         * @param left one URI to compare
         * @param right the other URI to compare
         * @return true if the left is newer
         */
        @JvmStatic
        fun isNewer(left: URI, right: URI): Boolean {
            return isNewer(left, right, null, null)
        }

        /**
         * Returns whether the left is newer than the right by comparing their last
         * modified dates.
         *
         * @param left one URI to compare
         * @param right the other URI to compare
         * @param proxyHost the proxy host
         * @return true if the left is newer
         */
        @JvmStatic
        fun isNewer(left: URI, right: URI, proxyHost: String?): Boolean {
            return isNewer(left, right, proxyHost, null)
        }

        /**
         * Returns whether the left is newer than the right by comparing their last
         * modified dates.
         *
         * @param left one URI to compare
         * @param right the other URI to compare
         * @param proxyHost the proxy host
         * @param proxyPort the proxy port
         * @return true if the left is newer
         */
        @JvmStatic
        fun isNewer(left: URI, right: URI, proxyHost: String?, proxyPort: Int?): Boolean {
            return getLastModified(left, proxyHost, proxyPort) > getLastModified(right, proxyHost, proxyPort)
        }

        /**
         * Throw if the given URI does not use the 'file:' protocol
         *
         * @param uri
         *            The URI to check
         * @throws MalformedURLException
         *             If the protocol is not file:
         */
        @Throws(MalformedURLException::class)
        private fun checkFileURI(uri: URI) {
            if (!uri.scheme.equals(PROTOCOL_FILE)) {
                // TRANSLATOR: Error condition: The URL protocol "file:" was expected, but something else was found. {0} is a placeholder for the URL.
                throw MalformedURLException(JSMsg.gettext("The URL {0} is not a file.", uri))
            }
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
            NetUtil.cachedir = cachedir
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
         * A URI version of <code>File.createTempFile()</code>
         *
         * @param prefix the prefix of the temporary file
         * @param suffix the suffix of the temporary file
         * @return A new temporary URI
         * @throws IOException
         *             If something goes wrong creating the temp URI
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getTemporaryURI(prefix: String, suffix: String?): URI {
            val tempFile = File.createTempFile(prefix, suffix)
            return getURI(tempFile)
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

        /**
         * Where are temporary files cached.
         */
        private var cachedir: File? = null

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(NetUtil::class)
    }
}
