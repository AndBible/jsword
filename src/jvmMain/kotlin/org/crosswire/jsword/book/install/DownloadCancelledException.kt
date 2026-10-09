package org.crosswire.jsword.book.install

import java.net.URI

open class DownloadCancelledException(uri: URI?) : InstallException("Downloading $uri was cancelled")
