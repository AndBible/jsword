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
internal actual fun pathUri(path: Path): KUri = KUri("file", null, path.toString(), null)
internal actual fun validatedFileUriPath(uri: KUri): Path {
    require(uri.isAbsolute() && uri.getScheme() == "file") { "URI is not an absolute file URI" }
    require(uri.getRawPath() != null && uri.getPath()!!.startsWith('/')) { "URI is not hierarchical" }
    require(uri.getHost() == null && uri.getUserInfo() == null && uri.getPort() == -1) { "URI has an authority" }
    require(uri.getQuery() == null && uri.getFragment() == null) { "URI has a query or fragment" }
    return uri.getPath()!!.toPath()
}
