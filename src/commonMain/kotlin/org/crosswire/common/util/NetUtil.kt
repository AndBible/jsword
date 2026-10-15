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
import okio.IOException
import okio.FileNotFoundException
import kotlin.jvm.JvmStatic
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg

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
     * Check that the directories in the version directory really represent
     * versions.
     */
    open class IsDirectoryURIFilter
    /**
     * Simple ctor
     *
     * @param parent the parent directory
     */
    (parent: KUri?) : URIFilter {
        /* (non-Javadoc)
         * @see org.crosswire.common.util.URIFilter#accept(java.lang.String)
         */
        override fun accept(name: String?): Boolean {
            return isDirectory(lengthenURI(parent!!, name!!)!!)
        }

        private var parent: KUri? = parent
    }

    companion object {
        /** Validated File KUri projection; authoritative lexical IO must not use this Path. */
        @JvmStatic
        fun toPath(uri: KUri): okio.Path = validatedFileUriPath(uri)

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
         * URL/KUri separator
         */
        const val SEPARATOR: String = "/"

        /**
         * Separating the username from the rest of the URL/KUri
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
        fun copy(uri: KUri): KUri? {
            try {
                return KUri(uri.toString())
            } catch (e: Exception) {
                if (!netUriSyntaxFailure(e)) throw e
                jswordAssert(false) { e }
                return null
            }
        }

        /**
         * If the directory does not exist, create it. Note this currently only
         * works with file: type URIs
         *
         * @param orig
         *            The directory KUri to create
         * @throws NetMalformedURLException
         *                If the KUri is not valid
         */
        @JvmStatic
        @Throws(NetMalformedURLException::class)
        fun makeDirectory(orig: KUri) {
            checkFileURI(orig)

            val path = orig.getPath()!!.toPath()

            // If it is a file, except
            if (KFiles.isFile(path)) {
                // TRANSLATOR: Error condition: A directory was expected, but a file was found. {0} is a placeholder for the file.
                throw NetMalformedURLException(JSMsg.gettext("The URL {0} is a file.", orig))
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
                    throw NetMalformedURLException(JSMsg.gettext("The URL {0} could not be created as a directory.", orig))
                }
            }
        }

        /**
         * If the file does not exist, create it. Note this currently only works
         * with file: type URIs
         *
         * @param orig
         *            The file KUri to create
         * @throws NetMalformedURLException
         *                If the KUri is not valid
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(NetMalformedURLException::class, IOException::class)
        fun makeFile(orig: KUri) {
            checkFileURI(orig)

            val path = orig.getPath()!!.toPath()

            // If it is a file, except
            if (KFiles.isDirectory(path)) {
                // TRANSLATOR: Error condition: A file was expected, but a directory was found. {0} is a placeholder for the directory.
                throw NetMalformedURLException(JSMsg.gettext("The URL {0} is a directory.", orig))
            }

            // Is it already a directory ?
            if (!KFiles.isFile(path)) {
                KFiles.createEmptyFile(path)

                // Did that work?
                if (!KFiles.isFile(path)) {
                    // TRANSLATOR: Error condition: A file could not be created. {0} is a placeholder for the file
                    throw NetMalformedURLException(JSMsg.gettext("The URL {0} could not be created as a file.", orig))
                }
            }
        }

        /**
         * If there is a file at the other end of this KUri return true.
         *
         * @param uri
         *            The KUri to check
         * @return true if the KUri points at a file
         */
        @JvmStatic
        fun isFile(uri: KUri): Boolean {
            if (uri.getScheme().equals(PROTOCOL_FILE)) {
                return KFiles.isFile(uri.getPath()!!.toPath())
            }

            try {
                netCheckResource(uri)
                return true
            } catch (ex: IOException) {
                // the resource does not exist!
                return false
            }
        }

        /**
         * If there is a directory at the other end of this KUri return true. Note
         * non file: type KUri will always return false
         *
         * @param orig
         *            The KUri to check
         * @return true if the KUri points at a file: directory
         */
        @JvmStatic
        fun isDirectory(orig: KUri): Boolean {
            if (!orig.getScheme().equals(PROTOCOL_FILE)) {
                return false
            }

            return KFiles.isDirectory(orig.getPath()!!.toPath())
        }

        /**
         * If there is a writable directory or file at the other end of this KUri
         * return true. Note non file: type URIs will always return false
         *
         * @param orig
         *            The KUri to check
         * @return true if the KUri points at a writable file or directory
         */
        @JvmStatic
        fun canWrite(orig: KUri): Boolean {
            if (!orig.getScheme().equals(PROTOCOL_FILE)) {
                return false
            }

            return netCanWrite(orig)
        }

        /**
         * If there is a readable directory or file at the other end of this KUri
         * return true. Note non file: type URIs will always return false
         *
         * @param orig
         *            The KUri to check
         * @return true if the KUri points at a readable file or directory
         */
        @JvmStatic
        fun canRead(orig: KUri): Boolean {
            if (!orig.getScheme().equals(PROTOCOL_FILE)) {
                return false
            }

            return netCanRead(orig)
        }

        /**
         * Move a KUri from one place to another. Currently this only works for file:
         * URIs, however the interface should not need to change to handle more
         * complex URIs
         *
         * @param oldUri
         *            The KUri to move
         * @param newUri
         *            The destination KUri
         * @return whether the move happened
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun move(oldUri: KUri, newUri: KUri): Boolean {
            checkFileURI(oldUri)
            checkFileURI(newUri)

            return KFiles.move(oldUri.getPath()!!.toPath(), newUri.getPath()!!.toPath())
        }

        /**
         * Delete a KUri. Currently this only works for file: URIs, however the
         * interface should not need to change to handle more complex URIs
         *
         * @param orig
         *            The KUri to delete
         * @return whether the deleted happened
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun delete(orig: KUri): Boolean {
            checkFileURI(orig)

            return KFiles.delete(orig.getPath()!!.toPath())
        }



        /**
         * Utility to strip a string from the end of a KUri.
         *
         * @param orig
         *            The KUri to strip
         * @param strip
         *            The text to strip from the end of the KUri
         * @return The stripped KUri
         * @exception NetMalformedURLException
         *                If the KUri does not end in the given text
         */
        @JvmStatic
        @Throws(NetMalformedURLException::class)
        fun shortenURI(orig: KUri, strip: String): KUri {
            var file = orig.getPath()!!
            val lastChar = file[file.length - 1]
            if (isSeparator(lastChar)) {
                file = file.substring(0, file.length - 1)
            }

            val test = file.substring(file.length - strip.length)
            if (test != strip) {
                throw NetMalformedURLException(JSOtherMsg.lookupText("The URL {0} does not end in {1}.", orig, strip))
            }

            val newFile = file.substring(0, file.length - strip.length)

            try {
                return KUri(orig.getScheme(), orig.getUserInfo(), orig.getHost(), orig.getPort(), newFile, "",
                    "")
            } catch (e: Exception) {
                if (!netUriSyntaxFailure(e)) throw e
                throw NetMalformedURLException(JSOtherMsg.lookupText("The URL {0} does not end in {1}.", orig, strip))
            }
        }

        /**
         * Utility to add a string to the end of a KUri.
         *
         * @param orig
         *            The KUri to lengthen
         * @param anExtra
         *            The text to add to the end of the KUri
         * @return The lengthened KUri
         */
        @JvmStatic
        fun lengthenURI(orig: KUri, anExtra: String): KUri? {
            val extra = anExtra
            try {
                val path = StringBuilder(orig.getPath()!!)
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

                return KUri(orig.getScheme(), orig.getUserInfo(), orig.getHost(), orig.getPort(), path.toString(), orig.getQuery(), orig.getFragment())
            } catch (ex: Exception) {
                if (!netUriSyntaxFailure(ex)) throw ex
                return null
            }
        }

        private fun isSeparator(c: Char): Boolean {
            return c == '/' || c == '\\'
        }







        /**
         * List the items available assuming that this KUri points to a directory.
         * <p>
         * There are 2 methods of calculating the answer - if the KUri is a file: KUri
         * then we can just use File.list(), otherwise we ask for a file inside the
         * directory called index.txt and assume the directories contents to be
         * listed one per line.
         * <p>
         * If the KUri is a file: KUri then we execute both methods and warn if there
         * is a difference, but returning the values from the index.txt method.
         *
         * @param uri the KUri to list
         * @param filter the filter for the listing
         * @return the filtered list
         * @throws NetMalformedURLException
         *                If the KUri is not valid
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(NetMalformedURLException::class, IOException::class)
        fun list(uri: KUri, filter: URIFilter?): Array<String>? {
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
                if (uri.getScheme().equals(PROTOCOL_FILE)) {
                    return listByFile(uri, filter)
                }
            }

            // if we can - check that the index file is up to date.
            if (uri.getScheme().equals(PROTOCOL_FILE)) {
                val files = listByFile(uri, filter)

                // Check that the answers are the same
                if (files!!.size != reply.size) {
                    LOGGER.warn("index file for {} has incorrect number of entries.", uri.toString())
                } else {
                    val list = files.asList()
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
         * @param uri the KUri to list
         * @param filter the filter for the listing
         * @return the filtered list
         * @throws NetMalformedURLException
         *                If the KUri is not valid
         */
        @JvmStatic
        @Throws(NetMalformedURLException::class)
        fun listByFile(uri: KUri, filter: URIFilter?): Array<String>? {
            val dir = uri.getPath()!!.toPath()
            if (!KFiles.isDirectory(dir)) {
                // TRANSLATOR: Error condition: A directory was expected, but a file was found. {0} is a placeholder for the file.
                throw NetMalformedURLException(JSMsg.gettext("The URL {0} is not a directory", uri.toString()))
            }

            // null for a directory that cannot be listed, like File.list()
            return KFiles.listOrNull(dir)?.filter { filter!!.accept(it) }?.toTypedArray()
        }

        /**
         * List all the strings specified by the index file passed in. To be
         * acceptable it must be a non-0 length string, not commented with #, and
         * not the index file itself.
         *
         * @param index the KUri to list
         * @return the list.
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun listByIndexFile(index: KUri): Array<String> {
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
         * @param index the KUri to list
         * @param filter the filter for the listing
         * @return the list.
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun listByIndexFile(index: KUri, filter: URIFilter?): Array<String> {
            return netListByIndexFile(index, filter)
        }

        /**
         * Load up properties given by a KUri.
         *
         * @param uri
         *            the location of the properties
         * @return the properties given by the KUri
         * @throws IOException a problem with I/O happened
         */
        @JvmStatic
        @Throws(IOException::class)
        fun loadProperties(uri: KUri): PropertyMap {
            return netLoadProperties(uri)
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
        fun storeProperties(properties: PropertyMap?, uri: KUri, title: String?) {
            netStoreProperties(properties, uri, title)
        }

        /**
         * @param uri
         *            the resource whose size is wanted
         * @return the size of that resource
         */
        @JvmStatic
        suspend fun getSize(uri: KUri): Int {
            return getSize(uri, null, null)
        }

        @JvmStatic
        suspend fun getSize(uri: KUri, proxyHost: String?): Int {
            return getSize(uri, proxyHost, null)
        }

        /** https goes through the host transport (suspend); every other scheme stays a non-suspend local helper. */
        private fun isHttps(uri: KUri): Boolean = uri.getScheme()?.lowercase() == PROTOCOL_HTTPS

        /** The `Int` size of a file, or -1 when it does not fit (what `URLConnection.getContentLength` returned). */
        internal fun fileSizeToInt(size: Long): Int = if (size > Int.MAX_VALUE) -1 else size.toInt()

        @JvmStatic
        suspend fun getSize(uri: KUri, proxyHost: String?, proxyPort: Int?): Int {
            if (uri.getScheme().equals(PROTOCOL_FILE)) {
                val path = uri.getPath()!!.toPath()
                // Plain files only; anything else (missing, directory) keeps the URLConnection behaviour.
                if (KFiles.isFile(path)) {
                    return fileSizeToInt(KFiles.size(path))
                }
            }

            try {
                if (uri.getScheme().equals(PROTOCOL_HTTP)) {
                    val resource = WebResource(uri, proxyHost, proxyPort)
                    val size = resource.getSize()
                    resource.shutdown()
                    return size
                }
                if (isHttps(uri)) {
                    return netHttpsSize(uri)
                }

                return netUrlSize(uri)
            } catch (e: IOException) {
                return 0
            }
        }

        /**
         * When was the given KUri last modified. If no modification time is
         * available then this method return the current time.
         *
         * @param uri the KUri to examine
         * @return the last modified date
         */
        @JvmStatic
        suspend fun getLastModified(uri: KUri): Long {
            return getLastModified(uri, null, null)
        }

        /**
         * When was the given KUri last modified. If no modification time is
         * available then this method return the current time.
         *
         * @param uri the KUri to examine
         * @param proxyHost the proxy host
         * @return the last modified date
         */
        @JvmStatic
        suspend fun getLastModified(uri: KUri, proxyHost: String?): Long {
            return getLastModified(uri, proxyHost, null)
        }

        /**
         * When was the given KUri last modified. If no modification time is
         * available then this method return the current time.
         *
         * @param uri the KUri to examine
         * @param proxyHost the proxy host
         * @param proxyPort the proxy port
         * @return the last modified date
         */
        @JvmStatic
        suspend fun getLastModified(uri: KUri, proxyHost: String?, proxyPort: Int?): Long {
            if (uri.getScheme().equals(PROTOCOL_FILE)) {
                val path = uri.getPath()!!.toPath()
                // Plain files only; anything else (missing, directory) keeps the URLConnection behaviour.
                if (KFiles.isFile(path)) {
                    return KFiles.lastModifiedMillis(path)
                }
            }

            try {
                if (uri.getScheme().equals(PROTOCOL_HTTP)) {
                    val resource = WebResource(uri, proxyHost, proxyPort)
                    val time = resource.getLastModified()
                    resource.shutdown()
                    return time
                }
                if (isHttps(uri)) {
                    return netHttpsLastModified(uri)
                }

                return netUrlLastModified(uri)
            } catch (ex: IOException) {
                LOGGER.warn("Failed to get modified time", ex)
                return currentTimeMillis()
            }
        }

        /**
         * Returns whether the left is newer than the right by comparing their last
         * modified dates.
         *
         * @param left one KUri to compare
         * @param right the other KUri to compare
         * @return true if the left is newer
         */
        @JvmStatic
        suspend fun isNewer(left: KUri, right: KUri): Boolean {
            return isNewer(left, right, null, null)
        }

        /**
         * Returns whether the left is newer than the right by comparing their last
         * modified dates.
         *
         * @param left one KUri to compare
         * @param right the other KUri to compare
         * @param proxyHost the proxy host
         * @return true if the left is newer
         */
        @JvmStatic
        suspend fun isNewer(left: KUri, right: KUri, proxyHost: String?): Boolean {
            return isNewer(left, right, proxyHost, null)
        }

        /**
         * Returns whether the left is newer than the right by comparing their last
         * modified dates.
         *
         * @param left one KUri to compare
         * @param right the other KUri to compare
         * @param proxyHost the proxy host
         * @param proxyPort the proxy port
         * @return true if the left is newer
         */
        @JvmStatic
        suspend fun isNewer(left: KUri, right: KUri, proxyHost: String?, proxyPort: Int?): Boolean {
            return getLastModified(left, proxyHost, proxyPort) > getLastModified(right, proxyHost, proxyPort)
        }

        /**
         * Throw if the given KUri does not use the 'file:' protocol
         *
         * @param uri
         *            The KUri to check
         * @throws NetMalformedURLException
         *             If the protocol is not file:
         */
        @Throws(NetMalformedURLException::class)
        private fun checkFileURI(uri: KUri) {
            if (!uri.getScheme().equals(PROTOCOL_FILE)) {
                // TRANSLATOR: Error condition: The URL protocol "file:" was expected, but something else was found. {0} is a placeholder for the URL.
                throw NetMalformedURLException(JSMsg.gettext("The URL {0} is not a file.", uri))
            }
        }







        /**
         * A KUri version of <code>File.createTempFile()</code>
         *
         * @param prefix the prefix of the temporary file
         * @param suffix the suffix of the temporary file
         * @return A new temporary KUri
         * @throws IOException
         *             If something goes wrong creating the temp KUri
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getTemporaryURI(prefix: String, suffix: String?): KUri {
            return netTemporaryUri(prefix, suffix)
        }





        /**
         * Where are temporary files cached.
         */


        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(NetUtil::class)
    }
}
