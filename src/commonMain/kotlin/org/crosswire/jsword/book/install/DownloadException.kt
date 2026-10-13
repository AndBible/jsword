package org.crosswire.jsword.book.install

import kotlin.jvm.JvmField

import org.crosswire.common.util.KUri

open class DownloadException : InstallException {
    @JvmField
    var statusCode: Int

    @JvmField
    var uri: KUri?

    constructor(uri: KUri?, statusCode: Int) : super("Downloading $uri failed with status code $statusCode") {
        this.statusCode = statusCode
        this.uri = uri
    }

    constructor(uri: KUri?, statusCode: Int, cause: Throwable?) : super("Downloading $uri failed with status code $statusCode", cause) {
        this.statusCode = statusCode
        this.uri = uri
    }
}
