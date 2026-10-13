package org.crosswire.common.util

import okio.Path.Companion.toPath

/** K4 class-loader boundary; becomes expect/actual when CWProject moves in Task 16. */
internal actual fun setResourceHome(dirs: Array<java.net.URI?>) = CWClassLoader.setHome(dirs)

internal actual fun systemProperty(name: String?): String? = System.getProperty(name)

internal actual fun platformOsName(): String? = systemProperty("os.name")

internal actual fun platformDisplayLanguage(code: String): String = java.util.Locale(code).displayLanguage

internal actual fun setUriCachePath(path: okio.Path?) = NetUtilJvm.setURICacheDir(path?.toFile())

internal actual fun pathUri(path: okio.Path): KUri = path.toFile().toURI()

/** Preserve File(URI)'s validation rather than silently accepting non-file URI paths. */
internal actual fun validatedFileUriPath(uri: KUri): okio.Path = java.io.File(uri).path.toPath()
