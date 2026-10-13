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

import org.crosswire.common.progress.Progress
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.install.DownloadCancelledException
import org.crosswire.jsword.book.install.DownloadException
import okio.IOException
import okio.BufferedSink
import okio.buffer

/**
 * A WebResource is backed by an URL and potentially the proxy through which it
 * need go. It can get basic information about the resource and it can get the
 * resource. The requests are subject to a timeout, which can be set via the
 * constructor or previously by a call to set the default timeout. The initial
 * default timeout is 750 milliseconds.
 *
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class WebResource {
    /**
     * Construct a WebResource for the given URL, while timing out if too much
     * time has passed.
     *
     * @param theURI
     *            the Resource to get via HTTP
     */
    constructor(theURI: KUri?) : this(theURI, null, null, timeout.toLong())

    /**
     * Construct a WebResource for the given URL, while timing out if too much
     * time has passed.
     *
     * @param theURI
     *            the Resource to get via HTTP
     * @param theTimeout
     *            the length of time in milliseconds to allow a connection to
     *            respond before timing out
     */
    constructor(theURI: KUri?, theTimeout: Int) : this(theURI, null, null, theTimeout.toLong())

    /**
     * Construct a WebResource for the given URL, going through the optional
     * proxy and default port, while timing out if too much time has passed.
     *
     * @param theURI
     *            the Resource to get via HTTP
     * @param theProxyHost
     *            the proxy host or null
     */
    constructor(theURI: KUri?, theProxyHost: String?) : this(theURI, theProxyHost, null, timeout.toLong())

    /**
     * Construct a WebResource for the given URL, going through the optional
     * proxy and default port, while timing out if too much time has passed.
     *
     * @param theURI
     *            the Resource to get via HTTP
     * @param theProxyHost
     *            the proxy host or null
     * @param theTimeout
     *            the length of time in milliseconds to allow a connection to
     *            respond before timing out
     */
    constructor(theURI: KUri?, theProxyHost: String?, theTimeout: Int) : this(theURI, theProxyHost, null, theTimeout.toLong())

    /**
     * Construct a WebResource for the given URL, going through the optional
     * proxy and port, while timing out if too much time has passed.
     *
     * @param theURI
     *            the Resource to get via HTTP
     * @param theProxyHost
     *            the proxy host or null
     * @param theProxyPort
     *            the proxy port or null, where null means use the standard port
     */
    constructor(theURI: KUri?, theProxyHost: String?, theProxyPort: Int?) : this(theURI, theProxyHost, theProxyPort, timeout.toLong())

    /**
     * Construct a WebResource for the given URL, going through the optional
     * proxy and port, while timing out if too much time has passed.
     *
     * @param theURI
     *            the Resource to get via HTTP
     * @param theProxyHost
     *            the proxy host or null
     * @param theProxyPort
     *            the proxy port or null, where null means use the standard port
     * @param theTimeout
     *            the length of time in milliseconds to allow a connection to
     *            respond before timing out
     */
    constructor(theURI: KUri?, theProxyHost: String?, theProxyPort: Int?, theTimeout: Long) {
        uri = theURI
    }

    /**
     * When this WebResource is no longer needed it should be shutdown to return
     * underlying resources back to the OS.
     */
    open fun shutdown() {}

    /**
     * Determine the size of this WebResource.
     * <p>
     * Note that the http client may read the entire file to determine this.
     * </p>
     *
     * @return the size of the file
     */
    open val size: Int
        get() {
            try {
                val response = requireTransport().head(uri!!, timeout)
                if (response.status == 200) {
                    return response.contentLength
                }
                val reason = response.reason
                // TRANSLATOR: Common error condition: {0} is a placeholder for the
                // URL of what could not be found.
                Reporter.informUser(this, JSMsg.gettext("Unable to find: {0}", reason + ':' + uri!!.getPath()))
            } catch (e: IOException) {
                return 0
            }
            return 0
        }

    /**
     * Determine the last modified date of this WebResource.
     * <p>
     * Note that the http client may read the entire file.
     * </p>
     *
     * @return the last mod date of the file
     */
    open val lastModified: Long
        get() {
            try {
                val response = requireTransport().head(uri!!, timeout)
                if (response.status == 200) {
                    return response.lastModified
                }
                val reason = response.reason
                // TRANSLATOR: Common error condition: {0} is a placeholder for the
                // URL of what could not be found.
                Reporter.informUser(this, JSMsg.gettext("Unable to find: {0}", reason + ':' + uri!!.getPath()))
            } catch (e: IOException) {
                return currentTimeMillis()
            }
            return currentTimeMillis()
        }

    /**
     * Copy this WebResource to the destination and report progress.
     *
     * @param dest
     *            the URI of the destination, typically a file:///.
     * @param meter
     *            the job on which to report progress
     * @throws LucidException when an error is encountered
     */
    @Throws(LucidException::class)
    open fun copy(dest: KUri?, meter: Progress?) {
        var `in`: HttpBody? = null
        var out: BufferedSink? = null
        var statusCode = 0

        try {
            `in` = requireTransport().get(uri!!, timeout)
            // Initialize the meter, if present
            if (meter != null) {
                // Find out how big it is
                var size = `in`.contentLength
                // Sometimes the Content-Length is not given and we have to grab it via HEAD method
                if (size == 0) {
                    size = this.size
                }
                meter.totalWork = size
            }
            statusCode = `in`.status
            `in`.prepareRead()

            // Download the index file
            out = webResourceOutputSink(dest!!).buffer()

            val buf = ByteArray(4096)
            var count = `in`.read(buf)
            while (-1 != count) {
                if (meter != null) {
                    meter.incrementWorkDone(count)
                }
                out.write(buf, 0, count)
                out.emit()
                count = `in`.read(buf)
            }
        } catch (e: TransportCancelledException) {
            throw DownloadCancelledException(uri)
        } catch (e: IOException) {
            // TRANSLATOR: Common error condition: {0} is a placeholder for the
            // URL of what could not be found.
            if (statusCode != 0) {
                throw DownloadException(uri, statusCode)
            }
            throw LucidException(JSMsg.gettext("Unable to find: {0}", uri.toString()), e)
        } finally {
            // Close the streams
            IOUtil.close(`in`)
            IOUtil.close(out)
        }
    }

    /**
     * Copy this WebResource to the destination.
     *
     * @param dest the destination URI
     * @throws LucidException when an error is encountered
     */
    @Throws(LucidException::class)
    open fun copy(dest: KUri?) {
        copy(dest, null)
    }

    private var uri: KUri?
    //private CloseableHttpClient client;

    companion object {
        @JvmStatic
        var transport: HttpTransport? = defaultHttpTransport()

        private fun requireTransport(): HttpTransport =
            transport ?: throw IllegalStateException("No HttpTransport set")

        @JvmStatic
        fun setHostnameWhitelist(hostnameWhitelist: List<String>?) {
            WebResource.hostnameWhitelist = hostnameWhitelist
        }

        /**
         * @return the timeout in milliseconds
         */
        @JvmStatic
        fun getTimeout(): Int {
            return timeout
        }

        /**
         * @param timeout
         *            the timeout to set in milliseconds
         */
        @JvmStatic
        fun setTimeout(timeout: Int) {
            WebResource.timeout = timeout
        }

        /**
         * Define a 750 ms timeout to get a connection
         */
        private var timeout = 750

        private var hostnameWhitelist: List<String>? = null
    }
}
