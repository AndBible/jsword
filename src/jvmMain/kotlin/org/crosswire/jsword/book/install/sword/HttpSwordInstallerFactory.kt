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

import java.util.regex.Pattern

import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.install.Installer
import org.crosswire.jsword.book.install.InstallerFactory

/**
 * A Factory for instances of HttpSwordInstaller.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Mark Goodwin
 * @author Joe Walker
 * @author DM Smith
 */
open class HttpSwordInstallerFactory : InstallerFactory {
    /*
     * (non-Javadoc)
     * 
     * @see org.crosswire.jsword.book.install.InstallerFactory#createInstaller()
     */
    override fun createInstaller(): Installer {
        return HttpSwordInstaller()
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.crosswire.jsword.book.install.InstallerFactory#createInstaller(java
     * .lang.String)
     */
    override fun createInstaller(url: String): Installer {
        val parts = commaPattern.split(url, 6)
        when (parts.size) {
            4 -> return createOldInstaller(parts)
            6 -> return createInstaller(parts)
            else -> throw IllegalArgumentException(JSOtherMsg.lookupText("Not enough / symbols in url: {0}", url))
        }
    }

    private fun createInstaller(parts: Array<String>): Installer {
        val reply: AbstractSwordInstaller = HttpSwordInstaller()

        reply.setHost(parts[0])
        reply.setPackageDirectory(parts[1])
        reply.setCatalogDirectory(parts[2])
        if (parts[3].length > 0) {
            reply.setProxyHost(parts[3])
            if (parts[4].length > 0) {
                reply.setProxyPort(Integer.valueOf(parts[4]))
            }
        }

        return reply
    }

    private fun createOldInstaller(parts: Array<String>): Installer {
        val reply: AbstractSwordInstaller = HttpSwordInstaller()

        reply.setHost(parts[0])
        reply.setPackageDirectory(parts[1] + '/' + PACKAGE_DIR)
        reply.setCatalogDirectory(parts[1] + '/' + LIST_DIR)
        if (parts[2].length > 0) {
            reply.setProxyHost(parts[2])
            if (parts[3].length > 0) {
                reply.setProxyPort(Integer.valueOf(parts[3]))
            }
        }

        return reply
    }

    private val commaPattern: Pattern = Pattern.compile(",")

    companion object {
        /**
         * The relative path of the dir holding the zip files
         */
        protected const val PACKAGE_DIR: String = "packages/rawzip"

        /**
         * The relative path of the dir holding the index file
         */
        private const val LIST_DIR: String = "raw"
    }
}
