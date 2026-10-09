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
 * © CrossWire Bible Society, 2015 - 2016
 */
package org.crosswire.common.util

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Reader
import java.io.Writer
import java.util.ArrayList
import java.util.Collections
import java.util.TreeMap

/**
 * A utility class for loading an INI style, Multimap configuration file.
 * <p>
 * SWORD defines a conf as an INI file with one or more sections.
 * Originally, all modules were described in a single conf, but
 * now each module has its own conf.
 * </p>
 * <p>
 * SWORD will be using a single conf to hold overrides for many
 * modules. This is the motivation for this class as opposed to
 * allowing only a single section as {@link IniSection}.
 * </p>
 * <p>
 * Since the most common use case is for a single section, this
 * implementation has an API for delegating to the first IniSection.
 * </p>
 *
 * This implementation allows for:
 * <ul>
 * <li><strong>Case Insensitive</strong> -- Section names, keys and values are case insensitive.</li>
 * <li><strong>Comments</strong> -- ; and # preceded only by white space indicate that a line is a comment.
 *              Note: SWORD does not support ; but it is present in some 3rd Party repositories such as IBT.</li>
 * <li><strong>Multiple Values</strong> -- Each key can have one or more values.</li>
 * <li><strong>Order</strong> -- Order of sections, keys and values are retained</li>
 * </ul>
 *
 * This implementation does not allow for:
 * <ul>
 * <li><strong>Globals</strong> -- (key,value) pairs before the first section.</li>
 * <li><strong>Quoted Values</strong> -- Values surrounded by "" or ''.
 *              If present they are part of the value.</li>
 * <li><strong>Retaining comments</strong> -- Comments are ignored.</li>
 * <li><strong>Comments after content</strong> -- Comments are on lines to themselves.</li>
 * <li><strong>:</strong> -- as an alternative for =.</li>
 * <li><strong>nulls</strong> -- null values.</li>
 * </ul>
 *
 * @author DM Smith
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.<br>
 */
internal class Ini
/**
 * Create an empty INI Config.
 */
// was package-private (the class too); no Java caller outside the package, but internal compiles to public
internal constructor() {
    /**
     * Start over.
     */
    fun clear() {
        sectionMap.clear()
        list.clear()
    }

    /**
     * Get the number of sections
     *
     * @return the number of known sections
     */
    fun size(): Int {
        return sectionMap.size
    }

    /**
     * Get an unmodifiable collection of the sections in this INI.
     *
     * @return the ordered section names
     */
    val sections: List<String>
        get() = Collections.unmodifiableList(list)

    fun getSectionName(index: Int): String {
        return list[index]
    }

    /**
     * Get the name of the first section.
     *
     * @return the name of the first section or null if there are no sections
     * @throws ArrayIndexOutOfBoundsException if there are no sections
     */
    val sectionName: String?
        get() = if (size() == 0) null else list[0]

    fun getValueSize(sectionName: String, key: String?): Int {
        val section = doGetSection(sectionName)
        return if (section == null) 0 else section.size(key)
    }

    /**
     * Get the number of values for a key in the first section
     *
     * @param key the key
     * @return the number of values for a key in the first section
     */
    fun getValueSize(key: String?): Int {
        val section = section
        return if (section == null) 0 else section.size(key)
    }

    /**
     * Get the value for the key specified by the index and the section.
     *
     * @param sectionName the name of the section
     * @param key the key for the section
     * @param index the index in the list of values
     * @return the value at the specified index
     * @throws ArrayIndexOutOfBoundsException when the index is out of bounds
     */
    fun getValue(sectionName: String, key: String?, index: Int): String? {
        val section = doGetSection(sectionName)
        return if (section == null) null else section.get(key, index)
    }

    /**
     * Get the first value for the key specified by the index and the section.
     *
     * @param sectionName the name of the section
     * @param key the key for the section
     * @return the value at the specified index
     * @throws ArrayIndexOutOfBoundsException when the index is out of bounds
     */
    fun getValue(sectionName: String, key: String?): String? {
        val section = doGetSection(sectionName)
        return if (section == null) null else section.get(key, 0)
    }

    /**
     * Get the value for the key specified by the index for the first section.
     *
     * @param key the key
     * @param index the index
     * @return the value at the specified index
     * @throws ArrayIndexOutOfBoundsException when the index is out of bounds
     */
    fun getValue(key: String?, index: Int): String? {
        val section = section
        return if (section == null) null else section.get(key, index)
    }

    /**
     * Get the first value for the key in the first section.
     *
     * @param key the key
     * @return the value at the specified index
     * @throws ArrayIndexOutOfBoundsException when the index is out of bounds
     */
    fun getValue(key: String?): String? {
        val section = section
        return if (section == null) null else section.get(key)
    }

    /**
     * Add a key/value pair to a section.
     * If the section does not exist, it is created.
     * A null for key or value is not allowed.
     * An empty string for a key is not allowed.
     *
     * @param sectionName the name of the section
     * @param key the key for the section
     * @param value the value for the key
     * @return {@code true} if the element was added or already was present
     */
    fun add(sectionName: String, key: String?, value: String?): Boolean {
        val section = getOrCreateSection(sectionName)
        return section.add(key, value)
    }

    /**
     * Replace a value for a key.
     * A null for key or value is not allowed.
     * An empty string for a key is not allowed.
     *
     * @param sectionName the name of the section
     * @param key the key for the section
     * @param value the value for the key
     * @return {@code true} if the element was added or already was present
     */
    fun replace(sectionName: String, key: String?, value: String?): Boolean {
        val section = getOrCreateSection(sectionName)
        return section.replace(key, value)
    }

    /**
     * Remove the value if present.
     * If it were the last value for the key, the key is removed.
     * If it were the last key, the section is removed.
     *
     * @param sectionName the name of the section
     * @param key the key for the section
     * @param value the value for the key
     * @return whether the value was present and removed
     */
    fun remove(sectionName: String, key: String?, value: String?): Boolean {
        val section = sectionMap[sectionName] ?: return false
        val changed = section.remove(key, value)
        if (changed) {
            if (section.isEmpty) {
                sectionMap.remove(sectionName)
                list.remove(sectionName)
            }
        }

        return changed
    }

    /**
     * Remove the key if present.
     * If it were the last key for the section, the section is removed.
     *
     * @param sectionName the name of the section
     * @param key the key for the section
     * @return whether the key was present and removed
     */
    fun remove(sectionName: String, key: String?): Boolean {
        val section = sectionMap[sectionName] ?: return false
        val changed = section.remove(key)
        sectionMap.remove(sectionName)
        list.remove(sectionName)

        return changed
    }

    // Routines that work on the first section
    /**
     * Get the first section.
     *
     * @return the first section or null if there are no sections
     */
    val section: IniSection?
        get() = if (size() == 0) null else sectionMap[list[0]]

    /**
     * Get the unmodifiable set of keys of the first section.
     * The set has insertion order.
     *
     * @return the keys of the first section
     */
    val keys: Collection<String>?
        get() {
            val section = section
            return if (section == null) null else section.keys
        }

    /**
     * Get the values of a key of the first section.
     * The collection has insertion order.
     * Note many keys only have one value.
     * A key that has no values returns null.
     *
     * @param key the key
     * @return the keyed values or null if the key doesn't exist
     */
    fun getValues(key: String?): Collection<String>? {
        val section = section
        return if (section == null) null else section.getValues(key)
    }

    /**
     * Add a value for the key. Duplicate values are not allowed.
     *
     * @param key the key for the section
     * @param value the value for the key
     * @return whether the value was added or is already present.
     */
    fun addValue(key: String?, value: String?): Boolean {
        val section = section
        return section == null || section.add(key, value)
    }

    /**
     * Remove the value if present in the first section.
     * If it were the last value for the key, the key is removed.
     * If it were the last key, the section is removed.
     *
     * @param key the key for the section
     * @param value the value for the key
     * @return whether the value was present and removed
     */
    fun removeValue(key: String?, value: String?): Boolean {
        val section = sectionName
        return section == null || remove(section, key, value)
    }

    /**
     * Remove the key if present.
     * If it were the last key for the section, the section is removed.
     *
     * @param key the key for the section
     * @return whether the key was present and removed
     */
    fun removeValue(key: String?): Boolean {
        val section = sectionName
        return section == null || remove(section, key)
    }

    /**
     * Replace a value for a key.
     * A null for key or value is not allowed.
     * An empty string for a key is not allowed.
     *
     * @param key the key for the section
     * @param value the value for the key
     * @return {@code true} if the element was added or already was present
     */
    fun replaceValue(key: String?, value: String?): Boolean {
        val section = section
        return section == null || section.replace(key, value)
    }

    @Throws(IOException::class)
    fun load(`is`: InputStream?, encoding: String?) {
        var reader: Reader? = null
        try {
            reader = InputStreamReader(`is`, encoding)
            doLoad(reader)
        } finally {
            if (reader != null) {
                reader.close()
                reader = null
            }
        }
    }

    /**
     * Load the INI from a file using the given encoding.
     *
     * @param file the file to load
     * @param encoding the encoding of the file
     * @throws IOException
     */
    @Throws(IOException::class)
    fun load(file: File?, encoding: String?) {
        var input: InputStream? = null
        try {
            input = FileInputStream(file)
            load(input, encoding)
        } finally {
            if (input != null) {
                input.close()
                input = null
            }
        }
    }

    /**
     * Load the conf from a buffer. This is used to load conf entries from the
     * mods.d.tar.gz file.
     *
     * @param buffer the buffer to load
     * @param encoding the character encoding for this INI
     * @throws IOException
     */
    @Throws(IOException::class)
    fun load(buffer: ByteArray?, encoding: String?) {
        var input: InputStream? = null
        try {
            input = ByteArrayInputStream(buffer)
            load(input, encoding)
        } finally {
            if (input != null) {
                input.close()
                input = null
            }
        }
    }

    /**
     * Save the INI to a file using the given encoding.
     *
     * @param file the file to load
     * @param encoding the encoding of the file
     * @throws IOException
     */
    @Throws(IOException::class)
    fun save(file: File?, encoding: String?) {
        var out: Writer? = null
        try {
            out = OutputStreamWriter(FileOutputStream(file), encoding)
            save(out)
        } finally {
            if (out != null) {
                out.close()
                out = null
            }
        }
    }

    /**
     * Output the Ini to the given Writer.
     *
     * @param out the Writer to which this Ini should be written
     */
    private fun save(out: Writer) {
        var writer: PrintWriter? = null
        if (out is PrintWriter) {
            writer = out
        } else {
            writer = PrintWriter(out)
        }

        for (sectionName in list) {
            val section = doGetSection(sectionName)
            section!!.save(writer)
        }
    }

    private fun doGetSection(sectionName: String): IniSection? {
        return sectionMap[sectionName]
    }

    /**
     * Get a section, creating it if necessary.
     *
     * @param sectionName
     * @return the found or created section
     */
    private fun getOrCreateSection(sectionName: String): IniSection {
        var section = sectionMap[sectionName]
        if (section == null) {
            section = IniSection(sectionName)
            sectionMap[sectionName] = section
            list.add(sectionName)
        }
        return section
    }

    @Throws(IOException::class)
    private fun doLoad(`in`: Reader) {
        var bin: BufferedReader? = null
        try {
            if (`in` is BufferedReader) {
                bin = `in`
            } else {
                // Quiet Android from complaining about using the default
                // BufferReader buffer size.
                // The actual buffer size is undocumented. So this is a good
                // idea any way.
                bin = BufferedReader(`in`, MAX_BUFF_SIZE)
            }

            var sectionName = ""
            val buf = StringBuilder()
            while (true) {
                // Empty out the buffer
                buf.setLength(0)
                val line = advance(bin) ?: break

                if (isSectionLine(line)) {
                    // The conf file contains a leading line of the form [KJV]
                    // This is the acronym by which Sword refers to it.
                    sectionName = line.substring(1, line.length - 1)
                    continue
                }

                // Is this a key line?
                val splitPos = getSplitPos(line)
                if (splitPos < 0) {
                    LOGGER.warn("Expected to see '=' in [{}]: {}", sectionName, line)
                    continue
                }

                val key = line.substring(0, splitPos).trim { it <= ' ' }
                if (key.length == 0) {
                    LOGGER.warn("Empty key in [{}]: {}", sectionName, line)
                }
                val value = more(bin, line.substring(splitPos + 1).trim { it <= ' ' })
                add(sectionName, key, value)
            }
        } finally {
            if (bin != null) {
                bin.close()
                bin = null
            }
        }
    }

    /**
     * Get the next line from the input
     *
     * @param bin The reader to get data from
     * @return the next line or null if there is nothing more
     * @throws IOException if encountered
     */
    @Throws(IOException::class)
    private fun advance(bin: BufferedReader): String? {
        // Get the next non-blank, non-comment line
        var trimmed: String? = null
        var line = bin.readLine()
        while (line != null) {
            // Remove leading and trailing whitespace
            trimmed = line.trim { it <= ' ' }

            // skip blank and comment lines
            if (!isCommentLine(trimmed)) {
                return trimmed
            }
            line = bin.readLine()
        }
        return null
    }

    /**
     * Determine if the given line is a blank or a comment line.
     *
     * @param line The line to check.
     * @return true if the line is empty or starts with one of the comment
     *         characters
     */
    private fun isCommentLine(line: String?): Boolean {
        if (line == null) {
            return false
        }
        if (line.length == 0) {
            return true
        }
        val firstChar = line[0]
        return firstChar == ';' || firstChar == '#'
    }

    /**
     * Is this line a [section]?
     *
     * @param line The line to check.
     * @return true if the line designates a section
     */
    private fun isSectionLine(line: String): Boolean {
        return line[0] == '[' && line[line.length - 1] == ']'
    }

    /**
     * Does this line of text represent a key/value pair?
     *
     * @param line The line to check.
     * @return the position of the split position or -1
     */
    private fun getSplitPos(line: String): Int {
        return line.indexOf('=')
    }

    /**
     * Get continuation lines, if any.
     */
    @Throws(IOException::class)
    private fun more(bin: BufferedReader, value: String): String {
        var moreCowBell = false
        var line: String? = value
        val buf = StringBuilder()

        do {
            moreCowBell = more(line!!)
            if (moreCowBell) {
                line = line.substring(0, line.length - 1).trim { it <= ' ' }
            }
            buf.append(line)
            if (moreCowBell) {
                buf.append('\n')
                line = advance(bin)
            }
        } while (moreCowBell && line != null)
        return buf.toString()
    }

    /**
     * A map of sections by section names.
     */
    private var sectionMap: MutableMap<String, IniSection> = TreeMap(String.CASE_INSENSITIVE_ORDER)

    /**
     * Indexed list of sections maintaining insertion order.
     */
    private var list: MutableList<String> = ArrayList()

    private companion object {
        /**
         * Is there more following this line
         *
         * @param line the trimmed string to check
         * @return whether this line continues
         */
        private fun more(line: String): Boolean {
            val length = line.length
            return length > 0 && line[length - 1] == '\\'
        }

        /**
         * Buffer size is based on file size but keep it with within reasonable limits
         */
        private const val MAX_BUFF_SIZE = 8 * 1024

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(Ini::class)
    }
}
