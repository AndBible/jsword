package org.crosswire.common.util

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun setResourceHome(dirs: Array<KUri?>): Unit
internal expect fun systemProperty(name: String?): String?
internal expect fun platformOsName(): String?
internal expect fun platformDisplayLanguage(code: String): String
internal expect fun setUriCachePath(path: okio.Path?): Unit
internal expect fun pathUri(path: okio.Path): KUri
internal expect fun validatedFileUriPath(uri: KUri): okio.Path
