package org.crosswire.jsword.book.sword

import java.io.File
import java.io.FilenameFilter
import okio.Path.Companion.toPath
import org.crosswire.jsword.book.BookException

/** JVM File signatures delegate to the shared Path state and preserve defensive copies. */
object SwordBookPathJvm {
    @JvmStatic @Throws(BookException::class)
    fun setAugmentPath(dirs: Array<File>?) = SwordBookPath.setAugmentPath(dirs?.map { it.path.toPath() }?.toTypedArray())
    @JvmStatic fun getAugmentPath(): Array<File> = SwordBookPath.getAugmentPath().map { it.toFile() }.toTypedArray()
    @JvmStatic fun getSwordPath(): Array<File> = SwordBookPath.getSwordPath().map { it.toFile() }.toTypedArray()
    @JvmStatic fun getBookList(dir: File): Array<String>? =
        if (dir.path.isEmpty()) null else SwordBookPath.getBookList(dir.path.toPath())
    @JvmStatic fun getSwordDownloadDir(): File = SwordBookPath.getSwordDownloadDir().toFile()
    @JvmStatic fun getDownloadDir(): File? = SwordBookPath.getDownloadDir()?.toFile()
    @JvmStatic fun setDownloadDir(dir: File) {
        // Empty File paths are ignored; okio represents them as dot, so check before conversion.
        if (dir.path.isNotEmpty()) SwordBookPath.setDownloadDir(dir.path.toPath())
    }
    internal class CustomFilenameFilter : FilenameFilter {
        override fun accept(parent: File?, name: String?): Boolean =
            !name!!.startsWith("globals.") && name.endsWith(SwordConstants.EXTENSION_CONF)
    }
}
