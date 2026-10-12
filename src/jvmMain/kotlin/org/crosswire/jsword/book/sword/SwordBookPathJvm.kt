package org.crosswire.jsword.book.sword

import java.io.File
import org.crosswire.jsword.book.BookException

/** JVM File signatures delegate to the shared Path state and preserve defensive copies. */
object SwordBookPathJvm {
    @JvmStatic @Throws(BookException::class)
    fun setAugmentPath(dirs: Array<File>?) = SwordBookPath.setAugmentLocations(dirs?.map { StorageLocation(it.path) }?.toTypedArray())
    @JvmStatic fun getAugmentPath(): Array<File> = SwordBookPath.getAugmentLocations().map { File(it.spelling) }.toTypedArray()
    @JvmStatic fun getSwordPath(): Array<File> = SwordBookPath.getSwordLocations().map { File(it.spelling) }.toTypedArray()
    @JvmStatic fun getBookList(dir: File): Array<String>? =
        if (dir.path.isEmpty()) null else SwordBookPath.getBookNames(StorageLocation(dir.path))
    @JvmStatic fun getSwordDownloadDir(): File = File(SwordBookPath.getSwordDownloadLocation().spelling)
    @JvmStatic fun getDownloadDir(): File? = SwordBookPath.getDownloadLocation()?.let { File(it.spelling) }
    @JvmStatic fun setDownloadDir(dir: File) {
        // Empty File paths are ignored; okio represents them as dot, so check before conversion.
        if (dir.path.isNotEmpty()) SwordBookPath.setDownloadLocation(StorageLocation(dir.path))
    }
}
