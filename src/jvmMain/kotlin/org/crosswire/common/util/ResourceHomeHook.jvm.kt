package org.crosswire.common.util

/** K4 class-loader boundary; becomes expect/actual when CWProject moves in Task 16. */
internal fun setResourceHome(dirs: Array<java.net.URI?>) = CWClassLoader.setHome(dirs)
