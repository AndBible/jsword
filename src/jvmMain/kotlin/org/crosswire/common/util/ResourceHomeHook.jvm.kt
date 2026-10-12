package org.crosswire.common.util

/** K4 class-loader boundary; becomes expect/actual when CWProject moves in Task 16. */
internal fun setResourceHome(dirs: Array<java.net.URI?>) = CWClassLoader.setHome(dirs)

internal fun systemProperty(name: String?): String? = System.getProperty(name)

internal fun platformOsName(): String? = systemProperty("os.name")

internal fun platformDisplayLanguage(code: String): String = java.util.Locale(code).displayLanguage

internal fun setUriCachePath(path: okio.Path?) = NetUtil.setURICacheDir(path?.toFile())

internal fun pathUri(path: okio.Path): KUri = path.toFile().toURI()
