package org.crosswire.common.util

import java.io.File
import java.io.InputStream
import java.io.PrintWriter
import java.io.Writer
import okio.Path.Companion.toPath

/** JVM-only stream and file entry points for the common-ready INI section. */
object IniSectionJvm {
    @JvmStatic
    @JvmOverloads
    @Throws(java.io.IOException::class)
    fun load(section: IniSection, input: InputStream?, encoding: String?, filter: Filter<String>? = null) {
        input!!.use { section.load(it.readBytes(), encoding, filter) }
    }

    @JvmStatic
    @JvmOverloads
    @Throws(java.io.IOException::class)
    fun load(section: IniSection, file: File?, encoding: String?, filter: Filter<String>? = null) =
        section.load(file!!.path.toPath(), encoding, filter)

    @JvmStatic
    @Throws(java.io.IOException::class)
    fun save(section: IniSection, file: File?, encoding: String?) = section.save(file!!.path.toPath(), encoding)

    @JvmStatic
    fun save(section: IniSection, output: Writer?) {
        val writer = if (output is PrintWriter) output else PrintWriter(output)
        section.save(writer as Appendable)
        writer.flush()
    }
}

// PrintWriter uses the cached platform separator, not a subsequently changed property.
internal var iniLineSeparator: String = System.lineSeparator()
internal fun platformIniLineSeparator(): String = iniLineSeparator

internal fun unmodifiableIniKeys(keys: Set<String>): Collection<String> = java.util.Collections.unmodifiableSet(keys)
internal fun unmodifiableIniValues(values: Collection<String>): Collection<String> = java.util.Collections.unmodifiableCollection(values)
