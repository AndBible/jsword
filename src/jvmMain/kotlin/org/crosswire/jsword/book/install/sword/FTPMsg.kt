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

import org.crosswire.common.util.MsgBase

/**
 * Compile safe Msg resource settings.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
internal class FTPMsg
/**
 * Passthrough ctor
 */
private constructor(
    /**
     * @return the name
     */
    val name: String
) : MsgBase("FTPMsg") {
    // was package-private: Kotlin has no package visibility, internal compiles to public
    companion object {
        @JvmField
        val AUTH_REFUSED: MsgBase = FTPMsg("SwordInstaller.AuthRefused")
        @JvmField
        val CONNECT_REFUSED: MsgBase = FTPMsg("SwordInstaller.ConnectRefused")
        @JvmField
        val CWD_REFUSED: MsgBase = FTPMsg("SwordInstaller.CWDRefused")
        @JvmField
        val DOWNLOAD_REFUSED: MsgBase = FTPMsg("SwordInstaller.DownloadRefused")
        @JvmField
        val URL_AT_COUNT: MsgBase = FTPMsg("SwordInstallerFactory.URLAtCount")
        @JvmField
        val URL_COLON_COUNT: MsgBase = FTPMsg("SwordInstallerFactory.URLColonCount")
    }
}
