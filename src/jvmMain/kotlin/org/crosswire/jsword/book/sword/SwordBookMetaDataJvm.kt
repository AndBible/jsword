package org.crosswire.jsword.book.sword

import java.io.File
import java.io.Writer
import java.io.PrintWriter
import java.io.IOException
import org.crosswire.common.util.KUri
import org.crosswire.common.util.IniSection
import org.crosswire.jsword.book.BookException
import okio.Path.Companion.toPath

/** File and Writer API boundary for metadata. */
object SwordBookMetaDataJvm {
    @JvmStatic @Throws(IOException::class, BookException::class)
    fun create(file: File, root: KUri?): SwordBookMetaData = SwordBookMetaData(file.path.toPath(), root)
    @JvmStatic fun getConfigFile(metadata: SwordBookMetaData): File? = metadata.configFile?.toFile()
    @JvmStatic
        fun normalize(out: Writer?, config: IniSection, order: Array<String>) {
            var writer: PrintWriter? = null
            if (out is PrintWriter) {
                writer = out
            } else {
                writer = PrintWriter(out)
            }
            val copy = IniSection(config)
            // History is a special case
            SwordBookMetaData.adjustHistory(copy)

            val knownKeys: MutableList<String> = ArrayList(copy.keys)
            writer.print("[")
            writer.print(copy.name)
            writer.print("]")
            writer.println()

            // Get the keys out in the specified order
            for (key in order) {
                knownKeys.remove(key)
                if (!copy.containsKey(key)) {
                    continue
                }
                val values = copy.getValues(key)!!
                val iter = values.iterator()
                var value: String
                while (iter.hasNext()) {
                    value = iter.next()
                    var newKey = key
                    // When key is History, it needs to be reversed
                    if (SwordBookMetaData.KEY_HISTORY.equals(key, ignoreCase = true)) {
                        val pos = value.indexOf(' ')
                        newKey += "_" + value.substring(0, pos)
                        value = value.substring(pos + 1)
                    }
                    writer.print(newKey)
                    writer.print("=")
                    writer.print(Regex("\n").replace(value, " \\\\\n"))
                    writer.println()
                }
            }

            val keys = knownKeys.iterator()
            while (keys.hasNext()) {
                val key = keys.next()
                val values = copy.getValues(key)!!
                val iter = values.iterator()
                var value: String
                while (iter.hasNext()) {
                    value = iter.next()
                    writer.print(key)
                    writer.print("=")
                    writer.print(Regex("\n").replace(value, " \\\\\n"))
                    writer.println()
                }
            }

            writer.flush()
        }

}
