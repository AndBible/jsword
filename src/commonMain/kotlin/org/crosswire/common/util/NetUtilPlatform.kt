package org.crosswire.common.util

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun netUriSyntaxFailure(error: Throwable): Boolean
internal expect fun netCanRead(uri: KUri): Boolean
internal expect fun netCanWrite(uri: KUri): Boolean
internal expect fun netCheckResource(uri: KUri): Unit
internal expect fun netUrlSize(uri: KUri): Int
/** The size of an https resource through the host transport; only the network part is suspend. */
internal expect suspend fun netHttpsSize(uri: KUri): Int
/** The last-modified time of an https resource through the host transport. */
internal expect suspend fun netHttpsLastModified(uri: KUri): Long
internal expect fun netLoadProperties(uri: KUri): PropertyMap
internal expect fun netStoreProperties(properties: PropertyMap?, uri: KUri, title: String?): Unit
internal expect fun netTemporaryUri(prefix: String, suffix: String?): KUri
internal expect fun netListByIndexFile(index: KUri, filter: URIFilter?): Array<String>
internal expect fun netUrlLastModified(uri: KUri): Long
internal expect class NetMalformedURLException(message: String?) : okio.IOException {
    constructor()
}
