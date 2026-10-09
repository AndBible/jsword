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
package org.crosswire.jsword.book.sword

/**
 * A utility class for loading the entries in a Sword book's conf file. Since
 * the conf files are manually maintained, there can be all sorts of errors in
 * them. This class does robust checking and reporting.
 * <p/>
 * <p/>
 * Config file format. See also: <a href=
 * "http://sword.sourceforge.net/cgi-bin/twiki/view/Swordapi/ConfFileLayout">
 * http://sword.sourceforge.net/cgi-bin/twiki/view/Swordapi/ConfFileLayout</a>
 * <p/>
 * <p/>
 * The contents of the About field are in rtf.
 * <p/>
 * \ is used as a continuation line.
 *
 * @author Mark Goodwin
 * @author Joe Walker
 * @author Jacky Cheung
 * @author DM Smith
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.<br>
 * The copyright to this program is held by its authors.
 */
class ConfigEntryTable
/**
 * Sometimes, we're creating the config off another config, and so need to ensure the initials match exactly.
 *
 * @param bookName the name of the book
 * @param isRootConfig true to indicate a root configuration
 * @param initials the set of initials used to identify this module. This could be different to the bookName (which may be lowercase)
 */
(bookName: String?, isRootConfig: Boolean, initials: String?) {
    /**
     * Create an empty Sword config for the named book.
     *
     * @param bookName the name of the book
     * @param isRootConfig true to indicate a root configuration
     */
    constructor(bookName: String?, isRootConfig: Boolean) : this(bookName, isRootConfig, null)

    /**
     * The set of initials identifying this book
     */
    private val initials: String? = initials ?: bookName

    /**
     * Build's a SWORD conf file as a string. The result is not identical to the
     * original, cleaning up problems in the original and re-arranging the
     * entries into a predictable order.
     *
     * @return the well-formed conf.
     */
    fun toConf(): String {
        val buf = StringBuilder()
        buf.append('[')
        buf.append(initials)
        buf.append("]\n")
        return buf.toString()
    }

    // The commented-out Java members (report(), toConf(...), RTF_PATTERN) of the original are omitted.
}
