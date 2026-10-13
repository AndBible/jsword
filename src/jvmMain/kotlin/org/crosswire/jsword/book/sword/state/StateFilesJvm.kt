package org.crosswire.jsword.book.sword.state

import java.io.File
import org.crosswire.jsword.book.sword.StorageLocation
import org.crosswire.jsword.versification.Testament

/** File-facing adapters for legacy JVM consumers; state IO retains lexical locations. */
object RawBackendStateJvm {
    @JvmStatic fun getTextFile(state: RawBackendState, testament: Testament?): File =
        File(state.getTextLocation(testament).spelling)
    @JvmStatic fun getIdxFile(state: RawBackendState, testament: Testament?): File =
        File(state.getIdxLocation(testament).spelling)
}

object RawLDBackendStateJvm {
    @JvmStatic fun getIdxFile(state: RawLDBackendState): File? =
        state.getIdxLocation()?.let { File(it.spelling) }
}

object RawFileBackendStateJvm {
    @JvmStatic fun getIncfile(state: RawFileBackendState): File? =
        state.getIncLocation()?.let { File(it.spelling) }
    @JvmStatic fun setIncfile(state: RawFileBackendState, file: File?) {
        state.setIncLocation(file?.let { StorageLocation(it.path) })
    }
}
