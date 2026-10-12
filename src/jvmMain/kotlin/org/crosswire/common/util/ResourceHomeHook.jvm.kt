package org.crosswire.common.util

import okio.Path.Companion.toPath

/** K4 class-loader boundary; becomes expect/actual when CWProject moves in Task 16. */
internal fun setResourceHome(dirs: Array<java.net.URI?>) = CWClassLoader.setHome(dirs)

internal fun systemProperty(name: String?): String? = System.getProperty(name)

internal fun platformOsName(): String? = systemProperty("os.name")

internal fun platformDisplayLanguage(code: String): String = java.util.Locale(code).displayLanguage

internal fun setUriCachePath(path: okio.Path?) = NetUtil.setURICacheDir(path?.toFile())

internal fun pathUri(path: okio.Path): KUri = path.toFile().toURI()

/** Preserve File(URI)'s validation rather than silently accepting non-file URI paths. */
internal fun validatedFileUriPath(uri: KUri): okio.Path = java.io.File(uri).path.toPath()
