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
import java.util.HashMap

/**
 * A utility class for a section of an INI style configuration file.
 * Keys and values are maintained in insertion order. A key may have more than one value.
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
class IniSection : Iterable<Any?> {
    /**
     * Create an empty INI config without a name.
     */
    constructor() : this(null as String?)

    /**
     * Create an empty INI Config.
     * @param name the section name
     */
    constructor(name: String?) {
        this.name = name
        section = HashMap()
        warnings = StringBuilder()
    }

    /**
     * Copy constructor
     *
     * @param config the config to copy
     */
    constructor(config: IniSection) {
        this.name = config.name
        section = HashMap()
        for (key in config.keys) {
            for (value in config.getValues(key)!!) {
                add(key, value)
            }
        }
    }

    /**
     * Start over.
     */
    fun clear() {
        section.clear()
        warnings!!.setLength(0)
        warnings!!.trimToSize()
        report = ""
    }

    /**
     * The [name] of this section. Set the name of this INI config.
     */
    var name: String?

    /**
     * Get the number of keys in this section.
     *
     * @return the count
     */
    fun size(): Int {
        return section.size
    }

    /**
     * Get the number of values for a key.
     *
     * @param key the key
     * @return the number of values for a key or 0 if the key does not exist.
     */
    fun size(key: String?): Int {
        val values: Collection<String>? = valuesOf(key)
        return if (values == null) 0 else values.size
    }

    /**
     * Determine whether this section has any keys
     *
     * @return {@code true} if this section is empty
     */
    val isEmpty: Boolean
        get() = section.isEmpty()

    override fun iterator(): Iterator<Any?> {
        return section.keys.iterator()
    }

    /**
     * Get the unmodifiable unordered list of keys.
     *
     * @return the set of keys
     */
    val keys: Collection<String>
        get() = Collections.unmodifiableSet(section.keys)

    /**
     * Returns {@code true} if the IniSection contains any values for the specified key.
     *
     * @param key key to search for in IniSection
     * @return {@code true} if the key exists
     */
    fun containsKey(key: String?): Boolean {
        return key != null && section.containsKey(key)
    }

    /**
     * Returns {@code true} if the IniSection contains the specified value for any key.
     *
     * @param value value to search for in IniSection
     * @return {@code true} if the value exists.
     */
    fun containsValue(value: String?): Boolean {
        for (collection in section.values) {
            if (collection.contains(value)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns {@code true} if the IniSection contains the specified value for the given key.
     *
     * @param key the key for the section
     * @param value value to search for in IniSection
     * @return {@code true} if the value exists.
     */
    fun containsValue(key: String?, value: String?): Boolean {
        val values: Collection<String>? = valuesOf(key)
        return values != null && values.contains(value)
    }

    /**
     * Add a value for the key. Duplicate values are not allowed.
     *
     * @param key the key for the section
     * @param value the value for the key
     * @return whether the value was added or is already present.
     */
    fun add(key: String?, value: String?): Boolean {
        if (!allowed(key, value)) {
            return false
        }

        val values = getOrCreateValues(key!!)
        if (values.contains(value)) {
            warnings!!.append("Duplicate value: ").append(key).append(" = ").append(value).append('\n')
            return true
        }
        return values.add(value!!)
    }

    /**
     * Get the unmodifiable collection of values of a key.
     * The collection has insertion order.
     * Note many keys only have one value.
     * A key that has no values returns null.
     *
     * @param key the key
     * @return the keyed values or null if the key doesn't exist
     */
    fun getValues(key: String?): Collection<String>? {
        if (containsKey(key)) {
            return Collections.unmodifiableCollection(valuesOf(key))
        }
        return null
    }

    /**
     * Get the value for the key specified by the index.
     *
     * @param key the key
     * @param index the index
     * @return the value at the specified index
     * @throws ArrayIndexOutOfBoundsException when the index is out of bounds
     */
    fun get(key: String?, index: Int): String? {
        val values = valuesOf(key)
        return if (values == null) null else values[index]
    }

    /**
     * Get the first value for the key.
     *
     * @param key the key
     * @return the value at the specified index or null
     */
    fun get(key: String?): String? {
        val values = valuesOf(key)
        return if (values == null) null else values[0]
    }

    fun get(key: String?, defaultValue: String?): String? {
        val values = valuesOf(key)
        return if (values == null) defaultValue else values[0]
    }

    /**
     * Remove the value if present.
     * If it were the last value for the key, the key is removed.
     *
     * @param key the key for the section
     * @param value the value for the key
     * @return whether the value was present and removed
     */
    fun remove(key: String?, value: String?): Boolean {
        val values = valuesOf(key) ?: return false

        val changed = values.remove(value)
        if (changed) {
            if (values.isEmpty()) {
                section.remove(key)
            }
        }

        return changed
    }

    /**
     * Remove the key and all its values, if present.
     *
     * @param key the key for the section
     * @return whether the key was present and removed
     */
    fun remove(key: String?): Boolean {
        valuesOf(key) ?: return false
        section.remove(key)
        return true
    }

    /**
     * Replace the value(s) for the key with a new value.
     *
     * @param key the key for the section
     * @param value the value for the key
     * @return whether the replace happened
     */
    fun replace(key: String?, value: String?): Boolean {
        if (!allowed(key, value)) {
            return false
        }

        val values = getOrCreateValues(key!!)
        values.clear()
        return values.add(value!!)
    }

    /**
     * Load the INI from an InputStream using the given encoding.
     *
     * @param is the InputStream to read from
     * @param encoding the encoding of the file
     * @throws IOException
     */
    @Throws(IOException::class)
    fun load(`is`: InputStream?, encoding: String?) {
        load(`is`, encoding, null)
    }

    /**
     * Load the INI from an InputStream using the given encoding. Filter keys as specified.
     *
     * @param is the InputStream to read from
     * @param encoding the encoding of the file
     * @param filter the filter, possibly null, for the desired keys
     * @throws IOException
     */
    @Throws(IOException::class)
    fun load(`is`: InputStream?, encoding: String?, filter: Filter<String>?) {
        var reader: Reader? = null
        try {
            reader = InputStreamReader(`is`, encoding)
            doLoad(reader, filter)
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
        load(file, encoding, null)
    }

    /**
     * Load the INI from a file using the given encoding. Filter keys as specified.
     *
     * @param file the file to load
     * @param encoding the encoding of the file
     * @param filter the filter, possibly null, for the desired keys
     * @throws IOException
     */
    @Throws(IOException::class)
    fun load(file: File?, encoding: String?, filter: Filter<String>?) {
        this.configFile = file
        this.charset = encoding
        var input: InputStream? = null
        try {
            input = FileInputStream(file)
            load(input, encoding, filter)
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
     * @param encoding the character encoding of this INI
     * @throws IOException
     */
    @Throws(IOException::class)
    fun load(buffer: ByteArray?, encoding: String?) {
        load(buffer, encoding, null)
    }

    /**
     * Load the conf from a buffer. Filter keys as specified.
     * This is used to load conf entries from the mods.d.tar.gz file.
     *
     * @param buffer the buffer to load
     * @param encoding the character encoding of this INI
     * @param filter the filter, possibly null, for the desired keys
     * @throws IOException
     */
    @Throws(IOException::class)
    fun load(buffer: ByteArray?, encoding: String?, filter: Filter<String>?) {
        var input: InputStream? = null
        try {
            input = ByteArrayInputStream(buffer)
            load(input, encoding, filter)
        } finally {
            if (input != null) {
                input.close()
                input = null
            }
        }
    }

    /**
     * Save this INI to the file from which it was loaded.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun save() {
        jswordAssert(configFile != null) { "" }
        jswordAssert(charset != null) { "" }
        if (configFile != null && charset != null) {
            save(configFile, charset)
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
        this.configFile = file
        this.charset = encoding
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
     * Output this section using the print writer. The section ends with a blank line.
     * The items are output in insertion order.
     *
     * @param out the output stream
     */
    fun save(out: Writer?) {
        var writer: PrintWriter? = null
        if (out is PrintWriter) {
            writer = out
        } else {
            writer = PrintWriter(out)
        }

        writer.print("[")
        writer.print(name)
        writer.print("]")
        writer.println()

        var first = true
        val keys = section.keys.iterator()
        while (keys.hasNext()) {
            val key = keys.next()
            val values = section[key]!!
            val iter = values.iterator()
            var value: String
            while (iter.hasNext()) {
                if (!first) {
                    writer.println()
                    first = false
                }
                value = iter.next()
                writer.print(key)
                writer.print(" = ")
                writer.print(format(value))
                writer.println()
            }
        }

        writer.flush()
    }

    /**
     * Obtain a report of issues with this IniSection. It only reports once per load.
     *
     * @return the report with one issue per line or an empty string if there are no issues
     */
    fun report(): String? {
        val str = report
        report = ""
        return str
    }

    /**
     * A helper to format the output of the content as expected
     * @param value the value to be formatted
     * @return the transformed value
     */
    private fun format(value: String): String {
        // Find continuations and replace newlines with a ' \'
        // Indenting the next line
        // Note: if the quoting of values is allowed this may need to be revisited.
        return value.replace("\n", " \\\n\t")
    }

    /**
     * The values of a key, or null when the key is null (a HashMap lookup of null finds nothing here) or unknown.
     */
    private fun valuesOf(key: String?): MutableList<String>? {
        return if (key == null) null else section[key]
    }

    private fun getOrCreateValues(key: String): MutableList<String> {
        var values = section[key]
        if (values == null) {
            values = ArrayList()
            section[key] = values
        }
        return values
    }

    @Throws(IOException::class)
    private fun doLoad(`in`: Reader, filter: Filter<String>?) {
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

            while (true) {
                val line = advance(bin) ?: break

                if (isSectionLine(line)) {
                    // The conf file contains a leading line of the form [KJV]
                    // This is the acronym by which Sword refers to it.
                    name = line.substring(1, line.length - 1)
                    continue
                }

                // Is this a key line?
                val splitPos = getSplitPos(line)
                if (splitPos < 0) {
                    warnings!!.append("Skipping: Expected to see '=' in: ").append(line).append('\n')
                    continue
                }

                val key = line.substring(0, splitPos).trim { it <= ' ' }
                val value = more(bin, line.substring(splitPos + 1).trim { it <= ' ' })
                if (filter == null || filter.test(key)) {
                    add(key, value)
                }
            }
            report = warnings!!.toString()
            warnings!!.setLength(0)
            warnings!!.trimToSize()
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
            if (trimmed.startsWith("\uFEFF")) {
                trimmed = trimmed.substring(1)
            }
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
                // Is this new line a potential key line?
                // It cannot both continue the prior
                // and also be a key line.
                val splitPos = getSplitPos(line!!)
                if (splitPos >= 0) {
                    warnings!!.append("Possible trailing continuation on previous line. Found: ").append(line).append('\n')
                }
            }
        } while (moreCowBell && line != null)
        return buf.toString()
    }

    private fun allowed(key: String?, value: String?): Boolean {
        if (key == null || key.length == 0 || value == null) {
            if (key == null) {
                warnings!!.append("Null keys not allowed: ").append(" = ").append(value).append('\n')
            } else if (key.length == 0) {
                warnings!!.append("Empty keys not allowed: ").append(" = ").append(value).append('\n')
            }
            if (value == null) {
                warnings!!.append("Null values are not allowed: ").append(key).append(" = ").append('\n')
            }
            return false
        }
        return true
    }

    /**
     * A map of values by key names.
     */
    private var section: MutableMap<String, MutableList<String>>

    private var configFile: File? = null

    private var charset: String? = null

    /**
     * Null in an IniSection made by the copy constructor, as in Java.
     */
    private var warnings: StringBuilder? = null

    private var report: String? = null

    companion object {
        /**
         * More: Is there more following this line
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
        private const val MAX_BUFF_SIZE = 2 * 1024
    }
}
