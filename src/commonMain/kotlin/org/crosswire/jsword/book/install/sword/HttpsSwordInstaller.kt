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
package org.crosswire.jsword.book.install.sword

import org.crosswire.common.progress.Progress
import org.crosswire.common.util.KUri
import org.crosswire.common.util.LucidException
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.WebResource
import org.crosswire.common.util.netUriSyntaxFailure
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.install.InstallException

/**
 * An implementation of Installer for reading data from Sword Web sites.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Mark Goodwin
 * @author Joe Walker
 * @author DM Smith
 */
open class HttpsSwordInstaller : AbstractSwordInstaller() {

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#getType()
     */
    override fun getType(): String {
        return "sword-https"
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#getSize(org.crosswire.jsword.book.Book)
     */
    override suspend fun getSize(book: Book): Int {
        return NetUtil.getSize(toRemoteURI(book)!!, proxyHost, proxyPort)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#toRemoteURI(org.crosswire.jsword.book.Book)
     */
    override fun toRemoteURI(book: Book): KUri? {
        try {
            return httpsUri(packageDirectory + '/' + book.initials + ZIP_SUFFIX)
        } catch (e: Exception) {
            if (!netUriSyntaxFailure(e)) throw e
            return null
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.sword.AbstractSwordInstaller#download(org.crosswire.common.progress.Progress, java.lang.String, java.lang.String, KUri)
     */
    @Throws(InstallException::class, kotlin.coroutines.cancellation.CancellationException::class)
    override suspend fun download(job: Progress?, dir: String?, file: String?, dest: KUri?) {
        val uri: KUri
        try {
            uri = httpsUri(joinPath(dir, file))
        } catch (ex: Exception) {
            if (!netUriSyntaxFailure(ex)) throw ex
            // TRANSLATOR: Common error condition: {0} is a placeholder for the URL of what could not be found.
            throw InstallException(JSMsg.gettext("Unable to find: {0}", dir + '/' + file), ex)
        }

        try {
            copy(job, uri, dest)
        } catch (ex: InstallException) {
            throw ex
        } catch (ex: LucidException) {
            // TRANSLATOR: Common error condition: {0} is a placeholder for the URL of what could not be found.
            throw InstallException(JSMsg.gettext("Unable to find: {0}", uri.toString()), ex)
        }
    }

    /** `dir/file` without doubling the slash when [dir] is "/" or ends with one. */
    private fun joinPath(dir: String?, file: String?): String = dir.orEmpty().trimEnd('/') + '/' + file

    /**
     * An https URI on [host], which may carry an explicit port (`host:8443`, `[::1]:8443`). The port
     * is split off so that it stays a port: passing `host:port` as the host argument is read as a
     * malformed IPv6 address. Without a port the URI has none, so a default-port repository is unchanged.
     */
    private fun httpsUri(path: String): KUri {
        val h = host
        val colon = h?.lastIndexOf(':') ?: -1
        // A trailing :digits is a port only when what precedes it is a plain host or a bracketed IPv6
        // literal; a bare IPv6 address (fe80::1, ::1) has more colons and no port.
        val port = if (h != null && colon >= 0 && h.substring(colon + 1).let { it.isNotEmpty() && it.all { c -> c in '0'..'9' } } &&
            h.substring(0, colon).let { !it.contains(':') || it.endsWith("]") }) {
            h.substring(colon + 1).toIntOrNull()
        } else null
        if (h == null || port == null) return KUri(NetUtil.PROTOCOL_HTTPS, h, path, null)
        // 443 is the https default: an explicit :443 gives the same URI as no port.
        if (port == 443) return KUri(NetUtil.PROTOCOL_HTTPS, h.substring(0, colon), path, null)
        return KUri(NetUtil.PROTOCOL_HTTPS, null, h.substring(0, colon), port, path, null, null)
    }

    override suspend fun indexLastUpdated(): Long? {
        try {
            val uri = httpsUri(joinPath(catalogDirectory, FILE_LIST_GZ))
            val transport = WebResource.transport ?: return 0L
            return transport.head(uri, WebResource.getTimeout()).lastModified
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return 0L
        }
    }

    /**
     * @param job
     * @param uri
     * @param dest
     * @throws LucidException
     */
    @Throws(LucidException::class, kotlin.coroutines.cancellation.CancellationException::class)
    private suspend fun copy(job: Progress?, uri: KUri, dest: KUri?) {
        if (job != null) {
            // TRANSLATOR: Progress label for downloading one or more files.
            job.sectionName = JSMsg.gettext("Downloading files")
        }

        val wr = WebResource(uri, proxyHost, proxyPort)
        wr.copy(dest, job)
        wr.shutdown()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.sword.AbstractSwordInstaller#equals(java.lang.Object)
     */
    override fun equals(other: Any?): Boolean {
        if (other !is HttpsSwordInstaller) {
            return false
        }
        val that: HttpsSwordInstaller = other

        if (!super.equals(that)) {
            return false
        }

        return true
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.sword.AbstractSwordInstaller#hashCode()
     */
    override fun hashCode(): Int {
        return super.hashCode()
    }
}
