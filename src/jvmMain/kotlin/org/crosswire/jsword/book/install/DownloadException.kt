package org.crosswire.jsword.book.install

import java.net.URI

open class DownloadException : InstallException {
    @JvmField
    var statusCode: Int

    @JvmField
    var uri: URI?

    constructor(uri: URI?, statusCode: Int) : super("Downloading $uri failed with status code $statusCode") {
        this.statusCode = statusCode
        this.uri = uri
    }

    constructor(uri: URI?, statusCode: Int, cause: Throwable?) : super("Downloading $uri failed with status code $statusCode", cause) {
        this.statusCode = statusCode
        this.uri = uri
    }
}
