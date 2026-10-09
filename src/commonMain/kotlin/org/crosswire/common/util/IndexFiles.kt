package org.crosswire.common.util

/** Parser for index files (one entry per line), shared by the JVM and iOS. */
object IndexFiles {
    /** The name of the index file itself; never listed as an entry. */
    const val INDEX_FILE: String = "index.txt"

    /**
     * Parse the text of an index file. Each line is pre-processed: comments (# to end of line) are
     * dropped, the line is trimmed (everything up to U+0020, like `java.lang.String.trim`), and blank
     * lines are ignored. An entry is also dropped when it is the index file's own name or when
     * [filter] rejects it.
     */
    fun parse(text: String, filter: ((String) -> Boolean)? = null): List<String> {
        val list = ArrayList<String>()
        for (line in text.lines()) {
            val hash = line.indexOf('#')
            val name = (if (hash >= 0) line.substring(0, hash) else line).trim { it <= ' ' }
            if (name.isNotEmpty() && name != INDEX_FILE && (filter == null || filter(name))) list.add(name)
        }
        return list
    }
}
