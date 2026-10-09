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

import java.net.URI
import java.net.URISyntaxException

import org.crosswire.common.progress.Progress
import org.crosswire.common.util.LucidException
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.WebResource
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
open class HttpSwordInstaller : AbstractSwordInstaller() {

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#getType()
     */
    override fun getType(): String {
        return "sword-http"
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#getSize(org.crosswire.jsword.book.Book)
     */
    override fun getSize(book: Book): Int {
        return NetUtil.getSize(toRemoteURI(book)!!, proxyHost, proxyPort)
    }

    override fun indexLastUpdated(): Long? {
        return 0L
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#toRemoteURI(org.crosswire.jsword.book.Book)
     */
    override fun toRemoteURI(book: Book): URI? {
        try {
            return URI(NetUtil.PROTOCOL_HTTP, host, packageDirectory + '/' + book.initials + ZIP_SUFFIX, null)
        } catch (e: URISyntaxException) {
            return null
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.sword.AbstractSwordInstaller#download(org.crosswire.common.progress.Progress, java.lang.String, java.lang.String, java.net.URI)
     */
    @Throws(InstallException::class)
    override fun download(job: Progress?, dir: String?, file: String?, dest: URI?) {
        val uri: URI
        try {
            uri = URI(NetUtil.PROTOCOL_HTTP, host, dir + '/' + file, null)
        } catch (ex: URISyntaxException) {
            // TRANSLATOR: Common error condition: {0} is a placeholder for the URL of what could not be found.
            throw InstallException(JSMsg.gettext("Unable to find: {0}", dir + '/' + file), ex)
        }

        try {
            copy(job, uri, dest)
        } catch (ex: LucidException) {
            // TRANSLATOR: Common error condition: {0} is a placeholder for the URL of what could not be found.
            throw InstallException(JSMsg.gettext("Unable to find: {0}", uri.toString()), ex)
        }
    }

    /**
     * @param job
     * @param uri
     * @param dest
     * @throws LucidException
     */
    @Throws(LucidException::class)
    private fun copy(job: Progress?, uri: URI, dest: URI?) {
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
        if (other !is HttpSwordInstaller) {
            return false
        }
        val that: HttpSwordInstaller = other

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
