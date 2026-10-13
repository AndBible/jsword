package org.crosswire.common.util

import platform.Foundation.currentLocale

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSLocale

private var uriCachePath: Path? = null

internal actual fun setResourceHome(dirs: Array<KUri?>) {
    // Native resources are embedded rather than loaded through a class loader.
}
internal actual fun systemProperty(name: String?): String? {
    requireNotNull(name)
    require(name.isNotEmpty())
    return when (name) {
        "user.home" -> NSHomeDirectory()
        "os.name" -> "iOS"
        else -> null
    }
}
internal actual fun platformOsName(): String? = "iOS"
internal actual fun platformDisplayLanguage(code: String): String =
    NSLocale.currentLocale.displayNameForKey(platform.Foundation.NSLocaleLanguageCode, code) ?: code
internal actual fun setUriCachePath(path: Path?) { uriCachePath = path }
internal fun nativeUriCachePath(): Path? = uriCachePath
internal actual fun pathUri(path: Path): KUri {
    var absolute = NativeLexicalFiles.absolute(path.toString())
    if (!absolute.endsWith('/') && NativeLexicalFiles.directory(absolute)) absolute += "/"
    return KUri("file", null, absolute, null)
}
internal actual fun validatedFileUriPath(uri: KUri): Path = nativeFileUriSpelling(uri).toPath()

/** Validate before projecting to Path: registry authorities need not have a host. */
internal fun nativeFileUriSpelling(uri: KUri): String {
    require(uri.isAbsolute() && uri.getScheme() == "file") { "URI is not an absolute file URI" }
    require(uri.getRawPath() != null && uri.getPath()!!.startsWith('/')) { "URI is not hierarchical" }
    require(uri.rawAuthority().isNullOrEmpty()) { "URI has an authority" }
    require(uri.getQuery() == null && uri.getFragment() == null) { "URI has a query or fragment" }
    return NativeLexicalFiles.spelling(uri.getPath()!!)
}
