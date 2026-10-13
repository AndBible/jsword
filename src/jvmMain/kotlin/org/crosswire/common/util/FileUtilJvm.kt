package org.crosswire.common.util

import java.io.File
import okio.Path.Companion.toOkioPath

object FileUtilJvm {
        /**
         * Deletes a file or a directory and all of its contents
         *
         * @param file
         *            or directory to delete
         * @return the list of files that could not be deleted
         */
        @JvmStatic
        fun delete(file: File): MutableList<File> {
            return KFiles.deleteRecursively(file.toOkioPath()).map { it.toFile() }.toMutableList()
        }

}
