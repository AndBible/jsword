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

import org.crosswire.common.activate.Activatable
import org.crosswire.common.activate.Activator
import org.crosswire.common.activate.Lock
import org.crosswire.common.util.FileUtil
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.Reporter
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.passage.DefaultKeyList
import org.crosswire.jsword.passage.Key
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * TreeKeyIndex reads Sword index files that are path based. Paths are of the
 * form /a/b/c, and can be of any depth. The ultimate output of a TreeKeyIndex
 * is the offset and length of a chunk of data in another file that can be read.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class TreeKeyIndex
/**
 * Simple ctor
 * 
 * @param sbmd 
 */
(sbmd: SwordBookMetaData?) : Activatable {
    private var bmd: SwordBookMetaData? = sbmd
    private var idxFile: File? = null
    private var datFile: File? = null
    private var idxRaf: RandomAccessFile? = null
    private var datRaf: RandomAccessFile? = null
    private var active: Boolean = false

    /**
     * @return the root TreeNode for the module.
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun getRoot(): TreeNode {
        return getTreeNode(getOffset(0))
    }

    /**
     * Get the parent of the TreeNode.
     * 
     * @param node
     * the node being worked upon
     * @return the parent node
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun getParent(node: TreeNode): TreeNode {
        return getTreeNode(getOffset(node.parent))
    }

    /**
     * Get the first child of the TreeNode.
     * 
     * @param node
     * the node being worked upon
     * @return the first child node
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun getFirstChild(node: TreeNode): TreeNode {
        return getTreeNode(getOffset(node.firstChild))
    }

    /**
     * Get the next sibling of the TreeNode.
     * 
     * @param node
     * the node being worked upon
     * @return the next sibling node
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun getNextSibling(node: TreeNode): TreeNode {
        return getTreeNode(getOffset(node.nextSibling))
    }

    /**
     * The idx file contains offsets into the dat file.
     * 
     * @param index
     * the record id
     * @return an offset into the dat file
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun getOffset(index: Int): Int {
        if (index == -1) {
            return -1
        }

        checkActive()
        val buffer = SwordUtil.readRAF(idxRaf, index.toLong(), 4)
        return SwordUtil.decodeLittleEndian32(buffer, 0)
    }

    /**
     * Given an offset get the TreeNode from the dat file.
     * 
     * @param offset
     * start of a TreeNode record in the dat file.
     * @return the TreeNode
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun getTreeNode(offset: Int): TreeNode {
        val node = TreeNode(offset)

        if (offset == -1) {
            return node
        }

        checkActive()
        var buffer = SwordUtil.readRAF(datRaf, offset.toLong(), 12)
        node.parent = SwordUtil.decodeLittleEndian32(buffer, 0)
        node.nextSibling = SwordUtil.decodeLittleEndian32(buffer, 4)
        node.firstChild = SwordUtil.decodeLittleEndian32(buffer, 8)

        buffer = SwordUtil.readUntilRAF(datRaf, 0.toByte())
        var size = buffer.size
        if (buffer[size - 1].toInt() == 0) {
            size--
        }

        val key: Key = DefaultKeyList(null, bmd!!.name)
        // Some of the keys have extraneous whitespace, so remove it.
        node.name = SwordUtil.decode(key.getName(), buffer, size, bmd!!.bookCharset).trim()

        buffer = SwordUtil.readNextRAF(datRaf, 2)
        val userDataSize = SwordUtil.decodeLittleEndian16(buffer, 0)
        if (userDataSize > 0) {
            node.userData = SwordUtil.readNextRAF(datRaf, userDataSize)
        }

        return node
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.activate.Activatable#activate(org.crosswire.common.activate.Lock)
     */
    final override fun activate(lock: Lock?) {
        val path: String
        try {
            path = getExpandedDataPath()
        } catch (e: BookException) {
            Reporter.informUser(this, e)
            return
        }

        val idx = File(path + EXTENSION_INDEX)
        idxFile = idx
        val dat = File(path + EXTENSION_DATA)
        datFile = dat

        if (!idx.canRead()) {
            // TRANSLATOR: Common error condition: The file could not be read. There can be many reasons.
            // {0} is a placeholder for the file.
            Reporter.informUser(this, BookException(JSMsg.gettext("Error reading {0}", idx.absolutePath)))
            return
        }

        if (!dat.canRead()) {
            // TRANSLATOR: Common error condition: The file could not be read. There can be many reasons.
            // {0} is a placeholder for the file.
            Reporter.informUser(this, BookException(JSMsg.gettext("Error reading {0}", dat.absolutePath)))
            return
        }

        try {
            idxRaf = RandomAccessFile(idx, FileUtil.MODE_READ)
            datRaf = RandomAccessFile(dat, FileUtil.MODE_READ)
        } catch (ex: IOException) {
            log.error("failed to open files", ex)
            idxRaf = null
            datRaf = null
        }
        active = true
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.activate.Activatable#deactivate(org.crosswire.common.activate.Lock)
     */
    final override fun deactivate(lock: Lock?) {
        try {
            if (idxRaf != null) {
                idxRaf!!.close()
            }
            if (datRaf != null) {
                datRaf!!.close()
            }
        } catch (ex: IOException) {
            log.error("failed to close nt files", ex)
        } finally {
            idxRaf = null
            datRaf = null
        }
        active = false
    }

    /**
     * Helper method so we can quickly activate ourselves on access
     */
    protected fun checkActive() {
        if (!active) {
            Activator.activate(this)
        }
    }

    @Throws(BookException::class)
    private fun getExpandedDataPath(): String {
        val loc = NetUtil.lengthenURI(bmd!!.library!!, bmd!!.getProperty(SwordBookMetaData.KEY_DATA_PATH)!!)

        if (loc == null) {
            // FIXME(DMS): missing parameter
            throw BookException(JSOtherMsg.lookupText("Missing data files for old and new testaments in {0}."))
        }

        return File(loc.path).absolutePath
    }

    private companion object {
        private const val EXTENSION_INDEX = ".idx"
        private const val EXTENSION_DATA = ".dat"

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(TreeKeyIndex::class.java)
    }
}
