package org.crosswire.common.util

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Reader
import java.io.Writer

/** JVM-only stream and file entry points for the common-ready INI section. */
object IniSectionJvm {
    @JvmStatic
    @JvmOverloads
    @Throws(java.io.IOException::class)
    fun load(section: IniSection, input: InputStream?, encoding: String?, filter: Filter<String>? = null) {
        var reader: Reader? = null
        try {
            reader = InputStreamReader(input, encoding)
            val buffered = BufferedReader(reader, 2 * 1024)
            section.loadLines({ buffered.readLine() }, { buffered.close() }, filter)
        } finally {
            // Legacy finally-close replaces a body failure; failed construction never owns input.
            reader?.close()
        }
    }

    @JvmStatic
    @JvmOverloads
    @Throws(java.io.IOException::class)
    fun load(section: IniSection, file: File?, encoding: String?, filter: Filter<String>? = null) {
        section.rememberSave(encoding, if (file == null) null else { { save(section, file, encoding) } })
        var input: InputStream? = null
        try {
            input = FileInputStream(file)
            load(section, input, encoding, filter)
        } finally {
            input?.close()
        }
    }

    @JvmStatic
    @Throws(java.io.IOException::class)
    fun save(section: IniSection, file: File?, encoding: String?) {
        section.rememberSave(encoding, if (file == null) null else { { save(section, file, encoding) } })
        var output: Writer? = null
        try {
            // Keep exact File identity and open/truncate before charset construction.
            output = OutputStreamWriter(FileOutputStream(file), encoding)
            save(section, output)
        } finally {
            output?.close()
        }
    }

    @JvmStatic
    fun save(section: IniSection, output: Writer?) {
        val writer = if (output is PrintWriter) output else PrintWriter(output)
        section.save(writer as Appendable)
        writer.flush()
    }
}

// PrintWriter uses the cached platform separator, not a subsequently changed property.
internal var iniLineSeparator: String = System.lineSeparator()
internal actual fun platformIniLineSeparator(): String = iniLineSeparator

internal actual fun unmodifiableIniKeys(keys: Set<String>): Collection<String> = java.util.Collections.unmodifiableSet(keys)
internal actual fun unmodifiableIniValues(values: Collection<String>): Collection<String> = java.util.Collections.unmodifiableCollection(values)
