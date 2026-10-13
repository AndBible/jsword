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

import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.sword.state.GenBookBackendState
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.crosswire.jsword.passage.DefaultKeyList
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.TreeKey
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import okio.IOException

/**
 * Backend for General Books.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class GenBookBackend
/**
 * Simple ctor
 * 
 * @param sbmd 
 */
(sbmd: SwordBookMetaData?) : AbstractBackend<GenBookBackendState>(sbmd) {
    /**
     * The raw index file
     */
    private val index: TreeKeyIndex = TreeKeyIndex(sbmd)

    @Throws(BookException::class)
    override fun initState(): GenBookBackendState {
        return OpenFileStateManager.instance().getGenBookBackendState(bookMetaData)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return getRawTextLength(key) > 0
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#size(org.crosswire.jsword.passage.Key)
     */
    override fun getRawTextLength(key: Key?): Int {
        try {
            val node = find(key)

            if (node == null) {
                return 0
            }

            val userData = node.getUserData()

            // Some entries may be empty.
            if (userData.size == 8) {
                return SwordUtil.decodeLittleEndian32(userData, 4)
            }

            return 0
        } catch (e: IOException) {
            return 0
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.StatefulFileBackedBackend#readRawContent(org.crosswire.jsword.book.sword.state.OpenFileState, org.crosswire.jsword.passage.Key)
     */
    @Throws(IOException::class, BookException::class)
    override fun readRawContent(state: GenBookBackendState, key: Key?): String {
        val node = find(key)

        if (node == null) {
            // TRANSLATOR: Error condition: Indicates that something could
            // not be found in the book.
            // {0} is a placeholder for the unknown key.
            // {1} is the short name of the book
            throw BookException(JSMsg.gettext("No entry for '{0}' in {1}.", key!!.getName(), bookMetaData.initials))
        }

        val userData = node.getUserData()

        // Some entries may be empty.
        if (userData.size == 8) {
            val start = SwordUtil.decodeLittleEndian32(userData, 0)
            val size = SwordUtil.decodeLittleEndian32(userData, 4)
            val data = SwordUtil.readRAF(state.getBdtRaf(), start.toLong(), size)
            decipher(data)
            return SwordUtil.decode(key!!.getName(), data, bookMetaData.bookCharset)
        }

        return ""
    }

    /**
     * Given a Key, find the TreeNode for it.
     * 
     * @param key
     * The key to use for searching
     * @return the found node, null otherwise
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun find(key: Key?): TreeNode? {
        // We need to search from the root, so navigate to the root, saving as
        // we go.
        val path: MutableList<String> = ArrayList()
        var parentKey: Key? = key
        while (parentKey != null && parentKey.getName().length > 0) {
            path.add(parentKey.getName())
            parentKey = parentKey.getParent()
        }

        var node: TreeNode? = index.getRoot()

        node = index.getFirstChild(node!!)

        for (i in path.size - 1 downTo 0) {
            val name = path[i]

            // Search among the siblings for the current level.
            while (node != null && name != node.name) {
                if (node.hasNextSibling()) {
                    node = index.getNextSibling(node)
                } else {
                    log.error("Could not find {}", name)
                    node = null
                }
            }

            // If we have found it but have not exhausted the path
            // we need to get more
            if (node != null && name == node.name && i > 0) {
                node = index.getFirstChild(node)
            }
        }

        // At this point we have either found it, returning it or have not,
        // returning null
        if (node != null && node.name == key!!.getName()) {
            return node
        }

        return null
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun readIndex(): Key {
        val bmd = bookMetaData
        var reply: Key = DefaultKeyList(null, bmd.name)

        try {
            val node = index.getRoot()
            reply = TreeKey(node.name, null)
            doReadIndex(node, reply)
        } catch (e: IOException) {
            log.error("Could not get read GenBook index", e)
        }

        return reply
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    @Throws(IOException::class)
    override fun setAliasKey(state: GenBookBackendState, alias: Key?, source: Key?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#setRawText(org.crosswire.jsword.passage.Key, java.lang.String)
     */
    @Throws(BookException::class, IOException::class)
    override fun setRawText(state: GenBookBackendState, key: Key?, text: String?) {
        throw UnsupportedOperationException()
    }

    /**
     * A helper function to recursively read the entire tree.
     * 
     * @param parentNode
     * the current node whose children are being sought
     * @param parentKey
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun doReadIndex(parentNode: TreeNode, parentKey: Key) {
        val currentNode = parentNode
        if (currentNode.hasChildren()) {
            var childNode = index.getFirstChild(currentNode)
            do {
                val childKey = TreeKey(childNode.name, parentKey)
                parentKey.addAll(childKey)

                // Build the tree as deep as possible
                doReadIndex(childNode, childKey)

                if (!childNode.hasNextSibling()) {
                    break
                }

                childNode = index.getNextSibling(childNode)
            } while (true)
        }
    }

    private companion object {
        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(GenBookBackend::class)
    }
}
